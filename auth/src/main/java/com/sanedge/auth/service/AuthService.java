package com.sanedge.auth.service;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;

import com.sanedge.auth.entity.Outbox;
import com.sanedge.auth.entity.RefreshToken;
import com.sanedge.auth.entity.ResetToken;
import com.sanedge.auth.repository.OutboxRepository;
import com.sanedge.auth.repository.RefreshTokenRepository;
import com.sanedge.auth.repository.ResetTokenRepository;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.utils.JwtUtil;
import com.sanedge.common.utils.PasswordUtil;
import io.opentelemetry.api.common.Attributes;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pb.role.RoleCommandService;
import pb.role.RoleService;
import pb.role.Role.ApiResponseRole;
import pb.role.RoleCommand.AssignRoleToUserRequest;
import pb.role.RoleQuery.FindByNameRoleRequest;
import pb.user.UserQueryService;
import pb.user.UserCommandService;
import pb.user.User.UserResponse;
import pb.user.User.FindAllUserRequest;
import pb.user.User.FindByIdUserRequest;
import com.sanedge.auth.domain.requests.RegisterRequest;
import com.sanedge.auth.domain.requests.ResetPasswordRequest;
import pb.user.UserCommand.CreateUserRequest;
import pb.user.UserCommand.UpdateUserRequest;
import pb.user.UserCommand.VerifyPasswordRequest;

