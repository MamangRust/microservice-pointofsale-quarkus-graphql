package com.sanedge.order_item.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Validator;
import pb.order.MutinyOrderCommandServiceGrpc.MutinyOrderCommandServiceStub;
import pb.order.MutinyOrderQueryServiceGrpc.MutinyOrderQueryServiceStub;
import pb.order.Order;
import pb.product.MutinyProductCommandServiceGrpc.MutinyProductCommandServiceStub;
import pb.product.MutinyProductServiceGrpc.MutinyProductServiceStub;
import pb.product.Product;

@ExtendWith(MockitoExtension.class)
class OrderItemCommandServiceImplTest {

    @Mock private OrderItemRepository orderItemRepository;
    @Mock private Validator validator;
    @Mock private RedisService redisService;
    @Mock private TracingMetrics tracingMetrics;

    @Mock private MutinyOrderQueryServiceStub orderQueryService;
    @Mock private MutinyOrderCommandServiceStub orderCommandService;
    @Mock private MutinyProductServiceStub productQueryService;
    @Mock private MutinyProductCommandServiceStub productCommandService;

    private OrderItemCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderItemCommandServiceImpl(orderItemRepository, validator, redisService, tracingMetrics);
        // inject gRPC stubs
        service.orderQueryService = orderQueryService;
        service.orderCommandService = orderCommandService;
        service.productQueryService = productQueryService;
        service.productCommandService = productCommandService;

