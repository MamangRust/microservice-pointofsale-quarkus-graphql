package com.sanedge.gateway.resource;

import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.sanedge.gateway.dto.OrderItemDto;
import com.sanedge.gateway.service.OrderItemService;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

@GraphQLApi
public class OrderItemResource {

        @Inject
        OrderItemService orderItemService;

        @Query("orderItems")
        @Description("List all order items")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderItemDto.ApiResponsePaginationOrderItem> listOrderItems(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return orderItemService.listOrderItems(page, size, search);
        }

        @Query("activeOrderItems")
        @Description("Get active order items")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderItemDto.ApiResponsePaginationOrderItemDeleteAt> getActiveOrderItems(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return orderItemService.getActiveOrderItems(page, size, search);
        }

        @Query("trashedOrderItems")
        @Description("Get trashed order items")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderItemDto.ApiResponsePaginationOrderItemDeleteAt> getTrashedOrderItems(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return orderItemService.getTrashedOrderItems(page, size, search);
        }

        @Query("orderItemsByOrder")
        @Description("Get order items by order ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderItemDto.ApiResponsesOrderItem> getOrderItemsByOrder(@Name("orderId") int orderId) {
                return orderItemService.getOrderItemsByOrder(orderId);
        }

        @Mutation("createOrderItem")
        @Description("Create a new order item")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<OrderItemDto.ApiResponseOrderItem> createOrderItem(
                        @Name("body") OrderItemDto.CreateOrderItemRequest body) {
                return orderItemService.createOrderItem(body);
        }

        @Mutation("updateOrderItem")
        @Description("Update order item")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<OrderItemDto.ApiResponseOrderItem> updateOrderItem(@Name("id") int id,
                        @Name("body") OrderItemDto.UpdateOrderItemRequest body) {
                return orderItemService.updateOrderItem(id, body);
        }

        @Mutation("deleteOrderItem")
        @Description("Soft-delete an order item")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<OrderItemDto.ApiResponseOrderItemDeleteAt> deleteOrderItem(@Name("id") int id) {
                return orderItemService.deleteOrderItem(id);
        }

        @Mutation("restoreOrderItem")
        @Description("Restore a soft-deleted order item")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<OrderItemDto.ApiResponseOrderItemDeleteAt> restoreOrderItem(@Name("id") int id) {
                return orderItemService.restoreOrderItem(id);
        }

        @Mutation("deleteOrderItemPermanent")
        @Description("Permanently delete an order item")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<OrderItemDto.ApiResponseOrderItemDelete> deleteOrderItemPermanent(@Name("id") int id) {
                return orderItemService.deleteOrderItemPermanent(id);
        }

        @Mutation("restoreAllOrderItems")
        @Description("Restore all soft-deleted order items")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<OrderItemDto.ApiResponseOrderItemAll> restoreAllOrderItems() {
                return orderItemService.restoreAllOrderItems();
        }

        @Mutation("deleteAllOrderItemsPermanent")
        @Description("Permanently delete all order items")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<OrderItemDto.ApiResponseOrderItemAll> deleteAllOrderItems() {
                return orderItemService.deleteAllOrderItems();
        }
}
