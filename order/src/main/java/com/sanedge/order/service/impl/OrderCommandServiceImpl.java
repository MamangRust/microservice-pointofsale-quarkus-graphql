package com.sanedge.order.service.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.adapter.cashier.CashierQueryPort;
import com.sanedge.common.adapter.merchant.MerchantQueryPort;
import com.sanedge.common.adapter.model.CreateOrderItemCmd;
import com.sanedge.common.adapter.model.Product;
import com.sanedge.common.adapter.model.UpdateOrderItemCmd;
import com.sanedge.common.adapter.model.UpdateProductCmd;
import com.sanedge.common.adapter.order_item.OrderItemCommandPort;
import com.sanedge.common.adapter.order_item.OrderItemQueryPort;
import com.sanedge.common.adapter.product.ProductCommandPort;
import com.sanedge.common.adapter.product.ProductQueryPort;
import com.sanedge.common.event.EventTopics;
import com.sanedge.order.domain.requests.CreateOrderRequest;
import com.sanedge.order.domain.requests.UpdateOrderRequest;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.entity.Order;
import com.sanedge.order.entity.Outbox;
import com.sanedge.order.repository.OrderCommandRepository;
import com.sanedge.order.repository.OrderQueryRepository;
import com.sanedge.order.repository.OutboxRepository;
import com.sanedge.order.service.OrderCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
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
    private final Validator validator;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    private final MerchantQueryPort merchantQueryPort;
    private final CashierQueryPort cashierQueryPort;
    private final ProductQueryPort productQueryPort;
    private final ProductCommandPort productCommandPort;
    private final OrderItemCommandPort orderItemCommandPort;
    private final OrderItemQueryPort orderItemQueryPort;
    private final OutboxRepository outboxRepository;

    @Inject
    public OrderCommandServiceImpl(OrderQueryRepository orderQueryRepository,
            OrderCommandRepository orderCommandRepository,
            Validator validator,
            RedisService redisService,
            TracingMetrics tracingMetrics,
            MerchantQueryPort merchantQueryPort,
            CashierQueryPort cashierQueryPort,
            ProductQueryPort productQueryPort,
            ProductCommandPort productCommandPort,
            OrderItemCommandPort orderItemCommandPort,
            OrderItemQueryPort orderItemQueryPort,
            OutboxRepository outboxRepository) {
        this.orderQueryRepository = orderQueryRepository;
        this.orderCommandRepository = orderCommandRepository;
        this.validator = validator;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
        this.merchantQueryPort = merchantQueryPort;
        this.cashierQueryPort = cashierQueryPort;
        this.productQueryPort = productQueryPort;
        this.productCommandPort = productCommandPort;
        this.orderItemCommandPort = orderItemCommandPort;
        this.orderItemQueryPort = orderItemQueryPort;
        this.outboxRepository = outboxRepository;
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

                    return merchantQueryPort.findByMerchantId(request.getMerchantId())
                            .chain(merchant -> cashierQueryPort.findById(request.getCashierId())
                                    .chain(cashier -> {
                                        Order order = new Order();
                                        order.setMerchantId(request.getMerchantId().longValue());
                                        order.setCashierId(request.getCashierId().longValue());
                                        order.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                                        order.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                                        order.setTotalPrice(0L);

                                        return orderCommandRepository.persist(order).map(v -> order);
                                    }))
                            .chain(order -> Multi.createFrom().iterable(request.getItems())
                                    .onItem()
                                    .transformToUniAndConcatenate(itemReq -> productQueryPort
                                            .findById(itemReq.getProductId())
                                            .chain(product -> {
                                                if (product.countInStock() < itemReq.getQuantity()) {
                                                    logger.error("Insufficient stock for product id={}",
                                                            itemReq.getProductId());
                                                    throw new IllegalArgumentException(
                                                            "Insufficient stock for product id="
                                                                    + itemReq.getProductId());
                                                }

                                                // Price is optional in the request — fall back to the product's price.
                                                Integer unitPrice = itemReq.getPrice() != null ? itemReq.getPrice()
                                                                : product.price();

                                                return Uni.combine().all().unis(
                                                        orderItemCommandPort.createOrderItem(new CreateOrderItemCmd(
                                                                order.getOrderId().intValue(),
                                                                itemReq.getProductId(),
                                                                itemReq.getQuantity(),
                                                                unitPrice)),
                                                        productCommandPort.update(new UpdateProductCmd(
                                                                product.id(),
                                                                product.merchantId(),
                                                                product.categoryId(),
                                                                product.name(),
                                                                product.description(),
                                                                product.price(),
                                                                product.countInStock() - itemReq.getQuantity(),
                                                                product.brand(),
                                                                product.weight(),
                                                                product.imageProduct()))).asTuple()
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
                                // F3: write order.created (order.event) and one order_item.created
                                // event per order line (order_item.event) into the transactional
                                // outbox — they are relayed to Kafka by the OutboxPublisher.
                                return persistOrderStats(savedOrder)
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

                                return cashierQueryPort.findById(request.getCashierId())
                                        .map(cashier -> order);
                            })
                            .chain(order -> Multi.createFrom().iterable(request.getItems())
                                    .onItem()
                                    .transformToUniAndConcatenate(itemReq -> productQueryPort
                                            .findById(itemReq.getProductId())
                                            .chain(product -> {
                                                // Price is optional in the request — fall back to the product's price.
                                                Integer unitPrice = itemReq.getPrice() != null ? itemReq.getPrice()
                                                                : product.price();

                                                if (itemReq.getOrderItemId() != null && itemReq.getOrderItemId() > 0) {
                                                    return orderItemCommandPort
                                                            .updateOrderItem(new UpdateOrderItemCmd(
                                                                    itemReq.getOrderItemId(),
                                                                    order.getOrderId().intValue(),
                                                                    itemReq.getProductId(),
                                                                    itemReq.getQuantity(),
                                                                    unitPrice))
                                                            .map(oi -> itemReq.getQuantity().longValue()
                                                                    * unitPrice.longValue());
                                                } else {
                                                    if (product.countInStock() < itemReq.getQuantity()) {
                                                        logger.error("Insufficient stock for product id={}",
                                                                itemReq.getProductId());
                                                        throw new IllegalArgumentException(
                                                                "Insufficient stock for product id="
                                                                        + itemReq.getProductId());
                                                    }

                                                    return Uni.combine().all().unis(
                                                            orderItemCommandPort.createOrderItem(new CreateOrderItemCmd(
                                                                    order.getOrderId().intValue(),
                                                                    itemReq.getProductId(),
                                                                    itemReq.getQuantity(),
                                                                    unitPrice)),
                                                            productCommandPort.update(new UpdateProductCmd(
                                                                    product.id(),
                                                                    product.merchantId(),
                                                                    product.categoryId(),
                                                                    product.name(),
                                                                    product.description(),
                                                                    product.price(),
                                                                    product.countInStock() - itemReq.getQuantity(),
                                                                    product.brand(),
                                                                    product.weight(),
                                                                    product.imageProduct()))).asTuple()
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

    private <T> Uni<T> runTraced(String operationName, String method, Attributes attributes,
            java.util.function.Supplier<Uni<T>> supplier) {
        return tracingMetrics.traceAndMeasure(operationName, method, attributes, supplier);
    }

    /**
     * Writes the domain events of a created order into the transactional
     * outbox: one {@code order.created} event to topic {@code order.event} and
     * one {@code order_item.created} event per order line to topic
     * {@code order_item.event}.
     *
     * <p>The rows are stored in the same database transaction as the order, so
     * an event exists if and only if the order was really created — even when
     * the broker is down. The {@code OutboxPublisher} relays them to Kafka
     * afterwards.
     */
    Uni<Void> persistOrderStats(Order order) {
        if (outboxRepository == null) {
            return Uni.createFrom().voidItem();
        }

        Uni<Void> writes = persistOutboxEvent(orderEvent(order));
        return orderItemQueryPort.findOrderItemByOrder(order.getOrderId().intValue())
                .chain(items -> {
                    Uni<Void> chain = writes;
                    for (var item : items) {
                        chain = chain.chain(() -> persistOutboxEvent(orderItemEvent(order, item)));
                    }
                    return chain;
                });
    }

    private Uni<Void> persistOutboxEvent(Outbox outbox) {
        return outboxRepository.persistEvent(outbox).replaceWithVoid();
    }

    private Outbox orderEvent(Order order) {
        String eventId = UUID.randomUUID().toString();
        JsonObject payload = new JsonObject()
                .put("order_id", order.getOrderId())
                .put("merchant_id", order.getMerchantId())
                .put("cashier_id", order.getCashierId())
                .put("status", "CREATED")
                .put("total_amount", order.getTotalPrice() != null ? order.getTotalPrice() : 0L);
        JsonObject event = new JsonObject()
                .put("event_id", eventId)
                .put("event_type", "order.created")
                .put("occurred_at", Instant.now().toString())
                .put("payload", payload);

        Outbox outbox = new Outbox();
        outbox.setAggregateType("ORDER");
        outbox.setAggregateId(String.valueOf(order.getOrderId()));
        outbox.setEventKey(String.valueOf(order.getOrderId()));
        outbox.setDomain(Outbox.DOMAIN_ORDER);
        outbox.setTopic(EventTopics.ORDER);
        outbox.setEventId(eventId);
        outbox.setPayload(event.encode());
        return outbox;
    }

    private Outbox orderItemEvent(Order order, com.sanedge.common.adapter.model.OrderItem item) {
        String eventId = UUID.randomUUID().toString();
        JsonObject payload = new JsonObject()
                .put("order_item_id", item.orderItemId())
                .put("order_id", item.orderId())
                .put("product_id", item.productId())
                .put("quantity", item.quantity())
                .put("unit_price", item.price())
                .put("subtotal", (long) item.price() * item.quantity());
        JsonObject event = new JsonObject()
                .put("event_id", eventId)
                .put("event_type", "order_item.created")
                .put("occurred_at", Instant.now().toString())
                .put("payload", payload);

        Outbox outbox = new Outbox();
        outbox.setAggregateType("ORDER_ITEM");
        outbox.setAggregateId(String.valueOf(item.orderItemId()));
        outbox.setEventKey(String.valueOf(item.orderItemId()));
        outbox.setDomain(Outbox.DOMAIN_ORDER);
        outbox.setTopic(EventTopics.ORDER_ITEM);
        outbox.setEventId(eventId);
        outbox.setPayload(event.encode());
        return outbox;
    }
}