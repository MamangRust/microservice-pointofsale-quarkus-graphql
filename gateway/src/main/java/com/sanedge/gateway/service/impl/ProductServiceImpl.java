package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.ProductDto;
import com.sanedge.gateway.service.FileService;
import com.sanedge.gateway.service.ProductService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProductServiceImpl implements ProductService {

    private static final Logger LOG = Logger.getLogger(ProductServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @Inject
    FileService fileService;

    @GrpcClient("product")
    pb.product.MutinyProductServiceGrpc.MutinyProductServiceStub productQueryService;

    @GrpcClient("product")
    pb.product.MutinyProductCommandServiceGrpc.MutinyProductCommandServiceStub productCommandService;

    @Override
    public Uni<ProductDto.UploadImageResponse> uploadImage(String base64Data, String fileName) {
        return telemetryHelper.traceAndMetric("product.uploadImage", () -> Uni.createFrom().item(() -> {
            String filename = "static/product/" + System.currentTimeMillis() + "_" + fileName;
            String savedPath = fileService.createFileImageBase64(base64Data, filename);
            if (savedPath == null) {
                throw new RuntimeException("Failed to upload image");
            }
            return new ProductDto.UploadImageResponse(savedPath);
        }).onFailure().invoke(throwable -> LOG.error("Failed to upload product image: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponsePaginationProduct> findAll(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("product.findAll", () -> productQueryService.findAll(pb.product.Product.FindAllProductRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(ProductDto.ApiResponsePaginationProduct::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list products: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponsePaginationProductDeleteAt> findByActive(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("product.findByActive", () -> productQueryService.findByActive(pb.product.Product.FindAllProductRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(ProductDto.ApiResponsePaginationProductDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list active products: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponsePaginationProductDeleteAt> findByTrashed(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("product.findByTrashed", () -> productQueryService.findByTrashed(pb.product.Product.FindAllProductRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(ProductDto.ApiResponsePaginationProductDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list trashed products: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponseProduct> findById(int id) {
        return telemetryHelper.traceAndMetric("product.findById", () -> productQueryService.findById(pb.product.Product.FindByIdProductRequest.newBuilder()
                .setId(id)
                .build())
                .map(ProductDto.ApiResponseProduct::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get product " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponsePaginationProduct> findByMerchant(int merchantId, String search, int categoryId, int minPrice, int maxPrice, int page, int size) {
        return telemetryHelper.traceAndMetric("product.findByMerchant", () -> productQueryService.findByMerchant(pb.product.Product.FindAllProductMerchantRequest.newBuilder()
                .setMerchantId(merchantId)
                .setSearch(search == null ? "" : search)
                .setCategoryId(categoryId)
                .setMinPrice(minPrice)
                .setMaxPrice(maxPrice)
                .setPage(page)
                .setPageSize(size)
                .build())
                .map(ProductDto.ApiResponsePaginationProduct::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list products by merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponsePaginationProduct> findByCategory(String categoryName, int page, int size, String search, int minPrice, int maxPrice) {
        return telemetryHelper.traceAndMetric("product.findByCategory", () -> productQueryService.findByCategory(pb.product.Product.FindAllProductCategoryRequest.newBuilder()
                .setCategoryName(categoryName == null ? "" : categoryName)
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .setMinprice(minPrice)
                .setMaxprice(maxPrice)
                .build())
                .map(ProductDto.ApiResponsePaginationProduct::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list products by category " + categoryName + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponseProduct> createProduct(ProductDto.CreateProductRequest body) {
        return telemetryHelper.traceAndMetric("product.createProduct", () -> productCommandService.create(pb.product.ProductCommand.CreateProductRequest.newBuilder()
                .setMerchantId(body.merchantId())
                .setCategoryId(body.categoryId())
                .setName(body.name())
                .setDescription(body.description())
                .setPrice(body.price())
                .setCountInStock(body.countInStock())
                .setBrand(body.brand())
                .setWeight(body.weight())
                .setImageProduct(body.imageProduct() == null ? "" : body.imageProduct())
                .build())
                .map(ProductDto.ApiResponseProduct::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create product: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponseProduct> updateProduct(int id, ProductDto.UpdateProductRequest body) {
        return telemetryHelper.traceAndMetric("product.updateProduct", () -> productCommandService.update(pb.product.ProductCommand.UpdateProductRequest.newBuilder()
                .setProductId(id)
                .setMerchantId(body.merchantId())
                .setCategoryId(body.categoryId())
                .setName(body.name())
                .setDescription(body.description())
                .setPrice(body.price())
                .setCountInStock(body.countInStock())
                .setBrand(body.brand())
                .setWeight(body.weight())
                .setImageProduct(body.imageProduct() == null ? "" : body.imageProduct())
                .build())
                .map(ProductDto.ApiResponseProduct::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update product " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponseProductDeleteAt> deleteProduct(int id) {
        return telemetryHelper.traceAndMetric("product.deleteProduct", () -> productCommandService.trashedProduct(pb.product.Product.FindByIdProductRequest.newBuilder()
                .setId(id)
                .build())
                .map(ProductDto.ApiResponseProductDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete product " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponseProductDeleteAt> restoreProduct(int id) {
        return telemetryHelper.traceAndMetric("product.restoreProduct", () -> productCommandService.restoreProduct(pb.product.Product.FindByIdProductRequest.newBuilder()
                .setId(id)
                .build())
                .map(ProductDto.ApiResponseProductDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore product " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponseProductDelete> deleteProductPermanent(int id) {
        return telemetryHelper.traceAndMetric("product.deleteProductPermanent", () -> productCommandService.deleteProductPermanent(pb.product.Product.FindByIdProductRequest.newBuilder()
                .setId(id)
                .build())
                .map(ProductDto.ApiResponseProductDelete::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete product " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponseProductAll> restoreAllProducts() {
        return telemetryHelper.traceAndMetric("product.restoreAllProducts", () -> productCommandService.restoreAllProduct(com.google.protobuf.Empty.getDefaultInstance())
                .map(ProductDto.ApiResponseProductAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all products: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponseProductAll> deleteAllProductsPermanent() {
        return telemetryHelper.traceAndMetric("product.deleteAllProductsPermanent", () -> productCommandService.deleteAllProductPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(ProductDto.ApiResponseProductAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all products: " + throwable.getMessage(), throwable)));
    }
}
