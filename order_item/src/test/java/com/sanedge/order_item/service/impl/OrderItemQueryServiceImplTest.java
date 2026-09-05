package com.sanedge.order_item.service.impl;

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
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.order_item.domain.requests.FindAllOrderItems;
import com.sanedge.order_item.domain.response.OrderItemResponse;
import com.sanedge.order_item.domain.response.OrderItemResponseDeleteAt;
import com.sanedge.order_item.entity.OrderItem;
import com.sanedge.order_item.repository.OrderItemRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class OrderItemQueryServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private OrderItemQueryServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new OrderItemQueryServiceImpl(orderItemRepository, redisService, objectMapper, tracingMetrics);

        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
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

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }

    private FindAllOrderItems findAllReq(int page, int size, String search) {
        FindAllOrderItems req = new FindAllOrderItems();
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search == null ? "" : search);
        return req;
    }

    @Nested
    @DisplayName("findAll tests")
    class FindAllTests {
        @Test void cacheMiss_fetchesFromDb() {
            FindAllOrderItems req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(orderItemRepository.findOrderItems(any(FindAllOrderItems.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockOrderItem(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<PagedResult<OrderItemResponse>> result = service.findAll(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data().getData()).hasSize(1);
            assertThat(result.data().getData().get(0).getProductId()).isEqualTo(10L);
        }
        @Test void cacheHit_returnsCached() {
            FindAllOrderItems req = findAllReq(1, 10, "");
            // Pre-construct JSON to avoid Jackson record+generics deserialization issues in unit tests
            String cachedJson = "{\"status\":\"success\",\"message\":\"Success\",\"data\":{\"data\":[{\"id\":1,\"orderId\":100,\"productId\":10,\"quantity\":2,\"price\":5000}],\"totalRecords\":1}}";
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(cachedJson));

            ApiResponse<PagedResult<OrderItemResponse>> result = service.findAll(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findByActive tests")
    class FindByActiveTests {
        @Test void cacheMiss_fetchesFromDb() {
            FindAllOrderItems req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(orderItemRepository.findActiveOrderItems(any(FindAllOrderItems.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockOrderItem(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<PagedResult<OrderItemResponseDeleteAt>> result = service.findByActive(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findByTrashed tests")
    class FindByTrashedTests {
        @Test void cacheMiss_fetchesFromDb() {
            FindAllOrderItems req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(orderItemRepository.findTrashedOrderItems(any(FindAllOrderItems.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockOrderItem(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<PagedResult<OrderItemResponseDeleteAt>> result = service.findByTrashed(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findOrderItemByOrder tests")
    class FindOrderItemByOrderTests {
        @Test void cacheMiss_fetchesFromDb() {
            Integer orderId = 100;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(orderItemRepository.findOrderItemByOrder(anyLong()))
                    .thenReturn(Uni.createFrom().item(List.of(createMockOrderItem(1L), createMockOrderItem(2L))));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<List<OrderItemResponse>> result = service.findOrderItemByOrder(orderId).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(2);
        }
        @Test void cacheHit_returnsCached() {
            Integer orderId = 100;
            ApiResponse<List<OrderItemResponse>> cached = ApiResponse.success("Success",
                    List.of(OrderItemResponse.from(createMockOrderItem(1L))));
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(toJson(cached)));

            ApiResponse<List<OrderItemResponse>> result = service.findOrderItemByOrder(orderId).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }
}