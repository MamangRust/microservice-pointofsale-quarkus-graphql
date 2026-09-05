package com.sanedge.cashier.domain.requests;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "FindAllCashiers", description = "Request untuk mencari semua cashier")
public class FindAllCashiers {
    @NotBlank(message = "search is required")
    @Parameter(description = "Kata kunci pencarian", example = "John")
    private String search;

    @Min(value = 1, message = "page harus minimal 1")
    @Parameter(description = "Halaman data (pagination)", example = "1")
    private Integer page;

    @Min(value = 1, message = "page_size minimal 1")
    @Max(value = 100, message = "page_size maksimal 100")
    @Parameter(description = "Jumlah data per halaman", example = "10")
    private Integer pageSize;
}
