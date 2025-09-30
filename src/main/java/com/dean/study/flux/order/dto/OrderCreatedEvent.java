package com.dean.study.flux.order.dto;

import java.time.Instant;

public record OrderCreatedEvent(Long orderId,
                                Long itemId,
                                String status,
                                Instant ts,
                                String eventId) {

}