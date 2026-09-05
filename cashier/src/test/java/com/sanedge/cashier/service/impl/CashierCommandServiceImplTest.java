package com.sanedge.cashier.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.cashier.domain.requests.CreateCashierRequest;
import com.sanedge.cashier.domain.requests.UpdateCashierRequest;
import com.sanedge.cashier.domain.response.CashierResponse;
import com.sanedge.cashier.domain.response.CashierResponseDeleteAt;
import com.sanedge.cashier.entity.Cashier;
import com.sanedge.cashier.repository.CashierCommandRepository;
import com.sanedge.cashier.repository.CashierQueryRepository;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class CashierCommandServiceImplTest {

    @Mock
    private CashierCommandRepository cashierCommandRepository;

    @Mock
    private CashierQueryRepository cashierQueryRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    @Mock
    private pb.merchant.MutinyMerchantQueryServiceGrpc.MutinyMerchantQueryServiceStub merchantQueryService;

    @Mock
    private pb.user.MutinyUserQueryServiceGrpc.MutinyUserQueryServiceStub userQueryService;

    private CashierCommandServiceImpl cashierCommandService;

    @BeforeEach
    void setUp() {
        cashierCommandService = new CashierCommandServiceImpl(
                cashierCommandRepository,
                cashierQueryRepository,
                redisService,
                tracingMetrics);

        // Inject @GrpcClient fields via reflection
        injectField(cashierCommandService, "merchantQueryService", merchantQueryService);
        injectField(cashierCommandService, "userQueryService", userQueryService);

        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics)
                .traceAndMeasure(
                        anyString(),
                        anyString(),
                        any(Attributes.class),
                        any());
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

    private Cashier createMockCashier(Long id, String name, Long merchantId, Long userId) {
        Cashier cashier = new Cashier();
        cashier.setCashierId(id);
        cashier.setName(name);
        cashier.setMerchantId(merchantId);
        cashier.setUserId(userId);
        cashier.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        cashier.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        return cashier;
    }

    @Test
    void updateCashier_success_updatesName() {
        Cashier existing = createMockCashier(1L, "OldName", 1L, 1L);
        UpdateCashierRequest req = new UpdateCashierRequest();
        req.setCashierId(1);
        req.setName("NewName");

        when(cashierCommandRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(existing));
        lenient().when(cashierCommandRepository.persist(any(Cashier.class))).thenReturn(Uni.createFrom().item(existing));
        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<CashierResponse> response = cashierCommandService.updateCashier(req).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Cashier updated successfully");
    }

    @Test
    void updateCashier_notFound_throwsException() {
        UpdateCashierRequest req = new UpdateCashierRequest();
        req.setCashierId(999);
        req.setName("NewName");

        when(cashierCommandRepository.findById(anyLong())).thenReturn(Uni.createFrom().nullItem());

        try {
            cashierCommandService.updateCashier(req).await().indefinitely();
            org.junit.jupiter.api.Assertions.fail("Expected exception");
        } catch (com.sanedge.common.exception.ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("not found");
        }
    }

    @Test
    void trashedCashier_success() {
        Cashier cashier = createMockCashier(1L, "TrashMe", 1L, 1L);
        when(cashierCommandRepository.trashed(1L)).thenReturn(Uni.createFrom().item(cashier));
        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<CashierResponseDeleteAt> response = cashierCommandService.trashedCashier(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Cashier trashed successfully");
        assertThat(response.data()).isNotNull();
    }

    @Test
    void trashedCashier_notFound_throwsException() {
        when(cashierCommandRepository.trashed(999L)).thenReturn(Uni.createFrom().nullItem());

        try {
            cashierCommandService.trashedCashier(999L).await().indefinitely();
            org.junit.jupiter.api.Assertions.fail("Expected exception");
        } catch (com.sanedge.common.exception.ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("not found");
        }
    }

    @Test
    void restoreCashier_success() {
        Cashier cashier = createMockCashier(1L, "RestoreMe", 1L, 1L);
        when(cashierCommandRepository.restore(1L)).thenReturn(Uni.createFrom().item(cashier));
        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<CashierResponseDeleteAt> response = cashierCommandService.restoreCashier(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Cashier restored successfully");
    }

    @Test
    void restoreCashier_notFound_throwsException() {
        when(cashierCommandRepository.restore(999L)).thenReturn(Uni.createFrom().nullItem());

        try {
            cashierCommandService.restoreCashier(999L).await().indefinitely();
            org.junit.jupiter.api.Assertions.fail("Expected exception");
        } catch (com.sanedge.common.exception.ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("not found");
        }
    }

    @Test
    void deletePermanent_success() {
        Cashier cashier = createMockCashier(1L, "DeleteMe", 1L, 1L);
        when(cashierCommandRepository.deletePermanent(1L)).thenReturn(Uni.createFrom().item(cashier));
        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<Boolean> response = cashierCommandService.deleteCashierPermanent(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.data()).isTrue();
    }

    @Test
    void deletePermanent_notFound_throwsException() {
        when(cashierCommandRepository.deletePermanent(999L)).thenReturn(Uni.createFrom().nullItem());

        try {
            cashierCommandService.deleteCashierPermanent(999L).await().indefinitely();
            org.junit.jupiter.api.Assertions.fail("Expected exception");
        } catch (com.sanedge.common.exception.ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("not found");
        }
    }

    @Test
    void restoreAllCashier_success() {
        when(cashierCommandRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(true));

        ApiResponse<Boolean> response = cashierCommandService.restoreAllCashier().await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.data()).isTrue();
    }

    @Test
    void restoreAllCashier_noTrashed_throwsException() {
        when(cashierCommandRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));

        try {
            cashierCommandService.restoreAllCashier().await().indefinitely();
            org.junit.jupiter.api.Assertions.fail("Expected exception");
        } catch (com.sanedge.common.exception.ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("No trashed cashiers");
        }
    }

    @Test
    void deleteAllCashierPermanent_success() {
        when(cashierCommandRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));

        ApiResponse<Boolean> response = cashierCommandService.deleteAllCashierPermanent().await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.data()).isTrue();
    }

    @Test
    void deleteAllCashierPermanent_noTrashed_throwsException() {
        when(cashierCommandRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));

        try {
            cashierCommandService.deleteAllCashierPermanent().await().indefinitely();
            org.junit.jupiter.api.Assertions.fail("Expected exception");
        } catch (com.sanedge.common.exception.ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("No trashed cashiers");
        }
    }
}
