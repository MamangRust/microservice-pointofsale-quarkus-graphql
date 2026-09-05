package com.sanedge.cashier.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "YearTotalSalesCashier", description = "Total penjualan tahunan per cashier")
public class YearTotalSalesCashier {
    @NotNull
    @Schema(description = "ID cashier", example = "456")
    private Integer cashierId;

    @NotNull
    @Schema(description = "Tahun", example = "2025")
    private Integer year;
}
