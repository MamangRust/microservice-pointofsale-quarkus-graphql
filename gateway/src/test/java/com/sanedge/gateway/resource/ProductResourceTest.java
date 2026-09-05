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

import com.sanedge.gateway.dto.ProductDto;
import com.sanedge.gateway.service.ProductService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class ProductResourceTest {

    @Mock ProductService productService;
    ProductResource resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new ProductResource();
        Field f = ProductResource.class.getDeclaredField("productService");
        f.setAccessible(true);
        f.set(resource, productService);
    }

    @Test void findAll_ok() {
        when(productService.findAll(anyInt(), anyInt(), anyString()))
            .thenReturn(Uni.createFrom().item(new ProductDto.ApiResponsePaginationProduct("success", "ok", List.of(), null)));
        assertThat(resource.findAll(1, 10, "").await().indefinitely().status()).isEqualTo("success");
    }

    @Test void findById_ok() {
        when(productService.findById(anyInt()))
            .thenReturn(Uni.createFrom().item(new ProductDto.ApiResponseProduct("success", "ok", null)));
        assertThat(resource.findById(1).await().indefinitely().status()).isEqualTo("success");
    }

    @Test void createProduct_ok() {
        when(productService.createProduct(any()))
            .thenReturn(Uni.createFrom().item(new ProductDto.ApiResponseProduct("success", "created", null)));
        assertThat(resource.createProduct(new ProductDto.CreateProductRequest(1, 1, "p", "d", 100, 10, "b", 1, 4.5f, "img.jpg", "123")).await().indefinitely().message()).isEqualTo("created");
    }

    @Test void deleteProduct_ok() {
        when(productService.deleteProduct(anyInt()))
            .thenReturn(Uni.createFrom().item(new ProductDto.ApiResponseProductDeleteAt("success", "trashed", null)));
        assertThat(resource.deleteProduct(1).await().indefinitely().message()).isEqualTo("trashed");
    }

    @Test void restoreProduct_ok() {
        when(productService.restoreProduct(anyInt()))
            .thenReturn(Uni.createFrom().item(new ProductDto.ApiResponseProductDeleteAt("success", "restored", null)));
        assertThat(resource.restoreProduct(1).await().indefinitely().message()).isEqualTo("restored");
    }
}
