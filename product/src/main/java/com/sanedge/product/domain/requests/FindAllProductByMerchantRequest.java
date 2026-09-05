package com.sanedge.product.domain.requests;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "FindAllProductByMerchantRequest", description = "Request untuk mengambil produk by merchant dengan filter")
public class FindAllProductByMerchantRequest {

    @NotNull
    @Parameter(description = "ID merchant", example = "1")
    private Integer merchantId;

    @Parameter(description = "Kata kunci pencarian (name/description/slug)", example = "kopi")
    private String search;

    @NotNull
    @Parameter(description = "ID kategori (0/null untuk semua)", example = "0")
    private Integer categoryId;

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
