package com.sanedge.cashier.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "MonthCashierIdRequest", description = "Request untuk laporan bulanan kasir berdasarkan cashier_id dan tahun")
public class MonthCashierIdRequest {

    @NotNull(message = "cashier_id is required")
    @Schema(description = "ID kasir", example = "101")
    private Integer cashierId;

    @NotNull(message = "year is required")
    @Schema(description = "Tahun laporan", example = "2025")
    private Integer year;
}
