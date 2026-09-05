package com.sanedge.product.domain.requests;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "FindAllProductByCategoryRequest", description = "Request untuk mengambil produk berdasarkan category name dengan filter")
public class FindAllProductByCategoryRequest {

    @NotNull
    @Parameter(description = "Nama kategori produk", example = "Minuman")
    private String categoryName;

    @Parameter(description = "Kata kunci pencarian (nullable)", example = "kopi")
    private String search;

    @Min(0)
    @Parameter(description = "Harga minimum (nullable)", example = "0")
    private Integer minPrice;

    @Min(0)
    @Parameter(description = "Harga maksimum (nullable)", example = "100000")
    private Integer maxPrice;

    @NotNull
    @Min(1)
    @Parameter(description = "Nomor halaman (mulai dari 1)", example = "1")
    private Integer page;

    @NotNull
    @Min(1)
    @Parameter(description = "Jumlah data per halaman", example = "10")
    private Integer pageSize;
}
