package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.ProductDto;
import io.smallrye.mutiny.Uni;

public interface ProductService {
    Uni<ProductDto.UploadImageResponse> uploadImage(String base64Data, String fileName);
    Uni<ProductDto.ApiResponsePaginationProduct> findAll(int page, int size, String search);
    Uni<ProductDto.ApiResponsePaginationProductDeleteAt> findByActive(int page, int size, String search);
    Uni<ProductDto.ApiResponsePaginationProductDeleteAt> findByTrashed(int page, int size, String search);
    Uni<ProductDto.ApiResponseProduct> findById(int id);
    Uni<ProductDto.ApiResponsePaginationProduct> findByMerchant(int merchantId, String search, int categoryId, int minPrice, int maxPrice, int page, int size);
    Uni<ProductDto.ApiResponsePaginationProduct> findByCategory(String categoryName, int page, int size, String search, int minPrice, int maxPrice);
    Uni<ProductDto.ApiResponseProduct> createProduct(ProductDto.CreateProductRequest body);
    Uni<ProductDto.ApiResponseProduct> updateProduct(int id, ProductDto.UpdateProductRequest body);
    Uni<ProductDto.ApiResponseProductDeleteAt> deleteProduct(int id);
    Uni<ProductDto.ApiResponseProductDeleteAt> restoreProduct(int id);
    Uni<ProductDto.ApiResponseProductDelete> deleteProductPermanent(int id);
    Uni<ProductDto.ApiResponseProductAll> restoreAllProducts();
    Uni<ProductDto.ApiResponseProductAll> deleteAllProductsPermanent();
}
