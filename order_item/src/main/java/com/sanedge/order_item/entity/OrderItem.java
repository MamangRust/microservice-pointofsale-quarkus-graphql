package com.sanedge.order_item.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "order_items", schema = "pos_order")
@AttributeOverride(name = "id", column = @Column(name = "order_item_id"))
public class OrderItem extends BaseModel {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer price;

    public Long getOrderItemId() {
        return this.id;
    }

    public void setOrderItemId(Long orderItemId) {
        this.id = orderItemId;
    }
}
