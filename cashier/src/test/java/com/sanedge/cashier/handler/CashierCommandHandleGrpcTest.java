package com.sanedge.cashier.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.cashier.domain.response.CashierResponse;
import com.sanedge.cashier.domain.response.CashierResponseDeleteAt;
import com.sanedge.cashier.service.CashierCommandService;
import com.sanedge.common.domain.response.ApiResponse;

import io.smallrye.mutiny.Uni;
import pb.cashier.Cashier.ApiResponseCashier;
import pb.cashier.Cashier.ApiResponseCashierDeleteAt;
import pb.cashier.Cashier.FindByIdCashierRequest;
import pb.cashier.CashierCommand.ApiResponseCashierAll;
import pb.cashier.CashierCommand.ApiResponseCashierDelete;

@ExtendWith(MockitoExtension.class)
class CashierCommandHandleGrpcTest {

    @Mock
    private CashierCommandService cashierCommandService;

    private CashierCommandGrpcHandler commandHandler;

    @BeforeEach
    void setUp() {
        commandHandler = new CashierCommandGrpcHandler();
        injectField(commandHandler, "cashierCommandService", cashierCommandService);
    }

    @Test
    void createCashier_success_mapsToProtoCorrectly() {
        pb.cashier.Cashier.CreateCashierRequest request = pb.cashier.Cashier.CreateCashierRequest.newBuilder()
                .setName("Cashier1").setMerchantId(1).setUserId(2).build();
        CashierResponse domainRes = createDomainCashierResponse();

        when(cashierCommandService.createCashier(any()))
                .thenReturn(Uni.createFrom().item(ApiResponse.success("Success", domainRes)));

        ApiResponseCashier response = commandHandler.createCashier(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getName()).isEqualTo("Cashier1");
        assertThat(response.getData().getMerchantId()).isEqualTo(1);
    }

    @Test
    void updateCashier_success_mapsToProtoCorrectly() {
        pb.cashier.Cashier.UpdateCashierRequest request = pb.cashier.Cashier.UpdateCashierRequest.newBuilder()
                .setCashierId(1).setName("Updated").build();
        CashierResponse domainRes = createDomainCashierResponse();

        when(cashierCommandService.updateCashier(any()))
                .thenReturn(Uni.createFrom().item(ApiResponse.success("Updated", domainRes)));

        ApiResponseCashier response = commandHandler.updateCashier(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getName()).isEqualTo("Cashier1");
    }

    @Test
    void trashedCashier_success_mapsDeletedAtToStringValue() {
        FindByIdCashierRequest request = FindByIdCashierRequest.newBuilder().setId(1).build();
        CashierResponseDeleteAt domainRes = createDomainCashierResponseDeleteAt();

        when(cashierCommandService.trashedCashier(1L))
                .thenReturn(Uni.createFrom().item(ApiResponse.success("Trashed", domainRes)));

        ApiResponseCashierDeleteAt response = commandHandler.trashedCashier(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().hasDeletedAt()).isTrue();
        assertThat(response.getData().getDeletedAt().getValue()).contains("2023-10-10");
    }

    @Test
    void restoreCashier_success_mapsToProtoCorrectly() {
        FindByIdCashierRequest request = FindByIdCashierRequest.newBuilder().setId(1).build();
        CashierResponseDeleteAt domainRes = createDomainCashierResponseDeleteAt();

        when(cashierCommandService.restoreCashier(1L))
                .thenReturn(Uni.createFrom().item(ApiResponse.success("Restored", domainRes)));

        ApiResponseCashierDeleteAt response = commandHandler.restoreCashier(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
    }

    @Test
    void deleteCashierPermanent_success_returnsVoidProto() {
        FindByIdCashierRequest request = FindByIdCashierRequest.newBuilder().setId(1).build();

        when(cashierCommandService.deleteCashierPermanent(1L))
                .thenReturn(Uni.createFrom().item(ApiResponse.success("Deleted", true)));

        ApiResponseCashierDelete response = commandHandler.deleteCashierPermanent(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Deleted");
    }

    @Test
    void restoreAllCashier_success_returnsVoidProto() {
        when(cashierCommandService.restoreAllCashier())
                .thenReturn(Uni.createFrom().item(ApiResponse.success("All Restored", true)));

        ApiResponseCashierAll response = commandHandler.restoreAllCashier(com.google.protobuf.Empty.getDefaultInstance())
                .await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
    }

    @Test
    void deleteAllCashierPermanent_success_returnsVoidProto() {
        when(cashierCommandService.deleteAllCashierPermanent())
                .thenReturn(Uni.createFrom().item(ApiResponse.success("All Deleted", true)));

        ApiResponseCashierAll response = commandHandler
                .deleteAllCashierPermanent(com.google.protobuf.Empty.getDefaultInstance()).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject " + fieldName, e);
        }
    }

    private CashierResponse createDomainCashierResponse() {
        return CashierResponse.builder()
                .id(1)
                .merchantId(1)
                .name("Cashier1")
                .createdAt(LocalDateTime.now().toString())
                .updatedAt(LocalDateTime.now().toString())
                .build();
    }

    private CashierResponseDeleteAt createDomainCashierResponseDeleteAt() {
        return CashierResponseDeleteAt.builder()
                .id(1)
                .merchantId(1)
                .name("Cashier1")
                .createdAt(LocalDateTime.now().toString())
                .updatedAt(LocalDateTime.now().toString())
                .deletedAt(LocalDateTime.of(2023, 10, 10, 10, 10).toString())
                .build();
    }
}
