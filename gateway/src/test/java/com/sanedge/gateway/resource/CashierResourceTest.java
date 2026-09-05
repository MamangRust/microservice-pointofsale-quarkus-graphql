package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.CashierDto;
import com.sanedge.gateway.service.CashierService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class CashierResourceTest {

    @Mock CashierService cashierService;
    CashierResource resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new CashierResource();
        Field f = CashierResource.class.getDeclaredField("cashierService");
        f.setAccessible(true);
        f.set(resource, cashierService);
    }

    @Test void listCashiers_ok() {
        when(cashierService.listCashiers(anyInt(), anyInt(), anyString()))
            .thenReturn(Uni.createFrom().item(new CashierDto.ApiResponsePaginationCashier("success", "ok", List.of(), null)));
        assertThat(resource.listCashiers(1, 10, "").await().indefinitely().status()).isEqualTo("success");
    }

    @Test void getCashier_ok() {
        when(cashierService.getCashier(anyInt()))
            .thenReturn(Uni.createFrom().item(new CashierDto.ApiResponseCashier("success", "ok", null)));
        assertThat(resource.getCashier(1).await().indefinitely().status()).isEqualTo("success");
    }

    @Test void createCashier_ok() {
        when(cashierService.createCashier(any()))
            .thenReturn(Uni.createFrom().item(new CashierDto.ApiResponseCashier("success", "created", null)));
        assertThat(resource.createCashier(new CashierDto.CreateCashierRequest(1, 1, "123")).await().indefinitely().message()).isEqualTo("created");
    }

    @Test void deleteCashier_ok() {
        when(cashierService.deleteCashier(anyInt()))
            .thenReturn(Uni.createFrom().item(new CashierDto.ApiResponseCashierDeleteAt("success", "trashed", null)));
        assertThat(resource.deleteCashier(1).await().indefinitely().message()).isEqualTo("trashed");
    }

    @Test void restoreCashier_ok() {
        when(cashierService.restoreCashier(anyInt()))
            .thenReturn(Uni.createFrom().item(new CashierDto.ApiResponseCashierDeleteAt("success", "restored", null)));
        assertThat(resource.restoreCashier(1).await().indefinitely().message()).isEqualTo("restored");
    }
}
