package com.sanedge.order.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.order.domain.requests.CreateOrderRequest;
import com.sanedge.order.domain.requests.UpdateOrderRequest;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.entity.Outbox;
import com.sanedge.order.entity.Order;
import com.sanedge.order.repository.OrderCommandRepository;
import com.sanedge.order.repository.OrderQueryRepository;
import com.sanedge.order.service.OrderCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@ApplicationScoped
public class OrderCommandServiceImpl implements OrderCommandService {
    private static final Logger logger = LoggerFactory.getLogger(OrderCommandServiceImpl.class);

    private final OrderQueryRepository orderQueryRepository;
    private final OrderCommandRepository orderCommandRepository;
    private final com.sanedge.order.repository.OutboxRepository outboxRepository;
    private final Validator validator;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    @GrpcClient("merchant")
    pb.merchant.MutinyMerchantQueryServiceGrpc.MutinyMerchantQueryServiceStub merchantQueryService;

    @GrpcClient("cashier")
    pb.cashier.MutinyCashierServiceGrpc.MutinyCashierServiceStub cashierQueryService;

    @GrpcClient("product")
    pb.product.MutinyProductServiceGrpc.MutinyProductServiceStub productQueryService;

    @GrpcClient("product")
    pb.product.MutinyProductCommandServiceGrpc.MutinyProductCommandServiceStub productCommandService;

    @GrpcClient("order_item")
    pb.order_item.MutinyOrderItemCommandServiceGrpc.MutinyOrderItemCommandServiceStub orderItemCommandService;

