package com.sanedge.transaction.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request untuk mengambil jumlah transaksi bulanan berdasarkan merchant")
public class MonthAmountTransactionMerchant {

    @NotNull
    @Schema(description = "ID merchant", example = "123")
    private Integer merchantId;

    @NotNull
    @Schema(description = "Tahun transaksi", example = "2025")
    private Integer year;

    @NotNull
    @Schema(description = "Bulan transaksi", example = "9")
    private Integer month;
}
