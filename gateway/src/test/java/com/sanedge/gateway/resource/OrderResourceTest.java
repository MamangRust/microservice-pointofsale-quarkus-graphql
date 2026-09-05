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

import com.sanedge.gateway.dto.OrderDto;
import com.sanedge.gateway.service.OrderService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class OrderResourceTest {

    @Mock
    OrderService orderService;

    OrderResource resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new OrderResource();
        Field f = OrderResource.class.getDeclaredField("orderService");
        f.setAccessible(true);
        f.set(resource, orderService);
    }

    @Test
    void listOrders_returnsSuccess() {
        var r = new OrderDto.ApiResponsePaginationOrder("success", "ok", List.of(), null);
        when(orderService.listOrders(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(r));
        assertThat(resource.listOrders(1, 10, "").await().indefinitely().status()).isEqualTo("success");
    }

    @Test
    void getOrder_returnsSuccess() {
        var r = new OrderDto.ApiResponseOrder("success", "ok", null);
        when(orderService.getOrder(anyInt())).thenReturn(Uni.createFrom().item(r));
        assertThat(resource.getOrder(1).await().indefinitely().status()).isEqualTo("success");
    }

    @Test
    void createOrder_returnsSuccess() {
        var r = new OrderDto.ApiResponseOrder("success", "created", null);
        when(orderService.createOrder(any())).thenReturn(Uni.createFrom().item(r));
        assertThat(resource.createOrder(new OrderDto.CreateOrderRequest(1, 1, List.of())).await().indefinitely().message()).isEqualTo("created");
    }

    @Test
    void updateOrder_returnsSuccess() {
        var r = new OrderDto.ApiResponseOrder("success", "updated", null);
        when(orderService.updateOrder(anyInt(), any())).thenReturn(Uni.createFrom().item(r));
        assertThat(resource.updateOrder(1, new OrderDto.UpdateOrderRequest(1, List.of())).await().indefinitely().message()).isEqualTo("updated");
    }

    @Test
    void deleteOrder_returnsSuccess() {
        var r = new OrderDto.ApiResponseOrderDeleteAt("success", "trashed", null);
        when(orderService.deleteOrder(anyInt())).thenReturn(Uni.createFrom().item(r));
        assertThat(resource.deleteOrder(1).await().indefinitely().message()).isEqualTo("trashed");
    }

    @Test
    void restoreOrder_returnsSuccess() {
        var r = new OrderDto.ApiResponseOrderDeleteAt("success", "restored", null);
        when(orderService.restoreOrder(anyInt())).thenReturn(Uni.createFrom().item(r));
        assertThat(resource.restoreOrder(1).await().indefinitely().message()).isEqualTo("restored");
    }
}
