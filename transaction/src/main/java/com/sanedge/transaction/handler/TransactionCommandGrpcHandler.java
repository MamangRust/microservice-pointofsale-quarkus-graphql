package com.sanedge.transaction.handler;

import com.sanedge.transaction.service.TransactionCommandService;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.transaction.MutinyTransactionCommandServiceGrpc;
import pb.transaction.Transaction.ApiResponseTransaction;
import pb.transaction.Transaction.ApiResponseTransactionDeleteAt;
import pb.transaction.Transaction.FindByIdTransactionRequest;
import pb.transaction.Transaction.TransactionResponse;
import pb.transaction.Transaction.TransactionResponseDeleteAt;
import pb.transaction.TransactionCommand.ApiResponseTransactionAll;
import pb.transaction.TransactionCommand.ApiResponseTransactionDelete;
import pb.transaction.TransactionCommand.CreateTransactionRequest;
import pb.transaction.TransactionCommand.UpdateTransactionRequest;

@GrpcService
@Singleton
public class TransactionCommandGrpcHandler extends MutinyTransactionCommandServiceGrpc.TransactionCommandServiceImplBase {

    @Inject
    TransactionCommandService transactionCommandService;

    @Override
    public Uni<ApiResponseTransaction> createTransaction(CreateTransactionRequest request) {
        com.sanedge.transaction.domain.requests.CreateTransactionRequest domainReq = 
                new com.sanedge.transaction.domain.requests.CreateTransactionRequest();
        
        int orderId = 1;
        try {
            orderId = Integer.parseInt(request.getCardNumber());
        } catch (NumberFormatException e) {
            // fallback
        }
        
        domainReq.setOrderID(orderId);
        domainReq.setMerchantID(request.getMerchantId());
        domainReq.setPaymentMethod(request.getPaymentMethod());
        domainReq.setAmount(request.getAmount());
        domainReq.setPaymentStatus("pending");
        domainReq.setIdempotencyKey(request.getIdempotencyKey());

        return transactionCommandService.create(domainReq)
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
    public Uni<ApiResponseTransaction> updateTransaction(UpdateTransactionRequest request) {
        com.sanedge.transaction.domain.requests.UpdateTransactionRequest domainReq = 
                new com.sanedge.transaction.domain.requests.UpdateTransactionRequest();
        
        int orderId = 1;
        try {
            orderId = Integer.parseInt(request.getCardNumber());
        } catch (NumberFormatException e) {
            // fallback
        }

        domainReq.setTransactionID(request.getTransactionId());
        domainReq.setOrderID(orderId);
        domainReq.setMerchantID(request.getMerchantId());
        domainReq.setPaymentMethod(request.getPaymentMethod());
        domainReq.setAmount(request.getAmount());
        domainReq.setPaymentStatus("pending");
        domainReq.setIdempotencyKey(request.getIdempotencyKey());

        return transactionCommandService.update(domainReq)
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
    public Uni<ApiResponseTransactionDeleteAt> trashedTransaction(FindByIdTransactionRequest request) {
        return transactionCommandService.trash(request.getTransactionId())
                .map(apiResp -> {
                    ApiResponseTransactionDeleteAt.Builder builder = ApiResponseTransactionDeleteAt.newBuilder()
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
    public Uni<ApiResponseTransactionDeleteAt> restoreTransaction(FindByIdTransactionRequest request) {
        return transactionCommandService.restore(request.getTransactionId())
                .map(apiResp -> {
                    ApiResponseTransactionDeleteAt.Builder builder = ApiResponseTransactionDeleteAt.newBuilder()
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
    public Uni<ApiResponseTransactionDelete> deleteTransactionPermanent(FindByIdTransactionRequest request) {
        return transactionCommandService.delete(request.getTransactionId())
                .map(apiResp -> ApiResponseTransactionDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseTransactionAll> restoreAllTransaction(com.google.protobuf.Empty request) {
        return transactionCommandService.restoreAll()
                .map(apiResp -> ApiResponseTransactionAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseTransactionAll> deleteAllTransactionPermanent(com.google.protobuf.Empty request) {
        return transactionCommandService.deleteAll()
                .map(apiResp -> ApiResponseTransactionAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
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
