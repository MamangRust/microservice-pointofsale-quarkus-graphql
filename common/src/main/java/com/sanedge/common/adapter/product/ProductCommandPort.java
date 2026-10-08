package com.sanedge.common.adapter.product;

import com.sanedge.common.adapter.model.Product;
import com.sanedge.common.adapter.model.UpdateProductCmd;

import io.smallrye.mutiny.Uni;

/**
 * Port for the product command service, replacing direct
 * {@code @GrpcClient("product")} usage in consumer services.
 */
public interface ProductCommandPort {

    Uni<Product> update(UpdateProductCmd cmd);
}