    @Inject
    public OrderCommandServiceImpl(OrderQueryRepository orderQueryRepository,
            OrderCommandRepository orderCommandRepository,
            com.sanedge.order.repository.OutboxRepository outboxRepository,
            Validator validator,
            RedisService redisService,
            TracingMetrics tracingMetrics) {
        this.orderQueryRepository = orderQueryRepository;
        this.orderCommandRepository = orderCommandRepository;
        this.outboxRepository = outboxRepository;
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
    public Uni<ApiResponse<OrderResponse>> create(CreateOrderRequest request) {
        logger.info("Creating new order for merchantId={} and cashierId={}", request.getMerchantId(),
                request.getCashierId());
        Attributes attrs = Attributes.builder()
                .put("merchant.id", request.getMerchantId() != null ? request.getMerchantId().toString() : "null")
                .put("cashier.id", request.getCashierId() != null ? request.getCashierId().toString() : "null")
                .build();

        return runTraced("createOrder", "create_order", attrs,
                () -> {
                    try {
                        validateRequest(request);
                    } catch (Exception e) {
                        return Uni.createFrom().item(new ApiResponse<>("error", e.getMessage(), null));
                    }

                    return merchantQueryService
                            .findByIdMerchant(pb.merchant.Merchant.FindByIdMerchantRequest.newBuilder()
                                    .setMerchantId(request.getMerchantId())
                                    .build())
                            .chain(merchantResponse -> {
                                if (!"success".equals(merchantResponse.getStatus()) || !merchantResponse.hasData()) {
                                    logger.error("Merchant not found with id={}", request.getMerchantId());
                                    throw new ResourceNotFoundException("Merchant not found");
                                }
                                return cashierQueryService
                                        .findById(pb.cashier.Cashier.FindByIdCashierRequest.newBuilder()
                                                .setId(request.getCashierId())
                                                .build());
                            })
                            .chain(cashierResponse -> {
                                if (!"success".equals(cashierResponse.getStatus()) || !cashierResponse.hasData()) {
                                    logger.error("Cashier not found with id={}", request.getCashierId());
                                    throw new ResourceNotFoundException("Cashier not found");
                                }

                                Order order = new Order();
                                order.setMerchantId(request.getMerchantId().longValue());
                                order.setCashierId(request.getCashierId().longValue());
                                order.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                                order.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                                order.setTotalPrice(0L);

                                return orderCommandRepository.persist(order).map(v -> order);
                            })
                            .chain(order -> Multi.createFrom().iterable(request.getItems())
                                    .onItem()
                                    .transformToUniAndConcatenate(itemReq -> productQueryService
                                            .findById(pb.product.Product.FindByIdProductRequest.newBuilder()
                                                    .setId(itemReq.getProductId())
                                                    .build())
                                            .chain(productResponse -> {
                                                if (!"success".equals(productResponse.getStatus())
                                                        || !productResponse.hasData()) {
                                                    logger.error("Product not found with id={}",
                                                            itemReq.getProductId());
                                                    throw new ResourceNotFoundException(
                                                            "Product not found with id=" + itemReq.getProductId());
                                                }
                                                pb.product.Product.ProductResponse product = productResponse.getData();
                                                if (product.getCountInStock() < itemReq.getQuantity()) {
                                                    logger.error("Insufficient stock for product id={}",
                                                            itemReq.getProductId());
                                                    throw new IllegalArgumentException(
                                                            "Insufficient stock for product id="
                                                                    + itemReq.getProductId());
                                                }

                                                // Price is optional in the request — fall back to the product's price.
                                                Integer unitPrice = itemReq.getPrice() != null ? itemReq.getPrice()
                                                                : product.getPrice();

                                                pb.order_item.OrderItemCommand.CreateOrderItemRequest orderItemReq = pb.order_item.OrderItemCommand.CreateOrderItemRequest
                                                        .newBuilder()
                                                        .setOrderId(order.getOrderId().intValue())
                                                        .setProductId(itemReq.getProductId())
                                                        .setQuantity(itemReq.getQuantity())
                                                        .setPrice(unitPrice)
                                                        .build();

                                                pb.product.ProductCommand.UpdateProductRequest productReq = pb.product.ProductCommand.UpdateProductRequest
                                                        .newBuilder()
                                                        .setProductId(product.getId())
                                                        .setMerchantId(product.getMerchantId())
                                                        .setCategoryId(product.getCategoryId())
                                                        .setName(product.getName())
                                                        .setDescription(product.getDescription())
                                                        .setPrice(product.getPrice())
                                                        .setCountInStock(
                                                                product.getCountInStock() - itemReq.getQuantity())
                                                        .setBrand(product.getBrand())
                                                        .setWeight(product.getWeight())
                                                        .setImageProduct(product.getImageProduct())
                                                        .build();

                                                return Uni.combine().all().unis(
                                                        orderItemCommandService.createOrderItem(orderItemReq),
                                                        productCommandService.update(productReq)).asTuple()
                                                        .map(t -> itemReq.getQuantity().longValue()
                                                                * unitPrice.longValue());
                                            }))
                                    .collect().in(java.util.ArrayList::new, List::add)
                                    .map(list -> {
                                        long sum = list.stream().mapToLong(val -> ((Number) val).longValue()).sum();
                                        order.setTotalPrice(sum);
                                        return order;
                                    }))
                            .chain(order -> orderCommandRepository.persist(order).map(v -> order))
                            .chain(savedOrder -> {
                                // F3: Write order.created event to outbox (same @WithTransaction)
                                return persistOrderCreatedEvent(savedOrder)
                                        .replaceWith(savedOrder);
                            })
                            .map(savedOrder -> {
                                logger.info("Order created successfully with id={}", savedOrder.getOrderId());
                                return ApiResponse.success("Order created successfully",
                                        OrderResponse.from(savedOrder));
                            })
                            .onFailure().recoverWithItem(e -> {
                                logger.error("Failed to create order", e);
                                return new ApiResponse<>("error", "Failed to create your order: " + e.getMessage(),
                                        null);
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<OrderResponse>> update(UpdateOrderRequest request) {
        logger.info("Updating order id={}", request.getOrderId());
        Attributes attrs = Attributes.builder()
                .put("order.id", request.getOrderId() != null ? request.getOrderId().toString() : "null")
                .build();

        return runTraced("updateOrder", "update_order", attrs,
                () -> {
                    try {
                        validateRequest(request);
                    } catch (Exception e) {
                        return Uni.createFrom().item(new ApiResponse<>("error", e.getMessage(), null));
                    }

                    String cacheKey = "order:" + request.getOrderId();

                    return orderQueryRepository.findOrderById(request.getOrderId().longValue())
                            .chain(order -> {
                                if (order == null) {
                                    logger.error("Order not found with id={}", request.getOrderId());
                                    throw new ResourceNotFoundException("Order not found");
                                }

                                return cashierQueryService
                                        .findById(pb.cashier.Cashier.FindByIdCashierRequest.newBuilder()
                                                .setId(request.getCashierId())
                                                .build())
                                        .chain(cashierResponse -> {
                                            if (!"success".equals(cashierResponse.getStatus())
                                                    || !cashierResponse.hasData()) {
                                                logger.error("Cashier not found with id={}", request.getCashierId());
                                                throw new ResourceNotFoundException("Cashier not found");
                                            }
                                            return Uni.createFrom().item(order);
                                        });
                            })
                            .chain(order -> Multi.createFrom().iterable(request.getItems())
                                    .onItem()
                                    .transformToUniAndConcatenate(itemReq -> productQueryService
                                            .findById(pb.product.Product.FindByIdProductRequest.newBuilder()
                                                    .setId(itemReq.getProductId())
                                                    .build())
                                            .chain(productResponse -> {
                                                if (!"success".equals(productResponse.getStatus())
                                                        || !productResponse.hasData()) {
                                                    logger.error("Product not found with id={}",
                                                            itemReq.getProductId());
                                                    throw new ResourceNotFoundException(
                                                            "Product not found with id=" + itemReq.getProductId());
                                                }
                                                pb.product.Product.ProductResponse product = productResponse.getData();

                                                // Price is optional in the request — fall back to the product's price.
                                                Integer unitPrice = itemReq.getPrice() != null ? itemReq.getPrice()
                                                                : product.getPrice();

                                                if (itemReq.getOrderItemId() != null && itemReq.getOrderItemId() > 0) {
                                                    return orderItemCommandService.updateOrderItem(
                                                            pb.order_item.OrderItemCommand.UpdateOrderItemRequest
                                                                    .newBuilder()
                                                                    .setOrderItemId(itemReq.getOrderItemId())
                                                                    .setOrderId(order.getOrderId().intValue())
                                                                    .setProductId(itemReq.getProductId())
                                                                    .setQuantity(itemReq.getQuantity())
                                                                    .setPrice(unitPrice)
                                                                    .build())
                                                            .map(resp -> {
                                                                if (!"success".equals(resp.getStatus())) {
                                                                    throw new ResourceNotFoundException(
                                                                            "Order item not found");
                                                                }
                                                                return itemReq.getQuantity().longValue()
                                                                        * unitPrice.longValue();
                                                            });
                                                } else {
                                                    if (product.getCountInStock() < itemReq.getQuantity()) {
                                                        logger.error("Insufficient stock for product id={}",
                                                                itemReq.getProductId());
                                                        throw new IllegalArgumentException(
                                                                "Insufficient stock for product id="
                                                                        + itemReq.getProductId());
                                                    }

                                                    pb.order_item.OrderItemCommand.CreateOrderItemRequest orderItemReq = pb.order_item.OrderItemCommand.CreateOrderItemRequest
                                                            .newBuilder()
                                                            .setOrderId(order.getOrderId().intValue())
                                                            .setProductId(itemReq.getProductId())
                                                            .setQuantity(itemReq.getQuantity())
                                                            .setPrice(unitPrice)
                                                            .build();

                                                    pb.product.ProductCommand.UpdateProductRequest productReq = pb.product.ProductCommand.UpdateProductRequest
                                                            .newBuilder()
                                                            .setProductId(product.getId())
                                                            .setMerchantId(product.getMerchantId())
                                                            .setCategoryId(product.getCategoryId())
                                                            .setName(product.getName())
                                                            .setDescription(product.getDescription())
                                                            .setPrice(product.getPrice())
                                                            .setCountInStock(
                                                                    product.getCountInStock() - itemReq.getQuantity())
                                                            .setBrand(product.getBrand())
                                                            .setWeight(product.getWeight())
                                                            .setImageProduct(product.getImageProduct())
                                                            .build();

                                                    return Uni.combine().all().unis(
                                                            orderItemCommandService.createOrderItem(orderItemReq),
                                                            productCommandService.update(productReq)).asTuple()
                                                            .map(t -> itemReq.getQuantity().longValue()
                                                                    * unitPrice.longValue());
                                                }
                                            }))
                                    .collect().in(java.util.ArrayList::new, List::add)
                                    .map(list -> {
                                        long sum = list.stream().mapToLong(val -> ((Number) val).longValue()).sum();
                                        order.setTotalPrice(sum);
                                        order.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                                        return order;
                                    }))
                            .chain(order -> orderCommandRepository.persist(order).map(v -> order))
                            .chain(savedOrder -> redisService.deleteReactive(cacheKey).map(v -> savedOrder))
                            .map(savedOrder -> {
                                logger.info("Order updated successfully with id={}", savedOrder.getOrderId());
                                return ApiResponse.success("Order updated successfully",
                                        OrderResponse.from(savedOrder));
                            })
                            .onFailure().recoverWithItem(e -> {
                                logger.error("Failed to update order id={}", request.getOrderId(), e);
                                return new ApiResponse<>("error", "Failed to update your order: " + e.getMessage(),
                                        null);
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<OrderResponseDeleteAt>> trash(Integer id) {
        logger.info("Trashing order id={}", id);
        Attributes attrs = Attributes.builder()
                .put("order.id", id.toString())
                .build();

        return runTraced("trashOrder", "trash_order", attrs,
                () -> {
                    String cacheKey = "order:" + id;

                    return orderCommandRepository.trashed(id.longValue())
                            .chain(order -> {
                                if (order == null) {
                                    logger.error("Order not found for trashing id={}", id);
                                    throw new ResourceNotFoundException("Order not found");
                                }
                                return redisService.deleteReactive(cacheKey).map(v -> order);
                            })
                            .map(order -> {
                                logger.info("Order trashed successfully with id={}", id);
                                return ApiResponse.success("Order trashed successfully",
                                        OrderResponseDeleteAt.from(order));
                            })
                            .onFailure().recoverWithItem(e -> {
                                logger.error("Failed to trash order id={}", id, e);
                                return new ApiResponse<>("error", "Failed to trash order: " + e.getMessage(), null);
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<OrderResponseDeleteAt>> restore(Integer id) {
        logger.info("Restoring order id={}", id);
        Attributes attrs = Attributes.builder()
                .put("order.id", id.toString())
                .build();

        return runTraced("restoreOrder", "restore_order", attrs,
                () -> {
                    String cacheKey = "order:" + id;

                    return orderCommandRepository.restore(id.longValue())
                            .chain(order -> {
                                if (order == null) {
                                    logger.error("Order not found for restoration id={}", id);
                                    throw new ResourceNotFoundException("Order not found or not in trash");
                                }
                                return redisService.deleteReactive(cacheKey).map(v -> order);
                            })
                            .map(order -> {
                                logger.info("Order restored successfully with id={}", id);
                                return ApiResponse.success("Order restored successfully",
                                        OrderResponseDeleteAt.from(order));
                            })
                            .onFailure().recoverWithItem(e -> {
                                logger.error("Failed to restore order id={}", id, e);
                                return new ApiResponse<>("error", "Failed to restore order: " + e.getMessage(), null);
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> delete(Integer id) {
        logger.info("Permanently deleting order id={}", id);
        Attributes attrs = Attributes.builder()
                .put("order.id", id.toString())
                .build();

        return runTraced("deleteOrderPermanent", "delete_order_permanent", attrs,
                () -> {
                    String cacheKey = "order:" + id;

                    return orderCommandRepository.deletePermanent(id.longValue())
                            .chain(order -> {
                                if (order == null) {
                                    logger.error("Order not found for permanent deletion id={}", id);
                                    throw new ResourceNotFoundException("Order not found or not in trash");
                                }
                                return redisService.deleteReactive(cacheKey).map(v -> true);
                            })
                            .map(deleted -> {
                                logger.info("Order permanently deleted with id={}", id);
                                return ApiResponse.success("Order permanently deleted successfully", true);
                            })
                            .onFailure().recoverWithItem(e -> {
                                logger.error("Failed to permanently delete order id={}", id, e);
                                return new ApiResponse<>("error",
                                        "Failed to permanently delete order: " + e.getMessage(), false);
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> restoreAll() {
        logger.info("Restoring ALL trashed orders");

        return runTraced("restoreAllOrders", "restore_all_orders", Attributes.empty(),
                () -> orderCommandRepository.restoreAllDeleted()
                        .map(restored -> {
                            if (!restored) {
                                throw new ResourceNotFoundException("No orders found in trash");
                            }

                            logger.info("All orders restored successfully");
                            return ApiResponse.success("All orders restored successfully", true);
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deleteAll() {
        logger.info("Permanently deleting ALL trashed orders");

        return runTraced("deleteAllOrders", "delete_all_orders", Attributes.empty(),
                () -> orderCommandRepository.deleteAllDeleted()
                        .map(deleted -> {
                            if (!deleted) {
                                throw new ResourceNotFoundException("No orders found in trash");
                            }

                            logger.info("All orders permanently deleted successfully");
                            return ApiResponse.success("All orders permanently deleted successfully", true);
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<OrderResponse>> updateOrderTotalPrice(Integer orderId, Integer totalPrice) {
        logger.info("Updating order total price for orderId={} to totalPrice={}", orderId, totalPrice);
        Attributes attrs = Attributes.builder()
                .put("order.id", orderId != null ? orderId.toString() : "null")
                .build();

        return runTraced("updateOrderTotalPrice", "update_order_total_price", attrs,
                () -> orderQueryRepository.findOrderById(orderId.longValue())
                        .chain(order -> {
                            if (order == null) {
                                logger.error("Order not found with id={}", orderId);
                                throw new ResourceNotFoundException("Order not found with id=" + orderId);
                            }
                            order.setTotalPrice(totalPrice.longValue());
                            return orderCommandRepository.persist(order);
                        })
                        .map(savedOrder -> {
                            logger.info("Order total price updated successfully for id={}", savedOrder.getOrderId());
                            return ApiResponse.success("Order total price updated successfully",
                                    OrderResponse.from(savedOrder));
                        })
                        .onFailure().recoverWithItem(e -> {
                            logger.error("Failed to update order total price", e);
                            return new ApiResponse<>("error", "Failed to update order total price: " + e.getMessage(),
                                    null);
                        }));
    }

    /**
     * Persists an order.created event to the outbox table within the same
     * DB transaction as the order creation (transactional outbox pattern).
     */
    private Uni<Void> persistOrderCreatedEvent(Order order) {
        io.vertx.core.json.JsonObject payload = new io.vertx.core.json.JsonObject()
                .put("order_id", order.getOrderId())
                .put("merchant_id", order.getMerchantId())
                .put("cashier_id", order.getCashierId())
                .put("total_amount", order.getTotalPrice())
                .put("status", "created")
                .put("occurred_at", java.time.Instant.now().toString());

        // Attach standard event envelope (event_id, schema_version, event_type, occurred_at)
        io.vertx.core.json.JsonObject eventPayload =
                com.sanedge.common.event.EventEnvelope.withDefaults(payload, "order.created");

        String eventId = eventPayload.getString("event_id");

        Outbox outbox = new Outbox();
        outbox.setAggregateType("Order");
        outbox.setAggregateId(String.valueOf(order.getOrderId()));
        outbox.setTopic("stats.pos.order.event");
        outbox.setPayload(eventPayload.encode());
        outbox.setDomain("order");
        outbox.setEventId(eventId);

        return outboxRepository.persist(outbox).replaceWithVoid();
    }

    private <T> Uni<T> runTraced(String operationName, String method, Attributes attributes,
            java.util.function.Supplier<Uni<T>> supplier) {
        return tracingMetrics.traceAndMeasure(operationName, method, attributes, supplier);
    }
}