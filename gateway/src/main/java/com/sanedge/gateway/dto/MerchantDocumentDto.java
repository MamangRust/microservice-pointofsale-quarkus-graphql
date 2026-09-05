package com.sanedge.gateway.dto;

import java.util.List;
import java.util.stream.Collectors;

public class MerchantDocumentDto {

    @org.eclipse.microprofile.graphql.Name("CreateMerchantDocumentRequest")
    public record CreateMerchantDocumentRequest(
        int merchantId,
        String documentType,
        String documentUrl
    ) {}

    @org.eclipse.microprofile.graphql.Name("UpdateMerchantDocumentRequest")
    public record UpdateMerchantDocumentRequest(
        int merchantId,
        String documentType,
        String documentUrl,
        String note,
        String status
    ) {}

    @org.eclipse.microprofile.graphql.Name("UpdateMerchantDocumentStatusRequest")
    public record UpdateMerchantDocumentStatusRequest(
        int merchantId,
        String note,
        String status
    ) {}

    @org.eclipse.microprofile.graphql.Name("MerchantDocument")
    public record MerchantDocument(
        int documentId,
        int merchantId,
        String documentType,
        String documentUrl,
        String status,
        String note,
        String uploadedAt,
        String updatedAt
    ) {
        public static MerchantDocument from(pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument proto) {
            return new MerchantDocument(
                proto.getDocumentId(),
                proto.getMerchantId(),
                proto.getDocumentType(),
                proto.getDocumentUrl(),
                proto.getStatus(),
                proto.getNote(),
                proto.getUploadedAt(),
                proto.getUpdatedAt()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("MerchantDocumentDeleteAt")
    public record MerchantDocumentDeleteAt(
        int documentId,
        int merchantId,
        String documentType,
        String documentUrl,
        String status,
        String note,
        String uploadedAt,
        String updatedAt,
        String deletedAt
    ) {
        public static MerchantDocumentDeleteAt from(pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt proto) {
            return new MerchantDocumentDeleteAt(
                proto.getDocumentId(),
                proto.getMerchantId(),
                proto.getDocumentType(),
                proto.getDocumentUrl(),
                proto.getStatus(),
                proto.getNote(),
                proto.getUploadedAt(),
                proto.getUpdatedAt(),
                proto.hasDeletedAt() ? proto.getDeletedAt().getValue() : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseMerchantDocument")
    public record ApiResponseMerchantDocument(
        String status,
        String message,
        MerchantDocument data
    ) {
        public static ApiResponseMerchantDocument from(pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument proto) {
            return new ApiResponseMerchantDocument(
                proto.getStatus(),
                proto.getMessage(),
                proto.hasData() ? MerchantDocument.from(proto.getData()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseMerchantDocumentDeleteAt")
    public record ApiResponseMerchantDocumentDeleteAt(
        String status,
        String message,
        MerchantDocumentDeleteAt data
    ) {
        public static ApiResponseMerchantDocumentDeleteAt from(pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt proto) {
            return new ApiResponseMerchantDocumentDeleteAt(
                proto.getStatus(),
                proto.getMessage(),
                proto.hasData() ? MerchantDocumentDeleteAt.from(proto.getData()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationMerchantDocument")
    public record ApiResponsePaginationMerchantDocument(
        String status,
        String message,
        List<MerchantDocument> data,
        PaginationMetaDto paginationMeta
    ) {
        public static ApiResponsePaginationMerchantDocument from(pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocument proto) {
            return new ApiResponsePaginationMerchantDocument(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(MerchantDocument::from).collect(Collectors.toList()),
                proto.hasPaginationMeta() ? PaginationMetaDto.from(proto.getPaginationMeta()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationMerchantDocumentDeleteAt")
    public record ApiResponsePaginationMerchantDocumentDeleteAt(
        String status,
        String message,
        List<MerchantDocumentDeleteAt> data,
        PaginationMetaDto paginationMeta
    ) {
        public static ApiResponsePaginationMerchantDocumentDeleteAt from(pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocumentAt proto) {
            return new ApiResponsePaginationMerchantDocumentDeleteAt(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(MerchantDocumentDeleteAt::from).collect(Collectors.toList()),
                proto.hasPaginationMeta() ? PaginationMetaDto.from(proto.getPaginationMeta()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseMerchantDocumentAll")
    public record ApiResponseMerchantDocumentAll(
        String status,
        String message
    ) {
        public static ApiResponseMerchantDocumentAll from(pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentAll proto) {
            return new ApiResponseMerchantDocumentAll(
                proto.getStatus(),
                proto.getMessage()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseMerchantDocumentDelete")
    public record ApiResponseMerchantDocumentDelete(
        String status,
        String message
    ) {
        public static ApiResponseMerchantDocumentDelete from(pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentDelete proto) {
            return new ApiResponseMerchantDocumentDelete(
                proto.getStatus(),
                proto.getMessage()
            );
        }
    }
}
