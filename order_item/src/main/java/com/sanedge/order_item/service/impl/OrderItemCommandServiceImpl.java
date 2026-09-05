package com.sanedge.order_item.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.order_item.domain.requests.CreateOrderItemRequest;
import com.sanedge.order_item.domain.requests.UpdateOrderItemRequest;
import com.sanedge.order_item.domain.response.OrderItemResponse;
import com.sanedge.order_item.domain.response.OrderItemResponseDeleteAt;
import com.sanedge.order_item.entity.OrderItem;
import com.sanedge.order_item.repository.OrderItemRepository;
import com.sanedge.order_item.service.OrderItemCommandService;

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
public class OrderItemCommandServiceImpl implements OrderItemCommandService {
    private static final Logger logger = LoggerFactory.getLogger(OrderItemCommandServiceImpl.class);

    private final OrderItemRepository orderItemRepository;
    private final Validator validator;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    @GrpcClient("order")
    pb.order.MutinyOrderQueryServiceGrpc.MutinyOrderQueryServiceStub orderQueryService;

    @GrpcClient("order")
    pb.order.MutinyOrderCommandServiceGrpc.MutinyOrderCommandServiceStub orderCommandService;

    @GrpcClient("product")
    pb.product.MutinyProductServiceGrpc.MutinyProductServiceStub productQueryService;

    @GrpcClient("product")
    pb.product.MutinyProductCommandServiceGrpc.MutinyProductCommandServiceStub productCommandService;

