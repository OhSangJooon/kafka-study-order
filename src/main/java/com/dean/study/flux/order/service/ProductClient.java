package com.dean.study.flux.order.service;

import com.dean.study.flux.order.dto.ItemDto;
import com.dean.study.flux.order.dto.OrderResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductClient {
    private final WebClient itemWebClient;

    /**
     * 정상 케이스: item 서비스의 /items/{id} 호출
     */
    @TimeLimiter(name = "product")
    @Retry(name = "product")
    @CircuitBreaker(name = "product", fallbackMethod = "fallbackOrderByItemId")
    public Mono<OrderResponse> orderByItemId(Long itemId) {
        log.debug("orderByItemId called on thread={}", Thread.currentThread().getName());
        return itemWebClient
                .get()
                // 가장 단순한 오버로드로 명시
                .uri("/items/{id}", itemId)
                .retrieve()
                // 메서드 레퍼런스 대신 람다로 명시하면 타입추론 이슈 방지
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        Mono.error(new IllegalArgumentException("Client error: " + resp.statusCode())))
                .bodyToMono(ItemDto.class)
                .map(OrderResponse::ofOk);
    }

    /**
     * 느린 원격 호출 시나리오: item 서비스의 /demo/db/sleep?sec=... 호출
     * TimeLimiter(2s)로 타임아웃 유도 → Retry → 실패 누적 시 CircuitBreaker 열림
     */
    @TimeLimiter(name = "product", fallbackMethod = "fallbackSlowDemo")
    @Retry(name = "product")
    @CircuitBreaker(name = "product", fallbackMethod = "fallbackSlowDemo")
    public Mono<OrderResponse> orderWithSlowRemote(int sec) {
        log.debug("orderWithSlowRemote called on thread={}", Thread.currentThread().getName());
        return itemWebClient.get()
                .uri(uri -> uri.path("/demo/db/test/sleep").queryParam("sec", sec).build())
                .retrieve()
                .bodyToMono(String.class)
                .map(msg -> new ItemDto(null, "DEMO-SLOW", 0))
                .map(OrderResponse::ofOk)
                .onErrorResume(t -> fallbackSlowDemo(sec, t));

    }

    /**
     * 서버→서버 호출에 subscribeOn 적용:
     * - 구독 시점(외부 호출 준비/실행)부터 boundedElastic에서 진행
     * - 응답 콜백(doOnNext)도 boundedElastic에서 실행되는 걸 볼 수 있습니다.
     */
    public Mono<String> testSubscribeOnCall() {
        log.info("[subscribeOn] start thread={}", Thread.currentThread().getName());

        return itemWebClient.get()
                .uri("/items/{id}", 1)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSubscribe(s -> log.info("[subscribeOn] onSubscribe thread={}", Thread.currentThread().getName()))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(body -> log.info("[subscribeOn] onNext thread={}", Thread.currentThread().getName()))
                .map(body -> "[subscribeOn] done");
    }

    /**
     * 서버→서버 호출에 publishOn 적용:
     * - 구독/요청은 기본 Netty 이벤트루프에서 진행
     * - publishOn 이후 체인은 boundedElastic으로 전환되어 응답 처리(doOnNext)가 elastic에서 실행
     */
    public Mono<String> testPublishOnCall() {
        log.info("[publishOn] start thread={}", Thread.currentThread().getName());

        return itemWebClient.get()
                .uri("/items/{id}", 1)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSubscribe(s -> log.info("[publishOn] onSubscribe thread={}", Thread.currentThread().getName()))
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(body -> log.info("[publishOn] onNext thread={}", Thread.currentThread().getName()))
                .map(body -> "[publishOn] done");
    }

    /* ----- fallback (메서드 시그니처 주의: 마지막 인자로 Throwable) ----- */

    private Mono<OrderResponse> fallbackOrderByItemId(Long itemId, Throwable t) {
        log.warn("fallbackOrderByItemId itemId={} cause={}", itemId, t.toString());
        return Mono.just(OrderResponse.ofFallback("orderByItemId failed: " + t.getClass().getSimpleName()));
    }

    private Mono<OrderResponse> fallbackSlowDemo(int sec, Throwable t) {
        log.warn("fallbackSlowDemo sec={} cause={}", sec, t.toString());
        return Mono.just(OrderResponse.ofFallback("slow remote failed: " + t.getClass().getSimpleName()));
    }
}