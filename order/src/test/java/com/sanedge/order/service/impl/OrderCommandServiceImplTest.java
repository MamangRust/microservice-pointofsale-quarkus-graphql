package com.sanedge.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.order.domain.requests.CreateOrderRequest;
import com.sanedge.order.domain.requests.UpdateOrderRequest;
import com.sanedge.order.domain.requests.CreateOrderItemRequest;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.entity.Order;
import com.sanedge.order.entity.Outbox;
import com.sanedge.order.entity.OutboxStatus;
import com.sanedge.order.repository.OrderCommandRepository;
import com.sanedge.order.repository.OrderQueryRepository;
import com.sanedge.order.repository.OutboxRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Validator;
import com.sanedge.common.adapter.cashier.CashierQueryPort;
import com.sanedge.common.adapter.merchant.MerchantQueryPort;
import com.sanedge.common.adapter.model.Cashier;
import com.sanedge.common.adapter.model.Merchant;
import com.sanedge.common.adapter.model.Product;
import com.sanedge.common.adapter.model.OrderItem;
import com.sanedge.common.adapter.order_item.OrderItemCommandPort;
import com.sanedge.common.adapter.order_item.OrderItemQueryPort;
import com.sanedge.common.adapter.product.ProductCommandPort;
import com.sanedge.common.adapter.product.ProductQueryPort;

@ExtendWith(MockitoExtension.class)
class OrderCommandServiceImplTest {

    @Mock
    private OrderQueryRepository orderQueryRepo;
    @Mock
    private OrderCommandRepository orderCommandRepo;
    @Mock
    private Validator validator;
    @Mock
    private RedisService redisService;
    @Mock
    private TracingMetrics tracingMetrics;

    @Mock
    private MerchantQueryPort merchantQueryPort;
    @Mock
    private CashierQueryPort cashierQueryPort;
    @Mock
    private ProductQueryPort productQueryPort;
    @Mock
    private ProductCommandPort productCommandPort;
    @Mock
    private OrderItemCommandPort orderItemCommandPort;
    @Mock
    private OrderItemQueryPort orderItemQueryPort;
    @Mock
    private OutboxRepository outboxRepository;

    private OrderCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderCommandServiceImpl(orderQueryRepo, orderCommandRepo, validator, redisService,
                tracingMetrics, merchantQueryPort, cashierQueryPort, productQueryPort, productCommandPort,
                orderItemCommandPort, orderItemQueryPort, outboxRepository);

