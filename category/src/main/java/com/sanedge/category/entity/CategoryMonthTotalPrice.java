package com.sanedge.category.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryMonthTotalPrice {
    private String year;
    private String month;
    private Long totalRevenue;
}
