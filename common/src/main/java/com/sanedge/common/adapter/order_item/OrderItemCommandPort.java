package com.sanedge.common.adapter.order_item;

import com.sanedge.common.adapter.model.CreateOrderItemCmd;
import com.sanedge.common.adapter.model.OrderItem;
import com.sanedge.common.adapter.model.UpdateOrderItemCmd;

import io.smallrye.mutiny.Uni;

/**
 * Port for the order_item command service, replacing direct
 * {@code @GrpcClient("order_item")} usage in consumer services.
 */
public interface OrderItemCommandPort {

    Uni<OrderItem> createOrderItem(CreateOrderItemCmd cmd);

    Uni<OrderItem> updateOrderItem(UpdateOrderItemCmd cmd);
}
