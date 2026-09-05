package com.sanedge.order.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.service.OrderQueryService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.order.Order;
import pb.order.OrderQuery;

@ExtendWith(MockitoExtension.class)
class OrderQueryGrpcHandlerTest {

    @Mock
    private OrderQueryService orderQueryService;

    private OrderQueryGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderQueryGrpcHandler();
        handler.orderQueryService = orderQueryService;
    }

    // helpers
    private OrderResponse createOrderResponse(Long id) {
        OrderResponse r = new OrderResponse();
        r.setId(id);
        r.setMerchantId(10);
        r.setCashierId(100);
        r.setTotalPrice(50000L);
        r.setCreatedAt(LocalDateTime.now().toString());
        r.setUpdatedAt(LocalDateTime.now().toString());
        return r;
    }

    private OrderResponseDeleteAt createOrderDeleteAt(Long id) {
        OrderResponseDeleteAt r = new OrderResponseDeleteAt();
        r.setId(id);
        r.setMerchantId(10);
        r.setCashierId(100);
        r.setTotalPrice(50000L);
        r.setCreatedAt(LocalDateTime.now().toString());
        r.setUpdatedAt(LocalDateTime.now().toString());
        r.setDeletedAt(LocalDateTime.now().toString());
        return r;
    }

    // findById
    @Test
    @DisplayName("findById - success")
    void findById_Success() {
        Order.FindByIdOrderRequest request = Order.FindByIdOrderRequest.newBuilder().setId(1).build();
        OrderResponse data = createOrderResponse(1L);
        ApiResponse<OrderResponse> apiResp = ApiResponse.success("Order found", data);
        when(orderQueryService.findById(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        Order.ApiResponseOrder response = handler.findById(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(1);
        assertThat(response.getData().getTotalPrice()).isEqualTo(50000);
    }

    @Test
    @DisplayName("findById - error")
    void findById_Error() {
        when(orderQueryService.findById(anyInt()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
        try {
            handler.findById(Order.FindByIdOrderRequest.newBuilder().setId(1).build()).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e).isNotNull();
        }
    }

    // findAll
    @Test
    @DisplayName("findAll - success")
    void findAll_Success() {
        Order.FindAllOrderRequest request = Order.FindAllOrderRequest.newBuilder()
                .setPage(1).setPageSize(10).build();
        OrderResponse data = createOrderResponse(1L);
        ApiResponsePagination<List<OrderResponse>> apiResp = new ApiResponsePagination<>(
                "success", "Orders retrieved", List.of(data), null);
        when(orderQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

        OrderQuery.ApiResponsePaginationOrder response = handler.findAll(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getData(0).getCashierId()).isEqualTo(100);
    }

    @Test
    @DisplayName("findAll - error")
    void findAll_Error() {
        when(orderQueryService.findAll(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findAll(Order.FindAllOrderRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // findByMerchant
    @Test
    @DisplayName("findByMerchant - success")
    void findByMerchant_Success() {
        Order.FindAllOrderMerchantRequest request = Order.FindAllOrderMerchantRequest.newBuilder()
                .setMerchantId(10).setPage(1).setPageSize(10).build();
        OrderResponse data = createOrderResponse(1L);
        ApiResponsePagination<List<OrderResponse>> apiResp = new ApiResponsePagination<>(
                "success", "Orders by merchant", List.of(data), null);
        when(orderQueryService.findByMerchantId(any())).thenReturn(Uni.createFrom().item(apiResp));

        OrderQuery.ApiResponsePaginationOrder response = handler.findByMerchant(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData(0).getMerchantId()).isEqualTo(10);
    }

    @Test
    @DisplayName("findByMerchant - error")
    void findByMerchant_Error() {
        when(orderQueryService.findByMerchantId(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findByMerchant(Order.FindAllOrderMerchantRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // findByActive
    @Test
    @DisplayName("findByActive - success")
    void findByActive_Success() {
        Order.FindAllOrderRequest request = Order.FindAllOrderRequest.newBuilder().setPage(1).build();
        OrderResponseDeleteAt data = createOrderDeleteAt(1L);
        ApiResponsePagination<List<OrderResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                "success", "Active orders", List.of(data), null);
        when(orderQueryService.findByActive(any())).thenReturn(Uni.createFrom().item(apiResp));

        OrderQuery.ApiResponsePaginationOrderDeleteAt response = handler.findByActive(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getData(0).hasDeletedAt()).isTrue();
    }

    @Test
    @DisplayName("findByActive - error")
    void findByActive_Error() {
        when(orderQueryService.findByActive(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findByActive(Order.FindAllOrderRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // findByTrashed
    @Test
    @DisplayName("findByTrashed - success")
    void findByTrashed_Success() {
        Order.FindAllOrderRequest request = Order.FindAllOrderRequest.newBuilder().build();
        OrderResponseDeleteAt data = createOrderDeleteAt(2L);
        ApiResponsePagination<List<OrderResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                "success", "Trashed orders", List.of(data), null);
        when(orderQueryService.findByTrashed(any())).thenReturn(Uni.createFrom().item(apiResp));

        OrderQuery.ApiResponsePaginationOrderDeleteAt response = handler.findByTrashed(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData(0).hasDeletedAt()).isTrue();
    }

    @Test
    @DisplayName("findByTrashed - error")
    void findByTrashed_Error() {
        when(orderQueryService.findByTrashed(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findByTrashed(Order.FindAllOrderRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // edge cases
    @Test
    @DisplayName("findAll - empty list")
    void findAll_Empty() {
        when(orderQueryService.findAll(any()))
                .thenReturn(Uni.createFrom().item(new ApiResponsePagination<>("success", "No orders", List.of(), null)));
        OrderQuery.ApiResponsePaginationOrder response = handler.findAll(
                Order.FindAllOrderRequest.newBuilder().build()).await().indefinitely();
        assertThat(response.getDataCount()).isZero();
    }

    @Test
    @DisplayName("findById - null data")
    void findById_NullData() {
        when(orderQueryService.findById(anyInt()))
                .thenReturn(Uni.createFrom().item(ApiResponse.success("No data", null)));
        Order.ApiResponseOrder response = handler.findById(
                Order.FindByIdOrderRequest.newBuilder().setId(1).build()).await().indefinitely();
        assertThat(response.hasData()).isFalse();
    }
}