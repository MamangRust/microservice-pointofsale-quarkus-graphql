package com.sanedge.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.order.domain.requests.FindAllOrderByMerchantRequest;
import com.sanedge.order.domain.requests.FindAllOrderRequest;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.entity.Order;
import com.sanedge.order.repository.OrderQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceImplTest {

    @Mock
    private OrderQueryRepository orderQueryRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private OrderQueryServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new OrderQueryServiceImpl(orderQueryRepository, redisService, objectMapper, tracingMetrics);

        // Lenient stubs to execute the supplier directly
        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
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

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private FindAllOrderRequest findAllReq(int page, int size, String search) {
        FindAllOrderRequest req = new FindAllOrderRequest();
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search == null ? "" : search);
        return req;
    }

    private FindAllOrderByMerchantRequest findAllByMerchantReq(Long merchantId, int page, int size, String search) {
        FindAllOrderByMerchantRequest req = new FindAllOrderByMerchantRequest();
        req.setMerchantId(merchantId.intValue());
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search == null ? "" : search);
        return req;
    }

    @Nested
    @DisplayName("findAll tests")
    class FindAllTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            FindAllOrderRequest req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(orderQueryRepository.findOrders(any(FindAllOrderRequest.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockOrder(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<OrderResponse>> result = service.findAll(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }

        @Test
        void cacheHit_returnsCached() {
            FindAllOrderRequest req = findAllReq(1, 10, "");
            ApiResponsePagination<List<OrderResponse>> cached = new ApiResponsePagination<>(
                    "success", "Orders retrieved successfully", List.of(OrderResponse.from(createMockOrder(1L))), null);
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(toJson(cached)));

            ApiResponsePagination<List<OrderResponse>> result = service.findAll(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findByActive tests")
    class FindByActiveTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            FindAllOrderRequest req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(orderQueryRepository.findActiveOrders(any(FindAllOrderRequest.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockOrder(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<OrderResponseDeleteAt>> result = service.findByActive(req).await()
                    .indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findByTrashed tests")
    class FindByTrashedTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            FindAllOrderRequest req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(orderQueryRepository.findTrashedOrders(any(FindAllOrderRequest.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockOrder(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<OrderResponseDeleteAt>> result = service.findByTrashed(req).await()
                    .indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findByMerchantId tests")
    class FindByMerchantIdTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            FindAllOrderByMerchantRequest req = findAllByMerchantReq(10L, 1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(orderQueryRepository.findOrdersByMerchant(any(FindAllOrderByMerchantRequest.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockOrder(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<OrderResponse>> result = service.findByMerchantId(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findById tests")
    class FindByIdTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            Integer id = 1;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(orderQueryRepository.findOrderById(anyLong()))
                    .thenReturn(Uni.createFrom().item(createMockOrder(id.longValue())));
            when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<OrderResponse> result = service.findById(id).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data().getId()).isEqualTo(1L);
        }

        @Test
        void notFound_returnsError() {
            Integer id = 999;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(orderQueryRepository.findOrderById(anyLong())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<OrderResponse> result = service.findById(id).await().indefinitely();
            assertThat(result.status()).isEqualTo("error");
            assertThat(result.message()).contains("Order not found");
        }
    }
}