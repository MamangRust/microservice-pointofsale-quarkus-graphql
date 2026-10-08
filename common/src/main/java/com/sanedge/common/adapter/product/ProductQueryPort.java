package com.sanedge.common.adapter.product;

import com.sanedge.common.adapter.model.Product;

import io.smallrye.mutiny.Uni;

/**
 * Port for the product query service, replacing direct
 * {@code @GrpcClient("product")} usage in consumer services.
 */
public interface ProductQueryPort {

    Uni<Product> findById(int productId);
}
