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

import com.sanedge.gateway.dto.UserDto;
import com.sanedge.gateway.service.UserService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class UserResourceTest {

    @Mock UserService userService;
    UserResource resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new UserResource();
        Field f = UserResource.class.getDeclaredField("userService");
        f.setAccessible(true);
        f.set(resource, userService);
    }

    @Test void listUsers_ok() {
        when(userService.listUsers(anyInt(), anyInt(), anyString()))
            .thenReturn(Uni.createFrom().item(new UserDto.ApiResponsePaginationUser("success", "ok", List.of(), null)));
        assertThat(resource.listUsers(1, 10, "").await().indefinitely().status()).isEqualTo("success");
    }

    @Test void getUser_ok() {
        when(userService.getUser(anyInt()))
            .thenReturn(Uni.createFrom().item(new UserDto.ApiResponseUser("success", "ok", null)));
        assertThat(resource.getUser(1).await().indefinitely().status()).isEqualTo("success");
    }

    @Test void createUser_ok() {
        when(userService.createUser(any()))
            .thenReturn(Uni.createFrom().item(new UserDto.ApiResponseUser("success", "created", null)));
        assertThat(resource.createUser(new UserDto.CreateUserRequest("J", "D", "e@m.com", "p", "p")).await().indefinitely().message()).isEqualTo("created");
    }

    @Test void deleteUser_ok() {
        when(userService.deleteUser(anyInt()))
            .thenReturn(Uni.createFrom().item(new UserDto.ApiResponseUserDeleteAt("success", "trashed", null)));
        assertThat(resource.deleteUser(1).await().indefinitely().message()).isEqualTo("trashed");
    }

    @Test void restoreUser_ok() {
        when(userService.restoreUser(anyInt()))
            .thenReturn(Uni.createFrom().item(new UserDto.ApiResponseUserDeleteAt("success", "restored", null)));
        assertThat(resource.restoreUser(1).await().indefinitely().message()).isEqualTo("restored");
    }
}
