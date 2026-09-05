package com.sanedge.order.service;

import com.sanedge.order.domain.requests.CreateOrderRequest;
import com.sanedge.order.domain.requests.UpdateOrderRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface OrderCommandService {
    Uni<ApiResponse<OrderResponse>> create(CreateOrderRequest request);
    Uni<ApiResponse<OrderResponse>> update(UpdateOrderRequest request);
    Uni<ApiResponse<OrderResponseDeleteAt>> trash(Integer id);
    Uni<ApiResponse<OrderResponseDeleteAt>> restore(Integer id);
    Uni<ApiResponse<Boolean>> delete(Integer id);
    Uni<ApiResponse<Boolean>> restoreAll();
    Uni<ApiResponse<Boolean>> deleteAll();
    Uni<ApiResponse<OrderResponse>> updateOrderTotalPrice(Integer orderId, Integer totalPrice);
}
