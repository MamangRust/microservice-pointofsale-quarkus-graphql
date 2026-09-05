package com.sanedge.category.handler;

import com.sanedge.category.domain.requests.FindAllCategory;
import com.sanedge.category.service.CategoryQueryService;

import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.category.MutinyCategoryQueryServiceGrpc;
import pb.category.Category.*;
import pb.category.CategoryQuery.*;

@GrpcService
@Singleton
public class CategoryQueryGrpcHandler extends MutinyCategoryQueryServiceGrpc.CategoryQueryServiceImplBase {

    @Inject
    CategoryQueryService categoryQueryService;

    @Override
    public Uni<ApiResponsePaginationCategoryDeleteAt> findByActive(FindAllCategoryRequest request) {
        FindAllCategory domainReq = new FindAllCategory();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return categoryQueryService.findByActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationCategoryDeleteAt.Builder builder = ApiResponsePaginationCategoryDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (var item : apiResp.data()) {
                            builder.addData(toProto(item));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(pb.common.PaginationMeta.newBuilder()
                                .setCurrentPage(apiResp.pagination().currentPage())
                                .setPageSize(apiResp.pagination().pageSize())
                                .setTotalPages(apiResp.pagination().totalPages())
                                .setTotalRecords(apiResp.pagination().totalRecords())
                                .build());
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponsePaginationCategoryDeleteAt> findByTrashed(FindAllCategoryRequest request) {
        FindAllCategory domainReq = new FindAllCategory();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return categoryQueryService.findByTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationCategoryDeleteAt.Builder builder = ApiResponsePaginationCategoryDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (var item : apiResp.data()) {
                            builder.addData(toProto(item));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(pb.common.PaginationMeta.newBuilder()
                                .setCurrentPage(apiResp.pagination().currentPage())
                                .setPageSize(apiResp.pagination().pageSize())
                                .setTotalPages(apiResp.pagination().totalPages())
                                .setTotalRecords(apiResp.pagination().totalRecords())
                                .build());
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponsePaginationCategory> findAll(FindAllCategoryRequest request) {
        FindAllCategory domainReq = new FindAllCategory();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return categoryQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationCategory.Builder builder = ApiResponsePaginationCategory.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (var item : apiResp.data()) {
                            builder.addData(toProto(item));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(pb.common.PaginationMeta.newBuilder()
                                .setCurrentPage(apiResp.pagination().currentPage())
                                .setPageSize(apiResp.pagination().pageSize())
                                .setTotalPages(apiResp.pagination().totalPages())
                                .setTotalRecords(apiResp.pagination().totalRecords())
                                .build());
                    }
                    return builder.build();
                })
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseCategory> findById(FindByIdCategoryRequest request) {
        return categoryQueryService.findById(request.getId())
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
