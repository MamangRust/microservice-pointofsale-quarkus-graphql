package com.sanedge.gateway.dto;

import java.util.List;
import java.util.stream.Collectors;

public class TransactionDto {

    @org.eclipse.microprofile.graphql.Name("CreateTransactionRequest")
    public record CreateTransactionRequest(
        String cardNumber,
        int amount,
        String paymentMethod,
        int merchantId
    ) {}

    @org.eclipse.microprofile.graphql.Name("UpdateTransactionRequest")
    public record UpdateTransactionRequest(
        String cardNumber,
        int amount,
        String paymentMethod,
        int merchantId
    ) {}

    @org.eclipse.microprofile.graphql.Name("TransactionResponse")
    public record TransactionResponse(
        int id,
        String cardNumber,
        String transactionNo,
        int amount,
        String paymentMethod,
        int merchantId,
        String transactionTime,
        String createdAt,
        String updatedAt
    ) {
        public static TransactionResponse from(pb.transaction.Transaction.TransactionResponse proto) {
            return new TransactionResponse(
                proto.getId(),
                proto.getCardNumber(),
                proto.getTransactionNo(),
                proto.getAmount(),
                proto.getPaymentMethod(),
                proto.getMerchantId(),
                proto.getTransactionTime(),
                proto.getCreatedAt(),
                proto.getUpdatedAt()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("TransactionResponseDeleteAt")
    public record TransactionResponseDeleteAt(
        int id,
        String cardNumber,
        String transactionNo,
        int amount,
        String paymentMethod,
        int merchantId,
        String transactionTime,
        String createdAt,
        String updatedAt,
        String deletedAt
    ) {
        public static TransactionResponseDeleteAt from(pb.transaction.Transaction.TransactionResponseDeleteAt proto) {
            return new TransactionResponseDeleteAt(
                proto.getId(),
                proto.getCardNumber(),
                proto.getTransactionNo(),
                proto.getAmount(),
                proto.getPaymentMethod(),
                proto.getMerchantId(),
                proto.getTransactionTime(),
                proto.getCreatedAt(),
                proto.getUpdatedAt(),
                proto.hasDeletedAt() ? proto.getDeletedAt().getValue() : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("TransactionMonthAmountResponse")
    public record TransactionMonthAmountResponse(
        String month,
        int totalAmount
    ) {
        public static TransactionMonthAmountResponse from(pb.transaction.stats.TransactionStatsAmount.TransactionMonthAmountResponse proto) {
            return new TransactionMonthAmountResponse(
                proto.getMonth(),
                proto.getTotalAmount()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("TransactionYearlyAmountResponse")
    public record TransactionYearlyAmountResponse(
        String year,
        int totalAmount
    ) {
        public static TransactionYearlyAmountResponse from(pb.transaction.stats.TransactionStatsAmount.TransactionYearlyAmountResponse proto) {
            return new TransactionYearlyAmountResponse(
                proto.getYear(),
                proto.getTotalAmount()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("TransactionMonthMethodResponse")
    public record TransactionMonthMethodResponse(
        String month,
        String paymentMethod,
        int totalTransactions,
        int totalAmount
    ) {
        public static TransactionMonthMethodResponse from(pb.transaction.stats.TransactionStatsMethod.TransactionMonthMethodResponse proto) {
            return new TransactionMonthMethodResponse(
                proto.getMonth(),
                proto.getPaymentMethod(),
                proto.getTotalTransactions(),
                proto.getTotalAmount()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("TransactionYearMethodResponse")
    public record TransactionYearMethodResponse(
        String year,
        String paymentMethod,
        int totalTransactions,
        int totalAmount
    ) {
        public static TransactionYearMethodResponse from(pb.transaction.stats.TransactionStatsMethod.TransactionYearMethodResponse proto) {
            return new TransactionYearMethodResponse(
                proto.getYear(),
                proto.getPaymentMethod(),
                proto.getTotalTransactions(),
                proto.getTotalAmount()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("TransactionMonthStatusSuccessResponse")
    public record TransactionMonthStatusSuccessResponse(
        String year,
        String month,
        int totalSuccess,
        int totalAmount
    ) {
        public static TransactionMonthStatusSuccessResponse from(pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusSuccessResponse proto) {
            return new TransactionMonthStatusSuccessResponse(
                proto.getYear(),
                proto.getMonth(),
                proto.getTotalSuccess(),
                proto.getTotalAmount()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("TransactionYearStatusSuccessResponse")
    public record TransactionYearStatusSuccessResponse(
        String year,
        int totalSuccess,
        int totalAmount
    ) {
        public static TransactionYearStatusSuccessResponse from(pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusSuccessResponse proto) {
            return new TransactionYearStatusSuccessResponse(
                proto.getYear(),
                proto.getTotalSuccess(),
                proto.getTotalAmount()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("TransactionMonthStatusFailedResponse")
    public record TransactionMonthStatusFailedResponse(
        String year,
        String month,
        int totalFailed,
        int totalAmount
    ) {
        public static TransactionMonthStatusFailedResponse from(pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusFailedResponse proto) {
            return new TransactionMonthStatusFailedResponse(
                proto.getYear(),
                proto.getMonth(),
                proto.getTotalFailed(),
                proto.getTotalAmount()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("TransactionYearStatusFailedResponse")
    public record TransactionYearStatusFailedResponse(
        String year,
        int totalFailed,
        int totalAmount
    ) {
        public static TransactionYearStatusFailedResponse from(pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusFailedResponse proto) {
            return new TransactionYearStatusFailedResponse(
                proto.getYear(),
                proto.getTotalFailed(),
                proto.getTotalAmount()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseTransaction")
    public record ApiResponseTransaction(
        String status,
        String message,
        TransactionResponse data
    ) {
        public static ApiResponseTransaction from(pb.transaction.Transaction.ApiResponseTransaction proto) {
            return new ApiResponseTransaction(
                proto.getStatus(),
                proto.getMessage(),
                proto.hasData() ? TransactionResponse.from(proto.getData()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseTransactions")
    public record ApiResponseTransactions(
        String status,
        String message,
        List<TransactionResponse> data
    ) {
        public static ApiResponseTransactions from(pb.transaction.Transaction.ApiResponseTransactions proto) {
            return new ApiResponseTransactions(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(TransactionResponse::from).collect(Collectors.toList())
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseTransactionDeleteAt")
    public record ApiResponseTransactionDeleteAt(
        String status,
        String message,
        TransactionResponseDeleteAt data
    ) {
        public static ApiResponseTransactionDeleteAt from(pb.transaction.Transaction.ApiResponseTransactionDeleteAt proto) {
            return new ApiResponseTransactionDeleteAt(
                proto.getStatus(),
                proto.getMessage(),
                proto.hasData() ? TransactionResponseDeleteAt.from(proto.getData()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseTransactionMonthAmount")
    public record ApiResponseTransactionMonthAmount(
        String status,
        String message,
        List<TransactionMonthAmountResponse> data
    ) {
        public static ApiResponseTransactionMonthAmount from(pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionMonthAmount proto) {
            return new ApiResponseTransactionMonthAmount(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(TransactionMonthAmountResponse::from).collect(Collectors.toList())
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseTransactionYearAmount")
    public record ApiResponseTransactionYearAmount(
        String status,
        String message,
        List<TransactionYearlyAmountResponse> data
    ) {
        public static ApiResponseTransactionYearAmount from(pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionYearAmount proto) {
            return new ApiResponseTransactionYearAmount(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(TransactionYearlyAmountResponse::from).collect(Collectors.toList())
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseTransactionMonthMethod")
    public record ApiResponseTransactionMonthMethod(
        String status,
        String message,
        List<TransactionMonthMethodResponse> data
    ) {
        public static ApiResponseTransactionMonthMethod from(pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionMonthMethod proto) {
            return new ApiResponseTransactionMonthMethod(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(TransactionMonthMethodResponse::from).collect(Collectors.toList())
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseTransactionYearMethod")
    public record ApiResponseTransactionYearMethod(
        String status,
        String message,
        List<TransactionYearMethodResponse> data
    ) {
        public static ApiResponseTransactionYearMethod from(pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionYearMethod proto) {
            return new ApiResponseTransactionYearMethod(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(TransactionYearMethodResponse::from).collect(Collectors.toList())
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseTransactionMonthStatusSuccess")
    public record ApiResponseTransactionMonthStatusSuccess(
        String status,
        String message,
        List<TransactionMonthStatusSuccessResponse> data
    ) {
        public static ApiResponseTransactionMonthStatusSuccess from(pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess proto) {
            return new ApiResponseTransactionMonthStatusSuccess(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(TransactionMonthStatusSuccessResponse::from).collect(Collectors.toList())
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseTransactionYearStatusSuccess")
    public record ApiResponseTransactionYearStatusSuccess(
        String status,
        String message,
        List<TransactionYearStatusSuccessResponse> data
    ) {
        public static ApiResponseTransactionYearStatusSuccess from(pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess proto) {
            return new ApiResponseTransactionYearStatusSuccess(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(TransactionYearStatusSuccessResponse::from).collect(Collectors.toList())
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseTransactionMonthStatusFailed")
    public record ApiResponseTransactionMonthStatusFailed(
        String status,
        String message,
        List<TransactionMonthStatusFailedResponse> data
    ) {
        public static ApiResponseTransactionMonthStatusFailed from(pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed proto) {
            return new ApiResponseTransactionMonthStatusFailed(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(TransactionMonthStatusFailedResponse::from).collect(Collectors.toList())
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseTransactionYearStatusFailed")
    public record ApiResponseTransactionYearStatusFailed(
        String status,
        String message,
        List<TransactionYearStatusFailedResponse> data
    ) {
        public static ApiResponseTransactionYearStatusFailed from(pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusFailed proto) {
            return new ApiResponseTransactionYearStatusFailed(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(TransactionYearStatusFailedResponse::from).collect(Collectors.toList())
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationTransaction")
    public record ApiResponsePaginationTransaction(
        String status,
        String message,
        List<TransactionResponse> data,
        PaginationMetaDto paginationMeta
    ) {
        public static ApiResponsePaginationTransaction from(pb.transaction.TransactionQuery.ApiResponsePaginationTransaction proto) {
            return new ApiResponsePaginationTransaction(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(TransactionResponse::from).collect(Collectors.toList()),
                proto.hasPaginationMeta() ? PaginationMetaDto.from(proto.getPaginationMeta()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationTransactionDeleteAt")
    public record ApiResponsePaginationTransactionDeleteAt(
        String status,
        String message,
        List<TransactionResponseDeleteAt> data,
        PaginationMetaDto paginationMeta
    ) {
        public static ApiResponsePaginationTransactionDeleteAt from(pb.transaction.TransactionQuery.ApiResponsePaginationTransactionDeleteAt proto) {
            return new ApiResponsePaginationTransactionDeleteAt(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(TransactionResponseDeleteAt::from).collect(Collectors.toList()),
                proto.hasPaginationMeta() ? PaginationMetaDto.from(proto.getPaginationMeta()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseTransactionAll")
    public record ApiResponseTransactionAll(
        String status,
        String message
    ) {
        public static ApiResponseTransactionAll from(pb.transaction.TransactionCommand.ApiResponseTransactionAll proto) {
            return new ApiResponseTransactionAll(
                proto.getStatus(),
                proto.getMessage()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseTransactionDelete")
    public record ApiResponseTransactionDelete(
        String status,
        String message
    ) {
        public static ApiResponseTransactionDelete from(pb.transaction.TransactionCommand.ApiResponseTransactionDelete proto) {
            return new ApiResponseTransactionDelete(
                proto.getStatus(),
                proto.getMessage()
            );
        }
    }
}
