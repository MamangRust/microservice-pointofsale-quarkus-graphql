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

import com.sanedge.gateway.dto.UserDto;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.user.MutinyUserQueryServiceGrpc.MutinyUserQueryServiceStub userQueryService;

    @Mock
    pb.user.MutinyUserCommandServiceGrpc.MutinyUserCommandServiceStub userCommandService;

    UserServiceImpl userService;

    @BeforeEach
    void setUp() throws Exception {
        userService = new UserServiceImpl();

        setField(userService, "telemetryHelper", telemetryHelper);
        setField(userService, "userQueryService", userQueryService);
        setField(userService, "userCommandService", userCommandService);

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
    void listUsers_returnsSuccess() {
        pb.user.User.UserResponse userProto = pb.user.User.UserResponse.newBuilder()
                .setId(1)
                .setFirstname("John")
                .setLastname("Doe")
                .setEmail("john@example.com")
                .build();

        pb.user.UserQuery.ApiResponsePaginationUser responseProto = pb.user.UserQuery.ApiResponsePaginationUser.newBuilder()
                .addData(userProto)
                .setStatus("success")
                .setMessage("Users found")
                .build();

        when(userQueryService.findAll(any(pb.user.User.FindAllUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UserDto.ApiResponsePaginationUser result = userService.listUsers(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).email()).isEqualTo("john@example.com");
    }

    @Test
    void getActiveUsers_returnsSuccess() {
        pb.user.User.UserResponseDeleteAt userProto = pb.user.User.UserResponseDeleteAt.newBuilder()
                .setId(1)
                .setFirstname("Active")
                .setLastname("User")
                .build();

        pb.user.UserQuery.ApiResponsePaginationUserDeleteAt responseProto = pb.user.UserQuery.ApiResponsePaginationUserDeleteAt.newBuilder()
                .addData(userProto)
                .setStatus("success")
                .setMessage("Active users")
                .build();

        when(userQueryService.findByActive(any(pb.user.User.FindAllUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UserDto.ApiResponsePaginationUserDeleteAt result = userService.getActiveUsers(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void getTrashedUsers_returnsSuccess() {
        pb.user.User.UserResponseDeleteAt userProto = pb.user.User.UserResponseDeleteAt.newBuilder()
                .setId(2)
                .setDeletedAt(com.google.protobuf.StringValue.of("2024-07-01T00:00:00Z"))
                .build();

        pb.user.UserQuery.ApiResponsePaginationUserDeleteAt responseProto = pb.user.UserQuery.ApiResponsePaginationUserDeleteAt.newBuilder()
                .addData(userProto)
                .setStatus("success")
                .setMessage("Trashed users")
                .build();

        when(userQueryService.findByTrashed(any(pb.user.User.FindAllUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UserDto.ApiResponsePaginationUserDeleteAt result = userService.getTrashedUsers(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getUser_returnsSuccess() {
        pb.user.User.UserResponse userProto = pb.user.User.UserResponse.newBuilder()
                .setId(1)
                .setFirstname("John")
                .setLastname("Doe")
                .setEmail("john@example.com")
                .build();

        pb.user.User.ApiResponseUser responseProto = pb.user.User.ApiResponseUser.newBuilder()
                .setData(userProto)
                .setStatus("success")
                .setMessage("User found")
                .build();

        when(userQueryService.findById(any(pb.user.User.FindByIdUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UserDto.ApiResponseUser result = userService.getUser(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().id()).isEqualTo(1);
    }

    @Test
    void createUser_returnsSuccess() {
        pb.user.User.UserResponse userProto = pb.user.User.UserResponse.newBuilder()
                .setId(1)
                .setFirstname("John")
                .setLastname("Doe")
                .setEmail("john@example.com")
                .build();

        pb.user.User.ApiResponseUser responseProto = pb.user.User.ApiResponseUser.newBuilder()
                .setData(userProto)
                .setStatus("success")
                .setMessage("User created")
                .build();

        when(userCommandService.create(any(pb.user.UserCommand.CreateUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UserDto.CreateUserRequest request = new UserDto.CreateUserRequest("John", "Doe", "john@example.com", "password", "password");
        UserDto.ApiResponseUser result = userService.createUser(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().email()).isEqualTo("john@example.com");
    }

    @Test
    void updateUser_returnsSuccess() {
        pb.user.User.UserResponse userProto = pb.user.User.UserResponse.newBuilder()
                .setId(1)
                .setFirstname("Jane")
                .setLastname("Smith")
                .setEmail("jane@example.com")
                .build();

        pb.user.User.ApiResponseUser responseProto = pb.user.User.ApiResponseUser.newBuilder()
                .setData(userProto)
                .setStatus("success")
                .setMessage("User updated")
                .build();

        when(userCommandService.update(any(pb.user.UserCommand.UpdateUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UserDto.UpdateUserRequest request = new UserDto.UpdateUserRequest("Jane", "Smith", "jane@example.com", "newpass", "newpass");
        UserDto.ApiResponseUser result = userService.updateUser(1, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().firstname()).isEqualTo("Jane");
    }

    @Test
    void deleteUser_returnsSuccess() {
        pb.user.User.UserResponseDeleteAt userProto = pb.user.User.UserResponseDeleteAt.newBuilder()
                .setId(1)
                .setFirstname("John")
                .setLastname("Doe")
                .setDeletedAt(com.google.protobuf.StringValue.of("2024-07-01T00:00:00Z"))
                .build();

        pb.user.User.ApiResponseUserDeleteAt responseProto = pb.user.User.ApiResponseUserDeleteAt.newBuilder()
                .setData(userProto)
                .setStatus("success")
                .setMessage("User trashed")
                .build();

        when(userCommandService.trashedUser(any(pb.user.User.FindByIdUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UserDto.ApiResponseUserDeleteAt result = userService.deleteUser(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("User trashed");
    }

    @Test
    void restoreUser_returnsSuccess() {
        pb.user.User.UserResponseDeleteAt userProto = pb.user.User.UserResponseDeleteAt.newBuilder()
                .setId(1)
                .build();

        pb.user.User.ApiResponseUserDeleteAt responseProto = pb.user.User.ApiResponseUserDeleteAt.newBuilder()
                .setData(userProto)
                .setStatus("success")
                .setMessage("User restored")
                .build();

        when(userCommandService.restoreUser(any(pb.user.User.FindByIdUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UserDto.ApiResponseUserDeleteAt result = userService.restoreUser(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void deleteUserPermanent_returnsSuccess() {
        pb.user.UserCommand.ApiResponseUserDelete responseProto = pb.user.UserCommand.ApiResponseUserDelete.newBuilder()
                .setStatus("success")
                .setMessage("Permanently deleted")
                .build();

        when(userCommandService.deleteUserPermanent(any(pb.user.User.FindByIdUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UserDto.ApiResponseUserDelete result = userService.deleteUserPermanent(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void restoreAllUser_returnsSuccess() {
        pb.user.UserCommand.ApiResponseUserAll responseProto = pb.user.UserCommand.ApiResponseUserAll.newBuilder()
                .setStatus("success")
                .setMessage("All users restored")
                .build();

        when(userCommandService.restoreAllUser(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UserDto.ApiResponseUserAll result = userService.restoreAllUser().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void deleteAllUserPermanent_returnsSuccess() {
        pb.user.UserCommand.ApiResponseUserAll responseProto = pb.user.UserCommand.ApiResponseUserAll.newBuilder()
                .setStatus("success")
                .setMessage("All users permanently deleted")
                .build();

        when(userCommandService.deleteAllUserPermanent(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UserDto.ApiResponseUserAll result = userService.deleteAllUserPermanent().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }
}
