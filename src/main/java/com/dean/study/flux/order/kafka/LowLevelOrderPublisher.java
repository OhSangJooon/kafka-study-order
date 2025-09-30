package com.dean.study.flux.order.kafka;

import com.dean.study.flux.order.dto.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class LowLevelOrderPublisher {
    private final KafkaTemplate<String, OrderCreatedEvent> orderKafkaTemplate;

    public Mono<Void> send(OrderCreatedEvent evt) {
        log.debug("[KafkaTemplate] send {}", evt);
        return Mono.fromFuture(orderKafkaTemplate.send("order-created-kafka", evt))
                .then();
    }
}