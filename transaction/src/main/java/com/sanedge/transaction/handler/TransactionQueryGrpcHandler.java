package com.sanedge.transaction.handler;

import com.sanedge.transaction.service.TransactionQueryService;

import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.transaction.MutinyTransactionQueryServiceGrpc;
import pb.transaction.Transaction.ApiResponseTransaction;
import pb.transaction.Transaction.ApiResponseTransactions;
import pb.transaction.Transaction.FindByIdTransactionRequest;
import pb.transaction.Transaction.TransactionResponse;
import pb.transaction.Transaction.TransactionResponseDeleteAt;
import pb.transaction.TransactionQuery.ApiResponsePaginationTransaction;
import pb.transaction.TransactionQuery.ApiResponsePaginationTransactionDeleteAt;
import pb.transaction.TransactionQuery.FindAllTransactionCardNumberRequest;
import pb.transaction.TransactionQuery.FindAllTransactionRequest;
import pb.transaction.TransactionQuery.FindTransactionByMerchantIdRequest;

@GrpcService
@Singleton
public class TransactionQueryGrpcHandler extends MutinyTransactionQueryServiceGrpc.TransactionQueryServiceImplBase {

    @Inject
    TransactionQueryService transactionQueryService;

    @Override
    public Uni<ApiResponsePaginationTransaction> findAllTransaction(FindAllTransactionRequest request) {
        com.sanedge.transaction.domain.requests.FindAllTransactionRequest domainReq = new com.sanedge.transaction.domain.requests.FindAllTransactionRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return transactionQueryService.findAllTransactions(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationTransaction.Builder builder = ApiResponsePaginationTransaction.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (var item : apiResp.data()) {
                            builder.addData(toProto(item));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPaginationMeta(pb.common.PaginationMeta.newBuilder()
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
    public Uni<ApiResponsePaginationTransaction> findAllTransactionByCardNumber(
            FindAllTransactionCardNumberRequest request) {
        com.sanedge.transaction.domain.requests.FindAllTransactionRequest domainReq = new com.sanedge.transaction.domain.requests.FindAllTransactionRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch() != null && !request.getSearch().isEmpty() ? request.getSearch()
                : request.getCardNumber());

        return transactionQueryService.findAllTransactions(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationTransaction.Builder builder = ApiResponsePaginationTransaction.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (var item : apiResp.data()) {
                            builder.addData(toProto(item));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPaginationMeta(pb.common.PaginationMeta.newBuilder()
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
    public Uni<ApiResponseTransaction> findByIdTransaction(FindByIdTransactionRequest request) {
        return transactionQueryService.findById(request.getTransactionId())
                .map(apiResp -> {
                    ApiResponseTransaction.Builder builder = ApiResponseTransaction.newBuilder()
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
    public Uni<ApiResponseTransactions> findTransactionByMerchantId(FindTransactionByMerchantIdRequest request) {
        com.sanedge.transaction.domain.requests.FindAllTransactionByMerchantRequest domainReq = new com.sanedge.transaction.domain.requests.FindAllTransactionByMerchantRequest();
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setPage(1);
        domainReq.setPageSize(100);
        domainReq.setSearch("");

        return transactionQueryService.findByMerchant(domainReq)
                .map(apiResp -> {
                    ApiResponseTransactions.Builder builder = ApiResponseTransactions.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (var item : apiResp.data()) {
                            builder.addData(toProto(item));
                        }
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponsePaginationTransactionDeleteAt> findByActiveTransaction(FindAllTransactionRequest request) {
        com.sanedge.transaction.domain.requests.FindAllTransactionRequest domainReq = new com.sanedge.transaction.domain.requests.FindAllTransactionRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return transactionQueryService.findByActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationTransactionDeleteAt.Builder builder = ApiResponsePaginationTransactionDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (var item : apiResp.data()) {
                            builder.addData(toProto(item));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPaginationMeta(pb.common.PaginationMeta.newBuilder()
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
    public Uni<ApiResponsePaginationTransactionDeleteAt> findByTrashedTransaction(FindAllTransactionRequest request) {
        com.sanedge.transaction.domain.requests.FindAllTransactionRequest domainReq = new com.sanedge.transaction.domain.requests.FindAllTransactionRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return transactionQueryService.findByTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationTransactionDeleteAt.Builder builder = ApiResponsePaginationTransactionDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (var item : apiResp.data()) {
                            builder.addData(toProto(item));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPaginationMeta(pb.common.PaginationMeta.newBuilder()
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

    private TransactionResponse toProto(com.sanedge.transaction.domain.response.TransactionResponse r) {
        if (r == null) {
            return TransactionResponse.getDefaultInstance();
        }
        return TransactionResponse.newBuilder()
                .setId(r.getId().intValue())
                .setCardNumber("")
                .setTransactionNo("TX-" + r.getOrderId())
                .setAmount(r.getAmount())
                .setPaymentMethod(r.getPaymentMethod())
                .setMerchantId(r.getMerchantId())
                .setTransactionTime(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "")
                .build();
    }

    private TransactionResponseDeleteAt toProto(com.sanedge.transaction.domain.response.TransactionResponseDeleteAt r) {
        if (r == null) {
            return TransactionResponseDeleteAt.getDefaultInstance();
        }
        var builder = TransactionResponseDeleteAt.newBuilder()
                .setId(r.getId().intValue())
                .setCardNumber("")
                .setTransactionNo("TX-" + r.getOrderId())
                .setAmount(r.getAmount())
                .setPaymentMethod(r.getPaymentMethod())
                .setMerchantId(r.getMerchantId())
                .setTransactionTime(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "");
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }
}
