package com.dean.study.flux.order.scs;

import com.dean.study.flux.order.dto.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScsOrderPublisher {
    private final StreamBridge streamBridge;

    public boolean send(OrderCreatedEvent evt) {
        log.debug("[SCS ORDER Kafka] send {}", evt);
        return streamBridge.send("order-out-0", MessageBuilder.withPayload(evt)
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                // 카프카 헤더 키를 추가하면 순서보장도 가능하다. -> 동일 key는 동일 파티션에만
                .setHeader(KafkaHeaders.KEY, String.valueOf(evt.orderId())) // Kafka producer Header에 파티션 Key 설정 -> 설정하지 않으면 라운드 로빈 방식으로 파티션 선택하지만 순서가 깨진다.
                .build());
    }

    public Mono<Map<String, Object>> sendBadSCS(String binding) {

        // JSON 깨진 문자열 (아예 유효하지 않게)
        String badJson = "{not-json}";

        // content-type은 JSON으로 위장
        boolean ok = streamBridge.send(binding,
                MessageBuilder.withPayload(badJson)
                        .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                        .build());

        return Mono.just(Map.of("status", ok ? "SENT" : "FAIL", "binding", binding));
    }
}