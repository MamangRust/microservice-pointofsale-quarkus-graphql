package com.sanedge.common.adapter.model;

import java.time.Instant;

/**
 * Domain view of an order item owned by the order_item service.
 */
public record OrderItem(
        int orderItemId,
        int orderId,
        int productId,
        int quantity,
        int price,
        Instant createdAt,
        Instant updatedAt) {
}
