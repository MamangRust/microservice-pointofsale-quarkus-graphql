package com.sanedge.common.adapter.model;

import java.time.Instant;

/**
 * Domain view of a cashier owned by the cashier service.
 */
public record Cashier(
        int id,
        int merchantId,
        String name,
        Instant createdAt,
        Instant updatedAt) {
}
