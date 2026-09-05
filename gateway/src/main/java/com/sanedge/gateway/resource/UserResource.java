package com.sanedge.gateway.resource;

import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.sanedge.gateway.dto.UserDto;
import com.sanedge.gateway.service.UserService;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

@GraphQLApi
public class UserResource {

        @Inject
        UserService userService;

        @Query("users")
        @Description("List all users")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<UserDto.ApiResponsePaginationUser> listUsers(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return userService.listUsers(page, size, search);
        }

        @Query("activeUsers")
        @Description("List active users")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<UserDto.ApiResponsePaginationUserDeleteAt> activeUsers(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return userService.getActiveUsers(page, size, search);
        }

        @Query("trashedUsers")
        @Description("List trashed users")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<UserDto.ApiResponsePaginationUserDeleteAt> trashedUsers(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return userService.getTrashedUsers(page, size, search);
        }

        @Query("user")
        @Description("Get user by ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<UserDto.ApiResponseUser> getUser(@Name("id") int id) {
                return userService.getUser(id);
        }

        @Mutation("createUser")
        @Description("Create a new user")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<UserDto.ApiResponseUser> createUser(@Name("body") UserDto.CreateUserRequest body) {
                return userService.createUser(body);
        }

        @Mutation("updateUser")
        @Description("Update user")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<UserDto.ApiResponseUser> updateUser(@Name("id") int id,
                        @Name("body") UserDto.UpdateUserRequest body) {
                return userService.updateUser(id, body);
        }

        @Mutation("deleteUser")
        @Description("Soft-delete a user")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<UserDto.ApiResponseUserDeleteAt> deleteUser(@Name("id") int id) {
                return userService.deleteUser(id);
        }

        @Mutation("restoreUser")
        @Description("Restore a soft-deleted user")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<UserDto.ApiResponseUserDeleteAt> restoreUser(@Name("id") int id) {
                return userService.restoreUser(id);
        }

        @Mutation("deleteUserPermanent")
        @Description("Permanently delete a user")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<UserDto.ApiResponseUserDelete> deleteUserPermanent(@Name("id") int id) {
                return userService.deleteUserPermanent(id);
        }

        @Mutation("restoreAllUsers")
        @Description("Restore all soft-deleted users")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<UserDto.ApiResponseUserAll> restoreAllUsers() {
                return userService.restoreAllUser();
        }

        @Mutation("deleteAllUsersPermanent")
        @Description("Permanently delete all soft-deleted users")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<UserDto.ApiResponseUserAll> deleteAllUsersPermanent() {
                return userService.deleteAllUserPermanent();
        }
}