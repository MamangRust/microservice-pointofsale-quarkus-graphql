package com.sanedge.product.handler;

import com.sanedge.product.service.ProductQueryService;

import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.product.MutinyProductServiceGrpc;
import pb.product.Product.*;
import pb.product.ProductQuery.*;

@GrpcService
@Singleton
public class ProductQueryGrpcHandler extends MutinyProductServiceGrpc.ProductServiceImplBase {

    @Inject
    ProductQueryService productQueryService;

    @Override
    public Uni<ApiResponsePaginationProduct> findAll(FindAllProductRequest request) {
        com.sanedge.product.domain.requests.FindAllProductRequest domainReq = new com.sanedge.product.domain.requests.FindAllProductRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return productQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationProduct.Builder builder = ApiResponsePaginationProduct.newBuilder()
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
    public Uni<ApiResponsePaginationProduct> findByMerchant(FindAllProductMerchantRequest request) {
        com.sanedge.product.domain.requests.FindAllProductByMerchantRequest domainReq = new com.sanedge.product.domain.requests.FindAllProductByMerchantRequest();

        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setCategoryId(request.getCategoryId());
        domainReq.setMinPrice(request.getMinPrice());
        domainReq.setMaxPrice(request.getMaxPrice());

        return productQueryService.findByMerchant(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationProduct.Builder builder = ApiResponsePaginationProduct.newBuilder()
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
    public Uni<ApiResponsePaginationProduct> findByCategory(FindAllProductCategoryRequest request) {
        com.sanedge.product.domain.requests.FindAllProductByCategoryRequest domainReq = new com.sanedge.product.domain.requests.FindAllProductByCategoryRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());
        domainReq.setCategoryName(request.getCategoryName());
        domainReq.setMinPrice(request.getMinprice());
        domainReq.setMaxPrice(request.getMaxprice());

        return productQueryService.findByCategoryName(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationProduct.Builder builder = ApiResponsePaginationProduct.newBuilder()
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
    public Uni<ApiResponseProduct> findById(FindByIdProductRequest request) {
        return productQueryService.findById((long) request.getId())
                .map(apiResp -> {
                    ApiResponseProduct.Builder builder = ApiResponseProduct.newBuilder()
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
    public Uni<ApiResponsePaginationProductDeleteAt> findByActive(FindAllProductRequest request) {
        com.sanedge.product.domain.requests.FindAllProductRequest domainReq = new com.sanedge.product.domain.requests.FindAllProductRequest();

        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return productQueryService.findActiveProducts(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationProductDeleteAt.Builder builder = ApiResponsePaginationProductDeleteAt
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
    public Uni<ApiResponsePaginationProductDeleteAt> findByTrashed(FindAllProductRequest request) {
        com.sanedge.product.domain.requests.FindAllProductRequest domainReq = new com.sanedge.product.domain.requests.FindAllProductRequest();

        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return productQueryService.findTrashedProducts(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationProductDeleteAt.Builder builder = ApiResponsePaginationProductDeleteAt
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

    private pb.product.Product.ProductResponse toProto(com.sanedge.product.domain.response.ProductResponse r) {
        if (r == null) {
            return pb.product.Product.ProductResponse.getDefaultInstance();
        }
        return pb.product.Product.ProductResponse.newBuilder()
                .setId(r.getId().intValue())
                .setMerchantId(r.getMerchantId())
                .setCategoryId(r.getCategoryId())
                .setName(r.getName())
                .setDescription(r.getDescription() != null ? r.getDescription() : "")
                .setPrice(r.getPrice())
                .setCountInStock(r.getCountInStock())
                .setBrand(r.getBrand() != null ? r.getBrand() : "")
                .setWeight(r.getWeight() != null ? r.getWeight() : 0)
                .setRating(0.0f)
                .setSlugProduct(r.getSlugProduct() != null ? r.getSlugProduct() : "")
                .setImageProduct(r.getImageProduct() != null ? r.getImageProduct() : "")
                .setBarcode("")
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "")
                .build();
    }

    private pb.product.Product.ProductResponseDeleteAt toProto(
            com.sanedge.product.domain.response.ProductResponseDeleteAt r) {
        if (r == null) {
            return pb.product.Product.ProductResponseDeleteAt.getDefaultInstance();
        }
        var builder = pb.product.Product.ProductResponseDeleteAt.newBuilder()
                .setId(r.getId().intValue())
                .setMerchantId(r.getMerchantId())
                .setCategoryId(r.getCategoryId())
                .setName(r.getName())
                .setDescription(r.getDescription() != null ? r.getDescription() : "")
                .setPrice(r.getPrice())
                .setCountInStock(r.getCountInStock())
                .setBrand(r.getBrand() != null ? r.getBrand() : "")
                .setWeight(r.getWeight() != null ? r.getWeight() : 0)
                .setRating(r.getRating() != null ? r.getRating() : 0.0f)
                .setSlugProduct(r.getSlugProduct() != null ? r.getSlugProduct() : "")
                .setImageProduct(r.getImageProduct() != null ? r.getImageProduct() : "")
                .setBarcode("")
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "");
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }
}
