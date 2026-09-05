package com.sanedge.gateway.resource;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.sanedge.gateway.dto.AuthDto;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;

@GraphQLApi
public class AuthResource {

    @GrpcClient("auth")
    pb.MutinyAuthServiceGrpc.MutinyAuthServiceStub authService;

    @Mutation("register")
    @Description("Register a new user")
    public Uni<AuthDto.RegisterResponse> register(AuthDto.RegisterRequest body) {
        return authService.registerUser(pb.Auth.RegisterRequest.newBuilder()
                .setFirstname(body.firstname())
                .setLastname(body.lastname())
                .setEmail(body.email())
                .setPassword(body.password())
                .setConfirmPassword(body.confirmPassword())
                .build())
                .map(AuthDto.RegisterResponse::from);
    }

    @Mutation("login")
    @Description("Login a user")
    public Uni<AuthDto.LoginResponse> login(AuthDto.LoginRequest body) {
        return authService.loginUser(pb.Auth.LoginRequest.newBuilder()
                .setEmail(body.email())
                .setPassword(body.password())
                .build())
                .map(AuthDto.LoginResponse::from);
    }

    @Mutation("verify")
    @Description("Verify user email by verification code")
    public Uni<AuthDto.SimpleResponse> verify(AuthDto.VerifyCodeRequest body) {
        return authService.verifyCode(pb.Auth.VerifyCodeRequest.newBuilder()
                .setCode(body.code())
                .build())
                .map(AuthDto.SimpleResponse::from);
    }

    @Mutation("forgotPassword")
    @Description("Initiate forgot password request")
    public Uni<AuthDto.SimpleResponse> forgotPassword(AuthDto.ForgotPasswordRequest body) {
        return authService.forgotPassword(pb.Auth.ForgotPasswordRequest.newBuilder()
                .setEmail(body.email())
                .build())
                .map(AuthDto.SimpleResponse::from);
    }

    @Mutation("resetPassword")
    @Description("Reset user password")
    public Uni<AuthDto.SimpleResponse> resetPassword(AuthDto.ResetPasswordRequest body) {
        return authService.resetPassword(pb.Auth.ResetPasswordRequest.newBuilder()
                .setResetToken(body.resetToken())
                .setPassword(body.password())
                .setConfirmPassword(body.confirmPassword())
                .build())
                .map(AuthDto.SimpleResponse::from);
    }

    @Mutation("refresh")
    @Description("Refresh user access token")
    public Uni<AuthDto.RefreshTokenResponse> refresh(AuthDto.RefreshTokenRequest body) {
        return authService.refreshToken(pb.Auth.RefreshTokenRequest.newBuilder()
                .setRefreshToken(body.refreshToken())
                .build())
                .map(AuthDto.RefreshTokenResponse::from);
    }

    @Query("me")
    @Description("Get current logged-in user profile")
    @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
    public Uni<AuthDto.GetMeResponse> getMe(@Name("userId") int userId) {
        return authService.getMe(pb.Auth.GetMeRequest.newBuilder()
                .setUserId(userId)
                .build())
                .map(AuthDto.GetMeResponse::from);
    }
}
