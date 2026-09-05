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

import com.sanedge.gateway.dto.MerchantDto;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.merchant.MutinyMerchantQueryServiceGrpc.MutinyMerchantQueryServiceStub merchantQueryService;

    @Mock
    pb.merchant.MutinyMerchantCommandServiceGrpc.MutinyMerchantCommandServiceStub merchantCommandService;

    MerchantServiceImpl merchantService;

    @BeforeEach
    void setUp() throws Exception {
        merchantService = new MerchantServiceImpl();

        setField(merchantService, "telemetryHelper", telemetryHelper);
        setField(merchantService, "merchantQueryService", merchantQueryService);
        setField(merchantService, "merchantCommandService", merchantCommandService);

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
    void listMerchants_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponse merchantProto = pb.merchant.Merchant.MerchantResponse.newBuilder()
                .setId(1)
                .setUserId(10)
                .setName("Merchant A")
                .setApiKey("key-123")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.merchant.MerchantQuery.ApiResponsePaginationMerchant responseProto =
                pb.merchant.MerchantQuery.ApiResponsePaginationMerchant.newBuilder()
                        .addData(merchantProto)
                        .setStatus("success")
                        .setMessage("Merchants found")
                        .build();

        when(merchantQueryService.findAllMerchant(any(pb.merchant.Merchant.FindAllMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDto.ApiResponsePaginationMerchant result =
                merchantService.listMerchants(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).name()).isEqualTo("Merchant A");
    }

    @Test
    void activeMerchants_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponseDeleteAt merchantProto = pb.merchant.Merchant.MerchantResponseDeleteAt.newBuilder()
                .setId(1)
                .setName("Active Merchant")
                .build();

        pb.merchant.MerchantQuery.ApiResponsePaginationMerchantDeleteAt responseProto =
                pb.merchant.MerchantQuery.ApiResponsePaginationMerchantDeleteAt.newBuilder()
                        .addData(merchantProto)
                        .setStatus("success")
                        .setMessage("Active merchants")
                        .build();

        when(merchantQueryService.findByActive(any(pb.merchant.Merchant.FindAllMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDto.ApiResponsePaginationMerchantDeleteAt result =
                merchantService.activeMerchants(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void trashedMerchants_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponseDeleteAt merchantProto = pb.merchant.Merchant.MerchantResponseDeleteAt.newBuilder()
                .setId(2)
                .setName("Trashed Merchant")
                .setDeletedAt(com.google.protobuf.StringValue.of("2024-06-01T00:00:00Z"))
                .build();

        pb.merchant.MerchantQuery.ApiResponsePaginationMerchantDeleteAt responseProto =
                pb.merchant.MerchantQuery.ApiResponsePaginationMerchantDeleteAt.newBuilder()
                        .addData(merchantProto)
                        .setStatus("success")
                        .setMessage("Trashed merchants")
                        .build();

        when(merchantQueryService.findByTrashed(any(pb.merchant.Merchant.FindAllMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDto.ApiResponsePaginationMerchantDeleteAt result =
                merchantService.trashedMerchants(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getMerchant_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponse merchantProto = pb.merchant.Merchant.MerchantResponse.newBuilder()
                .setId(1)
                .setName("Single Merchant")
                .build();

        pb.merchant.Merchant.ApiResponseMerchant responseProto =
                pb.merchant.Merchant.ApiResponseMerchant.newBuilder()
                        .setData(merchantProto)
                        .setStatus("success")
                        .setMessage("Merchant found")
                        .build();

        when(merchantQueryService.findByIdMerchant(any(pb.merchant.Merchant.FindByIdMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDto.ApiResponseMerchant result = merchantService.getMerchant(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().name()).isEqualTo("Single Merchant");
    }

    @Test
    void getMerchantByApiKey_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponse merchantProto = pb.merchant.Merchant.MerchantResponse.newBuilder()
                .setId(1)
                .setApiKey("api-key-123")
                .build();

        pb.merchant.Merchant.ApiResponseMerchant responseProto =
                pb.merchant.Merchant.ApiResponseMerchant.newBuilder()
                        .setData(merchantProto)
                        .setStatus("success")
                        .setMessage("Merchant by API key")
                        .build();

        when(merchantQueryService.findByApiKey(any(pb.merchant.Merchant.FindByApiKeyRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDto.ApiResponseMerchant result = merchantService.getMerchantByApiKey("api-key-123").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().apiKey()).isEqualTo("api-key-123");
    }

    @Test
    void getMerchantsByUserId_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponse merchantProto = pb.merchant.Merchant.MerchantResponse.newBuilder()
                .setId(1)
                .setUserId(10)
                .build();

        pb.merchant.Merchant.ApiResponsesMerchant responseProto =
                pb.merchant.Merchant.ApiResponsesMerchant.newBuilder()
                        .addData(merchantProto)
                        .setStatus("success")
                        .setMessage("Merchants by user")
                        .build();

        when(merchantQueryService.findByMerchantUserId(any(pb.merchant.Merchant.FindByMerchantUserIdRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDto.ApiResponsesMerchant result = merchantService.getMerchantsByUserId(10).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void createMerchant_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponse merchantProto = pb.merchant.Merchant.MerchantResponse.newBuilder()
                .setId(1)
                .setUserId(10)
                .setName("New Merchant")
                .setApiKey("generated-api-key")
                .build();

        pb.merchant.Merchant.ApiResponseMerchant responseProto =
                pb.merchant.Merchant.ApiResponseMerchant.newBuilder()
                        .setData(merchantProto)
                        .setStatus("success")
                        .setMessage("Merchant created")
                        .build();

        when(merchantCommandService.createMerchant(any(pb.merchant.MerchantCommand.CreateMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDto.CreateMerchantRequest request = new MerchantDto.CreateMerchantRequest(10, "New Merchant");
        MerchantDto.ApiResponseMerchant result = merchantService.createMerchant(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().name()).isEqualTo("New Merchant");
    }

    @Test
    void updateMerchant_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponse merchantProto = pb.merchant.Merchant.MerchantResponse.newBuilder()
                .setId(1)
                .setName("Updated Name")
                .build();

        pb.merchant.Merchant.ApiResponseMerchant responseProto =
                pb.merchant.Merchant.ApiResponseMerchant.newBuilder()
                        .setData(merchantProto)
                        .setStatus("success")
                        .setMessage("Merchant updated")
                        .build();

        when(merchantCommandService.updateMerchant(any(pb.merchant.MerchantCommand.UpdateMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDto.UpdateMerchantRequest request = new MerchantDto.UpdateMerchantRequest("Updated Name");
        MerchantDto.ApiResponseMerchant result = merchantService.updateMerchant(1, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().name()).isEqualTo("Updated Name");
    }

    @Test
    void deleteMerchant_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponseDeleteAt merchantProto = pb.merchant.Merchant.MerchantResponseDeleteAt.newBuilder()
                .setId(1)
                .setName("Merchant")
                .setDeletedAt(com.google.protobuf.StringValue.of("2024-06-01T00:00:00Z"))
                .build();

        pb.merchant.Merchant.ApiResponseMerchantDeleteAt responseProto =
                pb.merchant.Merchant.ApiResponseMerchantDeleteAt.newBuilder()
                        .setData(merchantProto)
                        .setStatus("success")
                        .setMessage("Merchant trashed")
                        .build();

        when(merchantCommandService.trashedMerchant(any(pb.merchant.Merchant.FindByIdMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDto.ApiResponseMerchantDeleteAt result = merchantService.deleteMerchant(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Merchant trashed");
    }

    @Test
    void restoreMerchant_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponseDeleteAt merchantProto = pb.merchant.Merchant.MerchantResponseDeleteAt.newBuilder()
                .setId(1)
                .setName("Merchant")
                .build();

        pb.merchant.Merchant.ApiResponseMerchantDeleteAt responseProto =
                pb.merchant.Merchant.ApiResponseMerchantDeleteAt.newBuilder()
                        .setData(merchantProto)
                        .setStatus("success")
                        .setMessage("Merchant restored")
                        .build();

        when(merchantCommandService.restoreMerchant(any(pb.merchant.Merchant.FindByIdMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDto.ApiResponseMerchantDeleteAt result = merchantService.restoreMerchant(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Merchant restored");
    }

    @Test
    void deleteMerchantPermanent_returnsSuccess() {
        pb.merchant.MerchantCommand.ApiResponseMerchantDelete responseProto =
                pb.merchant.MerchantCommand.ApiResponseMerchantDelete.newBuilder()
                        .setStatus("success")
                        .setMessage("Permanently deleted")
                        .build();

        when(merchantCommandService.deleteMerchantPermanent(any(pb.merchant.Merchant.FindByIdMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDto.ApiResponseMerchantDelete result = merchantService.deleteMerchantPermanent(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Permanently deleted");
    }

    @Test
    void restoreAllMerchants_returnsSuccess() {
        pb.merchant.MerchantCommand.ApiResponseMerchantAll responseProto =
                pb.merchant.MerchantCommand.ApiResponseMerchantAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All merchants restored")
                        .build();

        when(merchantCommandService.restoreAllMerchant(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDto.ApiResponseMerchantAll result = merchantService.restoreAllMerchants().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("All merchants restored");
    }

    @Test
    void deleteAllMerchantsPermanent_returnsSuccess() {
        pb.merchant.MerchantCommand.ApiResponseMerchantAll responseProto =
                pb.merchant.MerchantCommand.ApiResponseMerchantAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All merchants permanently deleted")
                        .build();

        when(merchantCommandService.deleteAllMerchantPermanent(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        MerchantDto.ApiResponseMerchantAll result = merchantService.deleteAllMerchantsPermanent().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("All merchants permanently deleted");
    }
}
