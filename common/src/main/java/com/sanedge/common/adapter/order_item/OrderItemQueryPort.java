package com.sanedge.common.adapter.order_item;

import java.util.List;

import com.sanedge.common.adapter.model.OrderItem;

import io.smallrye.mutiny.Uni;

/**
 * Port for the order_item query service, replacing direct
 * {@code @GrpcClient("order_item")} usage in consumer services.
 */
public interface OrderItemQueryPort {

    Uni<List<OrderItem>> findOrderItemByOrder(int orderId);
}