    @Inject
    public OrderItemCommandServiceImpl(OrderItemRepository orderItemRepository,
            Validator validator,
            RedisService redisService,
            TracingMetrics tracingMetrics) {
        this.orderItemRepository = orderItemRepository;
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

    private Uni<Void> updateOrderTotalPrice(Long orderId) {
        return orderItemRepository.findOrderItemByOrder(orderId)
                .chain(items -> {
                    long total = items.stream().mapToLong(item -> item.getPrice().longValue() * item.getQuantity())
                            .sum();
                    return orderCommandService
                            .updateOrderTotalPrice(pb.order.OrderCommand.UpdateOrderTotalPriceRequest.newBuilder()
                                    .setOrderId(orderId.intValue())
                                    .setTotalPrice((int) total)
                                    .build())
                            .map(res -> {
                                if ("success".equals(res.getStatus())) {
                                    logger.info("Order total price updated successfully for orderId={}", orderId);
                                } else {
                                    logger.error("Failed to update order total price for orderId={}: {}", orderId,
                                            res.getMessage());
                                }
                                return null;
                            })
                            .replaceWithVoid();
                });
    }

    private Uni<Void> clearCache(Long orderId) {
        if (orderId == null) {
            return Uni.createFrom().voidItem();
        }
        String cacheKey = "order_item:by_order:" + orderId;
        return redisService.deleteReactive(cacheKey);
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<OrderItemResponse>> create(CreateOrderItemRequest request) {
        logger.info("Creating new order item for orderId={} and productId={}", request.getOrderId(),
                request.getProductId());
        Attributes attrs = Attributes.builder()
                .put("order.id", request.getOrderId() != null ? request.getOrderId().toString() : "null")
                .put("product.id", request.getProductId() != null ? request.getProductId().toString() : "null")
                .build();

        return runTraced("createOrderItem", "create_order_item", attrs,
                () -> {
                    try {
                        validateRequest(request);
                    } catch (Exception e) {
                        return Uni.createFrom().item(new ApiResponse<>("error", e.getMessage(), null));
                    }

                    return orderQueryService.findById(pb.order.Order.FindByIdOrderRequest.newBuilder()
                            .setId(request.getOrderId().intValue())
                            .build())
                            .chain(orderRes -> {
                                if (orderRes == null || !"success".equals(orderRes.getStatus())) {
                                    logger.error("Order not found with id={}", request.getOrderId());
                                    throw new ResourceNotFoundException("Order not found");
                                }
                                return productQueryService
                                        .findById(pb.product.Product.FindByIdProductRequest.newBuilder()
                                                .setId(request.getProductId().intValue())
                                                .build());
                            })
                            .chain(productRes -> {
                                if (productRes == null || !"success".equals(productRes.getStatus())) {
                                    logger.error("Product not found with id={}", request.getProductId());
                                    throw new ResourceNotFoundException(
                                            "Product not found with id=" + request.getProductId());
                                }
                                var product = productRes.getData();
                                if (product.getCountInStock() < request.getQuantity()) {
                                    logger.error("Insufficient stock for product id={}", request.getProductId());
                                    throw new IllegalArgumentException(
                                            "Insufficient stock for product id=" + request.getProductId());
                                }

                                OrderItem orderItem = new OrderItem();
                                orderItem.setOrderId(request.getOrderId().longValue());
                                orderItem.setProductId(request.getProductId().longValue());
                                orderItem.setQuantity(request.getQuantity());
                                orderItem.setPrice(request.getPrice());
                                orderItem.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                                orderItem.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                                pb.product.ProductCommand.UpdateProductRequest updateReq = pb.product.ProductCommand.UpdateProductRequest
                                        .newBuilder()
                                        .setProductId(product.getId())
                                        .setMerchantId(product.getMerchantId())
                                        .setCategoryId(product.getCategoryId())
                                        .setName(product.getName())
                                        .setDescription(product.getDescription())
                                        .setPrice(product.getPrice())
                                        .setCountInStock(product.getCountInStock() - request.getQuantity())
                                        .setBrand(product.getBrand())
                                        .setWeight(product.getWeight())
                                        .setImageProduct(product.getImageProduct())
                                        .build();

                                return Uni.combine().all().unis(
                                        orderItemRepository.persist(orderItem),
                                        productCommandService.update(updateReq)).asTuple().map(t -> t.getItem1());
                            })
                            .chain(savedItem -> updateOrderTotalPrice(request.getOrderId().longValue())
                                    .chain(() -> clearCache(request.getOrderId().longValue()))
                                    .map(v -> savedItem))
                            .map(savedItem -> {
                                logger.info("Order item created successfully with id={}", savedItem.getOrderItemId());
                                return ApiResponse.success("Order item created successfully",
                                        OrderItemResponse.from(savedItem));
                            })
                            .onFailure().recoverWithItem(e -> {
                                logger.error("Failed to create order item", e);
                                return new ApiResponse<>("error", "Failed to create your order item: " + e.getMessage(),
                                        null);
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<OrderItemResponse>> update(UpdateOrderItemRequest request) {
        logger.info("Updating order item id={}", request.getOrderItemId());
        Attributes attrs = Attributes.builder()
                .put("order_item.id", request.getOrderItemId() != null ? request.getOrderItemId().toString() : "null")
                .build();

        return runTraced("updateOrderItem", "update_order_item", attrs,
                () -> {
                    try {
                        validateRequest(request);
                    } catch (Exception e) {
                        return Uni.createFrom().item(new ApiResponse<>("error", e.getMessage(), null));
                    }

                    return orderItemRepository.findById(request.getOrderItemId().longValue())
                            .chain(existingItem -> {
                                if (existingItem == null) {
                                    logger.error("Order item not found with id={}", request.getOrderItemId());
                                    throw new ResourceNotFoundException("Order item not found");
                                }

                                return productQueryService
                                        .findById(pb.product.Product.FindByIdProductRequest.newBuilder()
                                                .setId(request.getProductId().intValue())
                                                .build())
                                        .chain(productRes -> {
                                            if (productRes == null || !"success".equals(productRes.getStatus())) {
                                                logger.error("Product not found with id={}", request.getProductId());
                                                throw new ResourceNotFoundException(
                                                        "Product not found with id=" + request.getProductId());
                                            }
                                            var product = productRes.getData();

                                            int diff = request.getQuantity() - existingItem.getQuantity();
                                            if (diff > 0 && product.getCountInStock() < diff) {
                                                logger.error("Insufficient stock for product id={}",
                                                        request.getProductId());
                                                throw new IllegalArgumentException(
                                                        "Insufficient stock for product id=" + request.getProductId());
                                            }

                                            existingItem.setOrderId(request.getOrderId().longValue());
                                            existingItem.setProductId(request.getProductId().longValue());
                                            existingItem.setQuantity(request.getQuantity());
                                            existingItem.setPrice(request.getPrice());
                                            existingItem.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                                            pb.product.ProductCommand.UpdateProductRequest updateReq = pb.product.ProductCommand.UpdateProductRequest
                                                    .newBuilder()
                                                    .setProductId(product.getId())
                                                    .setMerchantId(product.getMerchantId())
                                                    .setCategoryId(product.getCategoryId())
                                                    .setName(product.getName())
                                                    .setDescription(product.getDescription())
                                                    .setPrice(product.getPrice())
                                                    .setCountInStock(product.getCountInStock() - diff)
                                                    .setBrand(product.getBrand())
                                                    .setWeight(product.getWeight())
                                                    .setImageProduct(product.getImageProduct())
                                                    .build();

                                            return Uni.combine().all().unis(
                                                    orderItemRepository.persist(existingItem),
                                                    productCommandService.update(updateReq)).asTuple()
                                                    .map(t -> t.getItem1());
                                        });
                            })
                            .chain(savedItem -> updateOrderTotalPrice(request.getOrderId().longValue())
                                    .chain(() -> clearCache(request.getOrderId().longValue()))
                                    .map(v -> savedItem))
                            .map(savedItem -> {
                                logger.info("Order item updated successfully with id={}", savedItem.getOrderItemId());
                                return ApiResponse.success("Order item updated successfully",
                                        OrderItemResponse.from(savedItem));
                            })
                            .onFailure().recoverWithItem(e -> {
                                logger.error("Failed to update order item id={}", request.getOrderItemId(), e);
                                return new ApiResponse<>("error", "Failed to update order item: " + e.getMessage(),
                                        null);
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<OrderItemResponseDeleteAt>> trash(Integer id) {
        logger.info("Trashing order item id={}", id);
        Attributes attrs = Attributes.builder()
                .put("order_item.id", id.toString())
                .build();

        return runTraced("trashOrderItem", "trash_order_item", attrs,
                () -> orderItemRepository.trashed(id.longValue())
                        .chain(item -> {
                            if (item == null) {
                                logger.error("Order item not found for trashing id={}", id);
                                throw new ResourceNotFoundException("Order item not found");
                            }
                            return updateOrderTotalPrice(item.getOrderId())
                                    .chain(() -> clearCache(item.getOrderId()))
                                    .map(v -> item);
                        })
                        .map(item -> {
                            logger.info("Order item trashed successfully with id={}", id);
                            return ApiResponse.success("Order item trashed successfully",
                                    OrderItemResponseDeleteAt.from(item));
                        })
                        .onFailure().recoverWithItem(e -> {
                            logger.error("Failed to trash order item id={}", id, e);
                            return new ApiResponse<>("error", "Failed to trash order item: " + e.getMessage(), null);
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<OrderItemResponseDeleteAt>> restore(Integer id) {
        logger.info("Restoring order item id={}", id);
        Attributes attrs = Attributes.builder()
                .put("order_item.id", id.toString())
                .build();

        return runTraced("restoreOrderItem", "restore_order_item", attrs,
                () -> orderItemRepository.restore(id.longValue())
                        .chain(item -> {
                            if (item == null) {
                                logger.error("Order item not found for restoration id={}", id);
                                throw new ResourceNotFoundException("Order item not found or not in trash");
                            }
                            return updateOrderTotalPrice(item.getOrderId())
                                    .chain(() -> clearCache(item.getOrderId()))
                                    .map(v -> item);
                        })
                        .map(item -> {
                            logger.info("Order item restored successfully with id={}", id);
                            return ApiResponse.success("Order item restored successfully",
                                    OrderItemResponseDeleteAt.from(item));
                        })
                        .onFailure().recoverWithItem(e -> {
                            logger.error("Failed to restore order item id={}", id, e);
                            return new ApiResponse<>("error", "Failed to restore order item: " + e.getMessage(), null);
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> delete(Integer id) {
        logger.info("Permanently deleting order item id={}", id);
        Attributes attrs = Attributes.builder()
                .put("order_item.id", id.toString())
                .build();

        return runTraced("deleteOrderItemPermanent", "delete_order_item_permanent", attrs,
                () -> orderItemRepository.deletePermanent(id.longValue())
                        .chain(item -> {
                            if (item == null) {
                                logger.error("Order item not found for permanent deletion id={}", id);
                                throw new ResourceNotFoundException("Order item not found or not in trash");
                            }
                            return updateOrderTotalPrice(item.getOrderId())
                                    .chain(() -> clearCache(item.getOrderId()))
                                    .map(v -> true);
                        })
                        .map(deleted -> {
                            logger.info("Order item permanently deleted with id={}", id);
                            return ApiResponse.success("Order item permanently deleted successfully", true);
                        })
                        .onFailure().recoverWithItem(e -> {
                            logger.error("Failed to permanently delete order item id={}", id, e);
                            return new ApiResponse<>("error",
                                    "Failed to permanently delete order item: " + e.getMessage(), false);
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> restoreAll() {
        logger.info("Restoring ALL trashed order items");

        return runTraced("restoreAllOrderItems", "restore_all_order_items", Attributes.empty(),
                () -> orderItemRepository.restoreAllDeleted()
                        .map(restored -> {
                            if (!restored) {
                                throw new ResourceNotFoundException("No order items found in trash");
                            }

                            logger.info("All order items restored successfully");
                            return ApiResponse.success("All order items restored successfully", true);
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deleteAll() {
        logger.info("Permanently deleting ALL trashed order items");

        return runTraced("deleteAllOrderItems", "delete_all_order_items", Attributes.empty(),
                () -> orderItemRepository.deleteAllDeleted()
                        .map(deleted -> {
                            if (!deleted) {
                                throw new ResourceNotFoundException("No order items found in trash");
                            }

                            logger.info("All order items permanently deleted successfully");
                            return ApiResponse.success("All order items permanently deleted successfully", true);
                        }));
    }

    private <T> Uni<T> runTraced(String operationName, String method, Attributes attributes,
            java.util.function.Supplier<Uni<T>> supplier) {
        return tracingMetrics.traceAndMeasure(operationName, method, attributes, supplier);
    }
}