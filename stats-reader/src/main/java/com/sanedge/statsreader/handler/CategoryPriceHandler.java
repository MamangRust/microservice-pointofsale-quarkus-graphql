package com.sanedge.statsreader.handler;

import com.sanedge.statsreader.cache.StatsQueryService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.category.stats.MutinyCategoryPriceServiceGrpc;
import pb.category.Category;

@GrpcService
@Singleton
public class CategoryPriceHandler
        extends MutinyCategoryPriceServiceGrpc.CategoryPriceServiceImplBase {

    @Inject
    StatsQueryService statsQuery;

    @Override
    public Uni<Category.ApiResponseCategoryMonthPrice> findMonthPrice(Category.FindYearCategory request) {
        return Uni.createFrom().item(Category.ApiResponseCategoryMonthPrice.newBuilder()
                .setStatus("success").setMessage("Category stats pending order_item pipeline").build());
    }

    @Override
    public Uni<Category.ApiResponseCategoryYearPrice> findYearPrice(Category.FindYearCategory request) {
        return Uni.createFrom().item(Category.ApiResponseCategoryYearPrice.newBuilder()
                .setStatus("success").setMessage("Category stats pending order_item pipeline").build());
    }

    @Override
    public Uni<Category.ApiResponseCategoryMonthPrice> findMonthPriceByMerchant(Category.FindYearCategoryByMerchant request) {
        return Uni.createFrom().item(Category.ApiResponseCategoryMonthPrice.newBuilder()
                .setStatus("success").setMessage("Category stats pending order_item pipeline").build());
    }

    @Override
    public Uni<Category.ApiResponseCategoryYearPrice> findYearPriceByMerchant(Category.FindYearCategoryByMerchant request) {
        return Uni.createFrom().item(Category.ApiResponseCategoryYearPrice.newBuilder()
                .setStatus("success").setMessage("Category stats pending order_item pipeline").build());
    }

    @Override
    public Uni<Category.ApiResponseCategoryMonthPrice> findMonthPriceById(Category.FindYearCategoryById request) {
        return Uni.createFrom().item(Category.ApiResponseCategoryMonthPrice.newBuilder()
                .setStatus("success").setMessage("Category stats pending order_item pipeline").build());
    }

    @Override
    public Uni<Category.ApiResponseCategoryYearPrice> findYearPriceById(Category.FindYearCategoryById request) {
        return Uni.createFrom().item(Category.ApiResponseCategoryYearPrice.newBuilder()
                .setStatus("success").setMessage("Category stats pending order_item pipeline").build());
    }
}
