package com.sanedge.gateway.resource;

import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.sanedge.gateway.dto.RoleDto;
import com.sanedge.gateway.service.RoleService;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

@GraphQLApi
public class RoleResource {

        @Inject
        RoleService roleService;

        @Query("roles")
        @Description("List all roles")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<RoleDto.ApiResponsePaginationRole> listRoles(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return roleService.listRoles(page, size, search);
        }

        @Query("role")
        @Description("Get role by ID")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<RoleDto.ApiResponseRole> getRole(@Name("id") int id) {
                return roleService.getRole(id);
        }

        @Query("roleByName")
        @Description("Get role by Name")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<RoleDto.ApiResponseRole> getRoleByName(@Name("name") String name) {
                return roleService.getRoleByName(name);
        }

        @Query("rolesByUserId")
        @Description("Get roles by User ID")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<RoleDto.ApiResponsesRole> getRolesByUserId(@Name("userId") int userId) {
                return roleService.getRolesByUserId(userId);
        }

        @Query("activeRoles")
        @Description("List active roles")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<RoleDto.ApiResponsePaginationRoleDeleteAt> activeRoles(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return roleService.activeRoles(page, size, search);
        }

        @Query("trashedRoles")
        @Description("List trashed roles")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<RoleDto.ApiResponsePaginationRoleDeleteAt> trashedRoles(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return roleService.trashedRoles(page, size, search);
        }

        @Mutation("createRole")
        @Description("Create a new role")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<RoleDto.ApiResponseRole> createRole(@Name("body") RoleDto.CreateRoleRequest body) {
                return roleService.createRole(body);
        }

        @Mutation("updateRole")
        @Description("Update role")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<RoleDto.ApiResponseRole> updateRole(@Name("id") int id,
                        @Name("body") RoleDto.UpdateRoleRequest body) {
                return roleService.updateRole(id, body);
        }

        @Mutation("deleteRole")
        @Description("Soft-delete a role")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<RoleDto.ApiResponseRoleDeleteAt> deleteRole(@Name("id") int id) {
                return roleService.deleteRole(id);
        }

        @Mutation("restoreRole")
        @Description("Restore a soft-deleted role")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<RoleDto.ApiResponseRoleDeleteAt> restoreRole(@Name("id") int id) {
                return roleService.restoreRole(id);
        }

        @Mutation("deleteRolePermanent")
        @Description("Permanently delete a role")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<RoleDto.ApiResponseRoleDelete> deleteRolePermanent(@Name("id") int id) {
                return roleService.deleteRolePermanent(id);
        }

        @Mutation("restoreAllRoles")
        @Description("Restore all soft-deleted roles")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<RoleDto.ApiResponseRoleAll> restoreAllRoles() {
                return roleService.restoreAllRoles();
        }

        @Mutation("deleteAllRolesPermanent")
        @Description("Permanently delete all soft-deleted roles")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<RoleDto.ApiResponseRoleAll> deleteAllRolesPermanent() {
                return roleService.deleteAllRolesPermanent();
        }

        @Mutation("assignRoleToUser")
        @Description("Assign role to user")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<RoleDto.ApiResponseUserRole> assignRoleToUser(@Name("body") RoleDto.AssignRoleToUserRequest body) {
                return roleService.assignRoleToUser(body);
        }

        @Mutation("removeRoleFromUser")
        @Description("Remove role from user")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<Boolean> removeRoleFromUser(@Name("body") RoleDto.AssignRoleToUserRequest body) {
                return roleService.removeRoleFromUser(body);
        }
}
