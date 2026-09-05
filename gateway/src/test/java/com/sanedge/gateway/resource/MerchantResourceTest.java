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

import com.sanedge.gateway.dto.MerchantDto;
import com.sanedge.gateway.service.MerchantService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class MerchantResourceTest {

    @Mock MerchantService merchantService;
    MerchantResource resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new MerchantResource();
        Field f = MerchantResource.class.getDeclaredField("merchantService");
        f.setAccessible(true);
        f.set(resource, merchantService);
    }

    @Test void listMerchants_ok() {
        when(merchantService.listMerchants(anyInt(), anyInt(), anyString()))
            .thenReturn(Uni.createFrom().item(new MerchantDto.ApiResponsePaginationMerchant("success", "ok", List.of(), null)));
        assertThat(resource.listMerchants(1, 10, "").await().indefinitely().status()).isEqualTo("success");
    }

    @Test void getMerchant_ok() {
        when(merchantService.getMerchant(anyInt()))
            .thenReturn(Uni.createFrom().item(new MerchantDto.ApiResponseMerchant("success", "ok", null)));
        assertThat(resource.getMerchant(1).await().indefinitely().status()).isEqualTo("success");
    }

    @Test void createMerchant_ok() {
        when(merchantService.createMerchant(any()))
            .thenReturn(Uni.createFrom().item(new MerchantDto.ApiResponseMerchant("success", "created", null)));
        assertThat(resource.createMerchant(new MerchantDto.CreateMerchantRequest(1, "T")).await().indefinitely().message()).isEqualTo("created");
    }

    @Test void deleteMerchant_ok() {
        when(merchantService.deleteMerchant(anyInt()))
            .thenReturn(Uni.createFrom().item(new MerchantDto.ApiResponseMerchantDeleteAt("success", "trashed", null)));
        assertThat(resource.deleteMerchant(1).await().indefinitely().message()).isEqualTo("trashed");
    }

    @Test void restoreMerchant_ok() {
        when(merchantService.restoreMerchant(anyInt()))
            .thenReturn(Uni.createFrom().item(new MerchantDto.ApiResponseMerchantDeleteAt("success", "restored", null)));
        assertThat(resource.restoreMerchant(1).await().indefinitely().message()).isEqualTo("restored");
    }
}
