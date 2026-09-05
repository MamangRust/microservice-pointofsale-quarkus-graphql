package com.sanedge.gateway.dto;

import java.util.List;
import java.util.stream.Collectors;

public class MerchantDto {

    @org.eclipse.microprofile.graphql.Name("CreateMerchantRequest")
    public record CreateMerchantRequest(
        int userId,
        String name
    ) {}

    @org.eclipse.microprofile.graphql.Name("UpdateMerchantRequest")
    public record UpdateMerchantRequest(
        String name
    ) {}

    @org.eclipse.microprofile.graphql.Name("MerchantResponse")
    public record MerchantResponse(
        int id,
        String name,
        String apiKey,
        String status,
        int userId,
        String createdAt,
        String updatedAt
    ) {
        public static MerchantResponse from(pb.merchant.Merchant.MerchantResponse proto) {
            return new MerchantResponse(
                proto.getId(),
                proto.getName(),
                proto.getApiKey(),
                proto.getStatus(),
                proto.getUserId(),
                proto.getCreatedAt(),
                proto.getUpdatedAt()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("MerchantResponseDeleteAt")
    public record MerchantResponseDeleteAt(
        int id,
        String name,
        String apiKey,
        String status,
        int userId,
        String createdAt,
        String updatedAt,
        String deletedAt
    ) {
        public static MerchantResponseDeleteAt from(pb.merchant.Merchant.MerchantResponseDeleteAt proto) {
            return new MerchantResponseDeleteAt(
                proto.getId(),
                proto.getName(),
                proto.getApiKey(),
                proto.getStatus(),
                proto.getUserId(),
                proto.getCreatedAt(),
                proto.getUpdatedAt(),
                proto.hasDeletedAt() ? proto.getDeletedAt().getValue() : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseMerchant")
    public record ApiResponseMerchant(
        String status,
        String message,
        MerchantResponse data
    ) {
        public static ApiResponseMerchant from(pb.merchant.Merchant.ApiResponseMerchant proto) {
            return new ApiResponseMerchant(
                proto.getStatus(),
                proto.getMessage(),
                proto.hasData() ? MerchantResponse.from(proto.getData()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseMerchantDeleteAt")
    public record ApiResponseMerchantDeleteAt(
        String status,
        String message,
        MerchantResponseDeleteAt data
    ) {
        public static ApiResponseMerchantDeleteAt from(pb.merchant.Merchant.ApiResponseMerchantDeleteAt proto) {
            return new ApiResponseMerchantDeleteAt(
                proto.getStatus(),
                proto.getMessage(),
                proto.hasData() ? MerchantResponseDeleteAt.from(proto.getData()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsesMerchant")
    public record ApiResponsesMerchant(
        String status,
        String message,
        List<MerchantResponse> data
    ) {
        public static ApiResponsesMerchant from(pb.merchant.Merchant.ApiResponsesMerchant proto) {
            return new ApiResponsesMerchant(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(MerchantResponse::from).collect(Collectors.toList())
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationMerchant")
    public record ApiResponsePaginationMerchant(
        String status,
        String message,
        List<MerchantResponse> data,
        PaginationMetaDto paginationMeta
    ) {
        public static ApiResponsePaginationMerchant from(pb.merchant.MerchantQuery.ApiResponsePaginationMerchant proto) {
            return new ApiResponsePaginationMerchant(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(MerchantResponse::from).collect(Collectors.toList()),
                proto.hasPaginationMeta() ? PaginationMetaDto.from(proto.getPaginationMeta()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationMerchantDeleteAt")
    public record ApiResponsePaginationMerchantDeleteAt(
        String status,
        String message,
        List<MerchantResponseDeleteAt> data,
        PaginationMetaDto paginationMeta
    ) {
        public static ApiResponsePaginationMerchantDeleteAt from(pb.merchant.MerchantQuery.ApiResponsePaginationMerchantDeleteAt proto) {
            return new ApiResponsePaginationMerchantDeleteAt(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(MerchantResponseDeleteAt::from).collect(Collectors.toList()),
                proto.hasPaginationMeta() ? PaginationMetaDto.from(proto.getPaginationMeta()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseMerchantAll")
    public record ApiResponseMerchantAll(
        String status,
        String message
    ) {
        public static ApiResponseMerchantAll from(pb.merchant.MerchantCommand.ApiResponseMerchantAll proto) {
            return new ApiResponseMerchantAll(
                proto.getStatus(),
                proto.getMessage()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseMerchantDelete")
    public record ApiResponseMerchantDelete(
        String status,
        String message
    ) {
        public static ApiResponseMerchantDelete from(pb.merchant.MerchantCommand.ApiResponseMerchantDelete proto) {
            return new ApiResponseMerchantDelete(
                proto.getStatus(),
                proto.getMessage()
            );
        }
    }
}
