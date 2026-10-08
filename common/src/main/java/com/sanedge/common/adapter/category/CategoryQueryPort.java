package com.sanedge.common.adapter.category;

import com.sanedge.common.adapter.model.Category;

import io.smallrye.mutiny.Uni;

/**
 * Port for the category query service, replacing direct
 * {@code @GrpcClient("category")} usage in consumer services.
 */
public interface CategoryQueryPort {

    Uni<Category> findByCategoryId(int categoryId);
}
