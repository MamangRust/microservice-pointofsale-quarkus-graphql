package com.sanedge.category.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryYearTotalPrice {
    private String year;
    private Long totalRevenue;
}
