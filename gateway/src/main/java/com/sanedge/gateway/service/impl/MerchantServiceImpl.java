package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.MerchantDto;
import com.sanedge.gateway.service.MerchantService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MerchantServiceImpl implements MerchantService {

    private static final Logger LOG = Logger.getLogger(MerchantServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("merchant")
    pb.merchant.MutinyMerchantQueryServiceGrpc.MutinyMerchantQueryServiceStub merchantQueryService;

    @GrpcClient("merchant")
    pb.merchant.MutinyMerchantCommandServiceGrpc.MutinyMerchantCommandServiceStub merchantCommandService;

    @Override
    public Uni<MerchantDto.ApiResponsePaginationMerchant> listMerchants(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.listMerchants", () -> merchantQueryService.findAllMerchant(pb.merchant.Merchant.FindAllMerchantRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(MerchantDto.ApiResponsePaginationMerchant::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list merchants: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponsePaginationMerchantDeleteAt> activeMerchants(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.activeMerchants", () -> merchantQueryService.findByActive(pb.merchant.Merchant.FindAllMerchantRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(MerchantDto.ApiResponsePaginationMerchantDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list active merchants: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponsePaginationMerchantDeleteAt> trashedMerchants(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.trashedMerchants", () -> merchantQueryService.findByTrashed(pb.merchant.Merchant.FindAllMerchantRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(MerchantDto.ApiResponsePaginationMerchantDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list trashed merchants: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponseMerchant> getMerchant(int id) {
        return telemetryHelper.traceAndMetric("merchant.getMerchant", () -> merchantQueryService.findByIdMerchant(pb.merchant.Merchant.FindByIdMerchantRequest.newBuilder()
                .setMerchantId(id)
                .build())
                .map(MerchantDto.ApiResponseMerchant::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get merchant " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponseMerchant> getMerchantByApiKey(String apiKey) {
        return telemetryHelper.traceAndMetric("merchant.getMerchantByApiKey", () -> merchantQueryService.findByApiKey(pb.merchant.Merchant.FindByApiKeyRequest.newBuilder()
                .setApiKey(apiKey)
                .build())
                .map(MerchantDto.ApiResponseMerchant::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get merchant by API key: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponsesMerchant> getMerchantsByUserId(int userId) {
        return telemetryHelper.traceAndMetric("merchant.getMerchantsByUserId", () -> merchantQueryService.findByMerchantUserId(pb.merchant.Merchant.FindByMerchantUserIdRequest.newBuilder()
                .setUserId(userId)
                .build())
                .map(MerchantDto.ApiResponsesMerchant::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get merchants by user " + userId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponseMerchant> createMerchant(MerchantDto.CreateMerchantRequest body) {
        return telemetryHelper.traceAndMetric("merchant.createMerchant", () -> merchantCommandService.createMerchant(pb.merchant.MerchantCommand.CreateMerchantRequest.newBuilder()
                .setUserId(body.userId())
                .setName(body.name())
                .build())
                .map(MerchantDto.ApiResponseMerchant::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponseMerchant> updateMerchant(int id, MerchantDto.UpdateMerchantRequest body) {
        return telemetryHelper.traceAndMetric("merchant.updateMerchant", () -> merchantCommandService.updateMerchant(pb.merchant.MerchantCommand.UpdateMerchantRequest.newBuilder()
                .setMerchantId(id)
                .setName(body.name())
                .build())
                .map(MerchantDto.ApiResponseMerchant::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponseMerchantDeleteAt> deleteMerchant(int id) {
        return telemetryHelper.traceAndMetric("merchant.deleteMerchant", () -> merchantCommandService.trashedMerchant(pb.merchant.Merchant.FindByIdMerchantRequest.newBuilder()
                .setMerchantId(id)
                .build())
                .map(MerchantDto.ApiResponseMerchantDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponseMerchantDeleteAt> restoreMerchant(int id) {
        return telemetryHelper.traceAndMetric("merchant.restoreMerchant", () -> merchantCommandService.restoreMerchant(pb.merchant.Merchant.FindByIdMerchantRequest.newBuilder()
                .setMerchantId(id)
                .build())
                .map(MerchantDto.ApiResponseMerchantDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore merchant " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponseMerchantDelete> deleteMerchantPermanent(int id) {
        return telemetryHelper.traceAndMetric("merchant.deleteMerchantPermanent", () -> merchantCommandService.deleteMerchantPermanent(pb.merchant.Merchant.FindByIdMerchantRequest.newBuilder()
                .setMerchantId(id)
                .build())
                .map(MerchantDto.ApiResponseMerchantDelete::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete merchant " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponseMerchantAll> restoreAllMerchants() {
        return telemetryHelper.traceAndMetric("merchant.restoreAllMerchants", () -> merchantCommandService.restoreAllMerchant(com.google.protobuf.Empty.getDefaultInstance())
                .map(MerchantDto.ApiResponseMerchantAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all merchants: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponseMerchantAll> deleteAllMerchantsPermanent() {
        return telemetryHelper.traceAndMetric("merchant.deleteAllMerchantsPermanent", () -> merchantCommandService.deleteAllMerchantPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(MerchantDto.ApiResponseMerchantAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all merchants: " + throwable.getMessage(), throwable)));
    }
}
