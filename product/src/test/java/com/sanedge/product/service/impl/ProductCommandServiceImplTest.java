package com.sanedge.product.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.product.domain.requests.CreateProductRequest;
import com.sanedge.product.domain.requests.UpdateProductRequest;
import com.sanedge.product.domain.response.ProductResponse;
import com.sanedge.product.domain.response.ProductResponseDeleteAt;
import com.sanedge.product.entity.Product;
import com.sanedge.product.repository.ProductCommandRepository;
import com.sanedge.product.repository.ProductQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Validator;
import pb.merchant.Merchant;
import pb.merchant.MutinyMerchantQueryServiceGrpc.MutinyMerchantQueryServiceStub;

@ExtendWith(MockitoExtension.class)
class ProductCommandServiceImplTest {

    @Mock
    private ProductCommandRepository productCommandRepo;
    @Mock
    private ProductQueryRepository productQueryRepo;
    @Mock
    private Validator validator;
    @Mock
    private RedisService redisService;
    @Mock
    private TracingMetrics tracingMetrics;
    @Mock
    private MutinyMerchantQueryServiceStub merchantQueryService;

    private ProductCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductCommandServiceImpl(productCommandRepo, productQueryRepo, validator, redisService,
                tracingMetrics);
        // inject gRPC stub via field (it's field-injected in the service)
        service.merchantQueryService = merchantQueryService;

