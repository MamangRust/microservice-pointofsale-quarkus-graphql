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

import com.sanedge.gateway.dto.MerchantDocumentDto;
import com.sanedge.gateway.service.MerchantDocumentService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class MerchantDocumentResourceTest {

    @Mock MerchantDocumentService merchantDocumentService;
    MerchantDocumentResource resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new MerchantDocumentResource();
        Field f = MerchantDocumentResource.class.getDeclaredField("merchantDocumentService");
        f.setAccessible(true);
        f.set(resource, merchantDocumentService);
    }

    @Test void listMerchantDocuments_ok() {
        when(merchantDocumentService.listMerchantDocuments(anyInt(), anyInt(), anyString()))
            .thenReturn(Uni.createFrom().item(new MerchantDocumentDto.ApiResponsePaginationMerchantDocument("success", "ok", List.of(), null)));
        assertThat(resource.listMerchantDocuments(1, 10, "").await().indefinitely().status()).isEqualTo("success");
    }

    @Test void getMerchantDocument_ok() {
        when(merchantDocumentService.getMerchantDocument(anyInt()))
            .thenReturn(Uni.createFrom().item(new MerchantDocumentDto.ApiResponseMerchantDocument("success", "ok", null)));
        assertThat(resource.getMerchantDocument(1).await().indefinitely().status()).isEqualTo("success");
    }

    @Test void createMerchantDocument_ok() {
        when(merchantDocumentService.createMerchantDocument(any()))
            .thenReturn(Uni.createFrom().item(new MerchantDocumentDto.ApiResponseMerchantDocument("success", "created", null)));
        assertThat(resource.createMerchantDocument(new MerchantDocumentDto.CreateMerchantDocumentRequest(1, "t", "u")).await().indefinitely().message()).isEqualTo("created");
    }

    @Test void deleteMerchantDocument_ok() {
        when(merchantDocumentService.deleteMerchantDocument(anyInt()))
            .thenReturn(Uni.createFrom().item(new MerchantDocumentDto.ApiResponseMerchantDocumentDeleteAt("success", "trashed", null)));
        assertThat(resource.deleteMerchantDocument(1).await().indefinitely().message()).isEqualTo("trashed");
    }

    @Test void restoreMerchantDocument_ok() {
        when(merchantDocumentService.restoreMerchantDocument(anyInt()))
            .thenReturn(Uni.createFrom().item(new MerchantDocumentDto.ApiResponseMerchantDocumentDeleteAt("success", "restored", null)));
        assertThat(resource.restoreMerchantDocument(1).await().indefinitely().message()).isEqualTo("restored");
    }
}
