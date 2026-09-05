package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.AuthDto;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class AuthResourceTest {

    @Mock
    pb.MutinyAuthServiceGrpc.MutinyAuthServiceStub authService;

    AuthResource resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new AuthResource();
        Field f = AuthResource.class.getDeclaredField("authService");
        f.setAccessible(true);
        f.set(resource, authService);
    }

    @Test
    void register_returnsSuccess() {
        var proto = pb.Auth.ApiResponseRegister.newBuilder()
                .setStatus("success").setMessage("registered").build();
        when(authService.registerUser(any(pb.Auth.RegisterRequest.class)))
                .thenReturn(Uni.createFrom().item(proto));

        var result = resource.register(new AuthDto.RegisterRequest("J", "D", "e@m.com", "p", "p")).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void login_returnsSuccess() {
        var tokenProto = pb.Auth.TokenResponse.newBuilder()
                .setAccessToken("at")
                .setRefreshToken("rt")
                .build();
        var proto = pb.Auth.ApiResponseLogin.newBuilder()
                .setStatus("success").setMessage("logged in")
                .setData(tokenProto).build();
        when(authService.loginUser(any(pb.Auth.LoginRequest.class)))
                .thenReturn(Uni.createFrom().item(proto));

        var result = resource.login(new AuthDto.LoginRequest("e@m.com", "p")).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void verify_returnsSuccess() {
        var proto = pb.Auth.ApiResponseVerifyCode.newBuilder()
                .setStatus("success").setMessage("verified").build();
        when(authService.verifyCode(any(pb.Auth.VerifyCodeRequest.class)))
                .thenReturn(Uni.createFrom().item(proto));

        var result = resource.verify(new AuthDto.VerifyCodeRequest("123")).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void forgotPassword_returnsSuccess() {
        var proto = pb.Auth.ApiResponseForgotPassword.newBuilder()
                .setStatus("success").setMessage("email sent").build();
        when(authService.forgotPassword(any(pb.Auth.ForgotPasswordRequest.class)))
                .thenReturn(Uni.createFrom().item(proto));

        var result = resource.forgotPassword(new AuthDto.ForgotPasswordRequest("e@m.com")).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void resetPassword_returnsSuccess() {
        var proto = pb.Auth.ApiResponseResetPassword.newBuilder()
                .setStatus("success").setMessage("password reset").build();
        when(authService.resetPassword(any(pb.Auth.ResetPasswordRequest.class)))
                .thenReturn(Uni.createFrom().item(proto));

        var result = resource.resetPassword(new AuthDto.ResetPasswordRequest("tok", "p", "p")).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void refresh_returnsSuccess() {
        var proto = pb.Auth.ApiResponseRefreshToken.newBuilder()
                .setStatus("success").setMessage("refreshed").build();
        when(authService.refreshToken(any(pb.Auth.RefreshTokenRequest.class)))
                .thenReturn(Uni.createFrom().item(proto));

        var result = resource.refresh(new AuthDto.RefreshTokenRequest("rt")).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
}
