package com.sanedge.common.adapter.order;

import com.sanedge.common.adapter.model.Order;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.order.Order.FindByIdOrderRequest;
import pb.order.Order.OrderResponse;
import pb.order.OrderCommand.UpdateOrderTotalPriceRequest;
import pb.order.OrderCommandService;
import pb.order.OrderQueryService;

@ApplicationScoped
public class OrderAdapter implements OrderQueryPort, OrderCommandPort {

    @GrpcClient("order")
    OrderQueryService query;

    @GrpcClient("order")
    OrderCommandService command;

    @Override
    public Uni<Order> findById(int orderId) {
        return query.findById(FindByIdOrderRequest.newBuilder().setId(orderId).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Order not found: " + orderId);
                    }
                    return toOrder(resp.getData());
                });
    }

    @Override
    public Uni<Void> updateOrderTotalPrice(int orderId, int totalPrice) {
        return command.updateOrderTotalPrice(UpdateOrderTotalPriceRequest.newBuilder()
                .setOrderId(orderId)
                .setTotalPrice(totalPrice)
                .build())
                .replaceWithVoid();
    }

    private static Order toOrder(OrderResponse o) {
        if (o == null) {
            return null;
        }
        return new Order(o.getId(), o.getMerchantId(), o.getCashierId(), o.getTotalPrice(),
                ProtoTime.parse(o.getCreatedAt()), ProtoTime.parse(o.getUpdatedAt()));
    }
}
