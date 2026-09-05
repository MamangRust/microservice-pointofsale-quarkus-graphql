package com.sanedge.cashier.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "MonthTotalSalesCashier", description = "Total penjualan bulanan per cashier")
public class MonthTotalSalesCashier {
    @NotNull
    @Schema(description = "ID cashier", example = "456")
    private Integer cashierId;

    @NotNull
    @Schema(description = "Tahun", example = "2025")
    private Integer year;

    @NotNull
    @Schema(description = "Bulan", example = "8")
    private Integer month;
}
