package com.sanedge.role.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.role.domain.requests.CreateRoleRequest;
import com.sanedge.role.domain.requests.UpdateRoleRequest;
import com.sanedge.role.domain.response.RoleResponse;
import com.sanedge.role.domain.response.RoleResponseDeleteAt;
import com.sanedge.role.domain.response.UserRoleResponse;
import com.sanedge.role.entity.Role;
import com.sanedge.role.repository.RoleRepository;
import com.sanedge.role.repository.UserRoleRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class RoleCommandServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private RoleCommandServiceImpl roleCommandService;

    @BeforeEach
    void setUp() {
        roleCommandService = new RoleCommandServiceImpl(
                roleRepository,
                userRoleRepository,
                redisService,
                tracingMetrics);

        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics)
                .traceAndMeasure(
                        anyString(),
                        anyString(),
                        any(Attributes.class),
                        any());
    }

    private Role createMockRole(Long id, String roleName) {
        Role role = new Role();
        role.id = id;
        role.setRoleName(roleName);
        return role;
    }

    @Test
    void createRole_roleAlreadyExists_returnsErrorResponse() {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Admin");

        Role existingRole = createMockRole(1L, "Admin");
        when(roleRepository.findByRoleName("Admin")).thenReturn(Uni.createFrom().item(existingRole));

        ApiResponse<RoleResponse> response = roleCommandService.create(request).await().indefinitely();

        assertThat(response.status()).isEqualTo("error");
        assertThat(response.message()).contains("already exists");
    }

    @Test
    void createRole_success_createsNewRole() {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("NewRole");

        lenient().when(roleRepository.findByRoleName("NewRole")).thenReturn(Uni.createFrom().nullItem());
        lenient().when(roleRepository.persist(any(Role.class))).thenAnswer(invocation -> {
            Role roleToPersist = invocation.getArgument(0);
            roleToPersist.id = 1L;
            return Uni.createFrom().item(roleToPersist);
        });
        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<RoleResponse> response = roleCommandService.create(request).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role created successfully");
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getName()).isEqualTo("NewRole");
    }

    @Test
    void updateRole_roleNotFound_returnsErrorResponse() {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRoleId(999);
        request.setName("UpdatedRole");

        when(roleRepository.findById(anyLong())).thenReturn(Uni.createFrom().nullItem());

        ApiResponse<RoleResponse> response = roleCommandService.update(request).await().indefinitely();

        assertThat(response.status()).isEqualTo("error");
        assertThat(response.message()).contains("not found");
    }

    @Test
    void updateRole_nameAlreadyExists_returnsErrorResponse() {
        Role existingRole = createMockRole(1L, "OldRoleName");

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRoleId(1);
        request.setName("AnotherAdmin");

        Role conflictingRole = createMockRole(2L, "AnotherAdmin");

        when(roleRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(existingRole));
        lenient().when(roleRepository.findByRoleName("AnotherAdmin")).thenReturn(Uni.createFrom().item(conflictingRole));

        ApiResponse<RoleResponse> response = roleCommandService.update(request).await().indefinitely();

        assertThat(response.status()).isEqualTo("error");
        assertThat(response.message()).contains("already exists");
    }

    @Test
    void updateRole_success_updatesRoleName() {
        Role existingRole = createMockRole(1L, "OldRoleName");

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRoleId(1);
        request.setName("NewRoleName");

        when(roleRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(existingRole));
        lenient().when(roleRepository.findByRoleName("NewRoleName")).thenReturn(Uni.createFrom().nullItem());
        lenient().when(roleRepository.persist(any(Role.class))).thenReturn(Uni.createFrom().item(existingRole));
        when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<RoleResponse> response = roleCommandService.update(request).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role updated successfully");
        assertThat(response.data().getName()).isEqualTo("NewRoleName");
    }

    @Test
    void updateRole_sameName_noOp_noException() {
        Role existingRole = createMockRole(1L, "SameRoleName");

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRoleId(1);
        request.setName("SameRoleName");

        when(roleRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(existingRole));

        ApiResponse<RoleResponse> response = roleCommandService.update(request).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role updated successfully");
    }

    @Test
    void trashRole_roleNotFound_returnsErrorResponse() {
        when(roleRepository.trash(999L)).thenReturn(Uni.createFrom().nullItem());

        ApiResponse<RoleResponseDeleteAt> response = roleCommandService.trash(999L).await().indefinitely();

        assertThat(response.status()).isEqualTo("error");
        assertThat(response.message()).contains("not found");
    }

    @Test
    void trashRole_success_trashRole() {
        Role trashedRole = createMockRole(1L, "TrashedRole");
        trashedRole.setDeletedAt(new java.sql.Timestamp(System.currentTimeMillis()));

        when(roleRepository.trash(anyLong())).thenReturn(Uni.createFrom().item(trashedRole));
        when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<RoleResponseDeleteAt> response = roleCommandService.trash(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role trashed successfully");
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getName()).isEqualTo("TrashedRole");
    }

    @Test
    void restoreRole_roleNotFound_returnsErrorResponse() {
        when(roleRepository.restore(1L)).thenReturn(Uni.createFrom().nullItem());

        ApiResponse<RoleResponseDeleteAt> response = roleCommandService.restore(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("error");
        assertThat(response.message()).contains("not found");
    }

    @Test
    void restoreRole_success_restoreRole() {
        Role restoredRole = createMockRole(1L, "RestoredRole");

        when(roleRepository.restore(anyLong())).thenReturn(Uni.createFrom().item(restoredRole));
        when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<RoleResponseDeleteAt> response = roleCommandService.restore(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role restored successfully");
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getName()).isEqualTo("RestoredRole");
    }

    @Test
    void deletePermanent_roleNotFound_returnsErrorResponse() {
        when(roleRepository.findById(999L)).thenReturn(Uni.createFrom().nullItem());

        ApiResponse<Void> response = roleCommandService.deletePermanent(999L).await().indefinitely();

        assertThat(response.status()).isEqualTo("error");
    }

    @Test
    void deletePermanent_success_deleteRole() {
        Role deletedRole = createMockRole(1L, "DeletedRole");

        when(roleRepository.findById(1L)).thenReturn(Uni.createFrom().item(deletedRole));
        when(roleRepository.deletePermanent(anyLong())).thenReturn(Uni.createFrom().item(deletedRole));
        when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<Void> response = roleCommandService.deletePermanent(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role deleted permanently");
    }

    @Test
    void assignRoleToUser_success_assignsRole() {
        com.sanedge.role.entity.UserRole userRole = new com.sanedge.role.entity.UserRole();
        userRole.setUserId(1L);
        userRole.setRole(createMockRole(2L, "Role2"));

        when(userRoleRepository.assignRole(anyLong(), anyLong()))
                .thenReturn(Uni.createFrom().item(userRole));
        lenient().when(roleRepository.findById(anyLong()))
                .thenReturn(Uni.createFrom().item(createMockRole(2L, "Role2")));

        ApiResponse<UserRoleResponse> response = roleCommandService.assignRoleToUser(1L, 2L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role assigned to user successfully");
        assertThat(response.data()).isNotNull();
    }

    @Test
    void removeRoleFromUser_success_removesRole() {
        when(userRoleRepository.removeRole(anyLong(), anyLong())).thenReturn(Uni.createFrom().item(true));
        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<Void> response = roleCommandService.removeRoleFromUser(1L, 2L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role removed from user successfully");
    }

    @Test
    void deleteAllTrashedRoles_noTrashedRoles_throwsResourceNotFoundException() {
        when(roleRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));

        try {
            roleCommandService.deleteAllTrashedRoles().await().indefinitely();
            org.junit.jupiter.api.Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("No roles found in trash");
        }
    }

    @Test
    void deleteAllTrashedRoles_success_deleteAll() {
        when(roleRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));
        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<Void> response = roleCommandService.deleteAllTrashedRoles().await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).contains("deleted permanently");
    }
}
