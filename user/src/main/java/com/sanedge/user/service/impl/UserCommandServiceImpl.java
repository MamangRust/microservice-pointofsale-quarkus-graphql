package com.sanedge.user.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.utils.PasswordUtil;
import com.sanedge.user.domain.requests.RegisterRequest;
import com.sanedge.user.domain.requests.UpdateUserRequest;
import com.sanedge.user.domain.response.UserResponse;
import com.sanedge.user.domain.response.UserResponseDeleteAt;
import com.sanedge.user.entity.Role;
import com.sanedge.user.entity.User;
import com.sanedge.user.repository.UserRepository;
import com.sanedge.user.service.UserCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

@ApplicationScoped
public class UserCommandServiceImpl implements UserCommandService {
        private static final Logger logger = LoggerFactory.getLogger(UserCommandServiceImpl.class);

        private final UserRepository userRepository;
        private final PasswordUtil passwordUtil;
        private final RedisService redisService;
        private final TracingMetrics tracingMetrics;

        @Inject
        @GrpcClient("role")
        pb.role.RoleService roleQueryService;

        @Inject
        public UserCommandServiceImpl(UserRepository userRepository,
                        PasswordUtil passwordUtil,
                        RedisService redisService,
                        TracingMetrics tracingMetrics) {
                this.userRepository = userRepository;
                this.passwordUtil = passwordUtil;
                this.redisService = redisService;
                this.tracingMetrics = tracingMetrics;
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<UserResponse>> createUser(RegisterRequest request) {
                // Derive a username from the email when none is provided (register
                // requests via the auth service don't carry a username field).
                String username = request.getUsername() == null || request.getUsername().isBlank()
                                ? request.getEmail().split("@")[0]
                                : request.getUsername();
                if (username.length() > 20) {
                        // Keep the unique tail (e.g. timestamp suffix) so derived usernames
                        // don't collide after truncation: first 12 + last 7 chars.
                        username = username.substring(0, 12) + username.substring(username.length() - 7);
                }
                final String resolvedUsername = username;

                logger.info("Creating new user with username: {}", resolvedUsername);
                Attributes attrs = Attributes.builder()
                                .put("user.username", resolvedUsername)
                                .put("user.email", request.getEmail())
                                .build();

                return runTraced("createUser", "create_user", attrs,
                                () -> {
                                        if (!request.getPassword().equals(request.getConfirmPassword())) {
                                                logger.warn("User creation failed - passwords do not match for username: {}",
                                                                resolvedUsername);
                                                throw new InvalidRequestException("Passwords do not match");
                                        }

                                        return userRepository.existsByUsername(resolvedUsername)
                                                        .chain(usernameExists -> {
                                                                if (usernameExists) {
                                                                        logger.warn("User creation failed - username already exists: {}",
                                                                                        resolvedUsername);
                                                                        throw new ResourceAlreadyExistsException(
                                                                                        "Username already exists");
                                                                }
                                                                return userRepository.existsByEmail(request.getEmail());
                                                        })
                                                        .chain(emailExists -> {
                                                                if (emailExists) {
                                                                        logger.warn("User creation failed - email already exists: {}",
                                                                                        request.getEmail());
                                                                        throw new ResourceAlreadyExistsException(
                                                                                        "Email already exists");
                                                                }

                                                                User user = new User();
                                                                user.setUsername(resolvedUsername);
                                                                user.setEmail(request.getEmail());
                                                                user.setFirstname(request.getFirstname());
                                                                user.setLastname(request.getLastname());
                                                                user.setPassword(passwordUtil
                                                                                .hashPassword(request.getPassword()));

                                                                Uni<Set<Role>> rolesUni;
                                                                if (request.getRoleNames() != null
                                                                                && !request.getRoleNames().isEmpty()) {
                                                                        List<Uni<Role>> roleUnis = request
                                                                                        .getRoleNames().stream()
                                                                                        .map(this::resolveRoleViaGrpc)
                                                                                        .collect(Collectors.toList());
                                                                        rolesUni = Uni.join().all(roleUnis)
                                                                                        .andFailFast()
                                                                                        .map(roles -> roles.stream()
                                                                                                        .filter(java.util.Objects::nonNull)
                                                                                                        .collect(Collectors
                                                                                                                        .toSet()));
                                                                } else {
                                                                        rolesUni = Uni.createFrom().item(Set.of());
                                                                }

                                                                return rolesUni.chain(rolesToAssign -> {
                                                                        if (rolesToAssign.isEmpty()) {
                                                                                return resolveRoleViaGrpc("ROLE_ADMIN")
                                                                                                .map(adminRole -> {
                                                                                                        Set<Role> roles = new java.util.HashSet<>();
                                                                                                        if (adminRole != null) {
                                                                                                                roles.add(adminRole);
                                                                                                        }
                                                                                                        return roles;
                                                                                                });
                                                                        }
                                                                        return Uni.createFrom().item(rolesToAssign);
                                                                }).chain(rolesToAssign -> {
                                                                        user.setRoles(rolesToAssign);
                                                                        return userRepository.persist(user)
                                                                                        .map(v -> {
                                                                                                UserResponse userResponse = UserResponse
                                                                                                                .from(user);
                                                                                                logger.info("Successfully created user with id: {} and username: {}",
                                                                                                                user.id,
                                                                                                                user.getUsername());
                                                                                                return ApiResponse
                                                                                                                .success("User registered successfully",
                                                                                                                                userResponse);
                                                                                        });
                                                                });
                                                        });
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<UserResponse>> updateUser(@Valid UpdateUserRequest request) {
                logger.info("Updating user with id: {}", request.getId());
                Attributes attrs = Attributes.builder()
                                .put("user.id", request.getId().toString())
                                .build();

                return runTraced("updateUser", "update_user", attrs,
                                () -> userRepository.findById(request.getId())
                                                .chain(existingUser -> {
                                                        if (existingUser == null) {
                                                                logger.warn("User update failed - user not found with id: {}",
                                                                                request.getId());
                                                                throw new ResourceNotFoundException(
                                                                                "User not found with id: "
                                                                                                + request.getId());
                                                        }

                                                        Uni<Void> checkUsernameFlow;
                                                        if (request.getUsername() != null
                                                                        && !request.getUsername().equals(
                                                                                        existingUser.getUsername())) {
                                                                checkUsernameFlow = userRepository
                                                                                .existsByUsername(request.getUsername())
                                                                                .map(exists -> {
                                                                                        if (exists) {
                                                                                                logger.warn("User update failed - username '{}' already in use",
                                                                                                                request.getUsername());
                                                                                                throw new ResourceAlreadyExistsException(
                                                                                                                "Username '" + request
                                                                                                                                .getUsername()
                                                                                                                                + "' is already in use");
                                                                                        }
                                                                                        existingUser.setUsername(request
                                                                                                        .getUsername());
                                                                                        return null;
                                                                                });
                                                        } else {
                                                                checkUsernameFlow = Uni.createFrom().nullItem();
                                                        }

                                                        return checkUsernameFlow.chain(v -> {
                                                                Uni<Void> checkEmailFlow;
                                                                if (request.getEmail() != null && !request.getEmail()
                                                                                .equals(existingUser.getEmail())) {
                                                                        checkEmailFlow = userRepository
                                                                                        .existsByEmail(request
                                                                                                        .getEmail())
                                                                                        .map(exists -> {
                                                                                                if (exists) {
                                                                                                        logger.warn("User update failed - email '{}' already in use",
                                                                                                                        request.getEmail());
                                                                                                        throw new ResourceAlreadyExistsException(
                                                                                                                        "Email '" + request
                                                                                                                                        .getEmail()
                                                                                                                                        + "' is already in use");
                                                                                                }
                                                                                                existingUser.setEmail(
                                                                                                                request.getEmail());
                                                                                                return null;
                                                                                        });
                                                                } else {
                                                                        checkEmailFlow = Uni.createFrom().nullItem();
                                                                }

                                                                return checkEmailFlow;
                                                        }).chain(v -> {
                                                                if (request.getPassword() != null) {
                                                                        if (!request.getPassword().equals(
                                                                                        request.getConfirmPassword())) {
                                                                                logger.warn("User update failed - passwords do not match for user id: {}",
                                                                                                request.getId());
                                                                                throw new InvalidRequestException(
                                                                                                "Passwords do not match");
                                                                        }
                                                                        existingUser.setPassword(passwordUtil
                                                                                        .hashPassword(request
                                                                                                        .getPassword()));
                                                                }

                                                                if (request.getFirstname() != null) {
                                                                        existingUser.setFirstname(
                                                                                        request.getFirstname());
                                                                }
                                                                if (request.getLastname() != null) {
                                                                        existingUser.setLastname(request.getLastname());
                                                                }

                                                                Uni<Void> rolesFlow;
                                                                if (request.getRoleNames() != null) {
                                                                        List<Uni<Role>> roleUnis = request
                                                                                        .getRoleNames().stream()
                                                                                        .map(this::resolveRoleViaGrpc)
                                                                                        .collect(Collectors.toList());
                                                                        rolesFlow = Uni.join().all(roleUnis)
                                                                                        .andFailFast()
                                                                                        .map(roles -> {
                                                                                                existingUser.getRoles()
                                                                                                                .clear();
                                                                                                existingUser.getRoles()
                                                                                                                .addAll(roles);
                                                                                                return null;
                                                                                        });
                                                                } else {
                                                                        rolesFlow = Uni.createFrom().nullItem();
                                                                }

                                                                return rolesFlow.chain(v3 -> {
                                                                        existingUser.setUpdatedAt(Timestamp
                                                                                        .valueOf(LocalDateTime.now()));
                                                                        return userRepository.persist(existingUser)
                                                                                        .chain(v4 -> {
                                                                                                UserResponse userResponse = UserResponse
                                                                                                                .from(existingUser);
                                                                                                String cacheKey = "user:"
                                                                                                                + request.getId();

                                                                                                return redisService
                                                                                                                .deleteReactive(cacheKey)
                                                                                                                .map(v5 -> {
                                                                                                                        logger.info("Invalidated cache for key: {}",
                                                                                                                                        cacheKey);
                                                                                                                        logger.info("Successfully updated user with id: {}",
                                                                                                                                        request.getId());
                                                                                                                        return ApiResponse
                                                                                                                                        .success("User updated successfully",
                                                                                                                                                        userResponse);
                                                                                                                });
                                                                                        });
                                                                });
                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to update user with id: {}",
                                                                        request.getId(), e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to update user: " + e.getMessage(),
                                                                        (UserResponse) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<UserResponseDeleteAt>> trashed(Long id) {
                logger.info("Trashing user with id: {}", id);
                Attributes attrs = Attributes.builder()
                                .put("user.id", id.toString())
                                .build();

                return runTraced("trashUser", "trash_user", attrs,
                                () -> userRepository.trash(id)
                                                .chain(trashedUser -> {
                                                        if (trashedUser == null) {
                                                                logger.warn("User trash failed - user not found with id: {}",
                                                                                id);
                                                                throw new ResourceNotFoundException(
                                                                                "Trashed user not found with id: "
                                                                                                + id);
                                                        }

                                                        UserResponseDeleteAt userResponseDeleteAt = UserResponseDeleteAt
                                                                        .from(trashedUser);
                                                        String cacheKey = "user:" + id;

                                                        return redisService.deleteReactive(cacheKey)
                                                                        .map(v -> {
                                                                                logger.info("Invalidated cache for key: {}",
                                                                                                cacheKey);
                                                                                logger.info("Successfully trashed user with id: {}",
                                                                                                id);
                                                                                return ApiResponse.success(
                                                                                                "User trashed successfully",
                                                                                                userResponseDeleteAt);
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to trash user with id: {}", id, e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to trash user: " + e.getMessage(),
                                                                        (UserResponseDeleteAt) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<UserResponseDeleteAt>> restore(Long id) {
                logger.info("Restoring user with id: {}", id);
                Attributes attrs = Attributes.builder()
                                .put("user.id", id.toString())
                                .build();

                return runTraced("restoreUser", "restore_user", attrs,
                                () -> userRepository.restore(id)
                                                .chain(restoredUser -> {
                                                        if (restoredUser == null) {
                                                                logger.warn("User restore failed - user not found with id: {}",
                                                                                id);
                                                                throw new ResourceNotFoundException(
                                                                                "Restore user not found with id: "
                                                                                                + id);
                                                        }

                                                        UserResponseDeleteAt userResponseDeleteAt = UserResponseDeleteAt
                                                                        .from(restoredUser);
                                                        String cacheKey = "user:" + id;

                                                        return redisService.deleteReactive(cacheKey)
                                                                        .map(v -> {
                                                                                logger.info("Invalidated cache for key: {}",
                                                                                                cacheKey);
                                                                                logger.info("Successfully restored user with id: {}",
                                                                                                id);
                                                                                return ApiResponse.success(
                                                                                                "User restored successfully",
                                                                                                userResponseDeleteAt);
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to restore user with id: {}", id, e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to restore user: " + e.getMessage(),
                                                                        (UserResponseDeleteAt) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> deletePermanent(Long id) {
                logger.info("Permanently deleting user with id: {}", id);
                Attributes attrs = Attributes.builder()
                                .put("user.id", id.toString())
                                .build();

                return runTraced("deleteUserPermanent", "delete_user_permanent", attrs,
                                () -> userRepository.findById(id)
                                                .chain(userToDelete -> {
                                                        if (userToDelete == null) {
                                                                logger.warn("Permanent delete failed - user not found with id: {}",
                                                                                id);
                                                                throw new ResourceNotFoundException(
                                                                                "User not found with id: " + id);
                                                        }

                                                        return userRepository.deletePermanent(id)
                                                                        .chain(v -> {
                                                                                String cacheKey = "user:" + id;
                                                                                return redisService.deleteReactive(
                                                                                                cacheKey)
                                                                                                .map(v2 -> {
                                                                                                        logger.info("Invalidated cache for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully permanently deleted user with id: {}",
                                                                                                                        id);
                                                                                                        return ApiResponse
                                                                                                                        .success("User deleted permanently");
                                                                                                });
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to permanently delete user with id: {}",
                                                                        id, e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to delete user permanently: "
                                                                                        + e.getMessage(),
                                                                        null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> restoreAllTrashedUsers() {
                logger.info("Restoring all trashed users");

                return runTraced("restoreAllTrashedUsers", "restore_all_trashed_users", Attributes.empty(),
                                () -> userRepository.restoreAllDeleted()
                                                .map(v -> {
                                                        if (!v) {
                                                                throw new ResourceNotFoundException(
                                                                                "No users found in trash");
                                                        }

                                                        logger.info("Successfully restored all trashed users");
                                                        return ApiResponse.success(
                                                                        "All trashed users have been restored successfully");
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> deleteAllTrashedUsers() {
                logger.info("Permanently deleting all trashed users");

                return runTraced("deleteAllTrashedUsers", "delete_all_trashed_users", Attributes.empty(),
                                () -> userRepository.deleteAllDeleted()
                                                .map(v -> {
                                                        if (!v) {
                                                                throw new ResourceNotFoundException(
                                                                                "No users found in trash");
                                                        }

                                                        logger.info("Successfully deleted all trashed users");
                                                        return ApiResponse.success(
                                                                        "All trashed users have been deleted permanently");
                                                }));
        }

        private Uni<Role> resolveRoleViaGrpc(String roleName) {
                return roleQueryService.findByNameRole(pb.role.RoleQuery.FindByNameRoleRequest.newBuilder()
                                .setName(roleName)
                                .build())
                                .chain(response -> {
                                        if (!response.hasData()) {
                                                return Uni.createFrom().failure(new ResourceNotFoundException(
                                                                "Role '" + roleName + "' not found in Role service"));
                                        }
                                        pb.role.Role.RoleResponse matchedRole = response.getData();
                                        Role role = new Role();
                                        role.id = (long) matchedRole.getId();
                                        role.setRoleName(matchedRole.getName());
                                        return Uni.createFrom().item(role);
                                });
        }

        private <T> Uni<T> runTraced(String operationName, String method, Attributes attributes,
                        java.util.function.Supplier<Uni<T>> supplier) {
                return tracingMetrics.traceAndMeasure(operationName, method, attributes, supplier);
        }
}