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

import com.sanedge.gateway.dto.ProductDto;
import com.sanedge.gateway.service.FileService;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    FileService fileService;

    @Mock
    pb.product.MutinyProductServiceGrpc.MutinyProductServiceStub productQueryService;

    @Mock
    pb.product.MutinyProductCommandServiceGrpc.MutinyProductCommandServiceStub productCommandService;

    ProductServiceImpl productService;

    @BeforeEach
    void setUp() throws Exception {
        productService = new ProductServiceImpl();

        setField(productService, "telemetryHelper", telemetryHelper);
        setField(productService, "fileService", fileService);
        setField(productService, "productQueryService", productQueryService);
        setField(productService, "productCommandService", productCommandService);

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
    void uploadImage_returnsPath() {
        String base64 = "base64data";
        String fileName = "test.png";
        String expectedPath = "static/product/123_test.png";

        when(fileService.createFileImageBase64(eq(base64), anyString()))
                .thenReturn(expectedPath);

        ProductDto.UploadImageResponse result =
                productService.uploadImage(base64, fileName).await().indefinitely();

        assertThat(result.url()).isEqualTo(expectedPath);
    }

    @Test
    void findAll_returnsSuccess() {
        pb.product.Product.ProductResponse productProto = pb.product.Product.ProductResponse.newBuilder()
                .setId(1)
                .setName("Product A")
                .setPrice(1000)
                .build();

        pb.product.ProductQuery.ApiResponsePaginationProduct responseProto =
                pb.product.ProductQuery.ApiResponsePaginationProduct.newBuilder()
                        .addData(productProto)
                        .setStatus("success")
                        .setMessage("Products found")
                        .build();

        when(productQueryService.findAll(any(pb.product.Product.FindAllProductRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        ProductDto.ApiResponsePaginationProduct result =
                productService.findAll(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).name()).isEqualTo("Product A");
    }

    @Test
    void findByActive_returnsSuccess() {
        pb.product.Product.ProductResponseDeleteAt productProto = pb.product.Product.ProductResponseDeleteAt.newBuilder()
                .setId(1)
                .setName("Active Product")
                .build();

        pb.product.ProductQuery.ApiResponsePaginationProductDeleteAt responseProto =
                pb.product.ProductQuery.ApiResponsePaginationProductDeleteAt.newBuilder()
                        .addData(productProto)
                        .setStatus("success")
                        .setMessage("Active products")
                        .build();

        when(productQueryService.findByActive(any(pb.product.Product.FindAllProductRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        ProductDto.ApiResponsePaginationProductDeleteAt result =
                productService.findByActive(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void findByTrashed_returnsSuccess() {
        pb.product.Product.ProductResponseDeleteAt productProto = pb.product.Product.ProductResponseDeleteAt.newBuilder()
                .setId(2)
                .build();

        pb.product.ProductQuery.ApiResponsePaginationProductDeleteAt responseProto =
                pb.product.ProductQuery.ApiResponsePaginationProductDeleteAt.newBuilder()
                        .addData(productProto)
                        .setStatus("success")
                        .setMessage("Trashed products")
                        .build();

        when(productQueryService.findByTrashed(any(pb.product.Product.FindAllProductRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        ProductDto.ApiResponsePaginationProductDeleteAt result =
                productService.findByTrashed(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void findById_returnsSuccess() {
        pb.product.Product.ProductResponse productProto = pb.product.Product.ProductResponse.newBuilder()
                .setId(1)
                .setName("Product X")
                .build();

        pb.product.Product.ApiResponseProduct responseProto =
                pb.product.Product.ApiResponseProduct.newBuilder()
                        .setData(productProto)
                        .setStatus("success")
                        .setMessage("Product found")
                        .build();

        when(productQueryService.findById(any(pb.product.Product.FindByIdProductRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        ProductDto.ApiResponseProduct result = productService.findById(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().name()).isEqualTo("Product X");
    }

    @Test
    void findByMerchant_returnsSuccess() {
        pb.product.Product.ProductResponse productProto = pb.product.Product.ProductResponse.newBuilder()
                .setId(3)
                .setMerchantId(200)
                .build();

        pb.product.ProductQuery.ApiResponsePaginationProduct responseProto =
                pb.product.ProductQuery.ApiResponsePaginationProduct.newBuilder()
                        .addData(productProto)
                        .setStatus("success")
                        .setMessage("Merchant products")
                        .build();

        when(productQueryService.findByMerchant(any(pb.product.Product.FindAllProductMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        ProductDto.ApiResponsePaginationProduct result =
                productService.findByMerchant(200, "", 0, 0, 0, 1, 10).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().get(0).merchantId()).isEqualTo(200);
    }

    @Test
    void findByCategory_returnsSuccess() {
        pb.product.Product.ProductResponse productProto = pb.product.Product.ProductResponse.newBuilder()
                .setId(4)
                .setCategoryId(5)
                .build();

        pb.product.ProductQuery.ApiResponsePaginationProduct responseProto =
                pb.product.ProductQuery.ApiResponsePaginationProduct.newBuilder()
                        .addData(productProto)
                        .setStatus("success")
                        .setMessage("Category products")
                        .build();

        when(productQueryService.findByCategory(any(pb.product.Product.FindAllProductCategoryRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        ProductDto.ApiResponsePaginationProduct result =
                productService.findByCategory("Electronics", 1, 10, "", 0, 0).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().get(0).categoryId()).isEqualTo(5);
    }

    @Test
    void createProduct_returnsSuccess() {
        pb.product.Product.ProductResponse productProto = pb.product.Product.ProductResponse.newBuilder()
                .setId(10)
                .setName("New Product")
                .build();

        pb.product.Product.ApiResponseProduct responseProto =
                pb.product.Product.ApiResponseProduct.newBuilder()
                        .setData(productProto)
                        .setStatus("success")
                        .setMessage("Product created")
                        .build();

        when(productCommandService.create(any(pb.product.ProductCommand.CreateProductRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        ProductDto.CreateProductRequest request = new ProductDto.CreateProductRequest(
                1, 2, "New Product", "Desc", 5000, 10, "Brand", 1, 4.5f, "image.png", "12345");

        ProductDto.ApiResponseProduct result = productService.createProduct(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().name()).isEqualTo("New Product");
    }

    @Test
    void updateProduct_returnsSuccess() {
        pb.product.Product.ProductResponse productProto = pb.product.Product.ProductResponse.newBuilder()
                .setId(10)
                .setName("Updated Product")
                .build();

        pb.product.Product.ApiResponseProduct responseProto =
                pb.product.Product.ApiResponseProduct.newBuilder()
                        .setData(productProto)
                        .setStatus("success")
                        .setMessage("Product updated")
                        .build();

        when(productCommandService.update(any(pb.product.ProductCommand.UpdateProductRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        ProductDto.UpdateProductRequest request = new ProductDto.UpdateProductRequest(
                1, 2, "Updated Product", "New desc", 7500, 20, "BrandX", 1, 4.0f, "img.jpg", "12345");

        ProductDto.ApiResponseProduct result = productService.updateProduct(10, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().name()).isEqualTo("Updated Product");
    }

    @Test
    void deleteProduct_returnsSuccess() {
        pb.product.Product.ProductResponseDeleteAt productProto = pb.product.Product.ProductResponseDeleteAt.newBuilder()
                .setId(10)
                .setDeletedAt(com.google.protobuf.StringValue.of("2024-07-01T00:00:00Z"))
                .build();

        pb.product.Product.ApiResponseProductDeleteAt responseProto =
                pb.product.Product.ApiResponseProductDeleteAt.newBuilder()
                        .setData(productProto)
                        .setStatus("success")
                        .setMessage("Product trashed")
                        .build();

        when(productCommandService.trashedProduct(any(pb.product.Product.FindByIdProductRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        ProductDto.ApiResponseProductDeleteAt result = productService.deleteProduct(10).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Product trashed");
    }

    @Test
    void restoreProduct_returnsSuccess() {
        pb.product.Product.ProductResponseDeleteAt productProto = pb.product.Product.ProductResponseDeleteAt.newBuilder()
                .setId(10)
                .build();

        pb.product.Product.ApiResponseProductDeleteAt responseProto =
                pb.product.Product.ApiResponseProductDeleteAt.newBuilder()
                        .setData(productProto)
                        .setStatus("success")
                        .setMessage("Product restored")
                        .build();

        when(productCommandService.restoreProduct(any(pb.product.Product.FindByIdProductRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        ProductDto.ApiResponseProductDeleteAt result = productService.restoreProduct(10).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void deleteProductPermanent_returnsSuccess() {
        pb.product.ProductCommand.ApiResponseProductDelete responseProto =
                pb.product.ProductCommand.ApiResponseProductDelete.newBuilder()
                        .setStatus("success")
                        .setMessage("Permanently deleted")
                        .build();

        when(productCommandService.deleteProductPermanent(any(pb.product.Product.FindByIdProductRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        ProductDto.ApiResponseProductDelete result = productService.deleteProductPermanent(10).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void restoreAllProducts_returnsSuccess() {
        pb.product.ProductCommand.ApiResponseProductAll responseProto =
                pb.product.ProductCommand.ApiResponseProductAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All products restored")
                        .build();

        when(productCommandService.restoreAllProduct(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        ProductDto.ApiResponseProductAll result = productService.restoreAllProducts().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void deleteAllProductsPermanent_returnsSuccess() {
        pb.product.ProductCommand.ApiResponseProductAll responseProto =
                pb.product.ProductCommand.ApiResponseProductAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All products permanently deleted")
                        .build();

        when(productCommandService.deleteAllProductPermanent(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        ProductDto.ApiResponseProductAll result = productService.deleteAllProductsPermanent().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }
}