        // Lenient stub for traceAndMeasure
        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());
        lenient().when(orderItemRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));
        lenient().when(orderItemRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(true));

        // common gRPC stubs
        lenient().when(orderQueryService.findById(any()))
                .thenReturn(Uni.createFrom().item(Order.ApiResponseOrder.newBuilder()
                        .setStatus("success")
                        .setData(Order.OrderResponse.newBuilder().setId(100).setTotalPrice(10000).build())
                        .build()));
        lenient().when(orderCommandService.updateOrderTotalPrice(any()))
                .thenReturn(Uni.createFrom().item(Order.ApiResponseOrder.newBuilder()
                        .setStatus("success")
                        .build()));
        lenient().when(productQueryService.findById(any()))
                .thenReturn(Uni.createFrom().item(pb.product.Product.ApiResponseProduct.newBuilder()
                        .setStatus("success")
                        .setData(Product.ProductResponse.newBuilder().setId(10).setCountInStock(100).setPrice(5000).build())
                        .build()));
        lenient().when(productCommandService.update(any()))
                .thenReturn(Uni.createFrom().item(pb.product.Product.ApiResponseProduct.newBuilder()
                        .setStatus("success").build()));
    }

    private OrderItem createMockOrderItem(Long id) {
        OrderItem item = new OrderItem();
        item.setOrderItemId(id);
        item.setOrderId(100L);
        item.setProductId(10L);
        item.setQuantity(2);
        item.setPrice(5000);
        item.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        item.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return item;
    }

    // ---------- create ----------
    @Nested
    @DisplayName("create tests")
    class CreateTests {
        @Test void success() {
            CreateOrderItemRequest req = new CreateOrderItemRequest();
            req.setOrderId(100);
            req.setProductId(10);
            req.setQuantity(2);
            req.setPrice(5000);

            when(orderItemRepository.persist(any(OrderItem.class))).thenAnswer(inv -> {
                OrderItem item = inv.getArgument(0);
                item.setOrderItemId(1L);
                return Uni.createFrom().item(item);
            });
            when(orderItemRepository.findOrderItemByOrder(anyLong()))
                    .thenReturn(Uni.createFrom().item(List.of(createMockOrderItem(1L))));

            ApiResponse<OrderItemResponse> resp = service.create(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getId()).isEqualTo(1L);
        }

        @Test void orderNotFound_throwsException() {
            when(orderQueryService.findById(any()))
                    .thenReturn(Uni.createFrom().item(Order.ApiResponseOrder.newBuilder().setStatus("error").build()));

            CreateOrderItemRequest req = new CreateOrderItemRequest();
            req.setOrderId(100);
            req.setProductId(10);
            req.setQuantity(2);
            req.setPrice(5000);
            ApiResponse<OrderItemResponse> resp = service.create(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Order not found");
        }
    }

    // ---------- update ----------
    @Nested
    @DisplayName("update tests")
    class UpdateTests {
        @Test void success() {
            UpdateOrderItemRequest req = new UpdateOrderItemRequest();
            req.setOrderItemId(1);
            req.setOrderId(100);
            req.setProductId(10);
            req.setQuantity(3);
            req.setPrice(5000);

            OrderItem savedItem = createMockOrderItem(1L);
            savedItem.setQuantity(3);
            savedItem.setPrice(5000);
            when(orderItemRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(createMockOrderItem(1L)));
            when(orderItemRepository.persist(any(OrderItem.class))).thenReturn(Uni.createFrom().item(savedItem));
            when(orderItemRepository.findOrderItemByOrder(anyLong()))
                    .thenReturn(Uni.createFrom().item(List.of(createMockOrderItem(1L))));

            ApiResponse<OrderItemResponse> resp = service.update(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
        }

        @Test void notFound_returnsError() {
            UpdateOrderItemRequest req = new UpdateOrderItemRequest();
            req.setOrderItemId(999);
            when(orderItemRepository.findById(anyLong())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<OrderItemResponse> resp = service.update(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Order item not found");
        }
    }

    // ---------- trash ----------
    @Nested
    @DisplayName("trash tests")
    class TrashTests {
        @Test void success() {
            Integer id = 1;
            OrderItem trashed = createMockOrderItem(1L);
            trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
            when(orderItemRepository.trashed(anyLong())).thenReturn(Uni.createFrom().item(trashed));
            when(orderItemRepository.findOrderItemByOrder(anyLong()))
                    .thenReturn(Uni.createFrom().item(List.of(createMockOrderItem(1L))));

            ApiResponse<OrderItemResponseDeleteAt> resp = service.trash(id).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getDeletedAt()).isNotNull();
        }

        @Test void notFound_returnsError() {
            when(orderItemRepository.trashed(anyLong())).thenReturn(Uni.createFrom().nullItem());
            ApiResponse<OrderItemResponseDeleteAt> resp = service.trash(1).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Order item not found");
        }
    }

    // ---------- restore ----------
    @Nested
    @DisplayName("restore tests")
    class RestoreTests {
        @Test void success() {
            Integer id = 1;
            when(orderItemRepository.restore(anyLong())).thenReturn(Uni.createFrom().item(createMockOrderItem(1L)));
            when(orderItemRepository.findOrderItemByOrder(anyLong()))
                    .thenReturn(Uni.createFrom().item(List.of(createMockOrderItem(1L))));

            ApiResponse<OrderItemResponseDeleteAt> resp = service.restore(id).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getDeletedAt()).isNull();
        }

        @Test void notFound_returnsError() {
            when(orderItemRepository.restore(anyLong())).thenReturn(Uni.createFrom().nullItem());
            ApiResponse<OrderItemResponseDeleteAt> resp = service.restore(1).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Order item not found");
        }
    }

    // ---------- delete ----------
    @Nested
    @DisplayName("delete tests")
    class DeleteTests {
        @Test void success() {
            Integer id = 1;
            when(orderItemRepository.deletePermanent(anyLong())).thenReturn(Uni.createFrom().item(createMockOrderItem(1L)));
            when(orderItemRepository.findOrderItemByOrder(anyLong()))
                    .thenReturn(Uni.createFrom().item(List.of(createMockOrderItem(1L))));

            ApiResponse<Boolean> resp = service.delete(id).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test void notFound_returnsError() {
            when(orderItemRepository.deletePermanent(anyLong())).thenReturn(Uni.createFrom().nullItem());
            ApiResponse<Boolean> resp = service.delete(1).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Order item not found");
        }
    }

    // ---------- restoreAll ----------
    @Nested
    @DisplayName("restoreAll tests")
    class RestoreAllTests {
        @Test void success() {
            ApiResponse<Boolean> resp = service.restoreAll().await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test void noTrashed_throwsException() {
            when(orderItemRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.restoreAll().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No order items found in trash");
        }
    }

    // ---------- deleteAll ----------
    @Nested
    @DisplayName("deleteAll tests")
    class DeleteAllTests {
        @Test void success() {
            ApiResponse<Boolean> resp = service.deleteAll().await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test void noTrashed_throwsException() {
            when(orderItemRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.deleteAll().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No order items found in trash");
        }
    }
}