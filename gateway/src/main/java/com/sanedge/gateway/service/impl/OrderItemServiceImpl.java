package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.OrderItemDto;
import com.sanedge.gateway.service.OrderItemService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderItemServiceImpl implements OrderItemService {

    private static final Logger LOG = Logger.getLogger(OrderItemServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("order_item")
    pb.order_item.MutinyOrderItemServiceGrpc.MutinyOrderItemServiceStub orderItemQueryService;

    @GrpcClient("order_item")
    pb.order_item.MutinyOrderItemCommandServiceGrpc.MutinyOrderItemCommandServiceStub orderItemCommandService;

    @Override
    public Uni<OrderItemDto.ApiResponsePaginationOrderItem> listOrderItems(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("orderItem.listOrderItems",
                orderItemQueryService.findAll(
                        pb.order_item.OrderItem.FindAllOrderItemRequest.newBuilder()
                                .setPage(page)
                                .setPageSize(size)
                                .setSearch(search == null ? "" : search)
                                .build())
                        .map(OrderItemDto.ApiResponsePaginationOrderItem::from)
        ).onFailure().invoke(throwable -> LOG.error("Failed to list order items: " + throwable.getMessage(), throwable));
    }

    @Override
    public Uni<OrderItemDto.ApiResponsePaginationOrderItemDeleteAt> getActiveOrderItems(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("orderItem.getActiveOrderItems",
                orderItemQueryService.findByActive(
                        pb.order_item.OrderItem.FindAllOrderItemRequest.newBuilder()
                                .setPage(page)
                                .setPageSize(size)
                                .setSearch(search == null ? "" : search)
                                .build())
                        .map(OrderItemDto.ApiResponsePaginationOrderItemDeleteAt::from)
        ).onFailure().invoke(throwable -> LOG.error("Failed to list active order items: " + throwable.getMessage(),
                throwable));
    }

    @Override
    public Uni<OrderItemDto.ApiResponsePaginationOrderItemDeleteAt> getTrashedOrderItems(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("orderItem.getTrashedOrderItems",
                orderItemQueryService.findByTrashed(
                        pb.order_item.OrderItem.FindAllOrderItemRequest.newBuilder()
                                .setPage(page)
                                .setPageSize(size)
                                .setSearch(search == null ? "" : search)
                                .build())
                        .map(OrderItemDto.ApiResponsePaginationOrderItemDeleteAt::from)
        ).onFailure().invoke(throwable -> LOG.error("Failed to list trashed order items: " + throwable.getMessage(),
                throwable));
    }

    @Override
    public Uni<OrderItemDto.ApiResponsesOrderItem> getOrderItemsByOrder(int orderId) {
        return telemetryHelper.traceAndMetric("orderItem.getOrderItemsByOrder",
                orderItemQueryService.findOrderItemByOrder(
                        pb.order_item.OrderItem.FindByIdOrderItemRequest.newBuilder()
                                .setOrderItemId(orderId)
                                .build())
                        .map(OrderItemDto.ApiResponsesOrderItem::from)
        ).onFailure().invoke(throwable -> LOG.error("Failed to get order items for order " + orderId + ": "
                + throwable.getMessage(), throwable));
    }

    @Override
    public Uni<OrderItemDto.ApiResponseOrderItem> createOrderItem(OrderItemDto.CreateOrderItemRequest body) {
        return telemetryHelper.traceAndMetric("orderItem.createOrderItem",
                orderItemCommandService.createOrderItem(
                        pb.order_item.OrderItemCommand.CreateOrderItemRequest.newBuilder()
                                .setOrderId(body.orderId())
                                .setProductId(body.productId())
                                .setQuantity(body.quantity())
                                .setPrice(body.price())
                                .build())
                        .map(OrderItemDto.ApiResponseOrderItem::from)
        ).onFailure().invoke(throwable -> LOG.error("Failed to create order item: " + throwable.getMessage(), throwable));
    }

    @Override
    public Uni<OrderItemDto.ApiResponseOrderItem> updateOrderItem(int id, OrderItemDto.UpdateOrderItemRequest body) {
        return telemetryHelper.traceAndMetric("orderItem.updateOrderItem",
                orderItemCommandService.updateOrderItem(
                        pb.order_item.OrderItemCommand.UpdateOrderItemRequest.newBuilder()
                                .setOrderItemId(id)
                                .setOrderId(body.orderId())
                                .setProductId(body.productId())
                                .setQuantity(body.quantity())
                                .setPrice(body.price())
                                .build())
                        .map(OrderItemDto.ApiResponseOrderItem::from)
        ).onFailure().invoke(throwable -> LOG.error("Failed to update order item " + id + ": "
                + throwable.getMessage(), throwable));
    }

    @Override
    public Uni<OrderItemDto.ApiResponseOrderItemDeleteAt> deleteOrderItem(int id) {
        return telemetryHelper.traceAndMetric("orderItem.deleteOrderItem",
                orderItemCommandService.trashedOrderItem(
                        pb.order_item.OrderItem.FindByIdOrderItemRequest.newBuilder()
                                .setOrderItemId(id)
                                .build())
                        .map(OrderItemDto.ApiResponseOrderItemDeleteAt::from)
        ).onFailure().invoke(throwable -> LOG.error("Failed to soft-delete order item " + id + ": "
                + throwable.getMessage(), throwable));
    }

    @Override
    public Uni<OrderItemDto.ApiResponseOrderItemDeleteAt> restoreOrderItem(int id) {
        return telemetryHelper.traceAndMetric("orderItem.restoreOrderItem",
                orderItemCommandService.restoreOrderItem(
                        pb.order_item.OrderItem.FindByIdOrderItemRequest.newBuilder()
                                .setOrderItemId(id)
                                .build())
                        .map(OrderItemDto.ApiResponseOrderItemDeleteAt::from)
        ).onFailure().invoke(throwable -> LOG.error("Failed to restore order item " + id + ": "
                + throwable.getMessage(), throwable));
    }

    @Override
    public Uni<OrderItemDto.ApiResponseOrderItemDelete> deleteOrderItemPermanent(int id) {
        return telemetryHelper.traceAndMetric("orderItem.deleteOrderItemPermanent",
                orderItemCommandService.deleteOrderItemPermanent(
                        pb.order_item.OrderItem.FindByIdOrderItemRequest.newBuilder()
                                .setOrderItemId(id)
                                .build())
                        .map(OrderItemDto.ApiResponseOrderItemDelete::from)
        ).onFailure().invoke(throwable -> LOG.error("Failed to permanently delete order item " + id + ": "
                + throwable.getMessage(), throwable));
    }

    @Override
    public Uni<OrderItemDto.ApiResponseOrderItemAll> restoreAllOrderItems() {
        return telemetryHelper.traceAndMetric("orderItem.restoreAllOrderItems",
                orderItemCommandService.restoreAllOrderItem(com.google.protobuf.Empty.getDefaultInstance())
                        .map(OrderItemDto.ApiResponseOrderItemAll::from)
        ).onFailure().invoke(throwable -> LOG.error("Failed to restore all order items: "
                + throwable.getMessage(), throwable));
    }

    @Override
    public Uni<OrderItemDto.ApiResponseOrderItemAll> deleteAllOrderItems() {
        return telemetryHelper.traceAndMetric("orderItem.deleteAllOrderItems",
                orderItemCommandService.deleteAllOrderItemPermanent(com.google.protobuf.Empty.getDefaultInstance())
                        .map(OrderItemDto.ApiResponseOrderItemAll::from)
        ).onFailure().invoke(throwable -> LOG.error("Failed to delete all order items: "
                + throwable.getMessage(), throwable));
    }
}