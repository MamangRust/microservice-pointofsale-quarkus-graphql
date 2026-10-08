package com.sanedge.common.adapter.model;

/**
 * Command to update an order item in the order_item service.
 */
public record UpdateOrderItemCmd(
        int orderItemId,
        int orderId,
        int productId,
        int quantity,
        int price) {
}
