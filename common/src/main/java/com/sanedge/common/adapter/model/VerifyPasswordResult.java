package com.sanedge.common.adapter.model;

/**
 * Result of a verify-password call against the user service.
 */
public record VerifyPasswordResult(
        boolean valid,
        User user,
        String message) {
}
