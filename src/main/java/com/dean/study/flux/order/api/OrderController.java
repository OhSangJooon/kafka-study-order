package com.dean.study.flux.order.api;

import com.dean.study.flux.order.dto.OrderCreatedEvent;
import com.dean.study.flux.order.dto.OrderResponse;
import com.dean.study.flux.order.kafka.LowLevelOrderPublisher;
import com.dean.study.flux.order.scs.ScsOrderPublisher;
import com.dean.study.flux.order.service.ProductClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@Slf4j
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final ProductClient client;
    private final LowLevelOrderPublisher lowLevelOrderPublisher;
    private final ScsOrderPublisher scsOrderPublisher;

    @PostMapping("/{itemId}")
    public Mono<Map<String, Object>> create(@PathVariable Long itemId,
                                            @RequestParam(defaultValue = "scs") String mode,
                                            @RequestParam(required = false) Long orderId) {
        long oid = (orderId != null) ? orderId
                : ThreadLocalRandom.current().nextLong(1_000_000);

        OrderCreatedEvent evt = new OrderCreatedEvent(oid, itemId, "CREATED", Instant.now());

        Mono<Void> pub = "kafka".equalsIgnoreCase(mode) ? lowLevelOrderPublisher.send(evt)
                : Mono.fromCallable(() -> {
                    scsOrderPublisher.send(evt);
                    return null;
                })
                .then();

        long start = System.currentTimeMillis();
        return pub.thenReturn(Map.of(
                "mode", mode,
                "status", "OK",
                "tookMs", System.currentTimeMillis() - start,
                "event", evt
        ));
    }


    /** 정상: item 서비스의 /items/{id}를 호출 */
    @GetMapping("/test/{itemId}")
    public Mono<OrderResponse> order(@PathVariable Long itemId) {
        log.info("order start itemId={} thread={}", itemId, Thread.currentThread().getName());
        return client.orderByItemId(itemId)
                .doOnNext(resp -> log.info("order end status={} thread={}", resp.status(), Thread.currentThread().getName()));
    }

    /** 느린 원격 호출 시나리오: /demo/db/sleep?sec=... */
    @GetMapping("/slow")
    public Mono<OrderResponse> orderSlow(@RequestParam(defaultValue = "5") int sec) {
        log.info("orderSlow start sec={} thread={}", sec, Thread.currentThread().getName());
        return client.orderWithSlowRemote(sec)
                .doOnNext(resp -> log.info("orderSlow end status={} thread={}", resp.status(), Thread.currentThread().getName()));
    }

    @GetMapping("/test/subscribeOn-call")
    public Mono<String> testSubscribeOnCall() {
        log.info("HTTP /orders/test/subscribeOn-call thread={}", Thread.currentThread().getName());
        return client.testSubscribeOnCall()
                .doOnNext(v -> log.info("HTTP /orders/test/subscribeOn-call end thread={}", Thread.currentThread().getName()));
    }

    @GetMapping("/test/publishOn-call")
    public Mono<String> testPublishOnCall() {
        log.info("HTTP /orders/test/publishOn-call thread={}", Thread.currentThread().getName());
        return client.testPublishOnCall()
                .doOnNext(v -> log.info("HTTP /orders/test/publishOn-call end thread={}", Thread.currentThread().getName()));
    }
}