        // Lenient stub for traceAndMeasure
        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());
        lenient().when(productCommandRepo.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));
        lenient().when(productCommandRepo.restoreAllDeleted()).thenReturn(Uni.createFrom().item(true));

        // common gRPC stub
        lenient().when(merchantQueryService.findByIdMerchant(any()))
                .thenReturn(Uni.createFrom().item(Merchant.ApiResponseMerchant.newBuilder()
                        .setStatus("success")
                        .setData(Merchant.MerchantResponse.newBuilder().setId(10).build())
                        .build()));
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

    private CreateProductRequest createReq() {
        CreateProductRequest r = new CreateProductRequest();
        r.setMerchantId(10);
        r.setCategoryId(100);
        r.setName("New Product");
        r.setDescription("desc");
        r.setPrice(5000);
        r.setCountInStock(10);
        r.setBrand("Brand");
        r.setWeight(500);
        r.setSlugProduct("new-product");
        r.setImageProduct("img.png");
        return r;
    }

    private UpdateProductRequest updateReq() {
        UpdateProductRequest r = new UpdateProductRequest();
        r.setProductId(1);
        r.setMerchantId(10);
        r.setCategoryId(200);
        r.setName("Updated");
        r.setDescription("desc2");
        r.setPrice(6000);
        r.setCountInStock(15);
        r.setBrand("Brand2");
        r.setWeight(600);
        r.setSlugProduct("updated-product");
        r.setImageProduct("img2.png");
        return r;
    }

    @Nested
    @DisplayName("createProduct tests")
    class CreateProductTests {
        @Test
        void success() {
            CreateProductRequest req = createReq();
            Product saved = createMockProduct(1L);
            saved.setName(req.getName());
            when(productCommandRepo.persist(any(Product.class))).thenReturn(Uni.createFrom().item(saved));

            ApiResponse<ProductResponse> resp = service.createProduct(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getId()).isEqualTo(1L);
            assertThat(resp.data().getName()).isEqualTo("New Product");
        }

        @Test
        void persistenceFailure_returnsError() {
            CreateProductRequest req = createReq();
            when(productCommandRepo.persist(any(Product.class)))
                    .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
            ApiResponse<ProductResponse> resp = service.createProduct(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Failed to create product");
        }
    }

    @Nested
    @DisplayName("updateProduct tests")
    class UpdateProductTests {
        @Test
        void success() {
            UpdateProductRequest req = updateReq();
            Product saved = createMockProduct(1L);
            saved.setName(req.getName());
            saved.setPrice(req.getPrice());
            saved.setCountInStock(req.getCountInStock());
            when(productQueryRepo.findProductById(anyLong())).thenReturn(Uni.createFrom().item(createMockProduct(1L)));
            when(productCommandRepo.persist(any(Product.class))).thenReturn(Uni.createFrom().item(saved));

            ApiResponse<ProductResponse> resp = service.updateProduct(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getName()).isEqualTo("Updated");
        }

        @Test
        void productNotFound_returnsError() {
            UpdateProductRequest req = updateReq();
            when(productQueryRepo.findProductById(anyLong())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<ProductResponse> resp = service.updateProduct(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Product not found");
        }

        @Test
        void merchantNotFound_returnsError() {
            lenient().when(merchantQueryService.findByIdMerchant(any()))
                    .thenReturn(Uni.createFrom().item(Merchant.ApiResponseMerchant.newBuilder()
                            .setStatus("error").build()));

            UpdateProductRequest req = updateReq();

            ApiResponse<ProductResponse> resp = service.updateProduct(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Merchant not found");
        }
    }

    @Nested
    @DisplayName("trashedProduct tests")
    class TrashedProductTests {
        @Test
        void success() {
            Integer id = 1;
            Product trashed = createMockProduct(1L);
            trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
            when(productCommandRepo.trashed(anyLong())).thenReturn(Uni.createFrom().item(trashed));

            ApiResponse<ProductResponseDeleteAt> resp = service.trashedProduct(id).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getDeletedAt()).isNotNull();
        }

        @Test
        void notFound_returnsError() {
            when(productCommandRepo.trashed(anyLong())).thenReturn(Uni.createFrom().nullItem());
            ApiResponse<ProductResponseDeleteAt> resp = service.trashedProduct(1).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Failed to trash product");
        }
    }

    @Nested
    @DisplayName("restoreProduct tests")
    class RestoreProductTests {
        @Test
        void success() {
            Integer id = 1;
            when(productCommandRepo.restore(anyLong())).thenReturn(Uni.createFrom().item(createMockProduct(1L)));
            ApiResponse<ProductResponseDeleteAt> resp = service.restoreProduct(id).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getDeletedAt()).isNull();
        }

        @Test
        void notFound_returnsError() {
            when(productCommandRepo.restore(anyLong())).thenReturn(Uni.createFrom().nullItem());
            ApiResponse<ProductResponseDeleteAt> resp = service.restoreProduct(1).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Failed to restore product");
        }
    }

    @Nested
    @DisplayName("deleteProductPermanent tests")
    class DeleteProductPermanentTests {
        @Test
        void success() {
            Integer id = 1;

            when(productCommandRepo.findById(anyLong()))
                    .thenReturn(Uni.createFrom().item(createMockProduct(1L)));

            ApiResponse<Boolean> resp = service.deleteProductPermanent(id).await().indefinitely();

            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isFalse();
        }

        @Test
        void notFound_returnsFalse() {
            Integer id = 1;
            
            when(productCommandRepo.findById(anyLong()))
                    .thenReturn(Uni.createFrom().item((Product) null));

            ApiResponse<Boolean> resp = service.deleteProductPermanent(id).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isFalse();
        }
    }

    @Nested
    @DisplayName("restoreAllProducts tests")
    class RestoreAllProductsTests {
        @Test
        void success() {
            ApiResponse<Boolean> resp = service.restoreAllProducts().await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test
        void noTrashed_throwsException() {
            when(productCommandRepo.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.restoreAllProducts().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No products found in trash");
        }
    }

    @Nested
    @DisplayName("deleteAllProductsPermanent tests")
    class DeleteAllProductsPermanentTests {
        @Test
        void success() {
            ApiResponse<Boolean> resp = service.deleteAllProductsPermanent().await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test
        void noTrashed_throwsException() {
            when(productCommandRepo.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.deleteAllProductsPermanent().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No products found in trash");
        }
    }
}