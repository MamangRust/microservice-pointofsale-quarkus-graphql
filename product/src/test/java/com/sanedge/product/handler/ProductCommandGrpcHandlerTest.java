package com.sanedge.product.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.product.domain.response.ProductResponse;
import com.sanedge.product.domain.response.ProductResponseDeleteAt;
import com.sanedge.product.service.ProductCommandService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.product.Product;
import pb.product.ProductCommand;

@ExtendWith(MockitoExtension.class)
class ProductCommandGrpcHandlerTest {

    @Mock
    private ProductCommandService productCommandService;

    private ProductCommandGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProductCommandGrpcHandler();
        handler.productCommandService = productCommandService;
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

    // create
    @Test
    @DisplayName("create - success")
    void create_Success() {
        ProductCommand.CreateProductRequest request = ProductCommand.CreateProductRequest.newBuilder()
                .setMerchantId(10)
                .setCategoryId(100)
                .setName("New Product")
                .setDescription("desc")
                .setPrice(5000)
                .setCountInStock(20)
                .setBrand("Brand")
                .setWeight(500)
                .setImageProduct("img.png")
                .build();

        ProductResponse data = createProductResponse(1L);
        ApiResponse<ProductResponse> apiResp = ApiResponse.success("Product created", data);
        when(productCommandService.createProduct(any())).thenReturn(Uni.createFrom().item(apiResp));

        Product.ApiResponseProduct response = handler.create(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(1);
        assertThat(response.getData().getName()).isEqualTo("Test Product");
        assertThat(response.getData().getPrice()).isEqualTo(5000);
    }

    @Test
    @DisplayName("create - error")
    void create_Error() {
        when(productCommandService.createProduct(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.create(ProductCommand.CreateProductRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // update
    @Test
    @DisplayName("update - success")
    void update_Success() {
        ProductCommand.UpdateProductRequest request = ProductCommand.UpdateProductRequest.newBuilder()
                .setProductId(1)
                .setMerchantId(10)
                .setCategoryId(200)
                .setName("Updated")
                .setDescription("desc")
                .setPrice(6000)
                .setCountInStock(15)
                .setBrand("Brand2")
                .setWeight(600)
                .setImageProduct("img2.png")
                .build();

        ProductResponse data = createProductResponse(1L);
        data.setName("Updated");
        data.setPrice(6000);
        ApiResponse<ProductResponse> apiResp = ApiResponse.success("Product updated", data);
        when(productCommandService.updateProduct(any())).thenReturn(Uni.createFrom().item(apiResp));

        Product.ApiResponseProduct response = handler.update(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getName()).isEqualTo("Updated");
        assertThat(response.getData().getPrice()).isEqualTo(6000);
    }

    @Test
    @DisplayName("update - error")
    void update_Error() {
        when(productCommandService.updateProduct(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.update(ProductCommand.UpdateProductRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // trashedProduct
    @Test
    @DisplayName("trashedProduct - success")
    void trashed_Success() {
        Product.FindByIdProductRequest request = Product.FindByIdProductRequest.newBuilder().setId(1).build();
        ProductResponseDeleteAt data = createProductDeleteAt(1L);
        ApiResponse<ProductResponseDeleteAt> apiResp = ApiResponse.success("Trashed", data);
        when(productCommandService.trashedProduct(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        Product.ApiResponseProductDeleteAt response = handler.trashedProduct(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().hasDeletedAt()).isTrue();
    }

    @Test
    @DisplayName("trashedProduct - error")
    void trashed_Error() {
        when(productCommandService.trashedProduct(anyInt())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.trashedProduct(Product.FindByIdProductRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // restoreProduct
    @Test
    @DisplayName("restoreProduct - success")
    void restore_Success() {
        Product.FindByIdProductRequest request = Product.FindByIdProductRequest.newBuilder().setId(1).build();
        ProductResponseDeleteAt data = createProductDeleteAt(1L);
        data.setDeletedAt(null);
        ApiResponse<ProductResponseDeleteAt> apiResp = ApiResponse.success("Restored", data);
        when(productCommandService.restoreProduct(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        Product.ApiResponseProductDeleteAt response = handler.restoreProduct(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().hasDeletedAt()).isFalse();
    }

    @Test
    @DisplayName("restoreProduct - error")
    void restore_Error() {
        when(productCommandService.restoreProduct(anyInt())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.restoreProduct(Product.FindByIdProductRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // deleteProductPermanent
    @Test
    @DisplayName("deleteProductPermanent - success")
    void deletePermanent_Success() {
        Product.FindByIdProductRequest request = Product.FindByIdProductRequest.newBuilder().setId(1).build();
        ApiResponse<Boolean> apiResp = ApiResponse.success("Permanently deleted", true);
        when(productCommandService.deleteProductPermanent(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        ProductCommand.ApiResponseProductDelete response = handler.deleteProductPermanent(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Permanently deleted");
    }

    @Test
    @DisplayName("deleteProductPermanent - error")
    void deletePermanent_Error() {
        when(productCommandService.deleteProductPermanent(anyInt())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.deleteProductPermanent(Product.FindByIdProductRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // restoreAllProduct
    @Test
    @DisplayName("restoreAllProduct - success")
    void restoreAll_Success() {
        ApiResponse<Boolean> apiResp = ApiResponse.success("All restored", true);
        when(productCommandService.restoreAllProducts()).thenReturn(Uni.createFrom().item(apiResp));

        ProductCommand.ApiResponseProductAll response = handler.restoreAllProduct(Empty.getDefaultInstance()).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All restored");
    }

    @Test
    @DisplayName("restoreAllProduct - error")
    void restoreAll_Error() {
        when(productCommandService.restoreAllProducts()).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.restoreAllProduct(Empty.getDefaultInstance()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // deleteAllProductPermanent
    @Test
    @DisplayName("deleteAllProductPermanent - success")
    void deleteAll_Success() {
        ApiResponse<Boolean> apiResp = ApiResponse.success("All permanently deleted", true);
        when(productCommandService.deleteAllProductsPermanent()).thenReturn(Uni.createFrom().item(apiResp));

        ProductCommand.ApiResponseProductAll response = handler.deleteAllProductPermanent(Empty.getDefaultInstance()).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All permanently deleted");
    }

    @Test
    @DisplayName("deleteAllProductPermanent - error")
    void deleteAll_Error() {
        when(productCommandService.deleteAllProductsPermanent()).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.deleteAllProductPermanent(Empty.getDefaultInstance()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // edge cases
    @Test
    @DisplayName("create - null data")
    void create_NullData() {
        when(productCommandService.createProduct(any())).thenReturn(Uni.createFrom().item(ApiResponse.success("Created", null)));
        Product.ApiResponseProduct response = handler.create(ProductCommand.CreateProductRequest.newBuilder().build()).await().indefinitely();
        assertThat(response.hasData()).isFalse();
    }

    @Test
    @DisplayName("update - null data")
    void update_NullData() {
        when(productCommandService.updateProduct(any())).thenReturn(Uni.createFrom().item(ApiResponse.success("Updated", null)));
        Product.ApiResponseProduct response = handler.update(ProductCommand.UpdateProductRequest.newBuilder().build()).await().indefinitely();
        assertThat(response.hasData()).isFalse();
    }
}