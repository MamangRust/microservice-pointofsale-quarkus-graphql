package com.sanedge.cashier.handler;

import com.sanedge.cashier.domain.requests.FindAllCashierMerchant;
import com.sanedge.cashier.domain.requests.FindAllCashiers;
import com.sanedge.cashier.service.CashierQueryService;

import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.cashier.Cashier.ApiResponseCashier;
import pb.cashier.Cashier.FindAllCashierRequest;
import pb.cashier.Cashier.FindByIdCashierRequest;
import pb.cashier.Cashier.FindByMerchantCashierRequest;
import pb.cashier.CashierQuery.ApiResponsePaginationCashier;
import pb.cashier.CashierQuery.ApiResponsePaginationCashierDeleteAt;
import pb.cashier.MutinyCashierServiceGrpc;

@GrpcService
@Singleton
public class CashierQueryGrpcHandler extends MutinyCashierServiceGrpc.CashierServiceImplBase {

    @Inject
    CashierQueryService cashierQueryService;

    @Override
    public Uni<ApiResponseCashier> findById(FindByIdCashierRequest request) {
        return cashierQueryService.findById((long) request.getId())
                .map(apiResp -> {
                    ApiResponseCashier.Builder builder = ApiResponseCashier.newBuilder()
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
    public Uni<ApiResponsePaginationCashier> findAll(FindAllCashierRequest request) {
        FindAllCashiers domainReq = new FindAllCashiers();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return cashierQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationCashier.Builder builder = ApiResponsePaginationCashier.newBuilder()
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
    public Uni<ApiResponsePaginationCashierDeleteAt> findByActive(FindAllCashierRequest request) {
        FindAllCashiers domainReq = new FindAllCashiers();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return cashierQueryService.findByActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationCashierDeleteAt.Builder builder = ApiResponsePaginationCashierDeleteAt
                            .newBuilder()
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
    public Uni<ApiResponsePaginationCashierDeleteAt> findByTrashed(FindAllCashierRequest request) {
        FindAllCashiers domainReq = new FindAllCashiers();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return cashierQueryService.findByTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationCashierDeleteAt.Builder builder = ApiResponsePaginationCashierDeleteAt
                            .newBuilder()
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
    public Uni<ApiResponsePaginationCashier> findByMerchant(FindByMerchantCashierRequest request) {
        FindAllCashierMerchant domainReq = new FindAllCashierMerchant();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());
        domainReq.setMerchantId(request.getMerchantId());

        return cashierQueryService.findByMerchant(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationCashier.Builder builder = ApiResponsePaginationCashier.newBuilder()
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

    private pb.cashier.Cashier.CashierResponse toProto(com.sanedge.cashier.domain.response.CashierResponse r) {
        if (r == null) {
            return pb.cashier.Cashier.CashierResponse.getDefaultInstance();
        }
        return pb.cashier.Cashier.CashierResponse.newBuilder()
                .setId(r.getId())
                .setName(r.getName())
                .setMerchantId(r.getMerchantId())
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "")
                .build();
    }

    private pb.cashier.Cashier.CashierResponseDeleteAt toProto(
            com.sanedge.cashier.domain.response.CashierResponseDeleteAt r) {
        if (r == null) {
            return pb.cashier.Cashier.CashierResponseDeleteAt.getDefaultInstance();
        }
        var builder = pb.cashier.Cashier.CashierResponseDeleteAt.newBuilder()
                .setId(r.getId())
                .setName(r.getName())
                .setMerchantId(r.getMerchantId())
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "");
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }
}
