package com.sanedge.common.adapter.product;

import com.sanedge.common.adapter.model.Product;
import com.sanedge.common.adapter.model.UpdateProductCmd;
import com.sanedge.common.adapter.support.AdapterException;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.product.Product.FindByIdProductRequest;
import pb.product.Product.ProductResponse;
import pb.product.ProductCommand.UpdateProductRequest;
import pb.product.ProductCommandService;
import pb.product.ProductService;

@ApplicationScoped
public class ProductAdapter implements ProductQueryPort, ProductCommandPort {

    @GrpcClient("product")
    ProductService query;

    @GrpcClient("product")
    ProductCommandService command;

    @Override
    public Uni<Product> findById(int productId) {
        return query.findById(FindByIdProductRequest.newBuilder().setId(productId).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Product not found: " + productId);
                    }
                    return toProduct(resp.getData());
                });
    }

    @Override
    public Uni<Product> update(UpdateProductCmd cmd) {
        return command.update(UpdateProductRequest.newBuilder()
                .setProductId(cmd.productId())
                .setMerchantId(cmd.merchantId())
                .setCategoryId(cmd.categoryId())
                .setName(cmd.name())
                .setDescription(cmd.description())
                .setPrice(cmd.price())
                .setCountInStock(cmd.countInStock())
                .setBrand(cmd.brand())
                .setWeight(cmd.weight())
                .setImageProduct(cmd.imageProduct())
                .build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()
                            || !"success".equalsIgnoreCase(resp.getStatus())) {
                        throw new AdapterException("Failed to update product: "
                                + (resp == null ? "no response" : resp.getMessage()));
                    }
                    return toProduct(resp.getData());
                });
    }

    private static Product toProduct(ProductResponse p) {
        if (p == null) {
            return null;
        }
        return new Product(p.getId(), p.getMerchantId(), p.getCategoryId(), p.getName(), p.getDescription(),
                p.getPrice(), p.getCountInStock(), p.getBrand(), p.getWeight(), 0.0f, p.getSlugProduct(),
                p.getImageProduct(), p.getBarcode(), ProtoTime.parse(p.getCreatedAt()),
                ProtoTime.parse(p.getUpdatedAt()));
    }
}
