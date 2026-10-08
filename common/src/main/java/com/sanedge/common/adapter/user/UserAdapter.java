package com.sanedge.common.adapter.user;

import com.sanedge.common.adapter.model.CreateUserCmd;
import com.sanedge.common.adapter.model.UpdateUserCmd;
import com.sanedge.common.adapter.model.User;
import com.sanedge.common.adapter.model.VerifyPasswordResult;
import com.sanedge.common.adapter.support.AdapterException;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.user.User.FindAllUserRequest;
import pb.user.User.FindByIdUserRequest;
import pb.user.User.UserResponse;
import pb.user.UserCommand.CreateUserRequest;
import pb.user.UserCommand.UpdateUserRequest;
import pb.user.UserCommand.VerifyPasswordRequest;
import pb.user.UserCommand.VerifyPasswordResponse;
import pb.user.UserCommandService;
import pb.user.UserQueryService;

@ApplicationScoped
public class UserAdapter implements UserQueryPort, UserCommandPort {

    @GrpcClient("user")
    UserQueryService query;

    @GrpcClient("user")
    UserCommandService command;

    @Override
    public Uni<User> findById(int id) {
        return query.findById(FindByIdUserRequest.newBuilder().setId(id).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("User not found: " + id);
                    }
                    return toUser(resp.getData());
                });
    }

    @Override
    public Uni<User> findByEmail(String email) {
        return query.findAll(FindAllUserRequest.newBuilder().setSearch(email).setPage(1).setPageSize(1).build())
                .map(resp -> {
                    if (resp == null) {
                        return null;
                    }
                    for (UserResponse u : resp.getDataList()) {
                        if (u != null && email.equalsIgnoreCase(u.getEmail())) {
                            return toUser(u);
                        }
                    }
                    return null;
                });
    }

    @Override
    public Uni<VerifyPasswordResult> verifyPassword(String email, String password) {
        return command.verifyPassword(VerifyPasswordRequest.newBuilder().setEmail(email).setPassword(password).build())
                .map(resp -> {
                    if (resp == null) {
                        throw new AdapterException("verifyPassword returned no response");
                    }
                    return toVerifyPasswordResult(resp);
                });
    }

    @Override
    public Uni<User> create(CreateUserCmd cmd) {
        return command.create(CreateUserRequest.newBuilder()
                .setFirstname(cmd.firstname())
                .setLastname(cmd.lastname())
                .setEmail(cmd.email())
                .setPassword(cmd.password())
                .setConfirmPassword(cmd.confirmPassword())
                .build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()
                            || !"success".equalsIgnoreCase(resp.getStatus())) {
                        throw new AdapterException("Failed to create user: "
                                + (resp == null ? "no response" : resp.getMessage()));
                    }
                    return toUser(resp.getData());
                });
    }

    @Override
    public Uni<User> update(UpdateUserCmd cmd) {
        return command.update(UpdateUserRequest.newBuilder()
                .setId(cmd.id())
                .setFirstname(cmd.firstname())
                .setLastname(cmd.lastname())
                .setEmail(cmd.email())
                .setPassword(cmd.password())
                .setConfirmPassword(cmd.confirmPassword())
                .build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()
                            || !"success".equalsIgnoreCase(resp.getStatus())) {
                        throw new AdapterException("Failed to update user: "
                                + (resp == null ? "no response" : resp.getMessage()));
                    }
                    return toUser(resp.getData());
                });
    }

    private static User toUser(UserResponse u) {
        if (u == null) {
            return null;
        }
        return new User(u.getId(), u.getFirstname(), u.getLastname(), u.getEmail(),
                ProtoTime.parse(u.getCreatedAt()), ProtoTime.parse(u.getUpdatedAt()));
    }

    private static VerifyPasswordResult toVerifyPasswordResult(VerifyPasswordResponse resp) {
        return new VerifyPasswordResult(resp.getValid(), toUser(resp.getUser()), null);
    }
}
