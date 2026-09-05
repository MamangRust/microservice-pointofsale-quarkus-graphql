package com.sanedge.cashier.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "YearCashierMerchantRequest", description = "Request laporan tahunan kasir berdasarkan merchant_id dan tahun")
public class YearCashierMerchantRequest {

    @NotNull(message = "merchant_id is required")
    @Schema(description = "ID merchant", example = "5001")
    private Integer merchantId;

    @NotNull(message = "year is required")
    @Schema(description = "Tahun laporan", example = "2025")
    private Integer year;
}
