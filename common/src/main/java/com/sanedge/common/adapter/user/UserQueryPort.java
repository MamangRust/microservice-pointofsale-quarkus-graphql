package com.sanedge.common.adapter.user;

import com.sanedge.common.adapter.model.User;
import com.sanedge.common.adapter.model.VerifyPasswordResult;

import io.smallrye.mutiny.Uni;

/**
 * Port for the user query service, replacing direct
 * {@code @GrpcClient("user")} usage in consumer services.
 */
public interface UserQueryPort {

    Uni<User> findById(int id);

    /**
     * Returns the matching user, or {@code null} when none exists. Callers decide
     * whether an absent user is a conflict or a not-found condition.
     */
    Uni<User> findByEmail(String email);

    Uni<VerifyPasswordResult> verifyPassword(String email, String password);
}
