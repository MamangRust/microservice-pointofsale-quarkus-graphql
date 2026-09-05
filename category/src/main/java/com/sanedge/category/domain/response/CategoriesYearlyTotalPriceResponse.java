package com.sanedge.category.domain.response;

import java.util.List;

import com.sanedge.category.entity.CategoryYearTotalPrice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriesYearlyTotalPriceResponse {
    private String year;
    private Long totalRevenue;

    public static CategoriesYearlyTotalPriceResponse from(CategoryYearTotalPrice response) {
        return CategoriesYearlyTotalPriceResponse.builder()
                .year(response.getYear())
                .totalRevenue((long) response.getTotalRevenue())
                .build();
    }

    public static List<CategoriesYearlyTotalPriceResponse> fromList(List<CategoryYearTotalPrice> responses) {
        if (responses == null)
            return List.of();
        return responses.stream().map(CategoriesYearlyTotalPriceResponse::from).toList();
    }
}
