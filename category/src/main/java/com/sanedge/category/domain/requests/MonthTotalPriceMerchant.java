package com.sanedge.category.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "MonthTotalPriceMerchant", description = "Total harga bulanan per merchant")
public class MonthTotalPriceMerchant {
    @NotNull
    @Schema(description = "ID merchant", example = "2001")
    private Integer merchantId;

    @NotNull
    @Schema(description = "Tahun", example = "2025")
    private Integer year;

    @NotNull
    @Schema(description = "Bulan", example = "7")
    private Integer month;
}
