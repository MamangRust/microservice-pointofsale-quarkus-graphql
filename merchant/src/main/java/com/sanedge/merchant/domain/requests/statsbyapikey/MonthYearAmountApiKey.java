package com.sanedge.merchant.domain.requests.statsbyapikey;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.QueryParam;
import lombok.Data;

@Data
public class MonthYearAmountApiKey {
    @QueryParam("apiKey")
    @NotBlank(message = "api_key wajib diisi")
    private String apiKey;

    @QueryParam("year")
    @Min(2000)
    @Max(2100)
    private Long year;
}