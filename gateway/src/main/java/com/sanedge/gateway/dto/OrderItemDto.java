package com.sanedge.gateway.dto;

import java.util.List;
import java.util.stream.Collectors;

public class OrderItemDto {

    @org.eclipse.microprofile.graphql.Name("CreateOrderItemRequest")
    public record CreateOrderItemRequest(
            int orderId,
            int productId,
            int quantity,
            int price) {
    }

    @org.eclipse.microprofile.graphql.Name("UpdateOrderItemRequest")
    public record UpdateOrderItemRequest(
            int orderItemId,
            int orderId,
            int productId,
            int quantity,
            int price) {
    }

    @org.eclipse.microprofile.graphql.Name("OrderItemResponse")
    public record OrderItemResponse(
            int id,
            int orderId,
            int productId,
            int quantity,
            int price,
            String createdAt,
            String updatedAt) {
        public static OrderItemResponse from(pb.order_item.OrderItem.OrderItemResponse proto) {
            return new OrderItemResponse(
                    proto.getId(),
                    proto.getOrderId(),
                    proto.getProductId(),
                    proto.getQuantity(),
                    proto.getPrice(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt());
        }
    }

    @org.eclipse.microprofile.graphql.Name("OrderItemResponseDeleteAt")
    public record OrderItemResponseDeleteAt(
            int id,
            int orderId,
            int productId,
            int quantity,
            int price,
            String createdAt,
            String updatedAt,
            String deletedAt) {
        public static OrderItemResponseDeleteAt from(pb.order_item.OrderItem.OrderItemResponseDeleteAt proto) {
            return new OrderItemResponseDeleteAt(
                    proto.getId(),
                    proto.getOrderId(),
                    proto.getProductId(),
                    proto.getQuantity(),
                    proto.getPrice(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt(),
                    proto.hasDeletedAt() ? proto.getDeletedAt().getValue() : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseOrderItem")
    public record ApiResponseOrderItem(
            String status,
            String message,
            OrderItemResponse data) {
        public static ApiResponseOrderItem from(pb.order_item.OrderItem.ApiResponseOrderItem proto) {
            return new ApiResponseOrderItem(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? OrderItemResponse.from(proto.getData()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseOrderItemDeleteAt")
    public record ApiResponseOrderItemDeleteAt(
            String status,
            String message,
            OrderItemResponseDeleteAt data) {
        public static ApiResponseOrderItemDeleteAt from(pb.order_item.OrderItem.ApiResponseOrderItemDeleteAt proto) {
            return new ApiResponseOrderItemDeleteAt(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? OrderItemResponseDeleteAt.from(proto.getData()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsesOrderItem")
    public record ApiResponsesOrderItem(
            String status,
            String message,
            List<OrderItemResponse> data) {
        public static ApiResponsesOrderItem from(pb.order_item.OrderItem.ApiResponsesOrderItem proto) {
            return new ApiResponsesOrderItem(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(OrderItemResponse::from).collect(Collectors.toList()));
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseOrderItemDelete")
    public record ApiResponseOrderItemDelete(
            String status,
            String message) {
        public static ApiResponseOrderItemDelete from(pb.order_item.OrderItem.ApiResponseOrderItemDelete proto) {
            return new ApiResponseOrderItemDelete(
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseOrderItemAll")
    public record ApiResponseOrderItemAll(
            String status,
            String message) {
        public static ApiResponseOrderItemAll from(pb.order_item.OrderItem.ApiResponseOrderItemAll proto) {
            return new ApiResponseOrderItemAll(
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationOrderItem")
    public record ApiResponsePaginationOrderItem(
            String status,
            String message,
            List<OrderItemResponse> data,
            PaginationMetaDto paginationMeta) {
        public static ApiResponsePaginationOrderItem from(
                pb.order_item.OrderItemQuery.ApiResponsePaginationOrderItem proto) {
            return new ApiResponsePaginationOrderItem(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(OrderItemResponse::from).collect(Collectors.toList()),
                    proto.hasPaginationMeta() ? PaginationMetaDto.from(proto.getPaginationMeta()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationOrderItemDeleteAt")
    public record ApiResponsePaginationOrderItemDeleteAt(
            String status,
            String message,
            List<OrderItemResponseDeleteAt> data,
            PaginationMetaDto paginationMeta) {
        public static ApiResponsePaginationOrderItemDeleteAt from(
                pb.order_item.OrderItemQuery.ApiResponsePaginationOrderItemDeleteAt proto) {
            return new ApiResponsePaginationOrderItemDeleteAt(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(OrderItemResponseDeleteAt::from).collect(Collectors.toList()),
                    proto.hasPaginationMeta() ? PaginationMetaDto.from(proto.getPaginationMeta()) : null);
        }
    }
}
