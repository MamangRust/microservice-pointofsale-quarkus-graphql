package com.sanedge.product.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.product.domain.response.ProductResponse;
import com.sanedge.product.domain.response.ProductResponseDeleteAt;
import com.sanedge.product.service.ProductQueryService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.product.Product;
import pb.product.ProductQuery;

@ExtendWith(MockitoExtension.class)
class ProductQueryGrpcHandlerTest {

    @Mock
    private ProductQueryService productQueryService;

    private ProductQueryGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProductQueryGrpcHandler();
        handler.productQueryService = productQueryService;
    }

    // helpers
    private ProductResponse createProductResponse(Long id) {
        ProductResponse r = new ProductResponse();
        r.setId(id);
        r.setMerchantId(10);
        r.setCategoryId(100);
        r.setName("Test Product");
        r.setDescription("desc");
        r.setPrice(5000);
        r.setCountInStock(20);
        r.setBrand("Brand");
        r.setWeight(500);
        r.setSlugProduct("test-product");
        r.setImageProduct("img.png");
        r.setCreatedAt(LocalDateTime.now().toString());
        r.setUpdatedAt(LocalDateTime.now().toString());
        return r;
    }

    private ProductResponseDeleteAt createProductDeleteAt(Long id) {
        ProductResponseDeleteAt r = new ProductResponseDeleteAt();
        r.setId(id);
        r.setMerchantId(10);
        r.setCategoryId(100);
        r.setName("Trashed");
        r.setDescription("desc");
        r.setPrice(5000);
        r.setCountInStock(20);
        r.setBrand("Brand");
        r.setWeight(500);
        r.setSlugProduct("trashed");
        r.setImageProduct("img.png");
        r.setCreatedAt(LocalDateTime.now().toString());
        r.setUpdatedAt(LocalDateTime.now().toString());
        r.setDeletedAt(LocalDateTime.now().toString());
        return r;
    }

    // findAll
    @Test
    @DisplayName("findAll - success")
    void findAll_Success() {
        Product.FindAllProductRequest request = Product.FindAllProductRequest.newBuilder()
                .setPage(1).setPageSize(10).build();
        ProductResponse data = createProductResponse(1L);
        ApiResponsePagination<List<ProductResponse>> apiResp = new ApiResponsePagination<>(
                "success", "Products retrieved", List.of(data), null);
        when(productQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

        ProductQuery.ApiResponsePaginationProduct response = handler.findAll(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getData(0).getName()).isEqualTo("Test Product");
        assertThat(response.getData(0).getPrice()).isEqualTo(5000);
    }

    @Test
    @DisplayName("findAll - error")
    void findAll_Error() {
        when(productQueryService.findAll(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
        try {
            handler.findAll(Product.FindAllProductRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e).isNotNull();
        }
    }

    // findByMerchant
    @Test
    @DisplayName("findByMerchant - success")
    void findByMerchant_Success() {
        Product.FindAllProductMerchantRequest request = Product.FindAllProductMerchantRequest.newBuilder()
                .setMerchantId(10).setPage(1).setPageSize(10).build();
        ProductResponse data = createProductResponse(1L);
        ApiResponsePagination<List<ProductResponse>> apiResp = new ApiResponsePagination<>(
                "success", "Products by merchant", List.of(data), null);
        when(productQueryService.findByMerchant(any())).thenReturn(Uni.createFrom().item(apiResp));

        ProductQuery.ApiResponsePaginationProduct response = handler.findByMerchant(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData(0).getMerchantId()).isEqualTo(10);
    }

    @Test
    @DisplayName("findByMerchant - error")
    void findByMerchant_Error() {
        when(productQueryService.findByMerchant(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findByMerchant(Product.FindAllProductMerchantRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // findByCategory
    @Test
    @DisplayName("findByCategory - success")
    void findByCategory_Success() {
        Product.FindAllProductCategoryRequest request = Product.FindAllProductCategoryRequest.newBuilder()
                .setCategoryName("Electronics").setPage(1).setPageSize(10).build();
        ProductResponse data = createProductResponse(1L);
        ApiResponsePagination<List<ProductResponse>> apiResp = new ApiResponsePagination<>(
                "success", "Products by category", List.of(data), null);
        when(productQueryService.findByCategoryName(any())).thenReturn(Uni.createFrom().item(apiResp));

        ProductQuery.ApiResponsePaginationProduct response = handler.findByCategory(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
    }

    @Test
    @DisplayName("findByCategory - error")
    void findByCategory_Error() {
        when(productQueryService.findByCategoryName(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findByCategory(Product.FindAllProductCategoryRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // findById
    @Test
    @DisplayName("findById - success")
    void findById_Success() {
        Product.FindByIdProductRequest request = Product.FindByIdProductRequest.newBuilder().setId(1).build();
        ProductResponse data = createProductResponse(1L);
        ApiResponse<ProductResponse> apiResp = ApiResponse.success("Product found", data);
        when(productQueryService.findById(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

        Product.ApiResponseProduct response = handler.findById(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("findById - error")
    void findById_Error() {
        when(productQueryService.findById(anyLong())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findById(Product.FindByIdProductRequest.newBuilder().setId(1).build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // findByActive
    @Test
    @DisplayName("findByActive - success")
    void findByActive_Success() {
        Product.FindAllProductRequest request = Product.FindAllProductRequest.newBuilder().setPage(1).build();
        ProductResponseDeleteAt data = createProductDeleteAt(1L);
        ApiResponsePagination<List<ProductResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                "success", "Active products", List.of(data), null);
        when(productQueryService.findActiveProducts(any())).thenReturn(Uni.createFrom().item(apiResp));

        ProductQuery.ApiResponsePaginationProductDeleteAt response = handler.findByActive(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getData(0).hasDeletedAt()).isTrue();
    }

    @Test
    @DisplayName("findByActive - error")
    void findByActive_Error() {
        when(productQueryService.findActiveProducts(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findByActive(Product.FindAllProductRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // findByTrashed
    @Test
    @DisplayName("findByTrashed - success")
    void findByTrashed_Success() {
        Product.FindAllProductRequest request = Product.FindAllProductRequest.newBuilder().build();
        ProductResponseDeleteAt data = createProductDeleteAt(2L);
        ApiResponsePagination<List<ProductResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                "success", "Trashed products", List.of(data), null);
        when(productQueryService.findTrashedProducts(any())).thenReturn(Uni.createFrom().item(apiResp));

        ProductQuery.ApiResponsePaginationProductDeleteAt response = handler.findByTrashed(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData(0).hasDeletedAt()).isTrue();
    }

    @Test
    @DisplayName("findByTrashed - error")
    void findByTrashed_Error() {
        when(productQueryService.findTrashedProducts(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findByTrashed(Product.FindAllProductRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // edge cases
    @Test
    @DisplayName("findAll - empty list")
    void findAll_Empty() {
        when(productQueryService.findAll(any()))
                .thenReturn(Uni.createFrom().item(new ApiResponsePagination<>("success", "No products", List.of(), null)));
        ProductQuery.ApiResponsePaginationProduct response = handler.findAll(
                Product.FindAllProductRequest.newBuilder().build()).await().indefinitely();
        assertThat(response.getDataCount()).isZero();
    }

    @Test
    @DisplayName("findById - null data")
    void findById_NullData() {
        when(productQueryService.findById(anyLong())).thenReturn(Uni.createFrom().item(ApiResponse.success("No data", null)));
        Product.ApiResponseProduct response = handler.findById(
                Product.FindByIdProductRequest.newBuilder().setId(1).build()).await().indefinitely();
        assertThat(response.hasData()).isFalse();
    }
}