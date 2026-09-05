package com.sanedge.cashier.handler;

import com.google.protobuf.Empty;
import com.sanedge.cashier.domain.requests.CreateCashierRequest;
import com.sanedge.cashier.domain.requests.UpdateCashierRequest;
import com.sanedge.cashier.domain.response.CashierResponse;
import com.sanedge.cashier.domain.response.CashierResponseDeleteAt;
import com.sanedge.cashier.service.CashierCommandService;

import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.cashier.Cashier.ApiResponseCashier;
import pb.cashier.Cashier.ApiResponseCashierDeleteAt;
import pb.cashier.Cashier.FindByIdCashierRequest;
import pb.cashier.CashierCommand.ApiResponseCashierAll;
import pb.cashier.CashierCommand.ApiResponseCashierDelete;
import pb.cashier.MutinyCashierCommandServiceGrpc;

@GrpcService
@Singleton
public class CashierCommandGrpcHandler extends MutinyCashierCommandServiceGrpc.CashierCommandServiceImplBase {

    @Inject
    CashierCommandService cashierCommandService;

    @Override
    public Uni<ApiResponseCashier> createCashier(pb.cashier.Cashier.CreateCashierRequest request) {
        CreateCashierRequest domainReq = new CreateCashierRequest();
        domainReq.setName(request.getName());
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setUserId(request.getUserId());

        return cashierCommandService.createCashier(domainReq)
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
    public Uni<ApiResponseCashier> updateCashier(pb.cashier.Cashier.UpdateCashierRequest request) {
        UpdateCashierRequest domainReq = new UpdateCashierRequest();
        domainReq.setCashierId(request.getCashierId());
        domainReq.setName(request.getName());

        return cashierCommandService.updateCashier(domainReq)
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
    public Uni<ApiResponseCashierDeleteAt> trashedCashier(FindByIdCashierRequest request) {
        return cashierCommandService.trashedCashier((long) request.getId())
                .map(apiResp -> {
                    ApiResponseCashierDeleteAt.Builder builder = ApiResponseCashierDeleteAt.newBuilder()
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
    public Uni<ApiResponseCashierDeleteAt> restoreCashier(FindByIdCashierRequest request) {
        return cashierCommandService.restoreCashier((long) request.getId())
                .map(apiResp -> {
                    ApiResponseCashierDeleteAt.Builder builder = ApiResponseCashierDeleteAt.newBuilder()
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
    public Uni<ApiResponseCashierDelete> deleteCashierPermanent(FindByIdCashierRequest request) {
        return cashierCommandService.deleteCashierPermanent((long) request.getId())
                .map(apiResp -> ApiResponseCashierDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseCashierAll> restoreAllCashier(Empty request) {
        return cashierCommandService.restoreAllCashier()
                .map(apiResp -> ApiResponseCashierAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseCashierAll> deleteAllCashierPermanent(Empty request) {
        return cashierCommandService.deleteAllCashierPermanent()
                .map(apiResp -> ApiResponseCashierAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    private pb.cashier.Cashier.CashierResponse toProto(CashierResponse r) {
        if (r == null) {
            return pb.cashier.Cashier.CashierResponse.getDefaultInstance();
        }
        return pb.cashier.Cashier.CashierResponse.newBuilder()
                .setId(r.getId().intValue())
                .setName(r.getName())
                .setMerchantId(r.getMerchantId().intValue())
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "")
                .build();
    }

    private pb.cashier.Cashier.CashierResponseDeleteAt toProto(CashierResponseDeleteAt r) {
        if (r == null) {
            return pb.cashier.Cashier.CashierResponseDeleteAt.getDefaultInstance();
        }
        var builder = pb.cashier.Cashier.CashierResponseDeleteAt.newBuilder()
                .setId(r.getId().intValue())
                .setName(r.getName())
                .setMerchantId(r.getMerchantId().intValue())
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "");
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }
}
