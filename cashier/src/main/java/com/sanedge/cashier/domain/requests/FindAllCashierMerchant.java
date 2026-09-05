package com.sanedge.cashier.domain.requests;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "FindAllCashierMerchant", description = "Request untuk mencari cashier per merchant")
public class FindAllCashierMerchant {
    @NotNull(message = "merchant_id is required")
    @Parameter(description = "ID merchant", example = "123")
    private Integer merchantId;

    @NotBlank(message = "search is required")
    @Parameter(description = "Kata kunci pencarian", example = "Cashier A")
    private String search;

    @Min(1)
    @Parameter(description = "Halaman data", example = "1")
    private Integer page;

    @Min(1)
    @Parameter(description = "Jumlah data per halaman", example = "10")
    private Integer pageSize;
}
