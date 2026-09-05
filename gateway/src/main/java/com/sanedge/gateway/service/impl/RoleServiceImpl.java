package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.RoleDto;
import com.sanedge.gateway.service.RoleService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RoleServiceImpl implements RoleService {

    private static final Logger LOG = Logger.getLogger(RoleServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("role")
    pb.role.MutinyRoleServiceGrpc.MutinyRoleServiceStub roleQueryService;

    @GrpcClient("role")
    pb.role.MutinyRoleCommandServiceGrpc.MutinyRoleCommandServiceStub roleCommandService;

    @Override
    public Uni<RoleDto.ApiResponsePaginationRole> listRoles(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("role.listRoles", () -> roleQueryService.findAllRole(pb.role.Role.FindAllRoleRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(RoleDto.ApiResponsePaginationRole::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list roles: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseRole> getRole(int id) {
        return telemetryHelper.traceAndMetric("role.getRole", () -> roleQueryService.findByIdRole(pb.role.Role.FindByIdRoleRequest.newBuilder()
                .setRoleId(id)
                .build())
                .map(RoleDto.ApiResponseRole::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get role " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseRole> getRoleByName(String name) {
        return telemetryHelper.traceAndMetric("role.getRoleByName", () -> roleQueryService.findByNameRole(pb.role.RoleQuery.FindByNameRoleRequest.newBuilder()
                .setName(name)
                .build())
                .map(RoleDto.ApiResponseRole::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get role by name " + name + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponsesRole> getRolesByUserId(int userId) {
        return telemetryHelper.traceAndMetric("role.getRolesByUserId", () -> roleQueryService.findByUserId(pb.role.Role.FindByIdUserRoleRequest.newBuilder()
                .setUserId(userId)
                .build())
                .map(RoleDto.ApiResponsesRole::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get roles for user " + userId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponsePaginationRoleDeleteAt> activeRoles(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("role.activeRoles", () -> roleQueryService.findByActive(pb.role.Role.FindAllRoleRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(RoleDto.ApiResponsePaginationRoleDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list active roles: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponsePaginationRoleDeleteAt> trashedRoles(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("role.trashedRoles", () -> roleQueryService.findByTrashed(pb.role.Role.FindAllRoleRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(RoleDto.ApiResponsePaginationRoleDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list trashed roles: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseRole> createRole(RoleDto.CreateRoleRequest body) {
        return telemetryHelper.traceAndMetric("role.createRole", () -> roleCommandService.createRole(pb.role.RoleCommand.CreateRoleRequest.newBuilder()
                .setName(body.name())
                .build())
                .map(RoleDto.ApiResponseRole::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create role: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseRole> updateRole(int id, RoleDto.UpdateRoleRequest body) {
        return telemetryHelper.traceAndMetric("role.updateRole", () -> roleCommandService.updateRole(pb.role.RoleCommand.UpdateRoleRequest.newBuilder()
                .setId(id)
                .setName(body.name())
                .build())
                .map(RoleDto.ApiResponseRole::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update role: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseRoleDeleteAt> deleteRole(int id) {
        return telemetryHelper.traceAndMetric("role.deleteRole", () -> roleCommandService.trashedRole(pb.role.Role.FindByIdRoleRequest.newBuilder()
                .setRoleId(id)
                .build())
                .map(RoleDto.ApiResponseRoleDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete role: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseRoleDeleteAt> restoreRole(int id) {
        return telemetryHelper.traceAndMetric("role.restoreRole", () -> roleCommandService.restoreRole(pb.role.Role.FindByIdRoleRequest.newBuilder()
                .setRoleId(id)
                .build())
                .map(RoleDto.ApiResponseRoleDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore role " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseRoleDelete> deleteRolePermanent(int id) {
        return telemetryHelper.traceAndMetric("role.deleteRolePermanent", () -> roleCommandService.deleteRolePermanent(pb.role.Role.FindByIdRoleRequest.newBuilder()
                .setRoleId(id)
                .build())
                .map(RoleDto.ApiResponseRoleDelete::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete role " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseRoleAll> restoreAllRoles() {
        return telemetryHelper.traceAndMetric("role.restoreAllRoles", () -> roleCommandService.restoreAllRole(com.google.protobuf.Empty.getDefaultInstance())
                .map(RoleDto.ApiResponseRoleAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all roles: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseRoleAll> deleteAllRolesPermanent() {
        return telemetryHelper.traceAndMetric("role.deleteAllRolesPermanent", () -> roleCommandService.deleteAllRolePermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(RoleDto.ApiResponseRoleAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all roles: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RoleDto.ApiResponseUserRole> assignRoleToUser(RoleDto.AssignRoleToUserRequest body) {
        return telemetryHelper.traceAndMetric("role.assignRoleToUser", () -> roleCommandService.assignRoleToUser(pb.role.RoleCommand.AssignRoleToUserRequest.newBuilder()
                .setUserId(body.userId())
                .setRoleId(body.roleId())
                .build())
                .map(RoleDto.ApiResponseUserRole::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to assign role to user: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<Boolean> removeRoleFromUser(RoleDto.AssignRoleToUserRequest body) {
        return telemetryHelper.traceAndMetric("role.removeRoleFromUser", () -> roleCommandService.removeRoleFromUser(pb.role.RoleCommand.RemoveRoleFromUserRequest.newBuilder()
                .setUserId(body.userId())
                .setRoleId(body.roleId())
                .build())
                .map(empty -> true)
                .onFailure().invoke(throwable -> LOG.error("Failed to remove role from user: " + throwable.getMessage(), throwable)));
    }
}
