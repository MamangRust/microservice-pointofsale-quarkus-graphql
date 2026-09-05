package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.MerchantDocumentDto;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;
import pb.merchant_document.MerchantDocumentCommand;
import pb.merchant_document.MerchantDocumentOuterClass;
import pb.merchant_document.MerchantDocumentQuery;

@ExtendWith(MockitoExtension.class)
class MerchantDocumentServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.merchant_document.MutinyMerchantDocumentQueryServiceGrpc.MutinyMerchantDocumentQueryServiceStub merchantDocumentQueryService;

    @Mock
    pb.merchant_document.MutinyMerchantDocumentCommandServiceGrpc.MutinyMerchantDocumentCommandServiceStub merchantDocumentCommandService;

    MerchantDocumentServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new MerchantDocumentServiceImpl();

        setField(service, "telemetryHelper", telemetryHelper);
        setField(service, "merchantDocumentQueryService", merchantDocumentQueryService);
        setField(service, "merchantDocumentCommandService", merchantDocumentCommandService);

        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(java.util.function.Supplier.class)))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<Uni<?>> supplier = inv.getArgument(1);
                    return supplier.get();
                });
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void listMerchantDocuments_returnsSuccess() {
        MerchantDocumentOuterClass.MerchantDocument docProto =
                MerchantDocumentOuterClass.MerchantDocument.newBuilder()
                        .setDocumentId(1)
                        .setMerchantId(100)
                        .setDocumentType("ID_CARD")
                        .setDocumentUrl("http://example.com/doc1")
                        .build();

        MerchantDocumentQuery.ApiResponsePaginationMerchantDocument responseProto =
                MerchantDocumentQuery.ApiResponsePaginationMerchantDocument.newBuilder()
                        .addData(docProto)
                        .setStatus("success")
                        .setMessage("Documents found")
                        .build();

        when(merchantDocumentQueryService.findAll(
                any(MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDocumentDto.ApiResponsePaginationMerchantDocument result =
                service.listMerchantDocuments(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).documentUrl()).isEqualTo("http://example.com/doc1");
    }

    @Test
    void activeMerchantDocuments_returnsSuccess() {
        MerchantDocumentOuterClass.MerchantDocumentDeleteAt docProto =
                MerchantDocumentOuterClass.MerchantDocumentDeleteAt.newBuilder()
                        .setDocumentId(1)
                        .setMerchantId(100)
                        .setDocumentType("ID_CARD")
                        .build();

        MerchantDocumentQuery.ApiResponsePaginationMerchantDocumentAt responseProto =
                MerchantDocumentQuery.ApiResponsePaginationMerchantDocumentAt.newBuilder()
                        .addData(docProto)
                        .setStatus("success")
                        .setMessage("Active documents")
                        .build();

        when(merchantDocumentQueryService.findAllActive(
                any(MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDocumentDto.ApiResponsePaginationMerchantDocumentDeleteAt result =
                service.activeMerchantDocuments(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void trashedMerchantDocuments_returnsSuccess() {
        MerchantDocumentOuterClass.MerchantDocumentDeleteAt docProto =
                MerchantDocumentOuterClass.MerchantDocumentDeleteAt.newBuilder()
                        .setDocumentId(2)
                        .build();

        MerchantDocumentQuery.ApiResponsePaginationMerchantDocumentAt responseProto =
                MerchantDocumentQuery.ApiResponsePaginationMerchantDocumentAt.newBuilder()
                        .addData(docProto)
                        .setStatus("success")
                        .setMessage("Trashed documents")
                        .build();

        when(merchantDocumentQueryService.findAllTrashed(
                any(MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDocumentDto.ApiResponsePaginationMerchantDocumentDeleteAt result =
                service.trashedMerchantDocuments(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getMerchantDocument_returnsSuccess() {
        MerchantDocumentOuterClass.MerchantDocument docProto =
                MerchantDocumentOuterClass.MerchantDocument.newBuilder()
                        .setDocumentId(1)
                        .setMerchantId(100)
                        .build();

        MerchantDocumentOuterClass.ApiResponseMerchantDocument responseProto =
                MerchantDocumentOuterClass.ApiResponseMerchantDocument.newBuilder()
                        .setData(docProto)
                        .setStatus("success")
                        .setMessage("Document found")
                        .build();

        when(merchantDocumentQueryService.findById(
                any(MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDocumentDto.ApiResponseMerchantDocument result =
                service.getMerchantDocument(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().documentId()).isEqualTo(1);
    }

    @Test
    void createMerchantDocument_returnsSuccess() {
        MerchantDocumentOuterClass.MerchantDocument docProto =
                MerchantDocumentOuterClass.MerchantDocument.newBuilder()
                        .setDocumentId(1)
                        .setMerchantId(100)
                        .setDocumentType("ID_CARD")
                        .setDocumentUrl("http://url")
                        .build();

        MerchantDocumentOuterClass.ApiResponseMerchantDocument responseProto =
                MerchantDocumentOuterClass.ApiResponseMerchantDocument.newBuilder()
                        .setData(docProto)
                        .setStatus("success")
                        .setMessage("Created")
                        .build();

        when(merchantDocumentCommandService.create(
                any(MerchantDocumentCommand.CreateMerchantDocumentRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDocumentDto.CreateMerchantDocumentRequest request =
                new MerchantDocumentDto.CreateMerchantDocumentRequest(100, "ID_CARD", "http://url");

        MerchantDocumentDto.ApiResponseMerchantDocument result =
                service.createMerchantDocument(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Created");
    }

    @Test
    void updateMerchantDocument_returnsSuccess() {
        MerchantDocumentOuterClass.MerchantDocument docProto =
                MerchantDocumentOuterClass.MerchantDocument.newBuilder()
                        .setDocumentId(1)
                        .setMerchantId(100)
                        .setDocumentType("PASSPORT")
                        .setDocumentUrl("http://newurl")
                        .build();

        MerchantDocumentOuterClass.ApiResponseMerchantDocument responseProto =
                MerchantDocumentOuterClass.ApiResponseMerchantDocument.newBuilder()
                        .setData(docProto)
                        .setStatus("success")
                        .setMessage("Updated")
                        .build();

        when(merchantDocumentCommandService.update(
                any(MerchantDocumentCommand.UpdateMerchantDocumentRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDocumentDto.UpdateMerchantDocumentRequest request =
                new MerchantDocumentDto.UpdateMerchantDocumentRequest(100, "PASSPORT", "http://newurl", "note", "pending");

        MerchantDocumentDto.ApiResponseMerchantDocument result =
                service.updateMerchantDocument(1, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Updated");
    }

    @Test
    void deleteMerchantDocument_returnsSuccess() {
        MerchantDocumentOuterClass.MerchantDocumentDeleteAt docProto =
                MerchantDocumentOuterClass.MerchantDocumentDeleteAt.newBuilder()
                        .setDocumentId(1)
                        .setDeletedAt(com.google.protobuf.StringValue.of("2024-06-01T00:00:00Z"))
                        .build();

        MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt responseProto =
                MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt.newBuilder()
                        .setData(docProto)
                        .setStatus("success")
                        .setMessage("Trashed")
                        .build();

        when(merchantDocumentCommandService.trashed(
                any(MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDocumentDto.ApiResponseMerchantDocumentDeleteAt result =
                service.deleteMerchantDocument(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Trashed");
    }

    @Test
    void restoreMerchantDocument_returnsSuccess() {
        MerchantDocumentOuterClass.MerchantDocumentDeleteAt docProto =
                MerchantDocumentOuterClass.MerchantDocumentDeleteAt.newBuilder()
                        .setDocumentId(1)
                        .build();

        MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt responseProto =
                MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt.newBuilder()
                        .setData(docProto)
                        .setStatus("success")
                        .setMessage("Restored")
                        .build();

        when(merchantDocumentCommandService.restore(
                any(MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDocumentDto.ApiResponseMerchantDocumentDeleteAt result =
                service.restoreMerchantDocument(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Restored");
    }

    @Test
    void deleteMerchantDocumentPermanent_returnsSuccess() {
        MerchantDocumentCommand.ApiResponseMerchantDocumentDelete responseProto =
                MerchantDocumentCommand.ApiResponseMerchantDocumentDelete.newBuilder()
                        .setStatus("success")
                        .setMessage("Permanently deleted")
                        .build();

        when(merchantDocumentCommandService.deletePermanent(
                any(MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDocumentDto.ApiResponseMerchantDocumentDelete result =
                service.deleteMerchantDocumentPermanent(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Permanently deleted");
    }

    @Test
    void restoreAllMerchantDocuments_returnsSuccess() {
        MerchantDocumentCommand.ApiResponseMerchantDocumentAll responseProto =
                MerchantDocumentCommand.ApiResponseMerchantDocumentAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All restored")
                        .build();

        when(merchantDocumentCommandService.restoreAll(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDocumentDto.ApiResponseMerchantDocumentAll result =
                service.restoreAllMerchantDocuments().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void deleteAllMerchantDocumentsPermanent_returnsSuccess() {
        MerchantDocumentCommand.ApiResponseMerchantDocumentAll responseProto =
                MerchantDocumentCommand.ApiResponseMerchantDocumentAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All permanently deleted")
                        .build();

        when(merchantDocumentCommandService.deleteAllPermanent(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDocumentDto.ApiResponseMerchantDocumentAll result =
                service.deleteAllMerchantDocumentsPermanent().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }
}
