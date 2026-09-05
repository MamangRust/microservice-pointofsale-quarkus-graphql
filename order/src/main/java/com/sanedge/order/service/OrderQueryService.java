package com.sanedge.order.service;

import java.util.List;

import com.sanedge.order.domain.requests.FindAllOrderByMerchantRequest;
import com.sanedge.order.domain.requests.FindAllOrderRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface OrderQueryService {
    Uni<ApiResponsePagination<List<OrderResponse>>> findAll(FindAllOrderRequest req);
    Uni<ApiResponsePagination<List<OrderResponseDeleteAt>>> findByActive(FindAllOrderRequest req);
    Uni<ApiResponsePagination<List<OrderResponseDeleteAt>>> findByTrashed(FindAllOrderRequest req);
    Uni<ApiResponsePagination<List<OrderResponse>>> findByMerchantId(FindAllOrderByMerchantRequest req);
    Uni<ApiResponse<OrderResponse>> findById(Integer id);
}
