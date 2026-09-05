package com.sanedge.order_item.handler;

import java.util.stream.Collectors;

import com.sanedge.order_item.domain.requests.FindAllOrderItems;
import com.sanedge.order_item.service.OrderItemQueryService;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import pb.common.PaginationMeta;
import pb.order_item.OrderItemQuery.ApiResponsePaginationOrderItem;
import pb.order_item.OrderItemQuery.ApiResponsePaginationOrderItemDeleteAt;
import pb.order_item.OrderItem.ApiResponsesOrderItem;
import pb.order_item.OrderItem.FindAllOrderItemRequest;
import pb.order_item.OrderItem.FindByIdOrderItemRequest;
import pb.order_item.MutinyOrderItemServiceGrpc;

@GrpcService
public class OrderItemQueryGrpcHandler extends MutinyOrderItemServiceGrpc.OrderItemServiceImplBase {

    @Inject
    OrderItemQueryService orderItemQueryService;

    @Override
    public Uni<ApiResponsePaginationOrderItem> findAll(FindAllOrderItemRequest request) {
        FindAllOrderItems req = new FindAllOrderItems();
        req.setSearch(request.getSearch());
        req.setPage(request.getPage());
        req.setPageSize(request.getPageSize());

        return orderItemQueryService.findAll(req)
                .map(apiRes -> {
                    if ("success".equals(apiRes.status()) && apiRes.data() != null) {
                        var pagedData = apiRes.data();
                        var pbList = pagedData.getData().stream()
                                .map(this::mapResponse)
                                .collect(Collectors.toList());

                        int totalRecords = pagedData.getTotalRecords();
                        int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 1;
                        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

                        var meta = PaginationMeta.newBuilder()
                                .setCurrentPage(request.getPage())
                                .setPageSize(request.getPageSize())
                                .setTotalPages(totalPages)
                                .setTotalRecords(totalRecords)
                                .build();

                        return ApiResponsePaginationOrderItem.newBuilder()
                                .setStatus("success")
                                .setMessage(apiRes.message())
                                .addAllData(pbList)
                                .setPaginationMeta(meta)
                                .build();
                    } else {
                        return ApiResponsePaginationOrderItem.newBuilder()
                                .setStatus("failed")
                                .setMessage(apiRes.message())
                                .build();
                    }
                })
                .onFailure().recoverWithItem(err -> ApiResponsePaginationOrderItem.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    @Override
    public Uni<ApiResponsePaginationOrderItemDeleteAt> findByActive(FindAllOrderItemRequest request) {
        FindAllOrderItems req = new FindAllOrderItems();
        req.setSearch(request.getSearch());
        req.setPage(request.getPage());
        req.setPageSize(request.getPageSize());

        return orderItemQueryService.findByActive(req)
                .map(apiRes -> {
                    if ("success".equals(apiRes.status()) && apiRes.data() != null) {
                        var pagedData = apiRes.data();
                        var pbList = pagedData.getData().stream()
                                .map(this::mapResponseDeleteAt)
                                .collect(Collectors.toList());

                        int totalRecords = pagedData.getTotalRecords();
                        int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 1;
                        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

                        var meta = PaginationMeta.newBuilder()
                                .setCurrentPage(request.getPage())
                                .setPageSize(request.getPageSize())
                                .setTotalPages(totalPages)
                                .setTotalRecords(totalRecords)
                                .build();

                        return ApiResponsePaginationOrderItemDeleteAt.newBuilder()
                                .setStatus("success")
                                .setMessage(apiRes.message())
                                .addAllData(pbList)
                                .setPaginationMeta(meta)
                                .build();
                    } else {
                        return ApiResponsePaginationOrderItemDeleteAt.newBuilder()
                                .setStatus("failed")
                                .setMessage(apiRes.message())
                                .build();
                    }
                })
                .onFailure().recoverWithItem(err -> ApiResponsePaginationOrderItemDeleteAt.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    @Override
    public Uni<ApiResponsePaginationOrderItemDeleteAt> findByTrashed(FindAllOrderItemRequest request) {
        FindAllOrderItems req = new FindAllOrderItems();
        req.setSearch(request.getSearch());
        req.setPage(request.getPage());
        req.setPageSize(request.getPageSize());

        return orderItemQueryService.findByTrashed(req)
                .map(apiRes -> {
                    if ("success".equals(apiRes.status()) && apiRes.data() != null) {
                        var pagedData = apiRes.data();
                        var pbList = pagedData.getData().stream()
                                .map(this::mapResponseDeleteAt)
                                .collect(Collectors.toList());

                        int totalRecords = pagedData.getTotalRecords();
                        int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 1;
                        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

                        var meta = PaginationMeta.newBuilder()
                                .setCurrentPage(request.getPage())
                                .setPageSize(request.getPageSize())
                                .setTotalPages(totalPages)
                                .setTotalRecords(totalRecords)
                                .build();

                        return ApiResponsePaginationOrderItemDeleteAt.newBuilder()
                                .setStatus("success")
                                .setMessage(apiRes.message())
                                .addAllData(pbList)
                                .setPaginationMeta(meta)
                                .build();
                    } else {
                        return ApiResponsePaginationOrderItemDeleteAt.newBuilder()
                                .setStatus("failed")
                                .setMessage(apiRes.message())
                                .build();
                    }
                })
                .onFailure().recoverWithItem(err -> ApiResponsePaginationOrderItemDeleteAt.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    @Override
    public Uni<ApiResponsesOrderItem> findOrderItemByOrder(FindByIdOrderItemRequest request) {
        int orderId = request.getOrderId() > 0 ? request.getOrderId() : request.getOrderItemId();
        return orderItemQueryService.findOrderItemByOrder(orderId)
                .map(apiRes -> {
                    if ("success".equals(apiRes.status()) && apiRes.data() != null) {
                        var list = apiRes.data().stream()
                                .map(this::mapResponse)
                                .collect(Collectors.toList());

                        return ApiResponsesOrderItem.newBuilder()
                                .setStatus("success")
                                .setMessage(apiRes.message())
                                .addAllData(list)
                                .build();
                    } else {
                        return ApiResponsesOrderItem.newBuilder()
                                .setStatus("failed")
                                .setMessage(apiRes.message())
                                .build();
                    }
                })
                .onFailure().recoverWithItem(err -> ApiResponsesOrderItem.newBuilder()
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
