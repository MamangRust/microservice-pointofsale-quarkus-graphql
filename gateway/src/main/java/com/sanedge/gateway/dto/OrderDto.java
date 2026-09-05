package com.sanedge.gateway.dto;

import java.util.List;
import java.util.stream.Collectors;

public class OrderDto {

    @org.eclipse.microprofile.graphql.Name("CreateOrderItemRequest")
    public record CreateOrderItemRequest(
            int productId,
            int quantity) {
    }

    @org.eclipse.microprofile.graphql.Name("CreateOrderRequest")
    public record CreateOrderRequest(
            int merchantId,
            int cashierId,
            List<CreateOrderItemRequest> items) {
    }

    @org.eclipse.microprofile.graphql.Name("UpdateOrderItemRequest")
    public record UpdateOrderItemRequest(
            int orderItemId,
            int productId,
            int quantity) {
    }

    @org.eclipse.microprofile.graphql.Name("UpdateOrderRequest")
    public record UpdateOrderRequest(
            int cashierId,
            List<UpdateOrderItemRequest> items) {
    }

    @org.eclipse.microprofile.graphql.Name("OrderMonthlyResponse")
    public record OrderMonthlyResponse(
            String month,
            int orderCount,
            int totalRevenue,
            int totalItemsSold) {
        public static OrderMonthlyResponse from(pb.order.Order.OrderMonthlyResponse proto) {
            return new OrderMonthlyResponse(
                    proto.getMonth(),
                    proto.getOrderCount(),
                    proto.getTotalRevenue(),
                    proto.getTotalItemsSold());
        }
    }

    @org.eclipse.microprofile.graphql.Name("OrderYearlyResponse")
    public record OrderYearlyResponse(
            String year,
            int orderCount,
            int totalRevenue,
            int totalItemsSold,
            int activeCashiers,
            int uniqueProductsSold) {
        public static OrderYearlyResponse from(pb.order.Order.OrderYearlyResponse proto) {
            return new OrderYearlyResponse(
                    proto.getYear(),
                    proto.getOrderCount(),
                    proto.getTotalRevenue(),
                    proto.getTotalItemsSold(),
                    proto.getActiveCashiers(),
                    proto.getUniqueProductsSold());
        }
    }

