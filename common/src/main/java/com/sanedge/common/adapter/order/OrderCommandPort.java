package com.sanedge.common.adapter.order;

import io.smallrye.mutiny.Uni;

/**
 * Port for the order command service, replacing direct
 * {@code @GrpcClient("order")} usage in consumer services.
 */
public interface OrderCommandPort {

    Uni<Void> updateOrderTotalPrice(int orderId, int totalPrice);
}
