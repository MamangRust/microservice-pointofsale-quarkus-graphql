package com.sanedge.product.service;

import java.util.List;

import com.sanedge.product.domain.requests.FindAllProductByCategoryRequest;
import com.sanedge.product.domain.requests.FindAllProductByMerchantRequest;
import com.sanedge.product.domain.requests.FindAllProductRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.product.domain.response.ProductResponse;
import com.sanedge.product.domain.response.ProductResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface ProductQueryService {
    Uni<ApiResponsePagination<List<ProductResponse>>> findAll(FindAllProductRequest req);
    Uni<ApiResponsePagination<List<ProductResponseDeleteAt>>> findActiveProducts(FindAllProductRequest req);
    Uni<ApiResponsePagination<List<ProductResponseDeleteAt>>> findTrashedProducts(FindAllProductRequest req);
    Uni<ApiResponsePagination<List<ProductResponse>>> findByMerchant(FindAllProductByMerchantRequest req);
    Uni<ApiResponsePagination<List<ProductResponse>>> findByCategoryName(FindAllProductByCategoryRequest req);
    Uni<ApiResponse<ProductResponse>> findById(Long productId);
}
