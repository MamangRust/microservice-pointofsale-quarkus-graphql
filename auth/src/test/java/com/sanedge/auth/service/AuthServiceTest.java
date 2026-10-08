package com.sanedge.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sanedge.auth.domain.requests.RegisterRequest;
import com.sanedge.auth.domain.requests.ResetPasswordRequest;
import com.sanedge.auth.entity.Outbox;
import com.sanedge.auth.entity.RefreshToken;
import com.sanedge.auth.entity.ResetToken;
import com.sanedge.auth.repository.OutboxRepository;
import com.sanedge.auth.repository.RefreshTokenRepository;
import com.sanedge.auth.repository.ResetTokenRepository;
import com.sanedge.common.adapter.model.CreateUserCmd;
import com.sanedge.common.adapter.model.Role;
import com.sanedge.common.adapter.model.UpdateUserCmd;
import com.sanedge.common.adapter.model.User;
import com.sanedge.common.adapter.model.VerifyPasswordResult;
import com.sanedge.common.adapter.role.RoleCommandPort;
import com.sanedge.common.adapter.role.RoleQueryPort;
import com.sanedge.common.adapter.user.UserCommandPort;
import com.sanedge.common.adapter.user.UserQueryPort;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.utils.JwtUtil;
import com.sanedge.common.utils.PasswordUtil;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import pb.user.User.UserResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserQueryPort userQueryPort;

    @Mock
    UserCommandPort userCommandPort;

    @Mock
    RoleQueryPort roleQueryPort;

    @Mock
    RoleCommandPort roleCommandPort;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    ResetTokenRepository resetTokenRepository;

    @Mock
    RedisService redisService;

    @Mock
    OutboxRepository outboxRepository;

    @Mock
    JwtUtil jwtUtil;

    @Mock
    PasswordUtil passwordUtil;

    @Mock
    TracingMetrics tracingMetrics;

    @InjectMocks
    AuthService authServiceUnderTest;

    private RegisterRequest registerReq;

    @BeforeEach
    void setUp() {
        registerReq = new RegisterRequest();
        registerReq.setFirstName("John");
        registerReq.setLastName("Doe");
        registerReq.setEmail("john@example.com");
        registerReq.setPassword("SecurePass123!");

        // Production AuthService calls the 4-arg (name, op, attributes, supplier)
        // overload of traceAndMeasure. Stub it so the Supplier is invoked and
        // returns its Uni, instead of Mockito's default null Uni (NPE on .await()).
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        lenient().when(redisService.existsReactive(anyString())).thenReturn(Uni.createFrom().item(false));
        lenient().when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        lenient().when(redisService.setWithExpirationReactive(anyString(), anyString(), any(Long.class)))
                .thenReturn(Uni.createFrom().voidItem());
        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        lenient().when(outboxRepository.persist(any(Outbox.class)))
                .thenReturn(Uni.createFrom().nullItem());

        lenient().when(jwtUtil.generateToken(anyString(), any(), any(Long.class)))
                .thenReturn("access-token-john");
        lenient().when(jwtUtil.generateRefreshToken(anyString(), any(Long.class)))
                .thenReturn("refresh-token-john");
        lenient().when(jwtUtil.validateToken(anyString())).thenReturn(true);
        lenient().when(jwtUtil.getRefreshExpirationMs()).thenReturn(3600000L);

        lenient().when(passwordUtil.hashPassword(anyString())).thenReturn("hashed-password");
    }

    private static User john() {
        return new User(1, "John", "Doe", "john@example.com", null, null);
    }

    @Test
    void registerUser_shouldSucceed() {
        when(userQueryPort.findByEmail("john@example.com")).thenReturn(Uni.createFrom().nullItem());
        when(userCommandPort.create(any(CreateUserCmd.class))).thenReturn(Uni.createFrom().item(john()));
        when(roleQueryPort.findByName("ROLE_USER")).thenReturn(Uni.createFrom().item(new Role(1, "ROLE_USER", null, null)));
        when(roleCommandPort.assignRoleToUser(1, 1)).thenReturn(Uni.createFrom().voidItem());

        UserResponse result = authServiceUnderTest.register(registerReq).await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getFirstname()).isEqualTo("John");
        verify(userQueryPort).findByEmail("john@example.com");
        verify(userCommandPort).create(any(CreateUserCmd.class));
        ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).persist(outboxCaptor.capture());
        Outbox captured = outboxCaptor.getValue();
        assertThat(captured.getTopic()).isEqualTo("email-service-topic-auth-register");
        assertThat(captured.getAggregateType()).isEqualTo("USER");
        assertThat(captured.getPayload()).contains("\"email\":\"john@example.com\"");
    }

    @Test
    void registerUser_shouldFail_whenEmailAlreadyExists() {
        when(userQueryPort.findByEmail("john@example.com")).thenReturn(Uni.createFrom().item(john()));

        try {
            authServiceUnderTest.register(registerReq).await().indefinitely();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("already exists");
        }
    }

    @Test
    void login_shouldSucceed() {
        when(userQueryPort.verifyPassword("john@example.com", "SecurePass123!"))
                .thenReturn(Uni.createFrom().item(new VerifyPasswordResult(true, john(), null)));

        lenient().when(refreshTokenRepository.deleteByUserId(1L)).thenReturn(Uni.createFrom().item(1L));
        lenient().when(refreshTokenRepository.persist(any(RefreshToken.class)))
                .thenAnswer(inv -> Uni.createFrom().item((RefreshToken) inv.getArgument(0)));

        String[] tokens = authServiceUnderTest.login("john@example.com", "SecurePass123!").await()
                .indefinitely();

        assertThat(tokens).hasSize(2);
        assertThat(tokens[0]).isEqualTo("access-token-john");
        assertThat(tokens[1]).isEqualTo("refresh-token-john");
    }

    @Test
    void login_shouldFail_whenAccountLocked() {
        when(redisService.existsReactive(anyString())).thenReturn(Uni.createFrom().item(true));

        try {
            authServiceUnderTest.login("john@example.com", "wrong").await().indefinitely();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("locked");
        }
    }

    @Test
    void login_shouldFail_withInvalidCredentials() {
        when(userQueryPort.verifyPassword(anyString(), anyString()))
                .thenReturn(Uni.createFrom().item(new VerifyPasswordResult(false, null, null)));

        try {
            authServiceUnderTest.login("john@example.com", "wrong").await().indefinitely();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Invalid credentials");
        }
    }

    @Test
    void refresh_shouldSucceed() {
        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken("old-refresh-token");
        storedToken.setUserId(1L);
        storedToken.setExpiration(new Timestamp(System.currentTimeMillis() + 3600000));

        when(refreshTokenRepository.findByToken("old-refresh-token"))
                .thenReturn(Uni.createFrom().item(storedToken));
        when(refreshTokenRepository.persist(any(RefreshToken.class)))
                .thenAnswer(inv -> Uni.createFrom().item((RefreshToken) inv.getArgument(0)));

        when(userQueryPort.findById(1)).thenReturn(Uni.createFrom().item(john()));

        String[] tokens = authServiceUnderTest.refresh("old-refresh-token").await().indefinitely();

        assertThat(tokens).hasSize(2);
        assertThat(tokens[0]).isEqualTo("access-token-john");
        verify(refreshTokenRepository).persist(any(RefreshToken.class));
    }

    @Test
    void refresh_shouldFail_whenTokenInvalid() {
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        try {
            authServiceUnderTest.refresh("invalid-token").await().indefinitely();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Invalid or expired");
        }
    }

    @Test
    void refresh_shouldFail_whenTokenNotFound() {
        when(refreshTokenRepository.findByToken("unknown-token"))
                .thenReturn(Uni.createFrom().nullItem());

        try {
            authServiceUnderTest.refresh("unknown-token").await().indefinitely();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("invalid or expired");
        }
    }

    @Test
    void forgotPassword_shouldSucceed() {
        when(userQueryPort.findByEmail("john@example.com")).thenReturn(Uni.createFrom().item(john()));

        lenient().when(resetTokenRepository.deleteByUserId(1L)).thenReturn(Uni.createFrom().item(1L));
        lenient().when(resetTokenRepository.persist(any(ResetToken.class)))
                .thenAnswer(inv -> Uni.createFrom().item((ResetToken) inv.getArgument(0)));

        authServiceUnderTest.forgotPassword("john@example.com").await().indefinitely();

        verify(resetTokenRepository).persist(any(ResetToken.class));
        ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).persist(outboxCaptor.capture());
        Outbox captured = outboxCaptor.getValue();
        assertThat(captured.getTopic()).isEqualTo("email-service-topic-auth-forgot-password");
    }

    @Test
    void forgotPassword_shouldFail_whenUserNotFound() {
        when(userQueryPort.findByEmail("unknown@example.com")).thenReturn(Uni.createFrom().nullItem());

        try {
            authServiceUnderTest.forgotPassword("unknown@example.com").await().indefinitely();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("User not found");
        }
    }

    @Test
    void resetPassword_shouldSucceed() {
        ResetToken resetToken = new ResetToken();
        resetToken.setToken("valid-reset-token");
        resetToken.setUserId(1L);
        resetToken.setExpiration(new Timestamp(System.currentTimeMillis() + 900000));

        when(resetTokenRepository.findByToken("valid-reset-token"))
                .thenReturn(Uni.createFrom().item(resetToken));

        when(userQueryPort.findById(1)).thenReturn(Uni.createFrom().item(john()));

        when(userCommandPort.update(any(UpdateUserCmd.class))).thenReturn(Uni.createFrom().item(john()));

        lenient().when(resetTokenRepository.delete(any(ResetToken.class)))
                .thenReturn(Uni.createFrom().voidItem());

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("valid-reset-token");
        req.setPassword("NewPass123!");
        req.setConfirmPassword("NewPass123!");

        authServiceUnderTest.resetPassword(req).await().indefinitely();

        verify(userCommandPort).update(any(UpdateUserCmd.class));
    }

    @Test
    void resetPassword_shouldFail_whenPasswordsMismatch() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("some-token");
        req.setPassword("Pass1!");
        req.setConfirmPassword("Pass2!");

        try {
            authServiceUnderTest.resetPassword(req).await().indefinitely();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("do not match");
        }
    }

    @Test
    void resetPassword_shouldFail_whenTokenExpired() {
        ResetToken expiredToken = new ResetToken();
        expiredToken.setToken("expired-token");
        expiredToken.setUserId(1L);
        expiredToken.setExpiration(new Timestamp(System.currentTimeMillis() - 3600000));

        when(resetTokenRepository.findByToken("expired-token"))
                .thenReturn(Uni.createFrom().item(expiredToken));

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("expired-token");
        req.setPassword("NewPass123!");
        req.setConfirmPassword("NewPass123!");

        try {
            authServiceUnderTest.resetPassword(req).await().indefinitely();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("expired");
        }
    }

    @Test
    void logout_shouldSucceed() {
        when(refreshTokenRepository.deleteByToken("refresh-token-to-revoke"))
                .thenReturn(Uni.createFrom().item(1L));

        authServiceUnderTest.logout("refresh-token-to-revoke").await().indefinitely();

        verify(refreshTokenRepository).deleteByToken("refresh-token-to-revoke");
    }

    @Test
    void getMe_shouldReturnUser() {
        when(userQueryPort.findById(1)).thenReturn(Uni.createFrom().item(john()));

        UserResponse result = authServiceUnderTest.getMe(1L).await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void getMe_shouldFail_whenUserNotFound() {
        when(userQueryPort.findById(999)).thenReturn(
                Uni.createFrom().failure(new ResourceNotFoundException("not found")));

        try {
            authServiceUnderTest.getMe(999L).await().indefinitely();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("User not found");
        }
    }

    @Test
    void verifyEmail_shouldSucceed() {
        when(redisService.getReactive("verification_code:ABC123"))
                .thenReturn(Uni.createFrom().item("john@example.com"));

        authServiceUnderTest.verifyEmailByCode("ABC123").await().indefinitely();

        verify(redisService).deleteReactive("verification_code:ABC123");
    }

    @Test
    void verifyEmail_shouldFail_whenInvalidCode() {
        when(redisService.getReactive("verification_code:INVALID"))
                .thenReturn(Uni.createFrom().nullItem());

        try {
            authServiceUnderTest.verifyEmailByCode("INVALID").await().indefinitely();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Invalid or expired");
        }
    }

    /**
     * Finds the Supplier argument in the invocation (the 4-arg overload, arg
     * index 3), then invokes it and returns the resulting Uni.
     */
    private Answer<Uni<?>> invokeSupplier() {
        return invocation -> {
            Supplier<?> supplier = null;
            for (Object arg : invocation.getArguments()) {
                if (arg instanceof Supplier<?>) {
                    supplier = (Supplier<?>) arg;
                    break;
                }
            }
            return supplier != null ? (Uni<?>) supplier.get() : null;
        };
    }
}
