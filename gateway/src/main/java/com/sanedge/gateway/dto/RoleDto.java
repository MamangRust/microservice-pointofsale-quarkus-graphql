package com.sanedge.gateway.dto;

import java.util.List;
import java.util.stream.Collectors;

public class RoleDto {

    @org.eclipse.microprofile.graphql.Name("CreateRoleRequest")
    public record CreateRoleRequest(String name) {}

    @org.eclipse.microprofile.graphql.Name("UpdateRoleRequest")
    public record UpdateRoleRequest(String name) {}

    @org.eclipse.microprofile.graphql.Name("AssignRoleToUserRequest")
    public record AssignRoleToUserRequest(int userId, int roleId) {}

    @org.eclipse.microprofile.graphql.Name("RoleResponse")
    public record RoleResponse(
        int id,
        String name,
        String createdAt,
        String updatedAt
    ) {
        public static RoleResponse from(pb.role.Role.RoleResponse proto) {
            return new RoleResponse(
                proto.getId(),
                proto.getName(),
                proto.getCreatedAt(),
                proto.getUpdatedAt()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("RoleResponseDeleteAt")
    public record RoleResponseDeleteAt(
        int id,
        String name,
        String createdAt,
        String updatedAt,
        String deletedAt
    ) {
        public static RoleResponseDeleteAt from(pb.role.Role.RoleResponseDeleteAt proto) {
            return new RoleResponseDeleteAt(
                proto.getId(),
                proto.getName(),
                proto.getCreatedAt(),
                proto.getUpdatedAt(),
                proto.hasDeletedAt() ? proto.getDeletedAt().getValue() : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("UserRoleResponse")
    public record UserRoleResponse(
        int userRoleId,
        int userId,
        int roleId,
        String createdAt,
        String updatedAt
    ) {
        public static UserRoleResponse from(pb.role.RoleCommand.UserRoleResponse proto) {
            return new UserRoleResponse(
                proto.getUserRoleId(),
                proto.getUserId(),
                proto.getRoleId(),
                proto.getCreatedAt(),
                proto.getUpdatedAt()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseRole")
    public record ApiResponseRole(
        String status,
        String message,
        RoleResponse data
    ) {
        public static ApiResponseRole from(pb.role.Role.ApiResponseRole proto) {
            return new ApiResponseRole(
                proto.getStatus(),
                proto.getMessage(),
                proto.hasData() ? RoleResponse.from(proto.getData()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseRoleDeleteAt")
    public record ApiResponseRoleDeleteAt(
        String status,
        String message,
        RoleResponseDeleteAt data
    ) {
        public static ApiResponseRoleDeleteAt from(pb.role.Role.ApiResponseRoleDeleteAt proto) {
            return new ApiResponseRoleDeleteAt(
                proto.getStatus(),
                proto.getMessage(),
                proto.hasData() ? RoleResponseDeleteAt.from(proto.getData()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsesRole")
    public record ApiResponsesRole(
        String status,
        String message,
        List<RoleResponse> data
    ) {
        public static ApiResponsesRole from(pb.role.Role.ApiResponsesRole proto) {
            return new ApiResponsesRole(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(RoleResponse::from).collect(Collectors.toList())
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationRole")
    public record ApiResponsePaginationRole(
        String status,
        String message,
        List<RoleResponse> data,
        PaginationMetaDto paginationMeta
    ) {
        public static ApiResponsePaginationRole from(pb.role.RoleQuery.ApiResponsePaginationRole proto) {
            return new ApiResponsePaginationRole(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(RoleResponse::from).collect(Collectors.toList()),
                proto.hasPaginationMeta() ? PaginationMetaDto.from(proto.getPaginationMeta()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationRoleDeleteAt")
    public record ApiResponsePaginationRoleDeleteAt(
        String status,
        String message,
        List<RoleResponseDeleteAt> data,
        PaginationMetaDto paginationMeta
    ) {
        public static ApiResponsePaginationRoleDeleteAt from(pb.role.RoleQuery.ApiResponsePaginationRoleDeleteAt proto) {
            return new ApiResponsePaginationRoleDeleteAt(
                proto.getStatus(),
                proto.getMessage(),
                proto.getDataList().stream().map(RoleResponseDeleteAt::from).collect(Collectors.toList()),
                proto.hasPaginationMeta() ? PaginationMetaDto.from(proto.getPaginationMeta()) : null
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseRoleAll")
    public record ApiResponseRoleAll(
        String status,
        String message
    ) {
        public static ApiResponseRoleAll from(pb.role.RoleCommand.ApiResponseRoleAll proto) {
            return new ApiResponseRoleAll(
                proto.getStatus(),
                proto.getMessage()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseRoleDelete")
    public record ApiResponseRoleDelete(
        String status,
        String message
    ) {
        public static ApiResponseRoleDelete from(pb.role.RoleCommand.ApiResponseRoleDelete proto) {
            return new ApiResponseRoleDelete(
                proto.getStatus(),
                proto.getMessage()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseUserRole")
    public record ApiResponseUserRole(
        String status,
        String message,
        UserRoleResponse data
    ) {
        public static ApiResponseUserRole from(pb.role.RoleCommand.ApiResponseUserRole proto) {
            return new ApiResponseUserRole(
                proto.getStatus(),
                proto.getMessage(),
                proto.hasData() ? UserRoleResponse.from(proto.getData()) : null
            );
        }
    }
}
