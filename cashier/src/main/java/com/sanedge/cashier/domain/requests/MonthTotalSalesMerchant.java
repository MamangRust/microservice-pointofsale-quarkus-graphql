package com.sanedge.cashier.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "MonthTotalSalesMerchant", description = "Total penjualan bulanan per merchant")
public class MonthTotalSalesMerchant {
    @NotNull
    @Schema(description = "ID merchant", example = "123")
    private Integer merchantId;

    @NotNull
    @Schema(description = "Tahun", example = "2025")
    private Integer year;

    @NotNull
    @Schema(description = "Bulan", example = "9")
    private Integer month;
}
