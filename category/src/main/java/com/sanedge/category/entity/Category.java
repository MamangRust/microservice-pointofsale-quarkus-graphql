package com.sanedge.category.entity;

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
@Table(name = "categories", schema = "pos_catalog")
@AttributeOverride(name = "id", column = @Column(name = "category_id"))
public class Category extends BaseModel {

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Column(name = "slug_category", unique = true)
    private String slugCategory;

    public Long getCategoryId() {
        return this.id;
    }

    public void setCategoryId(Long categoryId) {
        this.id = categoryId;
    }
}
