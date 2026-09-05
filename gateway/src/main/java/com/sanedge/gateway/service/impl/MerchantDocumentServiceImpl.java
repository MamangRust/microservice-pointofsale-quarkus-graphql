package com.sanedge.gateway.service.impl;

import org.jboss.logging.Logger;

import com.sanedge.gateway.dto.MerchantDocumentDto;
import com.sanedge.gateway.service.MerchantDocumentService;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import pb.merchant_document.MerchantDocumentCommand;
import pb.merchant_document.MerchantDocumentOuterClass;
import pb.merchant_document.MutinyMerchantDocumentCommandServiceGrpc.MutinyMerchantDocumentCommandServiceStub;
import pb.merchant_document.MutinyMerchantDocumentQueryServiceGrpc.MutinyMerchantDocumentQueryServiceStub;

@ApplicationScoped
public class MerchantDocumentServiceImpl implements MerchantDocumentService {

    private static final Logger LOG = Logger.getLogger(MerchantDocumentServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("merchant")
    MutinyMerchantDocumentQueryServiceStub merchantDocumentQueryService;

    @GrpcClient("merchant")
    MutinyMerchantDocumentCommandServiceStub merchantDocumentCommandService;

    @Override
    public Uni<MerchantDocumentDto.ApiResponsePaginationMerchantDocument> listMerchantDocuments(int page, int size,
            String search) {
        return telemetryHelper.traceAndMetric("merchantDocument.listMerchantDocuments",
                () -> merchantDocumentQueryService
                        .findAll(MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.newBuilder()
                                .setPage(page)
                                .setPageSize(size)
                                .setSearch(search == null ? "" : search)
                                .build())
                        .map(MerchantDocumentDto.ApiResponsePaginationMerchantDocument::from)
                        .onFailure().invoke(throwable -> LOG
                                .error("Failed to list merchant documents: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponsePaginationMerchantDocumentDeleteAt> activeMerchantDocuments(int page,
            int size, String search) {
        return telemetryHelper
                .traceAndMetric("merchantDocument.activeMerchantDocuments",
                        () -> merchantDocumentQueryService
                                .findAllActive(MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.newBuilder()
                                        .setPage(page)
                                        .setPageSize(size)
                                        .setSearch(search == null ? "" : search)
                                        .build())
                                .map(MerchantDocumentDto.ApiResponsePaginationMerchantDocumentDeleteAt::from)
                                .onFailure()
                                .invoke(throwable -> LOG.error(
                                        "Failed to list active merchant documents: " + throwable.getMessage(),
                                        throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponsePaginationMerchantDocumentDeleteAt> trashedMerchantDocuments(int page,
            int size, String search) {
        return telemetryHelper
                .traceAndMetric("merchantDocument.trashedMerchantDocuments",
                        () -> merchantDocumentQueryService
                                .findAllTrashed(MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.newBuilder()
                                        .setPage(page)
                                        .setPageSize(size)
                                        .setSearch(search == null ? "" : search)
                                        .build())
                                .map(MerchantDocumentDto.ApiResponsePaginationMerchantDocumentDeleteAt::from)
                                .onFailure()
                                .invoke(throwable -> LOG.error(
                                        "Failed to list trashed merchant documents: " + throwable.getMessage(),
                                        throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponseMerchantDocument> getMerchantDocument(int id) {
        return telemetryHelper.traceAndMetric("merchantDocument.getMerchantDocument", () -> merchantDocumentQueryService
                .findById(MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                        .setDocumentId(id)
                        .build())
                .map(MerchantDocumentDto.ApiResponseMerchantDocument::from)
                .onFailure().invoke(throwable -> LOG
                        .error("Failed to get merchant document " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponseMerchantDocument> createMerchantDocument(
            MerchantDocumentDto.CreateMerchantDocumentRequest body) {
        return telemetryHelper.traceAndMetric("merchantDocument.createMerchantDocument",
                () -> merchantDocumentCommandService
                        .create(MerchantDocumentCommand.CreateMerchantDocumentRequest.newBuilder()
                                .setMerchantId(body.merchantId())
                                .setDocumentType(body.documentType())
                                .setDocumentUrl(body.documentUrl())
                                .build())
                        .map(MerchantDocumentDto.ApiResponseMerchantDocument::from)
                        .onFailure().invoke(throwable -> LOG
                                .error("Failed to create merchant document: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponseMerchantDocument> updateMerchantDocument(int id,
            MerchantDocumentDto.UpdateMerchantDocumentRequest body) {
        return telemetryHelper.traceAndMetric("merchantDocument.updateMerchantDocument",
                () -> merchantDocumentCommandService
                        .update(MerchantDocumentCommand.UpdateMerchantDocumentRequest.newBuilder()
                                .setDocumentId(id)
                                .setMerchantId(body.merchantId())
                                .setDocumentType(body.documentType())
                                .setDocumentUrl(body.documentUrl())
                                .setNote(body.note() == null ? "" : body.note())
                                .setStatus(body.status() == null ? "" : body.status())
                                .build())
                        .map(MerchantDocumentDto.ApiResponseMerchantDocument::from)
                        .onFailure().invoke(throwable -> LOG
                                .error("Failed to update merchant document: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponseMerchantDocumentDeleteAt> deleteMerchantDocument(int id) {
        return telemetryHelper
                .traceAndMetric("merchantDocument.deleteMerchantDocument",
                        () -> merchantDocumentCommandService
                                .trashed(MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                                        .setDocumentId(id)
                                        .build())
                                .map(MerchantDocumentDto.ApiResponseMerchantDocumentDeleteAt::from)
                                .onFailure()
                                .invoke(throwable -> LOG.error(
                                        "Failed to soft-delete merchant document: " + throwable.getMessage(),
                                        throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponseMerchantDocumentDeleteAt> restoreMerchantDocument(int id) {
        return telemetryHelper.traceAndMetric("merchantDocument.restoreMerchantDocument",
                () -> merchantDocumentCommandService
                        .restore(MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                                .setDocumentId(id)
                                .build())
                        .map(MerchantDocumentDto.ApiResponseMerchantDocumentDeleteAt::from)
                        .onFailure()
                        .invoke(throwable -> LOG.error(
                                "Failed to restore merchant document " + id + ": " + throwable.getMessage(),
                                throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponseMerchantDocumentDelete> deleteMerchantDocumentPermanent(int id) {
        return telemetryHelper.traceAndMetric("merchantDocument.deleteMerchantDocumentPermanent",
                () -> merchantDocumentCommandService
                        .deletePermanent(MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                                .setDocumentId(id)
                                .build())
                        .map(MerchantDocumentDto.ApiResponseMerchantDocumentDelete::from)
                        .onFailure()
                        .invoke(throwable -> LOG.error(
                                "Failed to permanently delete merchant document " + id + ": " + throwable.getMessage(),
                                throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponseMerchantDocumentAll> restoreAllMerchantDocuments() {
        return telemetryHelper
                .traceAndMetric("merchantDocument.restoreAllMerchantDocuments",
                        () -> merchantDocumentCommandService.restoreAll(com.google.protobuf.Empty.getDefaultInstance())
                                .map(MerchantDocumentDto.ApiResponseMerchantDocumentAll::from)
                                .onFailure()
                                .invoke(throwable -> LOG.error(
                                        "Failed to restore all merchant documents: " + throwable.getMessage(),
                                        throwable)));
    }

    @Override
    public Uni<MerchantDocumentDto.ApiResponseMerchantDocumentAll> deleteAllMerchantDocumentsPermanent() {
        return telemetryHelper.traceAndMetric("merchantDocument.deleteAllMerchantDocumentsPermanent",
                () -> merchantDocumentCommandService.deleteAllPermanent(com.google.protobuf.Empty.getDefaultInstance())
                        .map(MerchantDocumentDto.ApiResponseMerchantDocumentAll::from)
                        .onFailure()
                        .invoke(throwable -> LOG.error(
                                "Failed to permanently delete all merchant documents: " + throwable.getMessage(),
                                throwable)));
    }
}
