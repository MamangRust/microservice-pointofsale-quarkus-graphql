package com.sanedge.gateway.dto;

import java.util.List;
import java.util.stream.Collectors;

public class CashierDto {

    @org.eclipse.microprofile.graphql.Name("CreateCashierRequest")
    public record CreateCashierRequest(
            int merchantId,
            int userId,
            String name) {
    }

    @org.eclipse.microprofile.graphql.Name("UpdateCashierRequest")
    public record UpdateCashierRequest(
            String name) {
    }

    @org.eclipse.microprofile.graphql.Name("CashierResponse")
    public record CashierResponse(
            int id,
            int merchantId,
            String name,
            String createdAt,
            String updatedAt) {
        public static CashierResponse from(pb.cashier.Cashier.CashierResponse proto) {
            return new CashierResponse(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getName(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt());
        }
    }

    @org.eclipse.microprofile.graphql.Name("CashierResponseDeleteAt")
    public record CashierResponseDeleteAt(
            int id,
            int merchantId,
            String name,
            String createdAt,
            String updatedAt,
            String deletedAt) {
        public static CashierResponseDeleteAt from(pb.cashier.Cashier.CashierResponseDeleteAt proto) {
            return new CashierResponseDeleteAt(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getName(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt(),
                    proto.hasDeletedAt() ? proto.getDeletedAt().getValue() : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("CashierResponseMonthSales")
    public record CashierResponseMonthSales(
            String month,
            int cashierId,
            String cashierName,
            int orderCount,
            int totalSales) {
        public static CashierResponseMonthSales from(pb.cashier.Cashier.CashierResponseMonthSales proto) {
            return new CashierResponseMonthSales(
                    proto.getMonth(),
                    proto.getCashierId(),
                    proto.getCashierName(),
                    proto.getOrderCount(),
                    proto.getTotalSales());
        }
    }

    @org.eclipse.microprofile.graphql.Name("CashierResponseYearSales")
    public record CashierResponseYearSales(
            String year,
            int cashierId,
            String cashierName,
            int orderCount,
            int totalSales) {
        public static CashierResponseYearSales from(pb.cashier.Cashier.CashierResponseYearSales proto) {
            return new CashierResponseYearSales(
                    proto.getYear(),
                    proto.getCashierId(),
                    proto.getCashierName(),
                    proto.getOrderCount(),
                    proto.getTotalSales());
        }
    }

    @org.eclipse.microprofile.graphql.Name("CashierResponseMonthTotalSales")
    public record CashierResponseMonthTotalSales(
            String year,
            String month,
            int totalSales) {
        public static CashierResponseMonthTotalSales from(pb.cashier.Cashier.CashierResponseMonthTotalSales proto) {
            return new CashierResponseMonthTotalSales(
                    proto.getYear(),
                    proto.getMonth(),
                    proto.getTotalSales());
        }
    }

    @org.eclipse.microprofile.graphql.Name("CashierResponseYearTotalSales")
    public record CashierResponseYearTotalSales(
            String year,
            int totalSales) {
        public static CashierResponseYearTotalSales from(pb.cashier.Cashier.CashierResponseYearTotalSales proto) {
            return new CashierResponseYearTotalSales(
                    proto.getYear(),
                    proto.getTotalSales());
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCashier")
    public record ApiResponseCashier(
            String status,
            String message,
            CashierResponse data) {
        public static ApiResponseCashier from(pb.cashier.Cashier.ApiResponseCashier proto) {
            return new ApiResponseCashier(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? CashierResponse.from(proto.getData()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCashierDeleteAt")
    public record ApiResponseCashierDeleteAt(
            String status,
            String message,
            CashierResponseDeleteAt data) {
        public static ApiResponseCashierDeleteAt from(pb.cashier.Cashier.ApiResponseCashierDeleteAt proto) {
            return new ApiResponseCashierDeleteAt(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? CashierResponseDeleteAt.from(proto.getData()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCashierMonthSales")
    public record ApiResponseCashierMonthSales(
            String status,
            String message,
            List<CashierResponseMonthSales> data) {
        public static ApiResponseCashierMonthSales from(pb.cashier.Cashier.ApiResponseCashierMonthSales proto) {
            return new ApiResponseCashierMonthSales(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(CashierResponseMonthSales::from).collect(Collectors.toList()));
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCashierYearSales")
    public record ApiResponseCashierYearSales(
            String status,
            String message,
            List<CashierResponseYearSales> data) {
        public static ApiResponseCashierYearSales from(pb.cashier.Cashier.ApiResponseCashierYearSales proto) {
            return new ApiResponseCashierYearSales(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(CashierResponseYearSales::from).collect(Collectors.toList()));
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCashierMonthlyTotalSales")
    public record ApiResponseCashierMonthlyTotalSales(
            String status,
            String message,
            List<CashierResponseMonthTotalSales> data) {
        public static ApiResponseCashierMonthlyTotalSales from(
                pb.cashier.stats.CashierTotalSales.ApiResponseCashierMonthlyTotalSales proto) {
            return new ApiResponseCashierMonthlyTotalSales(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(CashierResponseMonthTotalSales::from).collect(Collectors.toList()));
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCashierYearlyTotalSales")
    public record ApiResponseCashierYearlyTotalSales(
            String status,
            String message,
            List<CashierResponseYearTotalSales> data) {
        public static ApiResponseCashierYearlyTotalSales from(
                pb.cashier.stats.CashierTotalSales.ApiResponseCashierYearlyTotalSales proto) {
            return new ApiResponseCashierYearlyTotalSales(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(CashierResponseYearTotalSales::from).collect(Collectors.toList()));
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationCashier")
    public record ApiResponsePaginationCashier(
            String status,
            String message,
            List<CashierResponse> data,
            PaginationMetaDto paginationMeta) {
        public static ApiResponsePaginationCashier from(pb.cashier.CashierQuery.ApiResponsePaginationCashier proto) {
            return new ApiResponsePaginationCashier(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(CashierResponse::from).collect(Collectors.toList()),
                    proto.hasPagination() ? PaginationMetaDto.from(proto.getPagination()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationCashierDeleteAt")
    public record ApiResponsePaginationCashierDeleteAt(
            String status,
            String message,
            List<CashierResponseDeleteAt> data,
            PaginationMetaDto paginationMeta) {
        public static ApiResponsePaginationCashierDeleteAt from(
                pb.cashier.CashierQuery.ApiResponsePaginationCashierDeleteAt proto) {
            return new ApiResponsePaginationCashierDeleteAt(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(CashierResponseDeleteAt::from).collect(Collectors.toList()),
                    proto.hasPagination() ? PaginationMetaDto.from(proto.getPagination()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCashierAll")
    public record ApiResponseCashierAll(
            String status,
            String message) {
        public static ApiResponseCashierAll from(pb.cashier.CashierCommand.ApiResponseCashierAll proto) {
            return new ApiResponseCashierAll(
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCashierDelete")
    public record ApiResponseCashierDelete(
            String status,
            String message) {
        public static ApiResponseCashierDelete from(pb.cashier.CashierCommand.ApiResponseCashierDelete proto) {
            return new ApiResponseCashierDelete(
                    proto.getStatus(),
                    proto.getMessage());
        }
    }
}
