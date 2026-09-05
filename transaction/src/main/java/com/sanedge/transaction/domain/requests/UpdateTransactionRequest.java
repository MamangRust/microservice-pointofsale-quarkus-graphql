package com.sanedge.transaction.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
@Schema(description = "Request untuk memperbarui data transaksi yang sudah ada")
public class UpdateTransactionRequest {

    @Null
    @Schema(description = "ID transaksi yang akan diperbarui", example = "1")
    private Integer transactionID;

    @NotNull
    @Schema(description = "ID order", example = "1")
    private Integer orderID;

    @NotNull
    @Schema(description = "ID merchant", example = "1")
    private Integer merchantID;

    @NotNull
    @Schema(description = "Metode pembayaran", example = "Credit Card")
    private String paymentMethod;

    @NotNull
    @Min(0)
    @Schema(description = "Jumlah pembayaran", example = "100000")
    private Integer amount;

    @Schema(description = "Status pembayaran (opsional)", example = "PAID")
    private String paymentStatus;

    @Schema(description = "Idempotency key klien (opsional)", example = "ord-100-cash-20260814")
    private String idempotencyKey;
}
