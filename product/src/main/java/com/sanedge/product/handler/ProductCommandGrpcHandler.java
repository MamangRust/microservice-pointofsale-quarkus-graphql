package com.sanedge.product.handler;

import com.sanedge.product.service.ProductCommandService;

import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.product.MutinyProductCommandServiceGrpc;
import pb.product.Product.ApiResponseProduct;
import pb.product.Product.ApiResponseProductDeleteAt;
import pb.product.Product.FindByIdProductRequest;
import pb.product.ProductCommand.ApiResponseProductAll;
import pb.product.ProductCommand.ApiResponseProductDelete;
import pb.product.ProductCommand.CreateProductRequest;
import pb.product.ProductCommand.UpdateProductRequest;

@GrpcService
@Singleton
public class ProductCommandGrpcHandler extends MutinyProductCommandServiceGrpc.ProductCommandServiceImplBase {

    @Inject
    ProductCommandService productCommandService;

    @Override
    public Uni<ApiResponseProduct> create(CreateProductRequest request) {
        com.sanedge.product.domain.requests.CreateProductRequest domainReq = new com.sanedge.product.domain.requests.CreateProductRequest();
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setCategoryId(request.getCategoryId());
        domainReq.setName(request.getName());
        domainReq.setDescription(request.getDescription());
        domainReq.setPrice(request.getPrice());
        domainReq.setCountInStock(request.getCountInStock());
        domainReq.setBrand(request.getBrand());
        domainReq.setWeight(request.getWeight());
        domainReq.setRating(5);
        domainReq.setSlugProduct(
                request.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", ""));
        domainReq.setImageProduct(request.getImageProduct());

        return productCommandService.createProduct(domainReq)
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
    public Uni<ApiResponseProduct> update(UpdateProductRequest request) {
        com.sanedge.product.domain.requests.UpdateProductRequest domainReq = new com.sanedge.product.domain.requests.UpdateProductRequest();
        domainReq.setProductId(request.getProductId());
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setCategoryId(request.getCategoryId());
        domainReq.setName(request.getName());
        domainReq.setDescription(request.getDescription());
        domainReq.setPrice(request.getPrice());
        domainReq.setCountInStock(request.getCountInStock());
        domainReq.setBrand(request.getBrand());
        domainReq.setWeight(request.getWeight());
        domainReq.setRating(5);
        String slug = request.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        domainReq.setSlugProduct(slug);
        domainReq.setImageProduct(request.getImageProduct());

        return productCommandService.updateProduct(domainReq)
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
    public Uni<ApiResponseProductDeleteAt> trashedProduct(FindByIdProductRequest request) {
        return productCommandService.trashedProduct(request.getId())
                .map(apiResp -> {
                    ApiResponseProductDeleteAt.Builder builder = ApiResponseProductDeleteAt.newBuilder()
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
    public Uni<ApiResponseProductDeleteAt> restoreProduct(FindByIdProductRequest request) {
        return productCommandService.restoreProduct(request.getId())
                .map(apiResp -> {
                    ApiResponseProductDeleteAt.Builder builder = ApiResponseProductDeleteAt.newBuilder()
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
    public Uni<ApiResponseProductDelete> deleteProductPermanent(FindByIdProductRequest request) {
        return productCommandService.deleteProductPermanent(request.getId())
                .map(apiResp -> ApiResponseProductDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseProductAll> restoreAllProduct(com.google.protobuf.Empty request) {
        return productCommandService.restoreAllProducts()
                .map(apiResp -> ApiResponseProductAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(GrpcErrorMapper::toStatusRuntimeException);
    }

    @Override
    public Uni<ApiResponseProductAll> deleteAllProductPermanent(com.google.protobuf.Empty request) {
        return productCommandService.deleteAllProductsPermanent()
                .map(apiResp -> ApiResponseProductAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
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
