package com.sanedge.common.adapter.role;

import com.sanedge.common.adapter.model.Role;

import io.smallrye.mutiny.Uni;

/**
 * Port for the role query service, replacing direct
 * {@code @GrpcClient("role")} usage in consumer services.
 */
public interface RoleQueryPort {

    Uni<Role> findByName(String name);
}
