package com.sanedge.product.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.sanedge.product.service.ProductCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@ApplicationScoped
public class ProductCommandServiceImpl implements ProductCommandService {
    private static final Logger logger = LoggerFactory.getLogger(ProductCommandServiceImpl.class);

    private final ProductCommandRepository productCommandRepository;
    private final ProductQueryRepository productQueryRepository;
    private final Validator validator;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    @Inject
    @GrpcClient("merchant")
    pb.merchant.MutinyMerchantQueryServiceGrpc.MutinyMerchantQueryServiceStub merchantQueryService;

    @Inject
    public ProductCommandServiceImpl(ProductCommandRepository productCommandRepository,
            ProductQueryRepository productQueryRepository,
            Validator validator,
            RedisService redisService,
            TracingMetrics tracingMetrics) {
        this.productCommandRepository = productCommandRepository;
        this.productQueryRepository = productQueryRepository;
        this.validator = validator;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    private <T> void validateRequest(T req) {
        Set<ConstraintViolation<T>> violations = validator.validate(req);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<T> violation : violations) {
                sb.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("; ");
            }
            logger.error("Validation failed: {}", sb);
            throw new ConstraintViolationException("Validation failed: " + sb, violations);
        }
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<ProductResponse>> createProduct(CreateProductRequest req) {
        logger.info("Creating product: {}", req.getName());
        Attributes attrs = Attributes.builder()
                .put("product.name", req.getName())
                .build();

        return runTraced("createProduct", "create_product", attrs,
                () -> {
                    try {
                        validateRequest(req);
                    } catch (Exception e) {
                        return Uni.createFrom()
                                .item(new ApiResponse<>("error", e.getMessage(), (ProductResponse) null));
                    }

                    Product product = new Product();
                    product.setMerchantId(req.getMerchantId().longValue());
                    product.setCategoryId(req.getCategoryId().longValue());
                    product.setName(req.getName());
                    product.setDescription(req.getDescription());
                    product.setPrice(req.getPrice());
                    product.setCountInStock(req.getCountInStock());
                    product.setBrand(req.getBrand());
                    product.setWeight(req.getWeight());
                    product.setSlugProduct(req.getSlugProduct());
                    product.setImageProduct(req.getImageProduct());
                    product.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                    product.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                    return productCommandRepository.persist(product)
                            .map(saved -> {
                                logger.info("Product created successfully with name={}", saved.getName());
                                return ApiResponse.success("Product created successfully", ProductResponse.from(saved));
                            })
                            .onFailure().recoverWithItem(e -> {
                                logger.error("Failed to create product: {}", req.getName(), e);
                                return new ApiResponse<>("error", "Failed to create product: " + e.getMessage(),
                                        (ProductResponse) null);
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<ProductResponse>> updateProduct(UpdateProductRequest req) {
        logger.info("Updating product ID: {}", req.getProductId());
        Attributes attrs = Attributes.builder()
                .put("product.id", req.getProductId())
                .build();

        return runTraced("updateProduct", "update_product", attrs,
                () -> {
                    try {
                        validateRequest(req);
                    } catch (Exception e) {
                        return Uni.createFrom()
                                .item(new ApiResponse<>("error", e.getMessage(), (ProductResponse) null));
                    }

                    return merchantQueryService
                            .findByIdMerchant(pb.merchant.Merchant.FindByIdMerchantRequest.newBuilder()
                                    .setMerchantId(req.getMerchantId())
                                    .build())
                            .onItem().transformToUni(apiResp -> {
                                if (apiResp == null || !apiResp.hasData()
                                        || !"success".equalsIgnoreCase(apiResp.getStatus())) {
                                    return Uni.createFrom().failure(new ResourceNotFoundException(
                                            "Merchant not found with id " + req.getMerchantId()));
                                }
                                return productQueryRepository.findProductById(req.getProductId().longValue());
                            })
                            .onItem().ifNull()
                            .failWith(() -> new ResourceNotFoundException(
                                    "Product not found with id " + req.getProductId()))
                            .chain(product -> {
                                if (req.getImageProduct() != null) {
                                    product.setImageProduct(req.getImageProduct());
                                }

                                product.setMerchantId(req.getMerchantId().longValue());
                                product.setCategoryId(req.getCategoryId().longValue());

                                product.setName(req.getName());
                                product.setDescription(req.getDescription());
                                product.setPrice(req.getPrice());
                                product.setCountInStock(req.getCountInStock());
                                product.setBrand(req.getBrand());
                                product.setWeight(req.getWeight());
                                product.setSlugProduct(req.getSlugProduct());
                                product.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                                return productCommandRepository.persist(product);
                            })
                            .chain(updated -> {
                                String cacheKey = "products:id:" + req.getProductId();
                                return redisService.deleteReactive(cacheKey)
                                        .replaceWith(updated);
                            })
                            .map(updated -> {
                                logger.info("Product updated successfully for ID={}", updated.getProductId());
                                return ApiResponse.success("Product updated successfully",
                                        ProductResponse.from(updated));
                            })
                            .onFailure().recoverWithItem(e -> {
                                logger.error("Failed to update product ID: {}", req.getProductId(), e);
                                String errorMsg = e instanceof ResourceNotFoundException
                                        ? "Resource not found: " + e.getMessage()
                                        : "Failed to update product";
                                return new ApiResponse<>("error", errorMsg, (ProductResponse) null);
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<ProductResponseDeleteAt>> trashedProduct(Integer productId) {
        logger.info("Trashing product ID: {}", productId);
        Attributes attrs = Attributes.builder()
                .put("product.id", productId)
                .build();

        return runTraced("trashedProduct", "trashed_product", attrs,
                () -> {
                    if (productId == null) {
                        return Uni.createFrom()
                                .item(new ApiResponse<>("error", "Product ID must not be null",
                                        (ProductResponseDeleteAt) null));
                    }

                    return productCommandRepository.trashed(productId.longValue())
                            .onItem().ifNull().failWith(() -> new ResourceNotFoundException("Product not found"))
                            .chain(trashed -> {
                                String cacheKey = "products:id:" + productId;
                                return redisService.deleteReactive(cacheKey)
                                        .replaceWith(trashed);
                            })
                            .map(trashed -> {
                                logger.info("Product soft deleted successfully for ID={}", trashed.getProductId());
                                return ApiResponse.success("Product trashed successfully",
                                        ProductResponseDeleteAt.from(trashed));
                            })
                            .onFailure().recoverWithItem(e -> {
                                logger.error("Failed to trash product ID: {}", productId, e);
                                return new ApiResponse<>("error", "Failed to trash product",
                                        (ProductResponseDeleteAt) null);
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<ProductResponseDeleteAt>> restoreProduct(Integer productId) {
        logger.info("Restoring product ID: {}", productId);
        Attributes attrs = Attributes.builder()
                .put("product.id", productId)
                .build();

        return runTraced("restoreProduct", "restore_product", attrs,
                () -> {
                    if (productId == null) {
                        return Uni.createFrom()
                                .item(new ApiResponse<>("error", "Product ID must not be null",
                                        (ProductResponseDeleteAt) null));
                    }

                    return productCommandRepository.restore(productId.longValue())
                            .onItem().ifNull()
                            .failWith(() -> new ResourceNotFoundException("Product not found or not deleted"))
                            .chain(restored -> {
                                String cacheKey = "products:id:" + productId;
                                return redisService.deleteReactive(cacheKey)
                                        .replaceWith(restored);
                            })
                            .map(restored -> {
                                logger.info("Product restored successfully for ID={}", restored.getProductId());
                                return ApiResponse.success("Product restored successfully",
                                        ProductResponseDeleteAt.from(restored));
                            })
                            .onFailure().recoverWithItem(e -> {
                                logger.error("Failed to restore product ID: {}", productId, e);
                                return new ApiResponse<>("error", "Failed to restore product",
                                        (ProductResponseDeleteAt) null);
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deleteProductPermanent(Integer productId) {
        logger.info("Permanently deleting product ID: {}", productId);
        Attributes attrs = Attributes.builder()
                .put("product.id", productId)
                .build();

        return runTraced("deleteProductPermanent", "delete_product_permanent", attrs,
                () -> {
                    if (productId == null) {
                        return Uni.createFrom().item(new ApiResponse<>("error", "Product ID must not be null", false));
                    }

                    return productCommandRepository.findById(productId.longValue())
                            .chain(deleted -> {
                                if (Boolean.TRUE.equals(deleted)) {
                                    String cacheKey = "products:id:" + productId;
                                    return redisService.deleteReactive(cacheKey)
                                            .replaceWith(true);
                                }
                                return Uni.createFrom().item(false);
                            })
                            .map(deleted -> {
                                logger.info("Product permanently deleted for ID={}: {}", productId, deleted);
                                return ApiResponse.success("Product permanently deleted", deleted);
                            })
                            .onFailure().recoverWithItem(e -> {
                                logger.error("Failed to permanently delete product ID: {}", productId, e);
                                return new ApiResponse<>("error", "Failed to permanently delete product", false);
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> restoreAllProducts() {
        logger.info("Restoring ALL trashed products");

        return runTraced("restoreAllProducts", "restore_all_products", Attributes.empty(),
                () -> productCommandRepository.restoreAllDeleted()
                        .map(restored -> {
                            if (!restored) {
                                throw new ResourceNotFoundException("No products found in trash");
                            }

                            logger.info("All trashed products restored: {}", restored);
                            return ApiResponse.success("All products restored successfully", restored);
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deleteAllProductsPermanent() {
        logger.info("Permanently deleting ALL trashed products");

        return runTraced("deleteAllProductsPermanent", "delete_all_products_permanent", Attributes.empty(),
                () -> productCommandRepository.deleteAllDeleted()
                        .map(deleted -> {
                            if (!deleted) {
                                throw new ResourceNotFoundException("No products found in trash");
                            }

                            logger.info("All trashed products permanently deleted: {}", deleted);
                            return ApiResponse.success("All products permanently deleted", deleted);
                        }));
    }

    private <T> Uni<T> runTraced(String operationName, String method, Attributes attributes,
            java.util.function.Supplier<Uni<T>> supplier) {
        return tracingMetrics.traceAndMeasure(operationName, method, attributes, supplier);
    }
}