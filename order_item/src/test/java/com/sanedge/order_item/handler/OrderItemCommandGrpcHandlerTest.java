package com.sanedge.order_item.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.order_item.domain.response.OrderItemResponse;
import com.sanedge.order_item.domain.response.OrderItemResponseDeleteAt;
import com.sanedge.order_item.entity.OrderItem;
import com.sanedge.order_item.service.OrderItemQueryService;

import io.smallrye.mutiny.Uni;
import pb.order_item.OrderItemQuery;

@ExtendWith(MockitoExtension.class)
class OrderItemQueryGrpcHandlerTest {

    @Mock
    private OrderItemQueryService orderItemQueryService;

    private OrderItemQueryGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderItemQueryGrpcHandler();
        handler.orderItemQueryService = orderItemQueryService;
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

    private OrderItemResponse createOrderItemResponse(Long id) {
        return OrderItemResponse.from(createMockOrderItem(id));
    }

    private OrderItemResponseDeleteAt createOrderItemDeleteAt(Long id) {
        OrderItemResponseDeleteAt r = OrderItemResponseDeleteAt.from(createMockOrderItem(id));
        r.setDeletedAt(LocalDateTime.now().toString());
        return r;
    }

    // findAll
    @Test
    @DisplayName("findAll - success")
    void findAll_Success() {
        pb.order_item.OrderItem.FindAllOrderItemRequest request = pb.order_item.OrderItem.FindAllOrderItemRequest.newBuilder()
                .setSearch("").setPage(1).setPageSize(10).build();

        OrderItemResponse data = createOrderItemResponse(1L);
        PagedResult<OrderItemResponse> pagedResult = new PagedResult<>(List.of(data), 1);
        ApiResponse<PagedResult<OrderItemResponse>> apiResp = ApiResponse.success("Order items retrieved", pagedResult);
        when(orderItemQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

        OrderItemQuery.ApiResponsePaginationOrderItem response = handler.findAll(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getData(0).getOrderId()).isEqualTo(100);
        assertThat(response.getData(0).getProductId()).isEqualTo(10);
    }

    @Test
    @DisplayName("findAll - error response")
    void findAll_Error() {
        pb.order_item.OrderItem.FindAllOrderItemRequest request = pb.order_item.OrderItem.FindAllOrderItemRequest.newBuilder().build();
        ApiResponse<PagedResult<OrderItemResponse>> apiResp = new ApiResponse<>("error", "DB error", null);
        when(orderItemQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

        OrderItemQuery.ApiResponsePaginationOrderItem response = handler.findAll(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("failed");
    }

    // findByActive
    @Test
    @DisplayName("findByActive - success")
    void findByActive_Success() {
        pb.order_item.OrderItem.FindAllOrderItemRequest request = pb.order_item.OrderItem.FindAllOrderItemRequest.newBuilder()
                .setPage(1).setPageSize(10).build();

        OrderItemResponseDeleteAt data = createOrderItemDeleteAt(1L);
        PagedResult<OrderItemResponseDeleteAt> pagedResult = new PagedResult<>(List.of(data), 1);
        ApiResponse<PagedResult<OrderItemResponseDeleteAt>> apiResp = ApiResponse.success("Active items", pagedResult);
        when(orderItemQueryService.findByActive(any())).thenReturn(Uni.createFrom().item(apiResp));

        OrderItemQuery.ApiResponsePaginationOrderItemDeleteAt response = handler.findByActive(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getData(0).hasDeletedAt()).isTrue();
    }

    @Test
    @DisplayName("findByActive - failure propagates")
    void findByActive_Error() {
        pb.order_item.OrderItem.FindAllOrderItemRequest request = pb.order_item.OrderItem.FindAllOrderItemRequest.newBuilder().build();
        when(orderItemQueryService.findByActive(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        OrderItemQuery.ApiResponsePaginationOrderItemDeleteAt response = handler.findByActive(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("failed");
    }

    // findByTrashed
    @Test
    @DisplayName("findByTrashed - success")
    void findByTrashed_Success() {
        pb.order_item.OrderItem.FindAllOrderItemRequest request = pb.order_item.OrderItem.FindAllOrderItemRequest.newBuilder().build();
        OrderItemResponseDeleteAt data = createOrderItemDeleteAt(2L);
        PagedResult<OrderItemResponseDeleteAt> pagedResult = new PagedResult<>(List.of(data), 1);
        ApiResponse<PagedResult<OrderItemResponseDeleteAt>> apiResp = ApiResponse.success("Trashed items", pagedResult);
        when(orderItemQueryService.findByTrashed(any())).thenReturn(Uni.createFrom().item(apiResp));

        OrderItemQuery.ApiResponsePaginationOrderItemDeleteAt response = handler.findByTrashed(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData(0).hasDeletedAt()).isTrue();
    }

    // findOrderItemByOrder
    @Test
    @DisplayName("findOrderItemByOrder - success")
    void findOrderItemByOrder_Success() {
        pb.order_item.OrderItem.FindByIdOrderItemRequest request = pb.order_item.OrderItem.FindByIdOrderItemRequest.newBuilder()
                .setOrderItemId(1).build();

        OrderItemResponse data = createOrderItemResponse(1L);
        ApiResponse<List<OrderItemResponse>> apiResp = ApiResponse.success("Items by order", List.of(data));
        when(orderItemQueryService.findOrderItemByOrder(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        pb.order_item.OrderItem.ApiResponsesOrderItem response = handler.findOrderItemByOrder(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getData(0).getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("findOrderItemByOrder - error")
    void findOrderItemByOrder_Error() {
        pb.order_item.OrderItem.FindByIdOrderItemRequest request = pb.order_item.OrderItem.FindByIdOrderItemRequest.newBuilder().build();
        when(orderItemQueryService.findOrderItemByOrder(anyInt())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        pb.order_item.OrderItem.ApiResponsesOrderItem response = handler.findOrderItemByOrder(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("failed");
    }

    // edge cases
    @Test
    @DisplayName("findAll - empty list")
    void findAll_Empty() {
        pb.order_item.OrderItem.FindAllOrderItemRequest request = pb.order_item.OrderItem.FindAllOrderItemRequest.newBuilder().build();
        PagedResult<OrderItemResponse> empty = new PagedResult<>(List.of(), 0);
        ApiResponse<PagedResult<OrderItemResponse>> apiResp = ApiResponse.success("No items", empty);
        when(orderItemQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

        OrderItemQuery.ApiResponsePaginationOrderItem response = handler.findAll(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isZero();
    }
}