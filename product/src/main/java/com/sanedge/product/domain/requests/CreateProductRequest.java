package com.sanedge.product.domain.requests;

import org.jboss.resteasy.reactive.RestForm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request untuk membuat produk baru")
public class CreateProductRequest {

    @NotNull
    @RestForm
    @Schema(description = "ID merchant", example = "1")
    private Integer merchantId;

    @NotNull
    @RestForm
    @Schema(description = "ID kategori produk", example = "2")
    private Integer categoryId;

    @NotNull
    @Size(min = 1)
    @RestForm
    @Schema(description = "Nama produk", example = "Kopi Arabica")
    private String name;

    @NotNull
    @RestForm
    @Schema(description = "Deskripsi produk", example = "Kopi Arabica spesial grade")
    private String description;

    @NotNull
    @Min(0)
    @RestForm
    @Schema(description = "Harga produk", example = "100000")
    private Integer price;

    @NotNull
    @Min(0)
    @RestForm
    @Schema(description = "Stok produk", example = "50")
    private Integer countInStock;

    @NotNull
    @RestForm
    @Schema(description = "Brand produk", example = "SanEdge Coffee")
    private String brand;

    @NotNull
    @Min(0)
    @RestForm
    @Schema(description = "Berat produk dalam gram", example = "500")
    private Integer weight;

    @NotNull
    @Min(0)
    @RestForm
    @Schema(description = "Rating produk", example = "5")
    private Integer rating;

    @NotNull
    @RestForm
    @Schema(description = "Slug produk", example = "kopi-arabica")
    private String slugProduct;

    @NotNull
    @RestForm
    @Schema(description = "Gambar produk")
    private String imageProduct;
}
