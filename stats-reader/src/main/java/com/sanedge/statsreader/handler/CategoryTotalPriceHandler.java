package com.sanedge.statsreader.handler;

import com.sanedge.statsreader.cache.StatsQueryService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.category.stats.MutinyCategoryTotalPriceServiceGrpc;
import pb.category.Category;

@GrpcService
@Singleton
public class CategoryTotalPriceHandler
        extends MutinyCategoryTotalPriceServiceGrpc.CategoryTotalPriceServiceImplBase {

    @Inject
    StatsQueryService statsQuery;

    // No order_item_daily in ClickHouse yet — return empty data
    private <T> T emptyResponse(Class<T> clazz, String message) {
        try {
            var builder = clazz.getMethod("newBuilder").invoke(null);
            builder.getClass().getMethod("setStatus", String.class).invoke(builder, "success");
            builder.getClass().getMethod("setMessage", String.class).invoke(builder, message);
            return (T) builder.getClass().getMethod("build").invoke(builder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Uni<Category.ApiResponseCategoryMonthlyTotalPrice> findMonthlyTotalPrices(
            Category.FindYearMonthTotalPrices request) {
        return Uni.createFrom().item(Category.ApiResponseCategoryMonthlyTotalPrice.newBuilder()
                .setStatus("success").setMessage("Category stats pending order_item pipeline").build());
    }

    @Override
    public Uni<Category.ApiResponseCategoryYearlyTotalPrice> findYearlyTotalPrices(
            Category.FindYearTotalPrices request) {
        return Uni.createFrom().item(Category.ApiResponseCategoryYearlyTotalPrice.newBuilder()
                .setStatus("success").setMessage("Category stats pending order_item pipeline").build());
    }

    @Override
    public Uni<Category.ApiResponseCategoryMonthlyTotalPrice> findMonthlyTotalPricesById(
            Category.FindYearMonthTotalPriceById request) {
        return Uni.createFrom().item(Category.ApiResponseCategoryMonthlyTotalPrice.newBuilder()
                .setStatus("success").setMessage("Category stats pending order_item pipeline").build());
    }

    @Override
    public Uni<Category.ApiResponseCategoryYearlyTotalPrice> findYearlyTotalPricesById(
            Category.FindYearTotalPriceById request) {
        return Uni.createFrom().item(Category.ApiResponseCategoryYearlyTotalPrice.newBuilder()
                .setStatus("success").setMessage("Category stats pending order_item pipeline").build());
    }

    @Override
    public Uni<Category.ApiResponseCategoryMonthlyTotalPrice> findMonthlyTotalPricesByMerchant(
            Category.FindYearMonthTotalPriceByMerchant request) {
        return Uni.createFrom().item(Category.ApiResponseCategoryMonthlyTotalPrice.newBuilder()
                .setStatus("success").setMessage("Category stats pending order_item pipeline").build());
    }

    @Override
    public Uni<Category.ApiResponseCategoryYearlyTotalPrice> findYearlyTotalPricesByMerchant(
            Category.FindYearTotalPriceByMerchant request) {
        return Uni.createFrom().item(Category.ApiResponseCategoryYearlyTotalPrice.newBuilder()
                .setStatus("success").setMessage("Category stats pending order_item pipeline").build());
    }
}
