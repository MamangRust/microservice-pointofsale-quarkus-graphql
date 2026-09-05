package com.sanedge.transaction.domain.requests;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request untuk mengambil daftar transaksi berdasarkan merchant dengan pagination dan pencarian")
public class FindAllTransactionByMerchantRequest {

    @NotNull
    @Parameter(description = "ID merchant", example = "1")
    private Integer merchantId;

    @NotNull
    @Parameter(description = "Kata kunci pencarian transaksi", example = "order123")
    private String search;

    @Min(1)
    @Parameter(description = "Nomor halaman", example = "1")
    private Integer page = 1;

    @Min(1)
    @Parameter(description = "Jumlah data per halaman", example = "10")
    private Integer pageSize = 10;
}
