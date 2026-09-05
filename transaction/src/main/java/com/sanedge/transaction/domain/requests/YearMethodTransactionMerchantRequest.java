package com.sanedge.transaction.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request untuk mengambil statistik transaksi tahunan berdasarkan metode pembayaran dan merchant")
public class YearMethodTransactionMerchantRequest {

    @NotNull(message = "Merchant ID wajib diisi")
    @Schema(description = "ID merchant", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer merchantId;

    @NotNull(message = "Tahun wajib diisi")
    @Min(value = 1900, message = "Tahun harus valid")
    @Schema(description = "Tahun transaksi", example = "2024", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer year;
}
