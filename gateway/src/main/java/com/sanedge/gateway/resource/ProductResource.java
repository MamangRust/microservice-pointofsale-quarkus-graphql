package com.sanedge.gateway.resource;

import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.sanedge.gateway.dto.ProductDto;
import com.sanedge.gateway.service.ProductService;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

@GraphQLApi
public class ProductResource {

    @Inject
    ProductService productService;

    @Mutation("uploadImage")
    @Description("Upload a product image using base64 format")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
    public Uni<ProductDto.UploadImageResponse> uploadImage(
            @Name("base64Data") String base64Data,
            @Name("fileName") String fileName) {
        return productService.uploadImage(base64Data, fileName);
    }

    @Query("products")
    @Description("List all products")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    public Uni<ProductDto.ApiResponsePaginationProduct> findAll(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return productService.findAll(page, size, search);
    }

    @Query("activeProducts")
    @Description("List active products")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    public Uni<ProductDto.ApiResponsePaginationProductDeleteAt> findByActive(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return productService.findByActive(page, size, search);
    }

    @Query("trashedProducts")
    @Description("List trashed products")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    public Uni<ProductDto.ApiResponsePaginationProductDeleteAt> findByTrashed(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return productService.findByTrashed(page, size, search);
    }

    @Query("product")
    @Description("Get product by ID")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    public Uni<ProductDto.ApiResponseProduct> findById(@Name("id") int id) {
        return productService.findById(id);
    }

    @Query("productsByMerchant")
    @Description("List products by merchant")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    public Uni<ProductDto.ApiResponsePaginationProduct> findByMerchant(
            @Name("merchantId") int merchantId,
            @Name("search") String search,
            @Name("categoryId") int categoryId,
            @Name("minPrice") @DefaultValue("0") int minPrice,
            @Name("maxPrice") @DefaultValue("0") int maxPrice,
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size) {
        return productService.findByMerchant(merchantId, search, categoryId, minPrice, maxPrice, page, size);
    }

    @Query("productsByCategory")
    @Description("List products by category")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    public Uni<ProductDto.ApiResponsePaginationProduct> findByCategory(
            @Name("categoryName") String categoryName,
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search,
            @Name("minPrice") @DefaultValue("0") int minPrice,
            @Name("maxPrice") @DefaultValue("0") int maxPrice) {
        return productService.findByCategory(categoryName, page, size, search, minPrice, maxPrice);
    }

    @Mutation("createProduct")
    @Description("Create a new product")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
    public Uni<ProductDto.ApiResponseProduct> createProduct(@Name("body") ProductDto.CreateProductRequest body) {
        return productService.createProduct(body);
    }

    @Mutation("updateProduct")
    @Description("Update product")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
    public Uni<ProductDto.ApiResponseProduct> updateProduct(@Name("id") int id,
            @Name("body") ProductDto.UpdateProductRequest body) {
        return productService.updateProduct(id, body);
    }

    @Mutation("deleteProduct")
    @Description("Soft-delete a product")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
    public Uni<ProductDto.ApiResponseProductDeleteAt> deleteProduct(@Name("id") int id) {
        return productService.deleteProduct(id);
    }

    @Mutation("restoreProduct")
    @Description("Restore a soft-deleted product")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
    public Uni<ProductDto.ApiResponseProductDeleteAt> restoreProduct(@Name("id") int id) {
        return productService.restoreProduct(id);
    }

    @Mutation("deleteProductPermanent")
    @Description("Permanently delete a product")
    @RolesAllowed({ "ROLE_ADMIN" })
    public Uni<ProductDto.ApiResponseProductDelete> deleteProductPermanent(@Name("id") int id) {
        return productService.deleteProductPermanent(id);
    }

    @Mutation("restoreAllProducts")
    @Description("Restore all soft-deleted products")
    @RolesAllowed({ "ROLE_ADMIN" })
    public Uni<ProductDto.ApiResponseProductAll> restoreAllProducts() {
        return productService.restoreAllProducts();
    }

    @Mutation("deleteAllProductsPermanent")
    @Description("Permanently delete all soft-deleted products")
    @RolesAllowed({ "ROLE_ADMIN" })
    public Uni<ProductDto.ApiResponseProductAll> deleteAllProductsPermanent() {
        return productService.deleteAllProductsPermanent();
    }
}
