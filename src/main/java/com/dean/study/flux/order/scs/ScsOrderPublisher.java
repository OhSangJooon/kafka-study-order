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

@Service
@RequiredArgsConstructor
@Slf4j
public class ScsOrderPublisher {
    private final StreamBridge streamBridge;

    public boolean send(OrderCreatedEvent evt) {
        log.debug("[SCS ORDER Kafka] send {}", evt);
        return streamBridge.send("order-out-0", MessageBuilder.withPayload(evt)
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                .setHeader(KafkaHeaders.KEY, String.valueOf(evt.orderId())) // Kafka producer Header에 파티션 Key 설정 -> 설정하지 않으면 라운드 로빈 방식으로 파티션 선택하지만 순서가 깨진다.
                .build());
    }
}