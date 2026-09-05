package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.RoleDto;
import com.sanedge.gateway.service.RoleService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class RoleResourceTest {

    @Mock RoleService roleService;
    RoleResource resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new RoleResource();
        Field f = RoleResource.class.getDeclaredField("roleService");
        f.setAccessible(true);
        f.set(resource, roleService);
    }

    @Test void listRoles_ok() {
        when(roleService.listRoles(anyInt(), anyInt(), anyString()))
            .thenReturn(Uni.createFrom().item(new RoleDto.ApiResponsePaginationRole("success", "ok", List.of(), null)));
        assertThat(resource.listRoles(1, 10, "").await().indefinitely().status()).isEqualTo("success");
    }

    @Test void getRole_ok() {
        when(roleService.getRole(anyInt()))
            .thenReturn(Uni.createFrom().item(new RoleDto.ApiResponseRole("success", "ok", null)));
        assertThat(resource.getRole(1).await().indefinitely().status()).isEqualTo("success");
    }

    @Test void createRole_ok() {
        when(roleService.createRole(any()))
            .thenReturn(Uni.createFrom().item(new RoleDto.ApiResponseRole("success", "created", null)));
        assertThat(resource.createRole(new RoleDto.CreateRoleRequest("Admin")).await().indefinitely().message()).isEqualTo("created");
    }

    @Test void deleteRole_ok() {
        when(roleService.deleteRole(anyInt()))
            .thenReturn(Uni.createFrom().item(new RoleDto.ApiResponseRoleDeleteAt("success", "trashed", null)));
        assertThat(resource.deleteRole(1).await().indefinitely().message()).isEqualTo("trashed");
    }

    @Test void restoreRole_ok() {
        when(roleService.restoreRole(anyInt()))
            .thenReturn(Uni.createFrom().item(new RoleDto.ApiResponseRoleDeleteAt("success", "restored", null)));
        assertThat(resource.restoreRole(1).await().indefinitely().message()).isEqualTo("restored");
    }
}
