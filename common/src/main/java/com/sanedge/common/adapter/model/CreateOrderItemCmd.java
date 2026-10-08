package com.sanedge.common.adapter.model;

/**
 * Command to create an order item in the order_item service.
 */
public record CreateOrderItemCmd(
        int orderId,
        int productId,
        int quantity,
        int price) {
}
