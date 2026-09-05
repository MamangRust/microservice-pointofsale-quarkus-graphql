package com.sanedge.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Status {
    PENDING("pending"),
    SUCCESS("success"),
    FAILED("failed");

    private final String value;
}
