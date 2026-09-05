package com.sanedge.product.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.product.domain.requests.FindAllProductByCategoryRequest;
import com.sanedge.product.domain.requests.FindAllProductByMerchantRequest;
import com.sanedge.product.domain.requests.FindAllProductRequest;
import com.sanedge.product.domain.response.ProductResponse;
import com.sanedge.product.domain.response.ProductResponseDeleteAt;
import com.sanedge.product.entity.Product;
import com.sanedge.product.repository.ProductQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class ProductQueryImplServiceTest {

    @Mock
    private ProductQueryRepository productQueryRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private ProductQueryImplService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new ProductQueryImplService(productQueryRepository, redisService, objectMapper, tracingMetrics);

        // Lenient stub to execute the supplier directly
        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
    }

    private Product createMockProduct(Long id) {
        Product p = new Product();
        p.setProductId(id);
        p.setName("Test Product");
        p.setMerchantId(10L);
        p.setCategoryId(100L);
        p.setPrice(5000);
        p.setCountInStock(20);
        p.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        p.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return p;
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }

    private FindAllProductRequest findAllReq(int page, int size, String search) {
        FindAllProductRequest req = new FindAllProductRequest();
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search == null ? "" : search);
        return req;
    }

    @Nested
    @DisplayName("findAll tests")
    class FindAllTests {
        @Test void cacheMiss_fetchesFromDb() {
            FindAllProductRequest req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(productQueryRepository.findAllProducts(any(FindAllProductRequest.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockProduct(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong())).thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<ProductResponse>> result = service.findAll(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
            assertThat(result.data().get(0).getName()).isEqualTo("Test Product");
        }
        @Test void cacheHit_returnsCached() {
            FindAllProductRequest req = findAllReq(1, 10, "");
            ApiResponsePagination<List<ProductResponse>> cached = new ApiResponsePagination<>(
                    "success", "Products retrieved successfully",
                    List.of(ProductResponse.from(createMockProduct(1L))), null);
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(toJson(cached)));

            ApiResponsePagination<List<ProductResponse>> result = service.findAll(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findActiveProducts tests")
    class FindActiveProductsTests {
        @Test void cacheMiss_fetchesFromDb() {
            FindAllProductRequest req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(productQueryRepository.findActiveProducts(any(FindAllProductRequest.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockProduct(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong())).thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<ProductResponseDeleteAt>> result = service.findActiveProducts(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findTrashedProducts tests")
    class FindTrashedProductsTests {
        @Test void cacheMiss_fetchesFromDb() {
            FindAllProductRequest req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(productQueryRepository.findTrashedProducts(any(FindAllProductRequest.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockProduct(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong())).thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<ProductResponseDeleteAt>> result = service.findTrashedProducts(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findByMerchant tests")
    class FindByMerchantTests {
        @Test void cacheMiss_fetchesFromDb() {
            FindAllProductByMerchantRequest req = new FindAllProductByMerchantRequest();
            req.setMerchantId(10); req.setPage(1); req.setPageSize(10); req.setSearch("");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(productQueryRepository.findProductsByMerchant(any(FindAllProductByMerchantRequest.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockProduct(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong())).thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<ProductResponse>> result = service.findByMerchant(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findByCategoryName tests")
    class FindByCategoryNameTests {
        @Test void cacheMiss_fetchesFromDb() {
            FindAllProductByCategoryRequest req = new FindAllProductByCategoryRequest();
            req.setCategoryName("Electronics"); req.setPage(1); req.setPageSize(10); req.setSearch("");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(productQueryRepository.findProductsByCategory(any(FindAllProductByCategoryRequest.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockProduct(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong())).thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<ProductResponse>> result = service.findByCategoryName(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findById tests")
    class FindByIdTests {
        @Test void cacheMiss_fetchesFromDb() {
            Long id = 1L;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(productQueryRepository.findProductById(anyLong())).thenReturn(Uni.createFrom().item(createMockProduct(id)));
            when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<ProductResponse> result = service.findById(id).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data().getId()).isEqualTo(id);
        }
        @Test void notFound_returnsError() {
            Long id = 999L;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(productQueryRepository.findProductById(anyLong())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<ProductResponse> result = service.findById(id).await().indefinitely();
            assertThat(result.status()).isEqualTo("error");
            assertThat(result.message()).contains("Product not found");
        }
    }
}