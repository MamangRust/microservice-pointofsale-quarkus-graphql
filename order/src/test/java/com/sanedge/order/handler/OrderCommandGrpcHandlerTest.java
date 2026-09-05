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

import com.google.protobuf.Empty;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.service.OrderCommandService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.order.Order;
import pb.order.OrderCommand;

@ExtendWith(MockitoExtension.class)
class OrderCommandGrpcHandlerTest {

    @Mock
    private OrderCommandService orderCommandService;

    private OrderCommandGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderCommandGrpcHandler();
        handler.orderCommandService = orderCommandService;
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

    // create
    @Test
    @DisplayName("create - success")
    void create_Success() {
        Order.CreateOrderRequest request = Order.CreateOrderRequest.newBuilder()
                .setMerchantId(10)
                .setCashierId(100)
                .addItems(pb.order.Order.CreateOrderItemRequest.newBuilder()
                        .setProductId(1)
                        .setQuantity(2)
                        .build())
                .build();

        OrderResponse data = createOrderResponse(1L);
        data.setTotalPrice(10000L);
        ApiResponse<OrderResponse> apiResp = ApiResponse.success("Order created", data);
        when(orderCommandService.create(any())).thenReturn(Uni.createFrom().item(apiResp));

        Order.ApiResponseOrder response = handler.create(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(1);
        assertThat(response.getData().getTotalPrice()).isEqualTo(10000);
    }

    @Test
    @DisplayName("create - error")
    void create_Error() {
        when(orderCommandService.create(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.create(Order.CreateOrderRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // update
    @Test
    @DisplayName("update - success")
    void update_Success() {
        Order.UpdateOrderRequest request = Order.UpdateOrderRequest.newBuilder()
                .setOrderId(1)
                .setCashierId(100)
                .addItems(pb.order.Order.UpdateOrderItemRequest.newBuilder()
                        .setOrderItemId(1)
                        .setProductId(2)
                        .setQuantity(3)
                        .build())
                .build();

        OrderResponse data = createOrderResponse(1L);
        data.setTotalPrice(15000L);
        ApiResponse<OrderResponse> apiResp = ApiResponse.success("Order updated", data);
        when(orderCommandService.update(any())).thenReturn(Uni.createFrom().item(apiResp));

        Order.ApiResponseOrder response = handler.update(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(1);
        assertThat(response.getData().getTotalPrice()).isEqualTo(15000);
    }

    @Test
    @DisplayName("update - error")
    void update_Error() {
        when(orderCommandService.update(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.update(Order.UpdateOrderRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // trashedOrder
    @Test
    @DisplayName("trashedOrder - success")
    void trashed_Success() {
        Order.FindByIdOrderRequest request = Order.FindByIdOrderRequest.newBuilder().setId(1).build();
        OrderResponseDeleteAt data = createOrderDeleteAt(1L);
        ApiResponse<OrderResponseDeleteAt> apiResp = ApiResponse.success("Trashed", data);
        when(orderCommandService.trash(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        Order.ApiResponseOrderDeleteAt response = handler.trashedOrder(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().hasDeletedAt()).isTrue();
    }

    @Test
    @DisplayName("trashedOrder - error")
    void trashed_Error() {
        when(orderCommandService.trash(anyInt())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.trashedOrder(Order.FindByIdOrderRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // restoreOrder
    @Test
    @DisplayName("restoreOrder - success")
    void restore_Success() {
        Order.FindByIdOrderRequest request = Order.FindByIdOrderRequest.newBuilder().setId(1).build();
        OrderResponseDeleteAt data = createOrderDeleteAt(1L);
        data.setDeletedAt(null);
        ApiResponse<OrderResponseDeleteAt> apiResp = ApiResponse.success("Restored", data);
        when(orderCommandService.restore(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        Order.ApiResponseOrderDeleteAt response = handler.restoreOrder(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().hasDeletedAt()).isFalse();
    }

    @Test
    @DisplayName("restoreOrder - error")
    void restore_Error() {
        when(orderCommandService.restore(anyInt())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.restoreOrder(Order.FindByIdOrderRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // deleteOrderPermanent
    @Test
    @DisplayName("deleteOrderPermanent - success")
    void deletePermanent_Success() {
        Order.FindByIdOrderRequest request = Order.FindByIdOrderRequest.newBuilder().setId(1).build();
        ApiResponse<Boolean> apiResp = ApiResponse.success("Deleted permanently", true);
        when(orderCommandService.delete(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        pb.order.Order.ApiResponseOrderDelete response = handler.deleteOrderPermanent(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Deleted permanently");
    }

    @Test
    @DisplayName("deleteOrderPermanent - error")
    void deletePermanent_Error() {
        when(orderCommandService.delete(anyInt())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.deleteOrderPermanent(Order.FindByIdOrderRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // restoreAllOrder
    @Test
    @DisplayName("restoreAllOrder - success")
    void restoreAll_Success() {
        ApiResponse<Boolean> apiResp = ApiResponse.success("All restored", true);
        when(orderCommandService.restoreAll()).thenReturn(Uni.createFrom().item(apiResp));

        pb.order.Order.ApiResponseOrderAll response = handler.restoreAllOrder(Empty.getDefaultInstance()).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All restored");
    }

    @Test
    @DisplayName("restoreAllOrder - error")
    void restoreAll_Error() {
        when(orderCommandService.restoreAll()).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.restoreAllOrder(Empty.getDefaultInstance()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // deleteAllOrderPermanent
    @Test
    @DisplayName("deleteAllOrderPermanent - success")
    void deleteAll_Success() {
        ApiResponse<Boolean> apiResp = ApiResponse.success("All permanently deleted", true);
        when(orderCommandService.deleteAll()).thenReturn(Uni.createFrom().item(apiResp));

        pb.order.Order.ApiResponseOrderAll response = handler.deleteAllOrderPermanent(Empty.getDefaultInstance()).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All permanently deleted");
    }

    @Test
    @DisplayName("deleteAllOrderPermanent - error")
    void deleteAll_Error() {
        when(orderCommandService.deleteAll()).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.deleteAllOrderPermanent(Empty.getDefaultInstance()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // updateOrderTotalPrice
    @Test
    @DisplayName("updateOrderTotalPrice - success")
    void updateTotalPrice_Success() {
        OrderCommand.UpdateOrderTotalPriceRequest request = OrderCommand.UpdateOrderTotalPriceRequest.newBuilder()
                .setOrderId(1)
                .setTotalPrice(99999)
                .build();

        OrderResponse data = createOrderResponse(1L);
        data.setTotalPrice(99999L);
        ApiResponse<OrderResponse> apiResp = ApiResponse.success("Total price updated", data);
        when(orderCommandService.updateOrderTotalPrice(anyInt(), anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        Order.ApiResponseOrder response = handler.updateOrderTotalPrice(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getTotalPrice()).isEqualTo(99999);
    }

    @Test
    @DisplayName("updateOrderTotalPrice - error")
    void updateTotalPrice_Error() {
        when(orderCommandService.updateOrderTotalPrice(anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.updateOrderTotalPrice(OrderCommand.UpdateOrderTotalPriceRequest.newBuilder().build())
                    .await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // edge cases
    @Test
    @DisplayName("create - null data")
    void create_NullData() {
        when(orderCommandService.create(any())).thenReturn(Uni.createFrom().item(ApiResponse.success("Created", null)));
        Order.ApiResponseOrder response = handler.create(Order.CreateOrderRequest.newBuilder().build()).await().indefinitely();
        assertThat(response.hasData()).isFalse();
    }

    @Test
    @DisplayName("update - null data")
    void update_NullData() {
        when(orderCommandService.update(any())).thenReturn(Uni.createFrom().item(ApiResponse.success("Updated", null)));
        Order.ApiResponseOrder response = handler.update(Order.UpdateOrderRequest.newBuilder().build()).await().indefinitely();
        assertThat(response.hasData()).isFalse();
    }
}