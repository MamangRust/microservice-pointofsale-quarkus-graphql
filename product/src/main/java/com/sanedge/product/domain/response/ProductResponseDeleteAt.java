package com.sanedge.product.domain.response;

import com.sanedge.product.entity.Product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDeleteAt {
    private Long id;
    private Integer merchantId;
    private Integer categoryId;
    private String name;
    private String description;
    private Integer price;
    private Integer countInStock;
    private String brand;
    private Integer weight;
    private Float rating;
    private String slugProduct;
    private String imageProduct;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static ProductResponseDeleteAt from(Product entity) {
        return ProductResponseDeleteAt.builder()
                .id(entity.getProductId())
                .merchantId(entity.getMerchantId().intValue())
                .categoryId(entity.getCategoryId().intValue())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .countInStock(entity.getCountInStock())
                .brand(entity.getBrand())
                .weight(entity.getWeight())
                .slugProduct(entity.getSlugProduct())
                .imageProduct(entity.getImageProduct())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                .deletedAt(entity.getDeletedAt() != null ? entity.getDeletedAt().toString() : null)
                .build();
    }
}
