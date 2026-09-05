package com.sanedge.merchant.domain.requests.statsbyid;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.ws.rs.QueryParam;
import lombok.Data;

@Data
public class MonthYearTotalAmountMerchant {
    @QueryParam("merchantId")
    @Min(value = 1, message = "merchant_id minimal 1")
    private Long merchantId;

    @QueryParam("year")
    @Min(2000)
    @Max(2100)
    private Long year;
}