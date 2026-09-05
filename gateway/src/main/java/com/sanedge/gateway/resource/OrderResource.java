package com.sanedge.gateway.resource;

import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.sanedge.gateway.dto.OrderDto;
import com.sanedge.gateway.service.OrderService;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

@GraphQLApi
public class OrderResource {

        @Inject
        OrderService orderService;

        @Query("orders")
        @Description("List all orders")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponsePaginationOrder> listOrders(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return orderService.listOrders(page, size, search);
        }

        @Query("activeOrders")
        @Description("Get active orders")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponsePaginationOrderDeleteAt> getActiveOrders(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return orderService.getActiveOrders(page, size, search);
        }

        @Query("trashedOrders")
        @Description("Get trashed orders")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponsePaginationOrderDeleteAt> getTrashedOrders(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return orderService.getTrashedOrders(page, size, search);
        }

        @Query("order")
        @Description("Get order by ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponseOrder> getOrder(@Name("id") int id) {
                return orderService.getOrder(id);
        }

        @Query("ordersByMerchant")
        @Description("Get orders by merchant ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponsePaginationOrder> getOrdersByMerchant(
                        @Name("merchantId") int merchantId,
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return orderService.getOrdersByMerchant(merchantId, page, size, search);
        }

        @Mutation("createOrder")
        @Description("Create a new order")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponseOrder> createOrder(@Name("body") OrderDto.CreateOrderRequest body) {
                return orderService.createOrder(body);
        }

        @Mutation("updateOrder")
        @Description("Update order")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponseOrder> updateOrder(@Name("id") int id,
                        @Name("body") OrderDto.UpdateOrderRequest body) {
                return orderService.updateOrder(id, body);
        }

        @Mutation("deleteOrder")
        @Description("Soft-delete an order")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponseOrderDeleteAt> deleteOrder(@Name("id") int id) {
                return orderService.deleteOrder(id);
        }

        @Mutation("restoreOrder")
        @Description("Restore a soft-deleted order")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponseOrderDeleteAt> restoreOrder(@Name("id") int id) {
                return orderService.restoreOrder(id);
        }

        @Mutation("deleteOrderPermanent")
        @Description("Permanently delete an order")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<OrderDto.ApiResponseOrderDelete> deleteOrderPermanent(@Name("id") int id) {
                return orderService.deleteOrderPermanent(id);
        }

        @Mutation("restoreAllOrders")
        @Description("Restore all soft-deleted orders")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<OrderDto.ApiResponseOrderAll> restoreAllOrders() {
                return orderService.restoreAllOrders();
        }

        @Mutation("deleteAllOrdersPermanent")
        @Description("Permanently delete all soft-deleted orders")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<OrderDto.ApiResponseOrderAll> deleteAllOrders() {
                return orderService.deleteAllOrders();
        }

        @Query("orderMonthlyTotalRevenues")
        @Description("Get monthly total revenues stats")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponseOrderMonthlyTotalRevenue> getMonthlyTotalRevenues(
                        @Name("year") int year,
                        @Name("month") int month) {
                return orderService.getMonthlyTotalRevenues(year, month);
        }

        @Query("orderYearlyTotalRevenues")
        @Description("Get yearly total revenues stats")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponseOrderYearlyTotalRevenue> getYearlyTotalRevenues(@Name("year") int year) {
                return orderService.getYearlyTotalRevenues(year);
        }

        @Query("orderMonthlyTotalRevenuesByMerchant")
        @Description("Get monthly total revenues stats by merchant ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponseOrderMonthlyTotalRevenue> getMonthlyTotalRevenuesByMerchant(
                        @Name("merchantId") int merchantId,
                        @Name("year") int year,
                        @Name("month") int month) {
                return orderService.getMonthlyTotalRevenuesByMerchant(merchantId, year, month);
        }

        @Query("orderYearlyTotalRevenuesByMerchant")
        @Description("Get yearly total revenues stats by merchant ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponseOrderYearlyTotalRevenue> getYearlyTotalRevenuesByMerchant(
                        @Name("merchantId") int merchantId,
                        @Name("year") int year) {
                return orderService.getYearlyTotalRevenuesByMerchant(merchantId, year);
        }

        @Query("orderMonthlyRevenues")
        @Description("Get monthly revenues stats")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponseOrderMonthly> getMonthlyRevenues(@Name("year") int year) {
                return orderService.getMonthlyRevenues(year);
        }

        @Query("orderYearlyRevenues")
        @Description("Get yearly revenues stats")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponseOrderYearly> getYearlyRevenues(@Name("year") int year) {
                return orderService.getYearlyRevenues(year);
        }

        @Query("orderMonthlyRevenuesByMerchant")
        @Description("Get monthly revenues stats by merchant ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponseOrderMonthly> getMonthlyRevenuesByMerchant(
                        @Name("merchantId") int merchantId,
                        @Name("year") int year) {
                return orderService.getMonthlyRevenuesByMerchant(merchantId, year);
        }

        @Query("orderYearlyRevenuesByMerchant")
        @Description("Get yearly revenues stats by merchant ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<OrderDto.ApiResponseOrderYearly> getYearlyRevenuesByMerchant(
                        @Name("merchantId") int merchantId,
                        @Name("year") int year) {
                return orderService.getYearlyRevenuesByMerchant(merchantId, year);
        }
}
