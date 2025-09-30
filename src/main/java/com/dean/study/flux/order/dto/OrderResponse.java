package com.dean.study.flux.order.dto;

import java.time.Instant;
import java.util.Map;

public record OrderResponse(
        String status,
        ItemDto item,
        Map<String, Object> meta
) {
    public static OrderResponse ofOk(ItemDto item) {
        return new OrderResponse("CREATED", item, Map.of("ts", Instant.now().toString()));
    }
    public static OrderResponse ofFallback(String reason) {
        return new OrderResponse("FALLBACK", null, Map.of("reason", reason, "ts", Instant.now().toString()));
    }
}
