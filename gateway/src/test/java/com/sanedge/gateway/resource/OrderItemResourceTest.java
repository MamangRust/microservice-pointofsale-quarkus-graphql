package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.OrderItemDto;
import com.sanedge.gateway.service.OrderItemService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class OrderItemResourceTest {

    @Mock
    OrderItemService orderItemService;

    OrderItemResource resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new OrderItemResource();
        Field f = OrderItemResource.class.getDeclaredField("orderItemService");
        f.setAccessible(true);
        f.set(resource, orderItemService);
    }

    @Test
    void listOrderItems_returnsSuccess() {
        var response = new OrderItemDto.ApiResponsePaginationOrderItem("success", "ok", List.of(), null);
        when(orderItemService.listOrderItems(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(response));

        var result = resource.listOrderItems(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getActiveOrderItems_returnsSuccess() {
        var response = new OrderItemDto.ApiResponsePaginationOrderItemDeleteAt("success", "ok", List.of(), null);
        when(orderItemService.getActiveOrderItems(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(response));

        var result = resource.getActiveOrderItems(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getTrashedOrderItems_returnsSuccess() {
        var response = new OrderItemDto.ApiResponsePaginationOrderItemDeleteAt("success", "ok", List.of(), null);
        when(orderItemService.getTrashedOrderItems(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(response));

        var result = resource.getTrashedOrderItems(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getOrderItemsByOrder_returnsSuccess() {
        var response = new OrderItemDto.ApiResponsesOrderItem("success", "ok", List.of());
        when(orderItemService.getOrderItemsByOrder(anyInt())).thenReturn(Uni.createFrom().item(response));

        var result = resource.getOrderItemsByOrder(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createOrderItem_returnsSuccess() {
        var body = new OrderItemDto.CreateOrderItemRequest(1, 1, 2, 5000);
        var response = new OrderItemDto.ApiResponseOrderItem("success", "created", null);
        when(orderItemService.createOrderItem(any())).thenReturn(Uni.createFrom().item(response));

        var result = resource.createOrderItem(body).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void updateOrderItem_returnsSuccess() {
        var body = new OrderItemDto.UpdateOrderItemRequest(1, 1, 1, 3, 6000);
        var response = new OrderItemDto.ApiResponseOrderItem("success", "updated", null);
        when(orderItemService.updateOrderItem(anyInt(), any())).thenReturn(Uni.createFrom().item(response));

        var result = resource.updateOrderItem(1, body).await().indefinitely();
        assertThat(result.message()).isEqualTo("updated");
    }

    @Test
    void deleteOrderItem_returnsSuccess() {
        var response = new OrderItemDto.ApiResponseOrderItemDeleteAt("success", "trashed", null);
        when(orderItemService.deleteOrderItem(anyInt())).thenReturn(Uni.createFrom().item(response));

        var result = resource.deleteOrderItem(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }

    @Test
    void restoreOrderItem_returnsSuccess() {
        var response = new OrderItemDto.ApiResponseOrderItemDeleteAt("success", "restored", null);
        when(orderItemService.restoreOrderItem(anyInt())).thenReturn(Uni.createFrom().item(response));

        var result = resource.restoreOrderItem(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

    @Test
    void deleteOrderItemPermanent_returnsSuccess() {
        var response = new OrderItemDto.ApiResponseOrderItemDelete("success", "deleted");
        when(orderItemService.deleteOrderItemPermanent(anyInt())).thenReturn(Uni.createFrom().item(response));

        var result = resource.deleteOrderItemPermanent(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void restoreAllOrderItems_returnsSuccess() {
        var response = new OrderItemDto.ApiResponseOrderItemAll("success", "all restored");
        when(orderItemService.restoreAllOrderItems()).thenReturn(Uni.createFrom().item(response));

        var result = resource.restoreAllOrderItems().await().indefinitely();
        assertThat(result.message()).isEqualTo("all restored");
    }

    @Test
    void deleteAllOrderItems_returnsSuccess() {
        var response = new OrderItemDto.ApiResponseOrderItemAll("success", "all deleted");
        when(orderItemService.deleteAllOrderItems()).thenReturn(Uni.createFrom().item(response));

        var result = resource.deleteAllOrderItems().await().indefinitely();
        assertThat(result.message()).isEqualTo("all deleted");
    }
}