        // Lenient stubs for traceAndMeasure
        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());
        lenient().when(orderCommandRepo.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));
        lenient().when(orderCommandRepo.restoreAllDeleted()).thenReturn(Uni.createFrom().item(true));

        // common port stubs for create/update
        lenient().when(merchantQueryPort.findByMerchantId(anyInt()))
                .thenReturn(Uni.createFrom().item(new Merchant(10, "", "", "", 100, null, null)));
        lenient().when(cashierQueryPort.findById(anyInt()))
                .thenReturn(Uni.createFrom().item(new Cashier(100, 10, "", null, null)));
        lenient().when(productQueryPort.findById(anyInt()))
                .thenReturn(Uni.createFrom().item(new Product(1, 10, 1, "name", "desc", 5000, 100, "brand", 1, 0.0f,
                        "slug", "img", "barcode", null, null)));
        lenient().when(productCommandPort.update(any()))
                .thenReturn(Uni.createFrom().item(new Product(1, 10, 1, "name", "desc", 5000, 98, "brand", 1, 0.0f,
                        "slug", "img", "barcode", null, null)));
        lenient().when(orderItemCommandPort.createOrderItem(any()))
                .thenReturn(Uni.createFrom().item(new OrderItem(1, 1, 1, 2, 5000, null, null)));
        lenient().when(orderItemCommandPort.updateOrderItem(any()))
                .thenReturn(Uni.createFrom().item(new OrderItem(1, 1, 1, 2, 5000, null, null)));
        lenient().when(orderItemQueryPort.findOrderItemByOrder(anyInt()))
                .thenReturn(Uni.createFrom().item(List.of(new OrderItem(1, 1, 1, 2, 5000, null, null))));

        // Transactional outbox used by the stats pipeline
        lenient().when(outboxRepository.persistEvent(any(Outbox.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Outbox) inv.getArgument(0)));
    }

    private Order createMockOrder(Long id) {
        Order order = new Order();
        order.setOrderId(id);
        order.setCashierId(100L);
        order.setMerchantId(10L);
        order.setTotalPrice(50000L);
        order.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        order.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return order;
    }

    // ---------- create ----------
    @Nested
    @DisplayName("create tests")
    class CreateTests {
        @Test
        void success() {
            CreateOrderRequest req = new CreateOrderRequest();
            req.setMerchantId(10);
            req.setCashierId(100);
            CreateOrderItemRequest item = new CreateOrderItemRequest();
            item.setProductId(1);
            item.setQuantity(2);
            item.setPrice(5000);
            req.setItems(List.of(item));

            when(orderCommandRepo.persist(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setOrderId(1L);
                return Uni.createFrom().item(o);
            });

            ApiResponse<OrderResponse> resp = service.create(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getId()).isEqualTo(1);
            assertThat(resp.data().getTotalPrice()).isEqualTo(10000L); // 2 * 5000
        }

        @Test
        void writesOrderAndOrderItemEventsIntoTheOutbox() {
            CreateOrderRequest req = new CreateOrderRequest();
            req.setMerchantId(10);
            req.setCashierId(100);
            CreateOrderItemRequest item = new CreateOrderItemRequest();
            item.setProductId(1);
            item.setQuantity(2);
            item.setPrice(5000);
            req.setItems(List.of(item));

            when(orderCommandRepo.persist(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setOrderId(1L);
                return Uni.createFrom().item(o);
            });

            service.create(req).await().indefinitely();

            ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
            verify(outboxRepository, times(2)).persistEvent(captor.capture());

            List<Outbox> rows = captor.getAllValues();
            assertThat(rows).extracting(Outbox::getTopic)
                    .containsExactlyInAnyOrder("order.event", "order_item.event");
            assertThat(rows).allSatisfy(row -> {
                assertThat(row.getDomain()).isEqualTo("order");
                assertThat(row.getEventId()).isNotBlank();
                assertThat(row.getStatus()).isEqualTo(OutboxStatus.PENDING);
                assertThat(row.getPayload()).contains("event_id");
            });
        }

        @Test
        void merchantNotFound_returnsError() {
            when(merchantQueryPort.findByMerchantId(anyInt()))
                    .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Merchant not found")));

            CreateOrderRequest req = new CreateOrderRequest();
            req.setMerchantId(10);
            req.setCashierId(100);
            ApiResponse<OrderResponse> resp = service.create(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Merchant not found");
        }
    }

    // ---------- trash ----------
    @Nested
    @DisplayName("trash tests")
    class TrashTests {
        @Test
        void success() {
            Integer id = 1;
            Order trashed = createMockOrder(1L);
            trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
            when(orderCommandRepo.trashed(anyLong())).thenReturn(Uni.createFrom().item(trashed));

            ApiResponse<OrderResponseDeleteAt> resp = service.trash(id).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getDeletedAt()).isNotNull();
        }

        @Test
        void notFound_returnsError() {
            Integer id = 999;
            when(orderCommandRepo.trashed(anyLong())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<OrderResponseDeleteAt> resp = service.trash(id).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Order not found");
        }
    }

    // ---------- restore ----------
    @Nested
    @DisplayName("restore tests")
    class RestoreTests {
        @Test
        void success() {
            Integer id = 1;
            when(orderCommandRepo.restore(anyLong())).thenReturn(Uni.createFrom().item(createMockOrder(1L)));

            ApiResponse<OrderResponseDeleteAt> resp = service.restore(id).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getDeletedAt()).isNull();
        }

        @Test
        void notFound_returnsError() {
            Integer id = 999;
            when(orderCommandRepo.restore(anyLong())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<OrderResponseDeleteAt> resp = service.restore(id).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Order not found");
        }
    }

    // ---------- delete (permanent) ----------
    @Nested
    @DisplayName("delete tests")
    class DeleteTests {
        @Test
        void success() {
            Integer id = 1;
            when(orderCommandRepo.deletePermanent(anyLong())).thenReturn(Uni.createFrom().item(createMockOrder(1L)));

            ApiResponse<Boolean> resp = service.delete(id).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test
        void notFound_returnsError() {
            Integer id = 999;
            when(orderCommandRepo.deletePermanent(anyLong())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<Boolean> resp = service.delete(id).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Order not found");
        }
    }

    // ---------- restoreAll ----------
    @Nested
    @DisplayName("restoreAll tests")
    class RestoreAllTests {
        @Test
        void success() {
            ApiResponse<Boolean> resp = service.restoreAll().await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test
        void noTrashed_throwsException() {
            when(orderCommandRepo.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.restoreAll().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No orders found in trash");
        }
    }

    // ---------- deleteAll ----------
    @Nested
    @DisplayName("deleteAll tests")
    class DeleteAllTests {
        @Test
        void success() {
            ApiResponse<Boolean> resp = service.deleteAll().await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test
        void noTrashed_throwsException() {
            when(orderCommandRepo.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.deleteAll().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No orders found in trash");
        }
    }

    // ---------- updateOrderTotalPrice ----------
    @Nested
    @DisplayName("updateOrderTotalPrice tests")
    class UpdateOrderTotalPriceTests {
        @Test
        void success() {
            Integer orderId = 1;
            Integer newTotal = 99999;
            Order order = createMockOrder(1L);
            order.setTotalPrice(newTotal.longValue());
            when(orderQueryRepo.findOrderById(anyLong())).thenReturn(Uni.createFrom().item(createMockOrder(1L)));
            when(orderCommandRepo.persist(any(Order.class))).thenReturn(Uni.createFrom().item(order));

            ApiResponse<OrderResponse> resp = service.updateOrderTotalPrice(orderId, newTotal).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getTotalPrice()).isEqualTo(newTotal.longValue());
        }

        @Test
        void orderNotFound_returnsError() {
            when(orderQueryRepo.findOrderById(anyLong())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<OrderResponse> resp = service.updateOrderTotalPrice(999, 5000).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Order not found");
        }
    }
}