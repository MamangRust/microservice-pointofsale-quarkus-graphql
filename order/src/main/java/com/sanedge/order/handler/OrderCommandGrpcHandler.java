package com.sanedge.order.handler;

import com.google.protobuf.Empty;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.service.OrderCommandService;

import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.order.MutinyOrderCommandServiceGrpc;
import pb.order.Order.ApiResponseOrder;
import pb.order.Order.ApiResponseOrderAll;
import pb.order.Order.ApiResponseOrderDelete;
import pb.order.Order.ApiResponseOrderDeleteAt;
import pb.order.Order.CreateOrderRequest;
import pb.order.Order.FindByIdOrderRequest;
import pb.order.Order.UpdateOrderRequest;
import pb.order.OrderCommand.UpdateOrderTotalPriceRequest;

@GrpcService
@Singleton
public class OrderCommandGrpcHandler extends MutinyOrderCommandServiceGrpc.OrderCommandServiceImplBase {

    @Inject
    OrderCommandService orderCommandService;

    @Override
    public Uni<ApiResponseOrder> create(CreateOrderRequest request) {
        com.sanedge.order.domain.requests.CreateOrderRequest domainReq = new com.sanedge.order.domain.requests.CreateOrderRequest();
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setCashierId(request.getCashierId());

        java.util.List<com.sanedge.order.domain.requests.CreateOrderItemRequest> domainItems = new java.util.ArrayList<>();
        for (var item : request.getItemsList()) {
            com.sanedge.order.domain.requests.CreateOrderItemRequest domainItem = new com.sanedge.order.domain.requests.CreateOrderItemRequest();
            domainItem.setProductId(item.getProductId());
            domainItem.setQuantity(item.getQuantity());
            domainItems.add(domainItem);
        }
        domainReq.setItems(domainItems);

        return orderCommandService.create(domainReq)
                .map(apiResp -> {
                    ApiResponseOrder.Builder builder = ApiResponseOrder.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseOrder> update(UpdateOrderRequest request) {
        com.sanedge.order.domain.requests.UpdateOrderRequest domainReq = new com.sanedge.order.domain.requests.UpdateOrderRequest();
        domainReq.setOrderId(request.getOrderId());
        domainReq.setCashierId(request.getCashierId());

        java.util.List<com.sanedge.order.domain.requests.UpdateOrderItemRequest> domainItems = new java.util.ArrayList<>();
        for (var item : request.getItemsList()) {
            com.sanedge.order.domain.requests.UpdateOrderItemRequest domainItem = new com.sanedge.order.domain.requests.UpdateOrderItemRequest();
            domainItem.setOrderItemId(item.getOrderItemId());
            domainItem.setProductId(item.getProductId());
            domainItem.setQuantity(item.getQuantity());
            domainItems.add(domainItem);
        }
        domainReq.setItems(domainItems);

        return orderCommandService.update(domainReq)
                .map(apiResp -> {
                    ApiResponseOrder.Builder builder = ApiResponseOrder.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseOrderDeleteAt> trashedOrder(FindByIdOrderRequest request) {
        return orderCommandService.trash(request.getId())
                .map(apiResp -> {
                    ApiResponseOrderDeleteAt.Builder builder = ApiResponseOrderDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseOrderDeleteAt> restoreOrder(FindByIdOrderRequest request) {
        return orderCommandService.restore(request.getId())
                .map(apiResp -> {
                    ApiResponseOrderDeleteAt.Builder builder = ApiResponseOrderDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseOrderDelete> deleteOrderPermanent(FindByIdOrderRequest request) {
        return orderCommandService.delete(request.getId())
                .map(apiResp -> ApiResponseOrderDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseOrderAll> restoreAllOrder(Empty request) {
        return orderCommandService.restoreAll()
                .map(apiResp -> ApiResponseOrderAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseOrderAll> deleteAllOrderPermanent(Empty request) {
        return orderCommandService.deleteAll()
                .map(apiResp -> ApiResponseOrderAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseOrder> updateOrderTotalPrice(UpdateOrderTotalPriceRequest request) {
        return orderCommandService.updateOrderTotalPrice(request.getOrderId(), request.getTotalPrice())
                .map(apiResp -> {
                    ApiResponseOrder.Builder builder = ApiResponseOrder.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    private pb.order.Order.OrderResponse toProto(OrderResponse r) {
        if (r == null) {
            return pb.order.Order.OrderResponse.getDefaultInstance();
        }
        return pb.order.Order.OrderResponse.newBuilder()
                .setId(r.getId().intValue())
                .setMerchantId(r.getMerchantId().intValue())
                .setCashierId(r.getCashierId().intValue())
                .setTotalPrice(r.getTotalPrice().intValue())
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "")
                .build();
    }

    private pb.order.Order.OrderResponseDeleteAt toProto(OrderResponseDeleteAt r) {
        if (r == null) {
            return pb.order.Order.OrderResponseDeleteAt.getDefaultInstance();
        }
        var builder = pb.order.Order.OrderResponseDeleteAt.newBuilder()
                .setId(r.getId().intValue())
                .setMerchantId(r.getMerchantId().intValue())
                .setCashierId(r.getCashierId().intValue())
                .setTotalPrice(r.getTotalPrice().intValue())
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "");
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }
}
