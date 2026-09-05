package com.sanedge.order.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMonth {
    private String month;
    private Integer orderCount;
    private Long totalRevenue;
    private Integer totalItemsSold;
}
