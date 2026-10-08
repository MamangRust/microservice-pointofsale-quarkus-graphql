package com.sanedge.common.adapter.role;

import io.smallrye.mutiny.Uni;

/**
 * Port for the role command service, replacing direct
 * {@code @GrpcClient("role")} usage in consumer services.
 */
public interface RoleCommandPort {

    Uni<Void> assignRoleToUser(int userId, int roleId);
}