@ApplicationScoped
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Inject
    @GrpcClient("user")
    UserQueryService userQueryService;

    @Inject
    @GrpcClient("user")
    UserCommandService userCommandService;

    @Inject
    @GrpcClient("role")
    RoleService roleService;

    @Inject
    @GrpcClient("role")
    RoleCommandService roleCommandService;

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Inject
    ResetTokenRepository resetTokenRepository;

    @Inject
    RedisService redisService;

    @Inject
    OutboxRepository outboxRepository;

    @Inject
    JwtUtil jwtUtil;

    @Inject
    PasswordUtil passwordUtil;

    @Inject
    TracingMetrics tracingMetrics;

    @WithTransaction
    public Uni<UserResponse> register(RegisterRequest req) {
        Attributes attrs = Attributes.builder()
                .put("user.email", req.getEmail())
                .build();

        return runTraced("registerUser", "register", attrs, () -> {
            log.info("Attempting to register user with email: {}", req.getEmail());
            return userQueryService
                    .findAll(FindAllUserRequest.newBuilder().setSearch(req.getEmail()).setPage(1).setPageSize(1).build())
                    .chain(findAllResponse -> {
                        if (findAllResponse.getDataCount() > 0) {
                            for (UserResponse u : findAllResponse.getDataList()) {
                                if (u.getEmail().equalsIgnoreCase(req.getEmail())) {
                                    return Uni.createFrom()
                                            .failure(new RuntimeException("User with this email already exists"));
                                }
                            }
                        }

                        CreateUserRequest createReq = CreateUserRequest.newBuilder()
                                .setFirstname(req.getFirstName())
                                .setLastname(req.getLastName())
                                .setEmail(req.getEmail())
                                .setPassword(req.getPassword())
                                .setConfirmPassword(req.getPassword())
                                .build();

                        return userCommandService.create(createReq);
                    })
                    .chain(createUserResponse -> {
                        if (!"success".equalsIgnoreCase(createUserResponse.getStatus())) {
                            return Uni.createFrom().failure(new RuntimeException(createUserResponse.getMessage()));
                        }

                        UserResponse user = createUserResponse.getData();
                        String verificationCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

                        return redisService.setWithExpirationReactive("verification:" + req.getEmail(), verificationCode, 900)
                                .chain(() -> redisService.setWithExpirationReactive(
                                        "verification_code:" + verificationCode,
                                        req.getEmail(), 900))
                                .chain(() -> sendWelcomeEmail(user, verificationCode))
                                .chain(() -> {
                                    return roleService
                                            .findByNameRole(
                                                    FindByNameRoleRequest.newBuilder().setName("ROLE_USER").build())
                                            .onFailure().recoverWithItem(err -> {
                                                log.warn("Failed to find ROLE_USER, using fallback ID 2: {}",
                                                        err.getMessage());
                                                return ApiResponseRole.newBuilder()
                                                        .setData(
                                                                pb.role.Role.RoleResponse.newBuilder().setId(2).build())
                                                        .build();
                                            })
                                            .chain(roleResp -> {
                                                int roleId = roleResp.hasData() ? roleResp.getData().getId() : 2;
                                                if (roleId <= 0) {
                                                    roleId = 2;
                                                }
                                                AssignRoleToUserRequest assignReq = AssignRoleToUserRequest.newBuilder()
                                                        .setUserId(user.getId())
                                                        .setRoleId(roleId)
                                                        .build();
                                                return roleCommandService.assignRoleToUser(assignReq)
                                                        .replaceWith(user);
                                            });
                                });
                    })
                    .invoke(user -> log.info("Successfully registered user: {}", user.getEmail()));
        });
    }

    @WithTransaction
    public Uni<String[]> login(String email, String password) {
        Attributes attrs = Attributes.builder()
                .put("user.email", email)
                .build();

        return runTraced("loginUser", "login", attrs, () -> {
            log.info("Attempting login for email: {}", email);
            String failedAttemptsKey = "failed_login:" + email;
            String lockKey = "account_locked:" + email;

            return redisService.existsReactive(lockKey)
                    .chain(locked -> {
                        if (locked) {
                            return Uni.createFrom()
                                    .failure(new RuntimeException("Account is locked due to too many failed attempts"));
                        }
                        return userCommandService.verifyPassword(VerifyPasswordRequest.newBuilder()
                                .setEmail(email)
                                .setPassword(password)
                                .build());
                    })
                    .chain(verifyRes -> {
                        if (!verifyRes.getValid()) {
                            return handleFailedLogin(email, failedAttemptsKey, lockKey);
                        }

                        UserResponse user = verifyRes.getUser();

                        String accessToken = jwtUtil.generateToken(user.getEmail(),
                                Collections.singletonList("ROLE_USER"),
                                (long) user.getId());
                        String refreshTokenStr = jwtUtil.generateRefreshToken(user.getEmail(), (long) user.getId());

                        RefreshToken rt = new RefreshToken();
                        rt.setUserId((long) user.getId());
                        rt.setToken(refreshTokenStr);
                        rt.setExpiration(new Timestamp(System.currentTimeMillis() + jwtUtil.getRefreshExpirationMs()));

                        return redisService.deleteReactive(failedAttemptsKey)
                                .chain(() -> refreshTokenRepository.deleteByUserId((long) user.getId()))
                                .chain(() -> refreshTokenRepository.persist(rt))
                                .map(v -> new String[] { accessToken, refreshTokenStr });
                    })
                    .invoke(tokens -> log.info("Successfully logged in user: {}", email));
        });
    }

    @WithTransaction
    public Uni<String[]> refresh(String refreshTokenStr) {
        Attributes attrs = Attributes.builder()
                .put("operation", "refresh")
                .build();

        return runTraced("refreshToken", "refresh", attrs, () -> {
            log.info("Attempting to refresh access token");
            if (!jwtUtil.validateToken(refreshTokenStr)) {
                return Uni.createFrom().failure(new RuntimeException("Invalid or expired refresh token"));
            }

            return refreshTokenRepository.findByToken(refreshTokenStr)
                    .chain(rt -> {
                        if (rt == null || rt.getExpiration().before(new Timestamp(System.currentTimeMillis()))) {
                            return Uni.createFrom()
                                    .failure(new RuntimeException("Refresh token is invalid or expired"));
                        }

                        return userQueryService
                                .findById(FindByIdUserRequest.newBuilder().setId(rt.getUserId().intValue()).build())
                                .chain(userRes -> {
                                    if (!"success".equalsIgnoreCase(userRes.getStatus()) || !userRes.hasData()) {
                                        return Uni.createFrom().failure(new RuntimeException("User not found"));
                                    }

                                    UserResponse user = userRes.getData();
                                    String newAccessToken = jwtUtil.generateToken(user.getEmail(),
                                            Collections.singletonList("ROLE_USER"), (long) user.getId());
                                    String newRefreshTokenStr = jwtUtil.generateRefreshToken(user.getEmail(),
                                            (long) user.getId());

                                    rt.setToken(newRefreshTokenStr);
                                    rt.setExpiration(
                                            new Timestamp(
                                                    System.currentTimeMillis() + jwtUtil.getRefreshExpirationMs()));

                                    return refreshTokenRepository.persist(rt)
                                            .map(v -> new String[] { newAccessToken, newRefreshTokenStr });
                                });
                    })
                    .invoke(tokens -> log.info("Successfully refreshed access token"));
        });
    }

    @WithTransaction
    public Uni<Void> forgotPassword(String email) {
        Attributes attrs = Attributes.builder()
                .put("user.email", email)
                .build();

        return runTraced("forgotPassword", "forgot_password", attrs, () -> {
            log.info("Initiated forgot password process for email: {}", email);
            return userQueryService
                    .findAll(FindAllUserRequest.newBuilder().setSearch(email).setPage(1).setPageSize(1).build())
                    .chain(findAllResponse -> {
                        if (findAllResponse.getDataCount() == 0) {
                            return Uni.createFrom().failure(new RuntimeException("User not found"));
                        }

                        UserResponse user = findAllResponse.getData(0);
                        String token = UUID.randomUUID().toString();

                        ResetToken resetToken = new ResetToken();
                        resetToken.setUserId((long) user.getId());
                        resetToken.setToken(token);
                        resetToken.setExpiration(new Timestamp(System.currentTimeMillis() + 900000)); // 15 mins

                        return resetTokenRepository.deleteByUserId((long) user.getId())
                                .chain(() -> resetTokenRepository.persist(resetToken))
                                .chain(() -> sendForgotPasswordEmail(user, token));
                    })
                    .invoke(() -> log.info("Successfully sent forgot password token to: {}", email));
        });
    }

    @WithTransaction
    public Uni<Void> resetPassword(ResetPasswordRequest req) {
        Attributes attrs = Attributes.builder()
                .put("operation", "reset_password")
                .build();

        return runTraced("resetPassword", "reset_password", attrs, () -> {
            log.info("Attempting reset password using reset token");
            if (!req.getPassword().equals(req.getConfirmPassword())) {
                return Uni.createFrom().failure(new RuntimeException("Passwords do not match"));
            }

            return resetTokenRepository.findByToken(req.getToken())
                    .chain(rt -> {
                        if (rt == null || rt.getExpiration().before(new Timestamp(System.currentTimeMillis()))) {
                            return Uni.createFrom().failure(new RuntimeException("Invalid or expired reset token"));
                        }

                        return userQueryService
                                .findById(FindByIdUserRequest.newBuilder().setId(rt.getUserId().intValue()).build())
                                .chain(userRes -> {
                                    if (!"success".equalsIgnoreCase(userRes.getStatus()) || !userRes.hasData()) {
                                        return Uni.createFrom().failure(new RuntimeException("User not found"));
                                    }

                                    UserResponse user = userRes.getData();
                                    UpdateUserRequest updateReq = UpdateUserRequest.newBuilder()
                                            .setId(user.getId())
                                            .setFirstname(user.getFirstname())
                                            .setLastname(user.getLastname())
                                            .setEmail(user.getEmail())
                                            .setPassword(req.getPassword())
                                            .setConfirmPassword(req.getConfirmPassword())
                                            .build();

                                    return userCommandService.update(updateReq);
                                })
                                .chain(updateRes -> {
                                    if (!"success".equalsIgnoreCase(updateRes.getStatus())) {
                                        return Uni.createFrom().failure(new RuntimeException(updateRes.getMessage()));
                                    }
                                    return resetTokenRepository.delete(rt);
                                })
                                .replaceWithVoid();
                    })
                    .invoke(() -> log.info("Successfully reset password"));
        });
    }

    @WithTransaction
    public Uni<Void> logout(String refreshTokenStr) {
        log.info("Attempting logout");
        return refreshTokenRepository.deleteByToken(refreshTokenStr)
                .replaceWithVoid()
                .invoke(() -> log.info("Successfully logged out"))
                .onFailure().invoke(err -> log.error("Logout failed, error: {}", err.getMessage()));
    }

    public Uni<Void> verifyEmailByCode(String code) {
        log.info("Attempting email verification with code: {}", code);
        String key = "verification_code:" + code;
        return redisService.getReactive(key)
                .chain(email -> {
                    if (email == null) {
                        return Uni.createFrom().failure(new RuntimeException("Invalid or expired verification code"));
                    }
                    return redisService.deleteReactive(key)
                            .chain(() -> redisService.deleteReactive("verification:" + email))
                            .replaceWithVoid();
                })
                .invoke(() -> log.info("Successfully verified email for code: {}", code))
                .onFailure()
                .invoke(err -> log.error("Email verification failed for code: {}, error: {}", code, err.getMessage()));
    }

    public Uni<UserResponse> getMe(Long userId) {
        log.info("Fetching profile for user ID: {}", userId);
        return userQueryService.findById(FindByIdUserRequest.newBuilder().setId(userId.intValue()).build())
                .map(res -> {
                    if (!"success".equalsIgnoreCase(res.getStatus()) || !res.hasData()) {
                        throw new RuntimeException("User not found");
                    }
                    return res.getData();
                })
                .invoke(user -> log.info("Successfully retrieved profile for user: {}", user.getEmail()))
                .onFailure().invoke(err -> log.error("Failed to retrieve profile for user ID: {}, error: {}", userId,
                        err.getMessage()));
    }

    private Uni<String[]> handleFailedLogin(String email, String failedAttemptsKey, String lockKey) {
        log.warn("Failed login attempt for email: {}", email);
        return redisService.getReactive(failedAttemptsKey)
                .chain(attemptsStr -> {
                    int currentAttempts = attemptsStr == null ? 0 : Integer.parseInt(attemptsStr);
                    int newAttempts = currentAttempts + 1;
                    if (newAttempts >= 5) {
                        log.error("Account locked due to 5 consecutive failed login attempts: {}", email);
                        return redisService.setWithExpirationReactive(lockKey, "true", 3600) // lock 1 hr
                                .chain(() -> redisService.deleteReactive(failedAttemptsKey))
                                .chain(() -> Uni.createFrom().failure(
                                        new RuntimeException("Account is locked due to too many failed attempts")));
                    } else {
                        log.warn("Incrementing failed login attempts for email: {}. Current failed attempts: {}", email,
                                newAttempts);
                        return redisService
                                .setWithExpirationReactive(failedAttemptsKey, String.valueOf(newAttempts), 600) // 10
                                                                                                                // mins
                                .chain(() -> Uni.createFrom().failure(
                                        new RuntimeException("Invalid credentials. Attempt " + newAttempts + " of 5")));
                    }
                });
    }

    /**
     * Writes the welcome-email event into the outbox table (transactional
     * outbox pattern) instead of publishing to Kafka directly — the row is
     * flushed by {@code OutboxPublisher} in the same DB transaction as the
     * user creation. Kafka being down no longer loses the notification.
     */
    private Uni<Void> sendWelcomeEmail(UserResponse user, String code) {
        String subject = "Welcome to Quarkus Modular Monolith";
        String body = String.format(
                "Hello %s %s,\n\nWelcome to our platform! Use the following code to verify your email address:\n\n%s\n\nRegards,\nSupport Team",
                user.getFirstname(), user.getLastname(), code);

        JsonObject payload = new JsonObject()
                .put("email", user.getEmail())
                .put("subject", subject)
                .put("body", body);

        return persistOutboxEvent("USER", String.valueOf(user.getId()),
                "email-service-topic-auth-register", payload);
    }

    private Uni<Void> sendForgotPasswordEmail(UserResponse user, String token) {
        String subject = "Reset Password Verification";
        String body = String.format(
                "Hello %s %s,\n\nYou have requested a password reset. Use the following token to reset your password:\n\n%s\n\nThis token will expire in 15 minutes.\n\nRegards,\nSupport Team",
                user.getFirstname(), user.getLastname(), token);

        JsonObject payload = new JsonObject()
                .put("email", user.getEmail())
                .put("subject", subject)
                .put("body", body);

        return persistOutboxEvent("USER", String.valueOf(user.getId()),
                "email-service-topic-auth-forgot-password", payload);
    }

    private Uni<Void> persistOutboxEvent(String aggregateType, String aggregateId, String topic, JsonObject payload) {
        Outbox outbox = new Outbox();
        outbox.setAggregateType(aggregateType);
        outbox.setAggregateId(aggregateId);
        outbox.setTopic(topic);
        // Phase 2 (event contract): attach the standard envelope (event_id,
        // schema_version, event_type, occurred_at) before persisting, so the
        // outbox replay keeps a stable event_id.
        outbox.setPayload(com.sanedge.common.event.EventEnvelope.withDefaults(payload, topic).encode());
        outbox.setDomain("auth");
        outbox.setEventId(outbox.getPayload() != null ?
                new io.vertx.core.json.JsonObject(outbox.getPayload()).getString("event_id") : null);
        return outboxRepository.persist(outbox).replaceWithVoid();
    }

    private <T> Uni<T> runTraced(String operationName, String method, Attributes attributes,
            Supplier<Uni<T>> supplier) {
        return tracingMetrics.traceAndMeasure(operationName, method, attributes, supplier);
    }
}