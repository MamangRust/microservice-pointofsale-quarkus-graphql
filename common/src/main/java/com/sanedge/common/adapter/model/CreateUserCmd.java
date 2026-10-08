package com.sanedge.common.adapter.model;

/**
 * Command to create a user in the user service.
 */
public record CreateUserCmd(
        String firstname,
        String lastname,
        String email,
        String password,
        String confirmPassword) {
}
