package com.sanedge.category.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.category.domain.requests.CreateCategoryRequest;
import com.sanedge.category.domain.requests.UpdateCategoryRequest;
import com.sanedge.category.domain.response.CategoryResponse;
import com.sanedge.category.domain.response.CategoryResponseDeleteAt;
import com.sanedge.category.entity.Category;
import com.sanedge.category.repository.CategoryCommandRepository;
import com.sanedge.category.repository.CategoryQueryRepository;
import com.sanedge.category.service.CategoryCommandService;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@ApplicationScoped
public class CategoryCommandServiceImpl implements CategoryCommandService {
    private static final Logger logger = LoggerFactory.getLogger(CategoryCommandServiceImpl.class);

    private final CategoryQueryRepository categoryQueryRepository;
    private final CategoryCommandRepository categoryCommandRepository;
    private final Validator validator;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    @Inject
    public CategoryCommandServiceImpl(CategoryQueryRepository categoryQueryRepository,
            CategoryCommandRepository categoryCommandRepository,
            Validator validator,
            RedisService redisService,
            TracingMetrics tracingMetrics) {
        this.categoryQueryRepository = categoryQueryRepository;
        this.categoryCommandRepository = categoryCommandRepository;
        this.validator = validator;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<CategoryResponse>> createCategory(CreateCategoryRequest req) {
        Attributes attrs = Attributes.builder()
                .put("category.name", req.getName())
                .build();

        return runTraced("createCategory", "create_category", attrs, () -> {
            logger.info("Creating category name={}", req.getName());

            try {
                validateRequest(req);
            } catch (Exception e) {
                return Uni.createFrom().item(new ApiResponse<>("error", e.getMessage(), null));
            }

            return categoryQueryRepository.findByName(req.getName())
                    .chain(existingCategory -> {
                        if (existingCategory != null) {
                            logger.warn("Category creation failed. Category name '{}' already exists", req.getName());
                            throw new IllegalArgumentException(
                                    "Category with name '" + req.getName() + "' already exists");
                        }

                        Category category = new Category();
                        category.setName(req.getName());
                        category.setDescription(req.getDescription());
                        category.setSlugCategory(req.getSlugCategory());
                        category.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                        category.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                        return categoryCommandRepository.persist(category)
                                .map(savedCategory -> {
                                    CategoryResponse response = CategoryResponse.from(savedCategory);
                                    logger.info("Category created successfully with id={}", response.getId());
                                    return ApiResponse.success("Category created successfully", response);
                                });
                    })
                    .onFailure().recoverWithItem(e -> {
                        logger.error("Failed to create category", e);
                        return new ApiResponse<>("error", e.getMessage(), null);
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<CategoryResponse>> updateCategory(UpdateCategoryRequest req) {
        Attributes attrs = Attributes.builder()
                .put("category.id", req.getCategoryId() != null ? req.getCategoryId().toString() : "null")
                .build();

        return runTraced("updateCategory", "update_category", attrs, () -> {
            if (req.getCategoryId() == null) {
                return Uni.createFrom().item(new ApiResponse<>("error", "category_id is required", null));
            }

            logger.info("Updating category id={}", req.getCategoryId());

            try {
                validateRequest(req);
            } catch (Exception e) {
                return Uni.createFrom().item(new ApiResponse<>("error", e.getMessage(), null));
            }

            return categoryCommandRepository.findById(req.getCategoryId().longValue())
                    .onItem().ifNull().failWith(() -> new ResourceNotFoundException("Category not found"))
                    .chain(category -> {
                        category.setName(req.getName());
                        category.setDescription(req.getDescription());
                        category.setSlugCategory(req.getSlugCategory());
                        category.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                        return categoryCommandRepository.persist(category)
                                .chain(savedCategory -> {
                                    String cacheKey = "category:" + req.getCategoryId();
                                    return redisService.deleteReactive(cacheKey)
                                            .map(deleted -> {
                                                CategoryResponse response = CategoryResponse.from(savedCategory);
                                                logger.info("Category updated successfully with id={}",
                                                        response.getId());
                                                return ApiResponse.success("Category updated successfully", response);
                                            });
                                });
                    })
                    .onFailure().recoverWithItem(e -> {
                        logger.error("Failed to update category id={}", req.getCategoryId(), e);
                        return new ApiResponse<>("error", e.getMessage(), null);
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<CategoryResponseDeleteAt>> trashedCategory(Integer categoryId) {
        Attributes attrs = Attributes.builder()
                .put("category.id", categoryId.toString())
                .build();

        return runTraced("trashedCategory", "trash_category", attrs, () -> {
            logger.info("Trashing category id={}", categoryId);

            return categoryCommandRepository.trashed(categoryId.longValue())
                    .onItem().ifNull().failWith(() -> new ResourceNotFoundException("Category not found"))
                    .chain(category -> {
                        String cacheKey = "category:" + categoryId;
                        return redisService.deleteReactive(cacheKey)
                                .map(deleted -> {
                                    CategoryResponseDeleteAt response = CategoryResponseDeleteAt.from(category);
                                    logger.info("Category trashed successfully with id={}", categoryId);
                                    return ApiResponse.success("Category trashed successfully", response);
                                });
                    })
                    .onFailure().recoverWithItem(e -> {
                        logger.error("Failed to trash category id={}", categoryId, e);
                        return new ApiResponse<>("error", "Failed to trash category: " + e.getMessage(), null);
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<CategoryResponseDeleteAt>> restoreCategory(Integer categoryId) {
        Attributes attrs = Attributes.builder()
                .put("category.id", categoryId.toString())
                .build();

        return runTraced("restoreCategory", "restore_category", attrs, () -> {
            logger.info("Restoring category id={}", categoryId);

            return categoryCommandRepository.restore(categoryId.longValue())
                    .onItem().ifNull()
                    .failWith(() -> new ResourceNotFoundException("Category not found or not trashed"))
                    .chain(category -> {
                        String cacheKey = "category:" + categoryId;
                        return redisService.deleteReactive(cacheKey)
                                .map(deleted -> {
                                    CategoryResponseDeleteAt response = CategoryResponseDeleteAt.from(category);
                                    logger.info("Category restored successfully with id={}", categoryId);
                                    return ApiResponse.success("Category restored successfully", response);
                                });
                    })
                    .onFailure().recoverWithItem(e -> {
                        logger.error("Failed to restore category id={}", categoryId, e);
                        return new ApiResponse<>("error", "Failed to restore category: " + e.getMessage(), null);
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deleteCategoryPermanent(Integer categoryId) {
        Attributes attrs = Attributes.builder()
                .put("category.id", categoryId.toString())
                .build();

        return runTraced("deleteCategoryPermanent", "delete_category_permanent", attrs, () -> {
            logger.info("Permanently deleting category id={}", categoryId);

            return categoryCommandRepository.deletePermanent(categoryId.longValue())
                    .onItem().ifNull()
                    .failWith(() -> new ResourceNotFoundException("Category not found or not trashed"))
                    .chain(category -> {
                        String cacheKey = "category:" + categoryId;
                        return redisService.deleteReactive(cacheKey)
                                .map(deleted -> {
                                    logger.info("Category permanently deleted with id={}", categoryId);
                                    return ApiResponse.success("Category permanently deleted", true);
                                });
                    })
                    .onFailure().recoverWithItem(e -> {
                        logger.error("Failed to permanently delete category id={}", categoryId, e);
                        return new ApiResponse<>("error", "Failed to permanently delete category: " + e.getMessage(),
                                false);
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> restoreAllCategories() {
        return runTraced("restoreAllCategories", "restore_all_categories", Attributes.empty(), () -> {
            logger.info("Restoring ALL trashed categories");

            return categoryCommandRepository.restoreAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed categories");
                        }

                        logger.info("All categories restored successfully");
                        return ApiResponse.success("All categories restored successfully", true);
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deleteAllCategoriesPermanent() {
        return runTraced("deleteAllCategoriesPermanent", "delete_all_categories_permanent", Attributes.empty(), () -> {
            logger.info("Permanently deleting ALL trashed categories");

            return categoryCommandRepository.deleteAllDeleted()
                    .map(success -> {
                        if (success) {
                            logger.info("All categories permanently deleted");
                            return ApiResponse.success("All categories permanently deleted", true);
                        }

                        logger.warn("No categories were permanently deleted");
                        return ApiResponse.success("No categories were permanently deleted", false);
                    });
        });
    }

    private <T> Uni<T> runTraced(String operationName, String method, Attributes attributes,
            Supplier<Uni<T>> supplier) {
        return tracingMetrics.traceAndMeasure(operationName, method, attributes, supplier);
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
}