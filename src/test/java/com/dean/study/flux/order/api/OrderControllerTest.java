package com.dean.study.flux.order.api;

import static org.junit.jupiter.api.Assertions.*;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 통합 테스트:
 * - MockWebServer로 item 서비스를 대체
 * - application.yml의 app.item-base-url 을 동적으로 MockWebServer URL로 교체
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class OrderControllerTest {

    private static MockWebServer mockItemServer;

    @Autowired
    WebTestClient webTestClient;

    @BeforeAll
    static void startMockServer() throws Exception {
        mockItemServer = new MockWebServer();
        mockItemServer.start();

        // 기본 Dispatcher: 경로에 따라 응답 분기
        mockItemServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest req) {
                String path = req.getPath();
                // 정상 케이스: /items/{id}
                if (path != null && path.matches("^/items/\\d+$")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("""
                                {"id":1,"name":"Coffee-1","price":3500}
                                """);
                }
                // 느린 원격 호출 시나리오: /demo/db/sleep?sec=...
                if (path != null && path.startsWith("/demo/db/sleep")) {
                    // Resilience4j TimeLimiter(2s)를 넘기도록 고의 지연
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "text/plain")
                            .setBody("ok")
                            .setBodyDelay(3, TimeUnit.SECONDS); // 3초 지연
                }
                // 미정의 경로 → 404
                return new MockResponse().setResponseCode(404);
            }
        });
    }

    @AfterAll
    static void shutdown() throws Exception {
        if (mockItemServer != null) {
            mockItemServer.shutdown();
        }
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        // app.item-base-url 을 MockWebServer 로 동적 변경
        String baseUrl = mockItemServer.url("/").toString().replaceAll("/$", "");
        registry.add("app.item-base-url", () -> baseUrl);
    }

    @Test
    void order_shouldReturn_CREATED_whenItemServiceOk() {
        webTestClient.get()
                .uri("/orders/{itemId}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("CREATED")
                .jsonPath("$.item.id").isEqualTo(1)
                .jsonPath("$.item.name").isEqualTo("Coffee-1")
                .jsonPath("$.item.price").isEqualTo(3500)
                .jsonPath("$.meta.ts").exists();
    }

    @Test
    void orderSlow_shouldFallback_whenTimeoutOccurs() {
        var body = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/orders/slow").queryParam("sec", 5).build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("FALLBACK")
                .jsonPath("$.meta.reason").exists()
                .returnResult()
                .getResponseBody();

        // 참고: 이유 문자열은 환경에 따라 TimeoutException / CallNotPermittedException 등이 될 수 있음
        assertThat(new String(body)).contains("FALLBACK");
    }
}