package com.sanedge.gateway.dto;

import java.util.List;
import java.util.stream.Collectors;

public class ProductDto {

    @org.eclipse.microprofile.graphql.Name("CreateProductRequest")
    public record CreateProductRequest(
            int merchantId,
            int categoryId,
            String name,
            String description,
            int price,
            int countInStock,
            String brand,
            int weight,
            float rating,
            String imageProduct,
            String barcode) {
    }

    @org.eclipse.microprofile.graphql.Name("UpdateProductRequest")
    public record UpdateProductRequest(
            int merchantId,
            int categoryId,
            String name,
            String description,
            int price,
            int countInStock,
            String brand,
            int weight,
            float rating,
            String imageProduct,
            String barcode) {
    }

    @org.eclipse.microprofile.graphql.Name("ProductResponse")
    public record ProductResponse(
            int id,
            int merchantId,
            int categoryId,
            String name,
            String description,
            int price,
            int countInStock,
            String brand,
            int weight,
            float rating,
            String slugProduct,
            String imageProduct,
            String barcode,
            String createdAt,
            String updatedAt) {
        public static ProductResponse from(pb.product.Product.ProductResponse proto) {
            return new ProductResponse(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getCategoryId(),
                    proto.getName(),
                    proto.getDescription(),
                    proto.getPrice(),
                    proto.getCountInStock(),
                    proto.getBrand(),
                    proto.getWeight(),
                    proto.getRating(),
                    proto.getSlugProduct(),
                    proto.getImageProduct(),
                    proto.getBarcode(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt());
        }
    }

    @org.eclipse.microprofile.graphql.Name("ProductResponseDeleteAt")
    public record ProductResponseDeleteAt(
            int id,
            int merchantId,
            int categoryId,
            String name,
            String description,
            int price,
            int countInStock,
            String brand,
            int weight,
            float rating,
            String slugProduct,
            String imageProduct,
            String barcode,
            String createdAt,
            String updatedAt,
            String deletedAt) {
        public static ProductResponseDeleteAt from(pb.product.Product.ProductResponseDeleteAt proto) {
            return new ProductResponseDeleteAt(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getCategoryId(),
                    proto.getName(),
                    proto.getDescription(),
                    proto.getPrice(),
                    proto.getCountInStock(),
                    proto.getBrand(),
                    proto.getWeight(),
                    proto.getRating(),
                    proto.getSlugProduct(),
                    proto.getImageProduct(),
                    proto.getBarcode(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt(),
                    proto.hasDeletedAt() ? proto.getDeletedAt().getValue() : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseProduct")
    public record ApiResponseProduct(
            String status,
            String message,
            ProductResponse data) {
        public static ApiResponseProduct from(pb.product.Product.ApiResponseProduct proto) {
            return new ApiResponseProduct(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? ProductResponse.from(proto.getData()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseProductDeleteAt")
    public record ApiResponseProductDeleteAt(
            String status,
            String message,
            ProductResponseDeleteAt data) {
        public static ApiResponseProductDeleteAt from(pb.product.Product.ApiResponseProductDeleteAt proto) {
            return new ApiResponseProductDeleteAt(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? ProductResponseDeleteAt.from(proto.getData()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationProduct")
    public record ApiResponsePaginationProduct(
            String status,
            String message,
            List<ProductResponse> data,
            PaginationMetaDto paginationMeta) {
        public static ApiResponsePaginationProduct from(pb.product.ProductQuery.ApiResponsePaginationProduct proto) {
            return new ApiResponsePaginationProduct(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(ProductResponse::from).collect(Collectors.toList()),
                    proto.hasPagination() ? PaginationMetaDto.from(proto.getPagination()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationProductDeleteAt")
    public record ApiResponsePaginationProductDeleteAt(
            String status,
            String message,
            List<ProductResponseDeleteAt> data,
            PaginationMetaDto paginationMeta) {
        public static ApiResponsePaginationProductDeleteAt from(
                pb.product.ProductQuery.ApiResponsePaginationProductDeleteAt proto) {
            return new ApiResponsePaginationProductDeleteAt(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(ProductResponseDeleteAt::from).collect(Collectors.toList()),
                    proto.hasPagination() ? PaginationMetaDto.from(proto.getPagination()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseProductAll")
    public record ApiResponseProductAll(
            String status,
            String message) {
        public static ApiResponseProductAll from(pb.product.ProductCommand.ApiResponseProductAll proto) {
            return new ApiResponseProductAll(
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseProductDelete")
    public record ApiResponseProductDelete(
            String status,
            String message) {
        public static ApiResponseProductDelete from(pb.product.ProductCommand.ApiResponseProductDelete proto) {
            return new ApiResponseProductDelete(
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    @org.eclipse.microprofile.graphql.Name("UploadImageResponse")
    public record UploadImageResponse(
            String url) {
    }
}
