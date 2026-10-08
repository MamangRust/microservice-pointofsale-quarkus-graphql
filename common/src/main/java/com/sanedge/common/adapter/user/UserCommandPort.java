package com.sanedge.common.adapter.user;

import com.sanedge.common.adapter.model.CreateUserCmd;
import com.sanedge.common.adapter.model.UpdateUserCmd;
import com.sanedge.common.adapter.model.User;

import io.smallrye.mutiny.Uni;

/**
 * Port for the user command service, replacing direct
 * {@code @GrpcClient("user")} usage in consumer services.
 */
public interface UserCommandPort {

    Uni<User> create(CreateUserCmd cmd);

    Uni<User> update(UpdateUserCmd cmd);
}
