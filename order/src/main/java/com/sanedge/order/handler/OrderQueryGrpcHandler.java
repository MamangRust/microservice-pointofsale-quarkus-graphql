package com.sanedge.order.handler;

import com.sanedge.order.domain.requests.FindAllOrderByMerchantRequest;
import com.sanedge.order.domain.requests.FindAllOrderRequest;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.service.OrderQueryService;

import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.order.MutinyOrderQueryServiceGrpc;
import pb.order.Order.ApiResponseOrder;
import pb.order.Order.FindAllOrderMerchantRequest;
import pb.order.Order.FindByIdOrderRequest;
import pb.order.OrderQuery.ApiResponsePaginationOrder;
import pb.order.OrderQuery.ApiResponsePaginationOrderDeleteAt;

@GrpcService
@Singleton
public class OrderQueryGrpcHandler extends MutinyOrderQueryServiceGrpc.OrderQueryServiceImplBase {

    @Inject
    OrderQueryService orderQueryService;

    @Override
    public Uni<ApiResponseOrder> findById(FindByIdOrderRequest request) {
        return orderQueryService.findById(request.getId())
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
    public Uni<ApiResponsePaginationOrder> findAll(pb.order.Order.FindAllOrderRequest request) {
        FindAllOrderRequest domainReq = new FindAllOrderRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return orderQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationOrder.Builder builder = ApiResponsePaginationOrder.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (var item : apiResp.data()) {
                            builder.addData(toProto(item));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(pb.common.PaginationMeta.newBuilder()
                                .setCurrentPage(apiResp.pagination().currentPage())
                                .setPageSize(apiResp.pagination().pageSize())
                                .setTotalPages(apiResp.pagination().totalPages())
                                .setTotalRecords(apiResp.pagination().totalRecords())
                                .build());
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponsePaginationOrder> findByMerchant(FindAllOrderMerchantRequest request) {
        FindAllOrderByMerchantRequest domainReq = new FindAllOrderByMerchantRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());
        domainReq.setMerchantId(request.getMerchantId());

        return orderQueryService.findByMerchantId(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationOrder.Builder builder = ApiResponsePaginationOrder.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (var item : apiResp.data()) {
                            builder.addData(toProto(item));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(pb.common.PaginationMeta.newBuilder()
                                .setCurrentPage(apiResp.pagination().currentPage())
                                .setPageSize(apiResp.pagination().pageSize())
                                .setTotalPages(apiResp.pagination().totalPages())
                                .setTotalRecords(apiResp.pagination().totalRecords())
                                .build());
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponsePaginationOrderDeleteAt> findByActive(pb.order.Order.FindAllOrderRequest request) {
        FindAllOrderRequest domainReq = new FindAllOrderRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return orderQueryService.findByActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationOrderDeleteAt.Builder builder = ApiResponsePaginationOrderDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (var item : apiResp.data()) {
                            builder.addData(toProto(item));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(pb.common.PaginationMeta.newBuilder()
                                .setCurrentPage(apiResp.pagination().currentPage())
                                .setPageSize(apiResp.pagination().pageSize())
                                .setTotalPages(apiResp.pagination().totalPages())
                                .setTotalRecords(apiResp.pagination().totalRecords())
                                .build());
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponsePaginationOrderDeleteAt> findByTrashed(pb.order.Order.FindAllOrderRequest request) {
        FindAllOrderRequest domainReq = new FindAllOrderRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return orderQueryService.findByTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationOrderDeleteAt.Builder builder = ApiResponsePaginationOrderDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (var item : apiResp.data()) {
                            builder.addData(toProto(item));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(pb.common.PaginationMeta.newBuilder()
                                .setCurrentPage(apiResp.pagination().currentPage())
                                .setPageSize(apiResp.pagination().pageSize())
                                .setTotalPages(apiResp.pagination().totalPages())
                                .setTotalRecords(apiResp.pagination().totalRecords())
                                .build());
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
