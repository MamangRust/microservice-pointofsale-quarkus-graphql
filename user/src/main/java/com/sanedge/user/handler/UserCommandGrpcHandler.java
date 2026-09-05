package com.sanedge.user.handler;

import com.sanedge.user.domain.requests.RegisterRequest;
import com.sanedge.user.service.UserCommandService;
import com.sanedge.user.service.UserQueryService;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import pb.user.MutinyUserCommandServiceGrpc;
import pb.user.User.ApiResponseUser;
import pb.user.User.ApiResponseUserDeleteAt;
import pb.user.User.FindByIdUserRequest;
import pb.user.User.UserResponse;
import pb.user.User.UserResponseDeleteAt;
import pb.user.UserCommand.ApiResponseUserAll;
import pb.user.UserCommand.ApiResponseUserDelete;
import pb.user.UserCommand.CreateUserRequest;
import pb.user.UserCommand.UpdateUserRequest;

@GrpcService
public class UserCommandGrpcHandler extends MutinyUserCommandServiceGrpc.UserCommandServiceImplBase {

    @Inject
    UserCommandService userCommandService;

    @Inject
    UserQueryService userQueryService;

    @Override
    public Uni<ApiResponseUser> create(CreateUserRequest request) {
        RegisterRequest req = new RegisterRequest();
        req.setFirstname(request.getFirstname());
        req.setLastname(request.getLastname());
        req.setEmail(request.getEmail());
        req.setPassword(request.getPassword());
        req.setConfirmPassword(request.getConfirmPassword());

        return userCommandService.createUser(req)
                .map(res -> {
                    ApiResponseUser.Builder builder = ApiResponseUser.newBuilder()
                            .setStatus(res.status())
                            .setMessage(res.message());
                    if (res.data() != null) {
                        builder.setData(mapToUserResponse(res.data()));
                    }
                    return builder.build();
                });
    }

    @Override
    public Uni<ApiResponseUser> update(UpdateUserRequest request) {
        com.sanedge.user.domain.requests.UpdateUserRequest req = com.sanedge.user.domain.requests.UpdateUserRequest
                .builder()
                .id(request.getId())
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(request.getPassword())
                .confirmPassword(request.getConfirmPassword())
                .build();

        return userCommandService.updateUser(req)
                .map(res -> {
                    ApiResponseUser.Builder builder = ApiResponseUser.newBuilder()
                            .setStatus(res.status())
                            .setMessage(res.message());
                    if (res.data() != null) {
                        builder.setData(mapToUserResponse(res.data()));
                    }
                    return builder.build();
                });
    }

    @Override
    public Uni<ApiResponseUserDeleteAt> trashedUser(FindByIdUserRequest request) {
        return userCommandService.trashed((long) request.getId())
                .map(res -> {
                    ApiResponseUserDeleteAt.Builder builder = ApiResponseUserDeleteAt.newBuilder()
                            .setStatus(res.status())
                            .setMessage(res.message());
                    if (res.data() != null) {
                        builder.setData(mapToUserResponseDeleteAt(res.data()));
                    }
                    return builder.build();
                });
    }

    @Override
    public Uni<ApiResponseUserDeleteAt> restoreUser(FindByIdUserRequest request) {
        return userCommandService.restore((long) request.getId())
                .map(res -> {
                    ApiResponseUserDeleteAt.Builder builder = ApiResponseUserDeleteAt.newBuilder()
                            .setStatus(res.status())
                            .setMessage(res.message());
                    if (res.data() != null) {
                        builder.setData(mapToUserResponseDeleteAt(res.data()));
                    }
                    return builder.build();
                });
    }

    @Override
    public Uni<ApiResponseUserDelete> deleteUserPermanent(FindByIdUserRequest request) {
        return userCommandService.deletePermanent((long) request.getId())
                .map(res -> ApiResponseUserDelete.newBuilder()
                        .setStatus(res.status())
                        .setMessage(res.message())
                        .build());
    }

    @Override
    public Uni<ApiResponseUserAll> restoreAllUser(com.google.protobuf.Empty request) {
        return userCommandService.restoreAllTrashedUsers()
                .map(res -> ApiResponseUserAll.newBuilder()
                        .setStatus(res.status())
                        .setMessage(res.message())
                        .build());
    }

    @Override
    public Uni<ApiResponseUserAll> deleteAllUserPermanent(com.google.protobuf.Empty request) {
        return userCommandService.deleteAllTrashedUsers()
                .map(res -> ApiResponseUserAll.newBuilder()
                        .setStatus(res.status())
                        .setMessage(res.message())
                        .build());
    }

    @Override
    public Uni<pb.user.UserCommand.VerifyPasswordResponse> verifyPassword(pb.user.UserCommand.VerifyPasswordRequest request) {
        return userQueryService.verifyPassword(request.getEmail(), request.getPassword())
                .map(res -> {
                    pb.user.UserCommand.VerifyPasswordResponse.Builder builder = pb.user.UserCommand.VerifyPasswordResponse.newBuilder();
                    if (res.data() != null) {
                        builder.setValid(true);
                        builder.setUser(mapToUserResponse(res.data()));
                    } else {
                        builder.setValid(false);
                    }
                    return builder.build();
                })
                .onFailure().recoverWithItem(err -> {
                    return pb.user.UserCommand.VerifyPasswordResponse.newBuilder()
                            .setValid(false)
                            .build();
                });
    }

    private UserResponse mapToUserResponse(com.sanedge.user.domain.response.UserResponse u) {
        return UserResponse.newBuilder()
                .setId(u.getId().intValue())
                .setFirstname(u.getFirstname())
                .setLastname(u.getLastname())
                .setEmail(u.getEmail())
                .setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "")
                .setUpdatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt().toString() : "")
                .build();
    }

    private UserResponseDeleteAt mapToUserResponseDeleteAt(com.sanedge.user.domain.response.UserResponseDeleteAt u) {
        UserResponseDeleteAt.Builder builder = UserResponseDeleteAt.newBuilder()
                .setId(u.getId().intValue())
                .setFirstname(u.getFirstname())
                .setLastname(u.getLastname())
                .setEmail(u.getEmail())
                .setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "")
                .setUpdatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt().toString() : "");
        if (u.getDeletedAt() != null) {
            builder.setDeletedAt(
                    com.google.protobuf.StringValue.newBuilder().setValue(u.getDeletedAt().toString()).build());
        }
        return builder.build();
    }
}
