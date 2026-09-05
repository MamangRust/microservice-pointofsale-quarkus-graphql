package com.sanedge.merchant.domain.requests.transactions;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.Data;

@Data
public class FindAllMerchantTransactionsByApiKey {
    @QueryParam("apiKey")
    @NotBlank(message = "api_key wajib diisi")
    private String apiKey;

    @QueryParam("page")
    @DefaultValue("1")
    @Parameter(description = "Halaman data", example = "1")
    @Min(value = 1)
    private Integer page = 1;

    @QueryParam("pageSize")
    @DefaultValue("10")
    @Parameter(description = "Jumlah data per halaman", example = "10")
    @Min(value = 1)
    private Integer pageSize = 10;

    @QueryParam("search")
    @DefaultValue("")
    @Parameter(description = "Pencarian transaksi")
    private String search = "";
}