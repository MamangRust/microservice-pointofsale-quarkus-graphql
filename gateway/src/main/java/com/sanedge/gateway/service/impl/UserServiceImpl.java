package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.UserDto;
import com.sanedge.gateway.service.UserService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class UserServiceImpl implements UserService {

    private static final Logger LOG = Logger.getLogger(UserServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("user")
    pb.user.MutinyUserQueryServiceGrpc.MutinyUserQueryServiceStub userQueryService;

    @GrpcClient("user")
    pb.user.MutinyUserCommandServiceGrpc.MutinyUserCommandServiceStub userCommandService;

    @Override
    public Uni<UserDto.ApiResponsePaginationUser> listUsers(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("user.listUsers", () -> userQueryService.findAll(pb.user.User.FindAllUserRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(UserDto.ApiResponsePaginationUser::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list users: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UserDto.ApiResponsePaginationUserDeleteAt> getActiveUsers(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("user.getActiveUsers", () -> userQueryService.findByActive(pb.user.User.FindAllUserRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(UserDto.ApiResponsePaginationUserDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list active users: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UserDto.ApiResponsePaginationUserDeleteAt> getTrashedUsers(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("user.getTrashedUsers", () -> userQueryService.findByTrashed(pb.user.User.FindAllUserRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(UserDto.ApiResponsePaginationUserDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list trashed users: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UserDto.ApiResponseUser> getUser(int id) {
        return telemetryHelper.traceAndMetric("user.getUser", () -> userQueryService.findById(pb.user.User.FindByIdUserRequest.newBuilder()
                .setId(id)
                .build())
                .map(UserDto.ApiResponseUser::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get user with id " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UserDto.ApiResponseUser> createUser(UserDto.CreateUserRequest body) {
        return telemetryHelper.traceAndMetric("user.createUser", () -> userCommandService.create(pb.user.UserCommand.CreateUserRequest.newBuilder()
                .setFirstname(body.firstname())
                .setLastname(body.lastname())
                .setEmail(body.email())
                .setPassword(body.password())
                .setConfirmPassword(body.confirmPassword())
                .build())
                .map(UserDto.ApiResponseUser::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create user: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UserDto.ApiResponseUser> updateUser(int id, UserDto.UpdateUserRequest body) {
        return telemetryHelper.traceAndMetric("user.updateUser", () -> userCommandService.update(pb.user.UserCommand.UpdateUserRequest.newBuilder()
                .setId(id)
                .setFirstname(body.firstname())
                .setLastname(body.lastname())
                .setEmail(body.email())
                .setPassword(body.password() == null ? "" : body.password())
                .setConfirmPassword(body.confirmPassword() == null ? "" : body.confirmPassword())
                .build())
                .map(UserDto.ApiResponseUser::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update user: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UserDto.ApiResponseUserDeleteAt> deleteUser(int id) {
        return telemetryHelper.traceAndMetric("user.deleteUser", () -> userCommandService.trashedUser(pb.user.User.FindByIdUserRequest.newBuilder()
                .setId(id)
                .build())
                .map(UserDto.ApiResponseUserDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete user: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UserDto.ApiResponseUserDeleteAt> restoreUser(int id) {
        return telemetryHelper.traceAndMetric("user.restoreUser", () -> userCommandService.restoreUser(pb.user.User.FindByIdUserRequest.newBuilder()
                .setId(id)
                .build())
                .map(UserDto.ApiResponseUserDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore user " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UserDto.ApiResponseUserDelete> deleteUserPermanent(int id) {
        return telemetryHelper.traceAndMetric("user.deleteUserPermanent", () -> userCommandService.deleteUserPermanent(pb.user.User.FindByIdUserRequest.newBuilder()
                .setId(id)
                .build())
                .map(UserDto.ApiResponseUserDelete::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete user " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UserDto.ApiResponseUserAll> restoreAllUser() {
        return telemetryHelper.traceAndMetric("user.restoreAllUser", () -> userCommandService.restoreAllUser(com.google.protobuf.Empty.getDefaultInstance())
                .map(UserDto.ApiResponseUserAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all users: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UserDto.ApiResponseUserAll> deleteAllUserPermanent() {
        return telemetryHelper.traceAndMetric("user.deleteAllUserPermanent", () -> userCommandService.deleteAllUserPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(UserDto.ApiResponseUserAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all users: " + throwable.getMessage(), throwable)));
    }
}
