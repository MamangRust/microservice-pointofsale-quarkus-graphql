package com.sanedge.common.adapter.category;

import com.sanedge.common.adapter.model.Category;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.category.Category.ApiResponseCategory;
import pb.category.Category.CategoryResponse;
import pb.category.Category.FindByIdCategoryRequest;
import pb.category.CategoryQueryService;

@ApplicationScoped
public class CategoryAdapter implements CategoryQueryPort {

    @GrpcClient("category")
    CategoryQueryService query;

    @Override
    public Uni<Category> findByCategoryId(int categoryId) {
        return query.findById(FindByIdCategoryRequest.newBuilder().setId(categoryId).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Category not found: " + categoryId);
                    }
                    return toCategory(resp.getData());
                });
    }

    private static Category toCategory(CategoryResponse c) {
        if (c == null) {
            return null;
        }
        return new Category(c.getId(), c.getName(), c.getDescription(), c.getSlugCategory(),
                c.getImageCategory(), ProtoTime.parse(c.getCreatedAt()), ProtoTime.parse(c.getUpdatedAt()));
    }
}
