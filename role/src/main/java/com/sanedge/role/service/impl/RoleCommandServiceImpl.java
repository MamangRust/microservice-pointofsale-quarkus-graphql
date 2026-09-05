package com.sanedge.role.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.role.domain.requests.CreateRoleRequest;
import com.sanedge.role.domain.requests.UpdateRoleRequest;
import com.sanedge.role.domain.response.RoleResponse;
import com.sanedge.role.domain.response.RoleResponseDeleteAt;
import com.sanedge.role.domain.response.UserRoleResponse;
import com.sanedge.role.entity.Role;
import com.sanedge.role.repository.RoleRepository;
import com.sanedge.role.repository.UserRoleRepository;
import com.sanedge.role.service.RoleCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class RoleCommandServiceImpl implements RoleCommandService {
        private static final Logger logger = LoggerFactory.getLogger(RoleCommandServiceImpl.class);

        private final RoleRepository roleRepository;
        private final UserRoleRepository userRoleRepository;
        private final RedisService redisService;
        private final TracingMetrics tracingMetrics;

        @Inject
        public RoleCommandServiceImpl(RoleRepository roleRepository, UserRoleRepository userRoleRepository,
                        RedisService redisService, TracingMetrics tracingMetrics) {
                this.roleRepository = roleRepository;
                this.userRoleRepository = userRoleRepository;
                this.redisService = redisService;
                this.tracingMetrics = tracingMetrics;
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<RoleResponse>> create(CreateRoleRequest request) {
                logger.info("Creating new role with name: {}", request.getName());
                Attributes attrs = Attributes.builder()
                                .put("role.name", request.getName())
                                .build();

                return runTraced("createRole", "create_role", attrs,
                                () -> roleRepository.findByRoleName(request.getName())
                                                .chain(existingRole -> {
                                                        if (existingRole != null) {
                                                                logger.error("Role creation failed - role already exists: {}",
                                                                                request.getName());
                                                                throw new ResourceAlreadyExistsException(
                                                                                "Role with name '" + request.getName()
                                                                                                + "' already exists");
                                                        }

                                                        Role newRole = new Role();
                                                        newRole.setRoleName(request.getName());
                                                        return roleRepository.persist(newRole)
                                                                        .map(v -> {
                                                                                RoleResponse roleResponse = RoleResponse
                                                                                                .from(newRole);
                                                                                logger.info("Successfully created role with id: {} and name: {}",
                                                                                                newRole.id,
                                                                                                newRole.getRoleName());
                                                                                return ApiResponse.success(
                                                                                                "Role created successfully",
                                                                                                roleResponse);
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to create role with name: {}",
                                                                        request.getName(), e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to create role: " + e.getMessage(),
                                                                        (RoleResponse) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<RoleResponse>> update(UpdateRoleRequest request) {
                logger.info("Updating role with id: {} to new name: {}", request.getRoleId(), request.getName());
                Attributes attrs = Attributes.builder()
                                .put("role.id", request.getRoleId().toString())
                                .put("role.new_name", request.getName())
                                .build();

                return runTraced("updateRole", "update_role", attrs,
                                () -> roleRepository.findById(request.getRoleId().longValue())
                                                .chain(existingRole -> {
                                                        if (existingRole == null) {
                                                                logger.error("Role update failed - role not found with id: {}",
                                                                                request.getRoleId());
                                                                throw new NotFoundException("Role not found with id: "
                                                                                + request.getRoleId());
                                                        }

                                                        Uni<Role> updateFlow;
                                                        if (!existingRole.getRoleName().equals(request.getName())) {
                                                                updateFlow = roleRepository
                                                                                .findByRoleName(request.getName())
                                                                                .chain(duplicateRole -> {
                                                                                        if (duplicateRole != null) {
                                                                                                logger.error("Role update failed - new name '{}' already exists for another role",
                                                                                                                request.getName());
                                                                                                throw new ResourceAlreadyExistsException(
                                                                                                                "Role with name '"
                                                                                                                                + request.getName()
                                                                                                                                + "' already exists");
                                                                                        }
                                                                                        existingRole.setRoleName(request
                                                                                                        .getName());
                                                                                        return roleRepository.persist(
                                                                                                        existingRole)
                                                                                                        .map(v -> existingRole);
                                                                                });
                                                        } else {
                                                                updateFlow = Uni.createFrom().item(existingRole);
                                                        }

                                                        return updateFlow.chain(updatedRole -> {
                                                                RoleResponse roleResponse = RoleResponse
                                                                                .from(updatedRole);
                                                                String cacheKey = "role:" + request.getRoleId();

                                                                return redisService.deleteReactive(cacheKey)
                                                                                .map(v -> {
                                                                                        logger.info("Invalidated cache for key: {}",
                                                                                                        cacheKey);
                                                                                        logger.info("Successfully updated role with id: {}",
                                                                                                        request.getRoleId());
                                                                                        return ApiResponse.success(
                                                                                                        "Role updated successfully",
                                                                                                        roleResponse);
                                                                                });
                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to update role with id: {}",
                                                                        request.getRoleId(), e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to update role: " + e.getMessage(),
                                                                        (RoleResponse) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<RoleResponseDeleteAt>> trash(Long id) {
                logger.info("Trashing role with id: {}", id);
                Attributes attrs = Attributes.builder()
                                .put("role.id", id.toString())
                                .build();

                return runTraced("trashRole", "trash_role", attrs,
                                () -> roleRepository.trash(id)
                                                .chain(trashedRole -> {
                                                        if (trashedRole == null) {
                                                                logger.error("Role trash failed - role not found with id: {}",
                                                                                id);
                                                                throw new NotFoundException(
                                                                                "Role not found with id: " + id);
                                                        }

                                                        RoleResponseDeleteAt roleResponseDeleteAt = RoleResponseDeleteAt
                                                                        .from(trashedRole);
                                                        String cacheKey = "role:" + id;

                                                        return redisService.deleteReactive(cacheKey)
                                                                        .map(v -> {
                                                                                logger.info("Invalidated cache for key: {}",
                                                                                                cacheKey);
                                                                                logger.info("Successfully trashed role with id: {}",
                                                                                                id);
                                                                                return ApiResponse.success(
                                                                                                "Role trashed successfully",
                                                                                                roleResponseDeleteAt);
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to trash role with id: {}", id, e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to trash role: " + e.getMessage(),
                                                                        (RoleResponseDeleteAt) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<RoleResponseDeleteAt>> restore(Long id) {
                logger.info("Restoring role with id: {}", id);
                Attributes attrs = Attributes.builder()
                                .put("role.id", id.toString())
                                .build();

                return runTraced("restoreRole", "restore_role", attrs,
                                () -> roleRepository.restore(id)
                                                .chain(restoredRole -> {
                                                        if (restoredRole == null) {
                                                                logger.error("Role restore failed - role not found with id: {}",
                                                                                id);
                                                                throw new NotFoundException(
                                                                                "Restored role not found with id: "
                                                                                                + id);
                                                        }

                                                        RoleResponseDeleteAt roleResponseDeleteAt = RoleResponseDeleteAt
                                                                        .from(restoredRole);
                                                        String cacheKey = "role:" + id;

                                                        return redisService.deleteReactive(cacheKey)
                                                                        .map(v -> {
                                                                                logger.info("Invalidated cache for key: {}",
                                                                                                cacheKey);
                                                                                logger.info("Successfully restored role with id: {}",
                                                                                                id);
                                                                                return ApiResponse.success(
                                                                                                "Role restored successfully",
                                                                                                roleResponseDeleteAt);
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to restore role with id: {}", id, e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to restore role: " + e.getMessage(),
                                                                        (RoleResponseDeleteAt) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> deletePermanent(Long id) {
                logger.info("Permanently deleting role with id: {}", id);
                Attributes attrs = Attributes.builder()
                                .put("role.id", id.toString())
                                .build();

                return runTraced("deleteRolePermanent", "delete_role_permanent", attrs,
                                () -> roleRepository.findById(id)
                                                .chain(roleToDelete -> {
                                                        if (roleToDelete == null) {
                                                                logger.error("Permanent delete failed - role not found with id: {}",
                                                                                id);
                                                                throw new NotFoundException(
                                                                                "Role not found with id: " + id);
                                                        }

                                                        return roleRepository.deletePermanent(id)
                                                                        .chain(v -> {
                                                                                String cacheKey = "role:" + id;
                                                                                return redisService.deleteReactive(
                                                                                                cacheKey)
                                                                                                .map(v2 -> {
                                                                                                        logger.info("Invalidated cache for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully permanently deleted role with id: {}",
                                                                                                                        id);
                                                                                                        return ApiResponse
                                                                                                                        .success("Role deleted permanently");
                                                                                                });
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to permanently delete role with id: {}",
                                                                        id, e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to delete role permanently: "
                                                                                        + e.getMessage(),
                                                                        null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> restoreAllTrashedRoles() {
                logger.info("Restoring all trashed roles");

                return runTraced("restoreAllTrashedRoles", "restore_all_trashed_roles", Attributes.empty(),
                                () -> roleRepository.restoreAllDeleted()
                                                .map(v -> {
                                                        logger.info("Successfully restored all trashed roles");
                                                        return ApiResponse.success(
                                                                        "All trashed roles have been restored successfully");
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to restore all trashed roles", e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to restore all trashed roles: "
                                                                                        + e.getMessage(),
                                                                        null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> deleteAllTrashedRoles() {
                logger.info("Permanently deleting all trashed roles");

                return runTraced("deleteAllTrashedRoles", "delete_all_trashed_roles", Attributes.empty(),
                                () -> roleRepository.deleteAllDeleted()
                                                .map(v -> {
                                                        if (!v) {
                                                                throw new ResourceNotFoundException(
                                                                                "No roles found in trash");
                                                        }

                                                        logger.info("Successfully deleted all trashed roles");
                                                        return ApiResponse.success(
                                                                        "All trashed roles have been deleted permanently");
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<UserRoleResponse>> assignRoleToUser(Long userId, Long roleId) {
                logger.info("Assigning role with id: {} to user with id: {}", roleId, userId);
                Attributes attrs = Attributes.builder()
                                .put("user.id", userId.toString())
                                .put("role.id", roleId.toString())
                                .build();

                return runTraced("assignRoleToUser", "assign_role_to_user", attrs,
                                () -> userRoleRepository.assignRole(userId, roleId)
                                                .map(userRole -> {
                                                        logger.info("Successfully assigned role with id: {} to user with id: {}",
                                                                        roleId, userId);
                                                        return ApiResponse.success("Role assigned to user successfully",
                                                                        UserRoleResponse.from(userRole));
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to assign role with id: {} to user with id: {}",
                                                                        roleId, userId, e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to assign role: " + e.getMessage(),
                                                                        (UserRoleResponse) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> removeRoleFromUser(Long userId, Long roleId) {
                logger.info("Removing role with id: {} from user with id: {}", roleId, userId);
                Attributes attrs = Attributes.builder()
                                .put("user.id", userId.toString())
                                .put("role.id", roleId.toString())
                                .build();

                return runTraced("removeRoleFromUser", "remove_role_from_user", attrs,
                                () -> userRoleRepository.removeRole(userId, roleId)
                                                .map(removed -> {
                                                        if (!removed) {
                                                                throw new NotFoundException(
                                                                                "UserRole association not found");
                                                        }
                                                        logger.info("Successfully removed role with id: {} from user with id: {}",
                                                                        roleId, userId);
                                                        return ApiResponse.<Void>success(
                                                                        "Role removed from user successfully");
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to remove role with id: {} from user with id: {}",
                                                                        roleId, userId, e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to remove role: " + e.getMessage(),
                                                                        null);
                                                }));
        }

        private <T> Uni<T> runTraced(String operationName, String method, Attributes attributes,
                        java.util.function.Supplier<Uni<T>> supplier) {
                return tracingMetrics.traceAndMeasure(operationName, method, attributes, supplier);
        }
}