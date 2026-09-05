package com.sanedge.product.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "products", schema = "pos_catalog")
@AttributeOverride(name = "id", column = @Column(name = "product_id"))
public class Product extends BaseModel {

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false, length = 255)
    private String name;

    private String description;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "count_in_stock", nullable = false)
    private Integer countInStock = 0;

    private String brand;
    private Integer weight;

    @Column(name = "slug_product", unique = true)
    private String slugProduct;

    @Column(name = "image_product")
    private String imageProduct;

    @Column(unique = true)
    private String barcode;

    public Long getProductId() {
        return this.id;
    }

    public void setProductId(Long productId) {
        this.id = productId;
    }
}
