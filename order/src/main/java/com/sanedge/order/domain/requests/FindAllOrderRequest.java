package com.sanedge.order.domain.requests;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "Request untuk mencari semua order dengan pagination dan pencarian")
public class FindAllOrderRequest {

    @Parameter(description = "Kata kunci pencarian", example = "produk")
    private String search;

    @Min(1)
    @Parameter(description = "Nomor halaman (mulai dari 1)", example = "1")
    private Integer page = 1;

    @Min(1)
    @Parameter(description = "Jumlah data per halaman", example = "10")
    private Integer pageSize = 10;
}
