package com.sanedge.order_item.handler;

import com.google.protobuf.Empty;
import com.sanedge.order_item.service.OrderItemCommandService;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import pb.order_item.OrderItem.ApiResponseOrderItem;
import pb.order_item.OrderItem.ApiResponseOrderItemAll;
import pb.order_item.OrderItem.ApiResponseOrderItemDelete;
import pb.order_item.OrderItem.ApiResponseOrderItemDeleteAt;
import pb.order_item.OrderItemCommand.CreateOrderItemRequest;
import pb.order_item.OrderItem.FindByIdOrderItemRequest;
import pb.order_item.MutinyOrderItemCommandServiceGrpc;
import pb.order_item.OrderItemCommand.UpdateOrderItemRequest;

@GrpcService
public class OrderItemCommandGrpcHandler extends MutinyOrderItemCommandServiceGrpc.OrderItemCommandServiceImplBase {

    @Inject
    OrderItemCommandService orderItemCommandService;

    @Override
    public Uni<ApiResponseOrderItem> createOrderItem(CreateOrderItemRequest request) {
        com.sanedge.order_item.domain.requests.CreateOrderItemRequest domainReq = new com.sanedge.order_item.domain.requests.CreateOrderItemRequest();
        domainReq.setOrderId(request.getOrderId());
        domainReq.setProductId(request.getProductId());
        domainReq.setQuantity(request.getQuantity());
        domainReq.setPrice(request.getPrice());

        return orderItemCommandService.create(domainReq)
                .map(apiRes -> {
                    if ("success".equals(apiRes.status())) {
                        return ApiResponseOrderItem.newBuilder()
                                .setStatus("success")
                                .setMessage(apiRes.message())
                                .setData(mapResponse(apiRes.data()))
                                .build();
                    } else {
                        return ApiResponseOrderItem.newBuilder()
                                .setStatus("failed")
                                .setMessage(apiRes.message())
                                .build();
                    }
                })
                .onFailure().recoverWithItem(err -> ApiResponseOrderItem.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    @Override
    public Uni<ApiResponseOrderItem> updateOrderItem(UpdateOrderItemRequest request) {
        com.sanedge.order_item.domain.requests.UpdateOrderItemRequest domainReq = new com.sanedge.order_item.domain.requests.UpdateOrderItemRequest();
        domainReq.setOrderItemId(request.getOrderItemId());
        domainReq.setOrderId(request.getOrderId());
        domainReq.setProductId(request.getProductId());
        domainReq.setQuantity(request.getQuantity());
        domainReq.setPrice(request.getPrice());

        return orderItemCommandService.update(domainReq)
                .map(apiRes -> {
                    if ("success".equals(apiRes.status())) {
                        return ApiResponseOrderItem.newBuilder()
                                .setStatus("success")
                                .setMessage(apiRes.message())
                                .setData(mapResponse(apiRes.data()))
                                .build();
                    } else {
                        return ApiResponseOrderItem.newBuilder()
                                .setStatus("failed")
                                .setMessage(apiRes.message())
                                .build();
                    }
                })
                .onFailure().recoverWithItem(err -> ApiResponseOrderItem.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    @Override
    public Uni<ApiResponseOrderItemDeleteAt> trashedOrderItem(FindByIdOrderItemRequest request) {
        return orderItemCommandService.trash(request.getOrderItemId())
                .map(apiRes -> {
                    if ("success".equals(apiRes.status())) {
                        return ApiResponseOrderItemDeleteAt.newBuilder()
                                .setStatus("success")
                                .setMessage(apiRes.message())
                                .setData(mapResponseDeleteAt(apiRes.data()))
                                .build();
                    } else {
                        return ApiResponseOrderItemDeleteAt.newBuilder()
                                .setStatus("failed")
                                .setMessage(apiRes.message())
                                .build();
                    }
                })
                .onFailure().recoverWithItem(err -> ApiResponseOrderItemDeleteAt.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    @Override
    public Uni<ApiResponseOrderItemDeleteAt> restoreOrderItem(FindByIdOrderItemRequest request) {
        return orderItemCommandService.restore(request.getOrderItemId())
                .map(apiRes -> {
                    if ("success".equals(apiRes.status())) {
                        return ApiResponseOrderItemDeleteAt.newBuilder()
                                .setStatus("success")
                                .setMessage(apiRes.message())
                                .setData(mapResponseDeleteAt(apiRes.data()))
                                .build();
                    } else {
                        return ApiResponseOrderItemDeleteAt.newBuilder()
                                .setStatus("failed")
                                .setMessage(apiRes.message())
                                .build();
                    }
                })
                .onFailure().recoverWithItem(err -> ApiResponseOrderItemDeleteAt.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    @Override
    public Uni<ApiResponseOrderItemDelete> deleteOrderItemPermanent(FindByIdOrderItemRequest request) {
        return orderItemCommandService.delete(request.getOrderItemId())
                .map(apiRes -> {
                    if ("success".equals(apiRes.status())) {
                        return ApiResponseOrderItemDelete.newBuilder()
                                .setStatus("success")
                                .setMessage(apiRes.message())
                                .build();
                    } else {
                        return ApiResponseOrderItemDelete.newBuilder()
                                .setStatus("failed")
                                .setMessage(apiRes.message())
                                .build();
                    }
                })
                .onFailure().recoverWithItem(err -> ApiResponseOrderItemDelete.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    @Override
    public Uni<ApiResponseOrderItemAll> restoreAllOrderItem(Empty request) {
        return orderItemCommandService.restoreAll()
                .map(apiRes -> {
                    if ("success".equals(apiRes.status())) {
                        return ApiResponseOrderItemAll.newBuilder()
                                .setStatus("success")
                                .setMessage(apiRes.message())
                                .build();
                    } else {
                        return ApiResponseOrderItemAll.newBuilder()
                                .setStatus("failed")
                                .setMessage(apiRes.message())
                                .build();
                    }
                })
                .onFailure().recoverWithItem(err -> ApiResponseOrderItemAll.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    @Override
    public Uni<ApiResponseOrderItemAll> deleteAllOrderItemPermanent(Empty request) {
        return orderItemCommandService.deleteAll()
                .map(apiRes -> {
                    if ("success".equals(apiRes.status())) {
                        return ApiResponseOrderItemAll.newBuilder()
                                .setStatus("success")
                                .setMessage(apiRes.message())
                                .build();
                    } else {
                        return ApiResponseOrderItemAll.newBuilder()
                                .setStatus("failed")
                                .setMessage(apiRes.message())
                                .build();
                    }
                })
                .onFailure().recoverWithItem(err -> ApiResponseOrderItemAll.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    private pb.order_item.OrderItem.OrderItemResponse mapResponse(com.sanedge.order_item.domain.response.OrderItemResponse res) {
        if (res == null) {
            return pb.order_item.OrderItem.OrderItemResponse.getDefaultInstance();
        }
        return pb.order_item.OrderItem.OrderItemResponse.newBuilder()
                .setId(res.getId().intValue())
                .setOrderId(res.getOrderId() != null ? res.getOrderId() : 0)
                .setProductId(res.getProductId() != null ? res.getProductId() : 0)
                .setQuantity(res.getQuantity() != null ? res.getQuantity() : 0)
                .setPrice(res.getPrice() != null ? res.getPrice() : 0)
                .setCreatedAt(res.getCreatedAt() != null ? res.getCreatedAt() : "")
                .setUpdatedAt(res.getUpdatedAt() != null ? res.getUpdatedAt() : "")
                .build();
    }

    private pb.order_item.OrderItem.OrderItemResponseDeleteAt mapResponseDeleteAt(com.sanedge.order_item.domain.response.OrderItemResponseDeleteAt res) {
        if (res == null) {
            return pb.order_item.OrderItem.OrderItemResponseDeleteAt.getDefaultInstance();
        }
        var builder = pb.order_item.OrderItem.OrderItemResponseDeleteAt.newBuilder()
                .setId(res.getId().intValue())
                .setOrderId(res.getOrderId() != null ? res.getOrderId() : 0)
                .setProductId(res.getProductId() != null ? res.getProductId() : 0)
                .setQuantity(res.getQuantity() != null ? res.getQuantity() : 0)
                .setPrice(res.getPrice() != null ? res.getPrice() : 0)
                .setCreatedAt(res.getCreatedAt() != null ? res.getCreatedAt() : "")
                .setUpdatedAt(res.getUpdatedAt() != null ? res.getUpdatedAt() : "");
        if (res.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(res.getDeletedAt()));
        }
        return builder.build();
    }
}
