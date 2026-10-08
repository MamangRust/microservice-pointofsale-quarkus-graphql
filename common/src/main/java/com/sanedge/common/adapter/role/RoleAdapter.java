package com.sanedge.common.adapter.role;

import com.sanedge.common.adapter.model.Role;
import com.sanedge.common.adapter.support.AdapterException;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.role.Role.RoleResponse;
import pb.role.RoleQuery.FindByNameRoleRequest;
import pb.role.RoleCommand.AssignRoleToUserRequest;
import pb.role.RoleCommandService;
import pb.role.RoleService;

@ApplicationScoped
public class RoleAdapter implements RoleQueryPort, RoleCommandPort {

    @GrpcClient("role")
    RoleService query;

    @GrpcClient("role")
    RoleCommandService command;

    @Override
    public Uni<Role> findByName(String name) {
        return query.findByNameRole(FindByNameRoleRequest.newBuilder().setName(name).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Role not found: " + name);
                    }
                    return toRole(resp.getData());
                });
    }

    @Override
    public Uni<Void> assignRoleToUser(int userId, int roleId) {
        return command.assignRoleToUser(AssignRoleToUserRequest.newBuilder()
                .setUserId(userId)
                .setRoleId(roleId)
                .build())
                .onItem().transform(resp -> {
                    if (resp == null || !"success".equalsIgnoreCase(resp.getStatus())) {
                        throw new AdapterException("Failed to assign role " + roleId + " to user " + userId);
                    }
                    return resp;
                })
                .replaceWithVoid();
    }

    private static Role toRole(RoleResponse r) {
        if (r == null) {
            return null;
        }
        return new Role(r.getId(), r.getName(), ProtoTime.parse(r.getCreatedAt()), ProtoTime.parse(r.getUpdatedAt()));
    }
}
