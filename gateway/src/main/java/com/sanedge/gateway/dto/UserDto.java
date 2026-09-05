package com.sanedge.gateway.dto;

import java.util.List;
import java.util.stream.Collectors;

public class UserDto {

    @org.eclipse.microprofile.graphql.Name("CreateUserRequest")
    public record CreateUserRequest(
        String firstname,
        String lastname,
        String email,
        String password,
        String confirmPassword
    ) {}

    @org.eclipse.microprofile.graphql.Name("UpdateUserRequest")
    public record UpdateUserRequest(
        String firstname,
        String lastname,
        String email,
        String password,
        String confirmPassword
    ) {}

    @org.eclipse.microprofile.graphql.Name("UserResponse")
    public record UserResponse(
        int id,
        String firstname,
        String lastname,
        String email,
        String createdAt,
        String updatedAt
    ) {
        public static UserResponse from(pb.user.User.UserResponse proto) {
            return new UserResponse(
                proto.getId(),
                proto.getFirstname(),
                proto.getLastname(),
                proto.getEmail(),
                proto.getCreatedAt(),
                proto.getUpdatedAt()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("UserResponseDeleteAt")
    public record UserResponseDeleteAt(
        int id,
        String firstname,
        String lastname,
        String email,
        String createdAt,
        String updatedAt,
        String deletedAt
    ) {
        public static UserResponseDeleteAt from(pb.user.User.UserResponseDeleteAt proto) {
            return new UserResponseDeleteAt(
                proto.getId(),
                proto.getFirstname(),
                proto.getLastname(),
                proto.getEmail(),
                proto.getCreatedAt(),
                proto.getUpdatedAt(),
                proto.hasDeletedAt() ? proto.getDeletedAt().getValue() : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseUser")
    public record ApiResponseUser(
        String status,
        String message,
        UserResponse data
    ) {
        public static ApiResponseUser from(pb.user.User.ApiResponseUser proto) {
            return new ApiResponseUser(
                proto.getStatus(),
                proto.getMessage(),
                proto.hasData() ? UserResponse.from(proto.getData()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseUserDeleteAt")
    public record ApiResponseUserDeleteAt(
        String status,
        String message,
        UserResponseDeleteAt data
    ) {
        public static ApiResponseUserDeleteAt from(pb.user.User.ApiResponseUserDeleteAt proto) {
            return new ApiResponseUserDeleteAt(
                proto.getStatus(),
                proto.getMessage(),
                proto.hasData() ? UserResponseDeleteAt.from(proto.getData()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationUser")
    public record ApiResponsePaginationUser(
        String status,
        String message,
        List<UserResponse> data,
        PaginationMetaDto paginationMeta
    ) {
        public static ApiResponsePaginationUser from(pb.user.UserQuery.ApiResponsePaginationUser proto) {
            return new ApiResponsePaginationUser(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(UserResponse::from).collect(Collectors.toList()),
                proto.hasPaginationMeta() ? PaginationMetaDto.from(proto.getPaginationMeta()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationUserDeleteAt")
    public record ApiResponsePaginationUserDeleteAt(
        String status,
        String message,
        List<UserResponseDeleteAt> data,
        PaginationMetaDto paginationMeta
    ) {
        public static ApiResponsePaginationUserDeleteAt from(pb.user.UserQuery.ApiResponsePaginationUserDeleteAt proto) {
            return new ApiResponsePaginationUserDeleteAt(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(UserResponseDeleteAt::from).collect(Collectors.toList()),
                proto.hasPaginationMeta() ? PaginationMetaDto.from(proto.getPaginationMeta()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseUserDelete")
    public record ApiResponseUserDelete(
        String status,
        String message
    ) {
        public static ApiResponseUserDelete from(pb.user.UserCommand.ApiResponseUserDelete proto) {
            return new ApiResponseUserDelete(
                proto.getStatus(),
                proto.getMessage()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseUserAll")
    public record ApiResponseUserAll(
        String status,
        String message
    ) {
        public static ApiResponseUserAll from(pb.user.UserCommand.ApiResponseUserAll proto) {
            return new ApiResponseUserAll(
                proto.getStatus(),
                proto.getMessage()
            );
        }
    }
}
