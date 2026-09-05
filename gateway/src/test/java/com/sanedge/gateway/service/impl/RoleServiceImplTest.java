package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.RoleDto;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.role.MutinyRoleServiceGrpc.MutinyRoleServiceStub roleQueryService;

    @Mock
    pb.role.MutinyRoleCommandServiceGrpc.MutinyRoleCommandServiceStub roleCommandService;

    RoleServiceImpl roleService;

    @BeforeEach
    void setUp() throws Exception {
        roleService = new RoleServiceImpl();

        setField(roleService, "telemetryHelper", telemetryHelper);
        setField(roleService, "roleQueryService", roleQueryService);
        setField(roleService, "roleCommandService", roleCommandService);

        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(java.util.function.Supplier.class)))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<Uni<?>> supplier = inv.getArgument(1);
                    return supplier.get();
                });
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void listRoles_returnsSuccess() {
        pb.role.Role.RoleResponse roleProto = pb.role.Role.RoleResponse.newBuilder()
                .setId(1)
                .setName("Admin")
                .build();

        pb.role.RoleQuery.ApiResponsePaginationRole responseProto = pb.role.RoleQuery.ApiResponsePaginationRole.newBuilder()
                .addData(roleProto)
                .setStatus("success")
                .setMessage("Roles found")
                .build();

        when(roleQueryService.findAllRole(any(pb.role.Role.FindAllRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RoleDto.ApiResponsePaginationRole result =
                roleService.listRoles(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).name()).isEqualTo("Admin");
    }

    @Test
    void getRole_returnsSuccess() {
        pb.role.Role.RoleResponse roleProto = pb.role.Role.RoleResponse.newBuilder()
                .setId(1)
                .setName("User")
                .build();

        pb.role.Role.ApiResponseRole responseProto = pb.role.Role.ApiResponseRole.newBuilder()
                .setData(roleProto)
                .setStatus("success")
                .setMessage("Role found")
                .build();

        when(roleQueryService.findByIdRole(any(pb.role.Role.FindByIdRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RoleDto.ApiResponseRole result = roleService.getRole(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().name()).isEqualTo("User");
    }

    @Test
    void getRoleByName_returnsSuccess() {
        pb.role.Role.RoleResponse roleProto = pb.role.Role.RoleResponse.newBuilder()
                .setId(2)
                .setName("Moderator")
                .build();

        pb.role.Role.ApiResponseRole responseProto = pb.role.Role.ApiResponseRole.newBuilder()
                .setData(roleProto)
                .setStatus("success")
                .setMessage("Role by name")
                .build();

        when(roleQueryService.findByNameRole(any(pb.role.RoleQuery.FindByNameRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RoleDto.ApiResponseRole result = roleService.getRoleByName("Moderator").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().name()).isEqualTo("Moderator");
    }

    @Test
    void getRolesByUserId_returnsSuccess() {
        pb.role.Role.RoleResponse roleProto = pb.role.Role.RoleResponse.newBuilder()
                .setId(1)
                .setName("Admin")
                .build();

        pb.role.Role.ApiResponsesRole responseProto = pb.role.Role.ApiResponsesRole.newBuilder()
                .addData(roleProto)
                .setStatus("success")
                .setMessage("Roles for user")
                .build();

        when(roleQueryService.findByUserId(any(pb.role.Role.FindByIdUserRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RoleDto.ApiResponsesRole result = roleService.getRolesByUserId(10).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void activeRoles_returnsSuccess() {
        pb.role.Role.RoleResponseDeleteAt roleProto = pb.role.Role.RoleResponseDeleteAt.newBuilder()
                .setId(1)
                .setName("Active Role")
                .build();

        pb.role.RoleQuery.ApiResponsePaginationRoleDeleteAt responseProto =
                pb.role.RoleQuery.ApiResponsePaginationRoleDeleteAt.newBuilder()
                        .addData(roleProto)
                        .setStatus("success")
                        .setMessage("Active roles")
                        .build();

        when(roleQueryService.findByActive(any(pb.role.Role.FindAllRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RoleDto.ApiResponsePaginationRoleDeleteAt result =
                roleService.activeRoles(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void trashedRoles_returnsSuccess() {
        pb.role.Role.RoleResponseDeleteAt roleProto = pb.role.Role.RoleResponseDeleteAt.newBuilder()
                .setId(2)
                .build();

        pb.role.RoleQuery.ApiResponsePaginationRoleDeleteAt responseProto =
                pb.role.RoleQuery.ApiResponsePaginationRoleDeleteAt.newBuilder()
                        .addData(roleProto)
                        .setStatus("success")
                        .setMessage("Trashed roles")
                        .build();

        when(roleQueryService.findByTrashed(any(pb.role.Role.FindAllRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RoleDto.ApiResponsePaginationRoleDeleteAt result =
                roleService.trashedRoles(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createRole_returnsSuccess() {
        pb.role.Role.RoleResponse roleProto = pb.role.Role.RoleResponse.newBuilder()
                .setId(3)
                .setName("New Role")
                .build();

        pb.role.Role.ApiResponseRole responseProto = pb.role.Role.ApiResponseRole.newBuilder()
                .setData(roleProto)
                .setStatus("success")
                .setMessage("Role created")
                .build();

        when(roleCommandService.createRole(any(pb.role.RoleCommand.CreateRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RoleDto.CreateRoleRequest request = new RoleDto.CreateRoleRequest("New Role");
        RoleDto.ApiResponseRole result = roleService.createRole(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().name()).isEqualTo("New Role");
    }

    @Test
    void updateRole_returnsSuccess() {
        pb.role.Role.RoleResponse roleProto = pb.role.Role.RoleResponse.newBuilder()
                .setId(1)
                .setName("Updated Role")
                .build();

        pb.role.Role.ApiResponseRole responseProto = pb.role.Role.ApiResponseRole.newBuilder()
                .setData(roleProto)
                .setStatus("success")
                .setMessage("Role updated")
                .build();

        when(roleCommandService.updateRole(any(pb.role.RoleCommand.UpdateRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RoleDto.UpdateRoleRequest request = new RoleDto.UpdateRoleRequest("Updated Role");
        RoleDto.ApiResponseRole result = roleService.updateRole(1, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().name()).isEqualTo("Updated Role");
    }

    @Test
    void deleteRole_returnsSuccess() {
        pb.role.Role.RoleResponseDeleteAt roleProto = pb.role.Role.RoleResponseDeleteAt.newBuilder()
                .setId(1)
                .setDeletedAt(com.google.protobuf.StringValue.of("2024-07-01T00:00:00Z"))
                .build();

        pb.role.Role.ApiResponseRoleDeleteAt responseProto = pb.role.Role.ApiResponseRoleDeleteAt.newBuilder()
                .setData(roleProto)
                .setStatus("success")
                .setMessage("Role trashed")
                .build();

        when(roleCommandService.trashedRole(any(pb.role.Role.FindByIdRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RoleDto.ApiResponseRoleDeleteAt result = roleService.deleteRole(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Role trashed");
    }

    @Test
    void restoreRole_returnsSuccess() {
        pb.role.Role.RoleResponseDeleteAt roleProto = pb.role.Role.RoleResponseDeleteAt.newBuilder()
                .setId(1)
                .build();

        pb.role.Role.ApiResponseRoleDeleteAt responseProto = pb.role.Role.ApiResponseRoleDeleteAt.newBuilder()
                .setData(roleProto)
                .setStatus("success")
                .setMessage("Role restored")
                .build();

        when(roleCommandService.restoreRole(any(pb.role.Role.FindByIdRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RoleDto.ApiResponseRoleDeleteAt result = roleService.restoreRole(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void deleteRolePermanent_returnsSuccess() {
        pb.role.RoleCommand.ApiResponseRoleDelete responseProto = pb.role.RoleCommand.ApiResponseRoleDelete.newBuilder()
                .setStatus("success")
                .setMessage("Permanently deleted")
                .build();

        when(roleCommandService.deleteRolePermanent(any(pb.role.Role.FindByIdRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RoleDto.ApiResponseRoleDelete result = roleService.deleteRolePermanent(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void restoreAllRoles_returnsSuccess() {
        pb.role.RoleCommand.ApiResponseRoleAll responseProto = pb.role.RoleCommand.ApiResponseRoleAll.newBuilder()
                .setStatus("success")
                .setMessage("All roles restored")
                .build();

        when(roleCommandService.restoreAllRole(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RoleDto.ApiResponseRoleAll result = roleService.restoreAllRoles().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void deleteAllRolesPermanent_returnsSuccess() {
        pb.role.RoleCommand.ApiResponseRoleAll responseProto = pb.role.RoleCommand.ApiResponseRoleAll.newBuilder()
                .setStatus("success")
                .setMessage("All roles permanently deleted")
                .build();

        when(roleCommandService.deleteAllRolePermanent(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RoleDto.ApiResponseRoleAll result = roleService.deleteAllRolesPermanent().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void assignRoleToUser_returnsSuccess() {
        pb.role.RoleCommand.ApiResponseUserRole responseProto = pb.role.RoleCommand.ApiResponseUserRole.newBuilder()
                .setStatus("success")
                .setMessage("Role assigned to user")
                .build();

        when(roleCommandService.assignRoleToUser(any(pb.role.RoleCommand.AssignRoleToUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RoleDto.AssignRoleToUserRequest request = new RoleDto.AssignRoleToUserRequest(10, 2);
        RoleDto.ApiResponseUserRole result = roleService.assignRoleToUser(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void removeRoleFromUser_returnsTrue() {
        when(roleCommandService.removeRoleFromUser(any(pb.role.RoleCommand.RemoveRoleFromUserRequest.class)))
                .thenReturn(Uni.createFrom().item(com.google.protobuf.Empty.getDefaultInstance()));

        RoleDto.AssignRoleToUserRequest request = new RoleDto.AssignRoleToUserRequest(10, 2);
        Boolean result = roleService.removeRoleFromUser(request).await().indefinitely();

        assertThat(result).isTrue();
    }
}
