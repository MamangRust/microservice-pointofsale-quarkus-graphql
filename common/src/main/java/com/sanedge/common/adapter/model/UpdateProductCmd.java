package com.sanedge.common.adapter.model;

/**
 * Command to update a product in the product service.
 */
public record UpdateProductCmd(
        int productId,
        int merchantId,
        int categoryId,
        String name,
        String description,
        int price,
        int countInStock,
        String brand,
        int weight,
        String imageProduct) {
}
