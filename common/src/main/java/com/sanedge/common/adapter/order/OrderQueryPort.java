package com.sanedge.common.adapter.order;

import com.sanedge.common.adapter.model.Order;

import io.smallrye.mutiny.Uni;

/**
 * Port for the order query service, replacing direct
 * {@code @GrpcClient("order")} usage in consumer services.
 */
public interface OrderQueryPort {

    Uni<Order> findById(int orderId);
}