    @org.eclipse.microprofile.graphql.Name("OrderResponse")
    public record OrderResponse(
            int id,
            int merchantId,
            int cashierId,
            int totalPrice,
            String createdAt,
            String updatedAt) {
        public static OrderResponse from(pb.order.Order.OrderResponse proto) {
            return new OrderResponse(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getCashierId(),
                    proto.getTotalPrice(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt());
        }
    }

    @org.eclipse.microprofile.graphql.Name("OrderResponseDeleteAt")
    public record OrderResponseDeleteAt(
            int id,
            int merchantId,
            int cashierId,
            int totalPrice,
            String createdAt,
            String updatedAt,
            String deletedAt) {
        public static OrderResponseDeleteAt from(pb.order.Order.OrderResponseDeleteAt proto) {
            return new OrderResponseDeleteAt(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getCashierId(),
                    proto.getTotalPrice(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt(),
                    proto.hasDeletedAt() ? proto.getDeletedAt().getValue() : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("OrderMonthlyTotalRevenueResponse")
    public record OrderMonthlyTotalRevenueResponse(
            String year,
            String month,
            int orderCount,
            int totalRevenue,
            int totalItemsSold) {
        public static OrderMonthlyTotalRevenueResponse from(pb.order.Order.OrderMonthlyTotalRevenueResponse proto) {
            return new OrderMonthlyTotalRevenueResponse(
                    proto.getYear(),
                    proto.getMonth(),
                    proto.getOrderCount(),
                    proto.getTotalRevenue(),
                    proto.getTotalItemsSold());
        }
    }

    @org.eclipse.microprofile.graphql.Name("OrderYearlyTotalRevenueResponse")
    public record OrderYearlyTotalRevenueResponse(
            String year,
            int orderCount,
            int totalRevenue,
            int totalItemsSold,
            int activeCashiers,
            int uniqueProductsSold) {
        public static OrderYearlyTotalRevenueResponse from(pb.order.Order.OrderYearlyTotalRevenueResponse proto) {
            return new OrderYearlyTotalRevenueResponse(
                    proto.getYear(),
                    proto.getOrderCount(),
                    proto.getTotalRevenue(),
                    proto.getTotalItemsSold(),
                    proto.getActiveCashiers(),
                    proto.getUniqueProductsSold());
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseOrder")
    public record ApiResponseOrder(
            String status,
            String message,
            OrderResponse data) {
        public static ApiResponseOrder from(pb.order.Order.ApiResponseOrder proto) {
            return new ApiResponseOrder(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? OrderResponse.from(proto.getData()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseOrderDeleteAt")
    public record ApiResponseOrderDeleteAt(
            String status,
            String message,
            OrderResponseDeleteAt data) {
        public static ApiResponseOrderDeleteAt from(pb.order.Order.ApiResponseOrderDeleteAt proto) {
            return new ApiResponseOrderDeleteAt(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? OrderResponseDeleteAt.from(proto.getData()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseOrderMonthly")
    public record ApiResponseOrderMonthly(
            String status,
            String message,
            List<OrderMonthlyResponse> data) {
        public static ApiResponseOrderMonthly from(pb.order.Order.ApiResponseOrderMonthly proto) {
            return new ApiResponseOrderMonthly(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(OrderMonthlyResponse::from).collect(Collectors.toList()));
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseOrderYearly")
    public record ApiResponseOrderYearly(
            String status,
            String message,
            List<OrderYearlyResponse> data) {
        public static ApiResponseOrderYearly from(pb.order.Order.ApiResponseOrderYearly proto) {
            return new ApiResponseOrderYearly(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(OrderYearlyResponse::from).collect(Collectors.toList()));
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseOrderMonthlyTotalRevenue")
    public record ApiResponseOrderMonthlyTotalRevenue(
            String status,
            String message,
            List<OrderMonthlyTotalRevenueResponse> data) {
        public static ApiResponseOrderMonthlyTotalRevenue from(
                pb.order.Order.ApiResponseOrderMonthlyTotalRevenue proto) {
            return new ApiResponseOrderMonthlyTotalRevenue(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(OrderMonthlyTotalRevenueResponse::from)
                            .collect(Collectors.toList()));
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseOrderYearlyTotalRevenue")
    public record ApiResponseOrderYearlyTotalRevenue(
            String status,
            String message,
            List<OrderYearlyTotalRevenueResponse> data) {
        public static ApiResponseOrderYearlyTotalRevenue from(pb.order.Order.ApiResponseOrderYearlyTotalRevenue proto) {
            return new ApiResponseOrderYearlyTotalRevenue(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(OrderYearlyTotalRevenueResponse::from)
                            .collect(Collectors.toList()));
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationOrder")
    public record ApiResponsePaginationOrder(
            String status,
            String message,
            List<OrderResponse> data,
            PaginationMetaDto paginationMeta) {
        public static ApiResponsePaginationOrder from(pb.order.OrderQuery.ApiResponsePaginationOrder proto) {
            return new ApiResponsePaginationOrder(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(OrderResponse::from).collect(Collectors.toList()),
                    proto.hasPagination() ? PaginationMetaDto.from(proto.getPagination()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationOrderDeleteAt")
    public record ApiResponsePaginationOrderDeleteAt(
            String status,
            String message,
            List<OrderResponseDeleteAt> data,
            PaginationMetaDto paginationMeta) {
        public static ApiResponsePaginationOrderDeleteAt from(
                pb.order.OrderQuery.ApiResponsePaginationOrderDeleteAt proto) {
            return new ApiResponsePaginationOrderDeleteAt(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(OrderResponseDeleteAt::from).collect(Collectors.toList()),
                    proto.hasPagination() ? PaginationMetaDto.from(proto.getPagination()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseOrderAll")
    public record ApiResponseOrderAll(
            String status,
            String message) {
        public static ApiResponseOrderAll from(pb.order.Order.ApiResponseOrderAll proto) {
            return new ApiResponseOrderAll(
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseOrderDelete")
    public record ApiResponseOrderDelete(
            String status,
            String message) {
        public static ApiResponseOrderDelete from(pb.order.Order.ApiResponseOrderDelete proto) {
            return new ApiResponseOrderDelete(
                    proto.getStatus(),
                    proto.getMessage());
        }
    }
}
