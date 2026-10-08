package com.sanedge.common.adapter.model;

import java.time.Instant;

/**
 * Domain view of an order owned by the order service.
 */
public record Order(
        int id,
        int merchantId,
        int cashierId,
        int totalPrice,
        Instant createdAt,
        Instant updatedAt) {
}
