package com.sanedge.common.adapter.order_item;

import java.util.ArrayList;
import java.util.List;

import com.sanedge.common.adapter.model.CreateOrderItemCmd;
import com.sanedge.common.adapter.model.OrderItem;
import com.sanedge.common.adapter.model.UpdateOrderItemCmd;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.order_item.OrderItem.FindByIdOrderItemRequest;
import pb.order_item.OrderItem.OrderItemResponse;
import pb.order_item.OrderItemCommand.CreateOrderItemRequest;
import pb.order_item.OrderItemCommand.UpdateOrderItemRequest;
import pb.order_item.OrderItemCommandService;
import pb.order_item.OrderItemService;

@ApplicationScoped
public class OrderItemAdapter implements OrderItemQueryPort, OrderItemCommandPort {

    @GrpcClient("order_item")
    OrderItemService query;

    @GrpcClient("order_item")
    OrderItemCommandService command;

    @Override
    public Uni<List<OrderItem>> findOrderItemByOrder(int orderId) {
        return query.findOrderItemByOrder(FindByIdOrderItemRequest.newBuilder().setOrderId(orderId).build())
                .map(resp -> {
                    List<OrderItem> result = new ArrayList<>();
                    if (resp != null && resp.getDataList() != null) {
                        for (OrderItemResponse item : resp.getDataList()) {
                            OrderItem mapped = toOrderItem(item);
                            if (mapped != null) {
                                result.add(mapped);
                            }
                        }
                    }
                    return result;
                });
    }

    @Override
    public Uni<OrderItem> createOrderItem(CreateOrderItemCmd cmd) {
        return command.createOrderItem(CreateOrderItemRequest.newBuilder()
                .setOrderId(cmd.orderId())
                .setProductId(cmd.productId())
                .setQuantity(cmd.quantity())
                .setPrice(cmd.price())
                .build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Failed to create order item");
                    }
                    return toOrderItem(resp.getData());
                });
    }

    @Override
    public Uni<OrderItem> updateOrderItem(UpdateOrderItemCmd cmd) {
        return command.updateOrderItem(UpdateOrderItemRequest.newBuilder()
                .setOrderItemId(cmd.orderItemId())
                .setOrderId(cmd.orderId())
                .setProductId(cmd.productId())
                .setQuantity(cmd.quantity())
                .setPrice(cmd.price())
                .build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Failed to update order item");
                    }
                    return toOrderItem(resp.getData());
                });
    }

    private static OrderItem toOrderItem(OrderItemResponse i) {
        if (i == null) {
            return null;
        }
        return new OrderItem(i.getId(), i.getOrderId(), i.getProductId(), i.getQuantity(), i.getPrice(),
                ProtoTime.parse(i.getCreatedAt()), ProtoTime.parse(i.getUpdatedAt()));
    }
}
