package com.sanedge.cashier.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "YearTotalSalesMerchant", description = "Total penjualan tahunan per merchant")
public class YearTotalSalesMerchant {
    @NotNull
    @Schema(description = "ID merchant", example = "123")
    private Integer merchantId;

    @NotNull
    @Schema(description = "Tahun", example = "2025")
    private Integer year;
}
