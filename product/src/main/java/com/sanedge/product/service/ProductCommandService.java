package com.sanedge.product.service;

import com.sanedge.product.domain.requests.CreateProductRequest;
import com.sanedge.product.domain.requests.UpdateProductRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.product.domain.response.ProductResponse;
import com.sanedge.product.domain.response.ProductResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface ProductCommandService {
    Uni<ApiResponse<ProductResponse>> createProduct(CreateProductRequest req);
    Uni<ApiResponse<ProductResponse>> updateProduct(UpdateProductRequest req);
    Uni<ApiResponse<ProductResponseDeleteAt>> trashedProduct(Integer productId);
    Uni<ApiResponse<ProductResponseDeleteAt>> restoreProduct(Integer productId);
    Uni<ApiResponse<Boolean>> deleteProductPermanent(Integer productId);
    Uni<ApiResponse<Boolean>> restoreAllProducts();
    Uni<ApiResponse<Boolean>> deleteAllProductsPermanent();
}
