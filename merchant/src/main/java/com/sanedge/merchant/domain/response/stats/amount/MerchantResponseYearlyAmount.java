package com.sanedge.merchant.domain.response.stats.amount;

import com.sanedge.merchant.entity.MerchantYearlyAmount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantResponseYearlyAmount {
    private String reportYear;
    private Long totalAmount;

    public static MerchantResponseYearlyAmount from(MerchantYearlyAmount dto) {
        return MerchantResponseYearlyAmount.builder()
                .reportYear(dto.getYear())
                .totalAmount(dto.getTotalAmount().longValue())
                .build();
    }
}