package com.sanedge.category.handler;

import com.google.protobuf.Empty;
import com.sanedge.category.domain.requests.CreateCategoryRequest;
import com.sanedge.category.domain.requests.UpdateCategoryRequest;
import com.sanedge.category.service.CategoryCommandService;

import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.category.MutinyCategoryCommandServiceGrpc;
import pb.category.Category.ApiResponseCategory;
import pb.category.Category.ApiResponseCategoryDeleteAt;
import pb.category.Category.FindByIdCategoryRequest;
import pb.category.CategoryCommand.ApiResponseCategoryAll;
import pb.category.CategoryCommand.ApiResponseCategoryDelete;

@GrpcService
@Singleton
public class CategoryCommandGrpcHandler extends MutinyCategoryCommandServiceGrpc.CategoryCommandServiceImplBase {

    @Inject
    CategoryCommandService categoryCommandService;

    @Override
    public Uni<ApiResponseCategory> create(pb.category.CategoryCommand.CreateCategoryRequest request) {
        CreateCategoryRequest domainReq = new CreateCategoryRequest();
        domainReq.setName(request.getName());
        domainReq.setDescription(request.getDescription());

        return categoryCommandService.createCategory(domainReq)
                .map(apiResp -> {
                    ApiResponseCategory.Builder builder = ApiResponseCategory.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseCategory> update(pb.category.CategoryCommand.UpdateCategoryRequest request) {
        UpdateCategoryRequest domainReq = new UpdateCategoryRequest();
        domainReq.setCategoryId(request.getCategoryId());
        domainReq.setName(request.getName());
        domainReq.setDescription(request.getDescription());

        return categoryCommandService.updateCategory(domainReq)
                .map(apiResp -> {
                    ApiResponseCategory.Builder builder = ApiResponseCategory.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseCategoryDeleteAt> trashedCategory(FindByIdCategoryRequest request) {
        return categoryCommandService.trashedCategory(request.getId())
                .map(apiResp -> {
                    ApiResponseCategoryDeleteAt.Builder builder = ApiResponseCategoryDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseCategoryDeleteAt> restoreCategory(FindByIdCategoryRequest request) {
        return categoryCommandService.restoreCategory(request.getId())
                .map(apiResp -> {
                    ApiResponseCategoryDeleteAt.Builder builder = ApiResponseCategoryDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseCategoryDelete> deleteCategoryPermanent(FindByIdCategoryRequest request) {
        return categoryCommandService.deleteCategoryPermanent(request.getId())
                .map(apiResp -> ApiResponseCategoryDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseCategoryAll> restoreAllCategory(Empty request) {
        return categoryCommandService.restoreAllCategories()
                .map(apiResp -> ApiResponseCategoryAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseCategoryAll> deleteAllCategoryPermanent(Empty request) {
        return categoryCommandService.deleteAllCategoriesPermanent()
                .map(apiResp -> ApiResponseCategoryAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    private pb.category.Category.CategoryResponse toProto(com.sanedge.category.domain.response.CategoryResponse r) {
        if (r == null) {
            return pb.category.Category.CategoryResponse.getDefaultInstance();
        }
        return pb.category.Category.CategoryResponse.newBuilder()
                .setId(r.getId().intValue())
                .setName(r.getName())
                .setDescription(r.getDescription() != null ? r.getDescription() : "")
                .setSlugCategory(r.getSlugCategory() != null ? r.getSlugCategory() : "")
                .setImageCategory(r.getImageCategory() != null ? r.getImageCategory() : "")
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "")
                .build();
    }

    private pb.category.Category.CategoryResponseDeleteAt toProto(
            com.sanedge.category.domain.response.CategoryResponseDeleteAt r) {
        if (r == null) {
            return pb.category.Category.CategoryResponseDeleteAt.getDefaultInstance();
        }
        var builder = pb.category.Category.CategoryResponseDeleteAt.newBuilder()
                .setId(r.getId().intValue())
                .setName(r.getName())
                .setDescription(r.getDescription() != null ? r.getDescription() : "")
                .setSlugCategory(r.getSlugCategory() != null ? r.getSlugCategory() : "")
                .setImageCategory(r.getImageCategory() != null ? r.getImageCategory() : "")
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "");
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }
}
