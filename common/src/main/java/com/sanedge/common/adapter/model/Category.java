package com.sanedge.common.adapter.model;

import java.time.Instant;

/**
 * Domain view of a category owned by the category service.
 */
public record Category(
        int id,
        String name,
        String description,
        String slugCategory,
        String imageCategory,
        Instant createdAt,
        Instant updatedAt) {
}
