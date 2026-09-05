package com.sanedge.role.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.role.domain.requests.FindAllRoles;
import com.sanedge.role.domain.response.RoleResponse;
import com.sanedge.role.domain.response.RoleResponseDeleteAt;
import com.sanedge.role.entity.Role;
import com.sanedge.role.repository.RoleRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class RoleQueryServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private RoleQueryServiceImpl roleQueryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        roleQueryService = new RoleQueryServiceImpl(
                roleRepository,
                redisService,
                objectMapper,
                tracingMetrics);

        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics)
                .traceAndMeasure(
                        anyString(),
                        anyString(),
                        any(Attributes.class),
                        any());
    }

    private Role createMockRole(Long id, String roleName) {
        Role role = new Role();
        role.id = id;
        role.setRoleName(roleName);
        return role;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize in test helper", e);
        }
    }

    @Test
    void findById_cacheHit_returnsCachedRoleWithoutHittingDb() {
        Role role = createMockRole(1L, "AdminRole");
        RoleResponse roleResponse = RoleResponse.from(role);
        String cachedJson = toJson(roleResponse);

        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(cachedJson));

        ApiResponse<RoleResponse> response = roleQueryService.findById(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role found");
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getName()).isEqualTo("AdminRole");

        verify(roleRepository, never()).findById(anyLong());
    }

    @Test
    void findById_cacheMiss_fetchesFromDbAndSavesToCache() {
        Role role = createMockRole(2L, "EditorRole");

        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(role));
        when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<RoleResponse> response = roleQueryService.findById(2L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role found");
        assertThat(response.data().getName()).isEqualTo("EditorRole");

        verify(roleRepository).findById(anyLong());
        verify(redisService).setReactive(anyString(), anyString());
    }

    @Test
    void findById_cacheMiss_roleNotFound_returnsErrorResponse() {
        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findById(anyLong())).thenReturn(Uni.createFrom().nullItem());

        ApiResponse<RoleResponse> response = roleQueryService.findById(999L).await().indefinitely();

        assertThat(response.status()).isEqualTo("error");
        assertThat(response.data()).isNull();

        verify(redisService, never()).setReactive(anyString(), anyString());
    }

    @Test
    void findByName_cacheHit_returnsCachedRoleWithoutHittingDb() {
        Role role = createMockRole(1L, "Admin");
        RoleResponse roleResponse = RoleResponse.from(role);
        String cachedJson = toJson(roleResponse);

        when(redisService.getReactive("role:name:Admin")).thenReturn(Uni.createFrom().item(cachedJson));

        ApiResponse<RoleResponse> response = roleQueryService.findByName("Admin").await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role found");
        assertThat(response.data().getName()).isEqualTo("Admin");

        verify(roleRepository, never()).findByRoleName(anyString());
    }

    @Test
    void findByName_cacheMiss_fetchesFromDbAndSavesToCache() {
        Role role = createMockRole(2L, "Editor");

        when(redisService.getReactive("role:name:Editor")).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findByRoleName(anyString())).thenReturn(Uni.createFrom().item(role));
        when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<RoleResponse> response = roleQueryService.findByName("Editor").await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role found");
        assertThat(response.data().getName()).isEqualTo("Editor");

        verify(roleRepository).findByRoleName("Editor");
        verify(redisService).setReactive(anyString(), anyString());
    }

    @Test
    void findByName_cacheMiss_roleNotFound_returnsErrorResponse() {
        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findByRoleName(anyString())).thenReturn(Uni.createFrom().nullItem());

        ApiResponse<RoleResponse> response = roleQueryService.findByName("NonExistent").await().indefinitely();

        assertThat(response.status()).isEqualTo("error");
        assertThat(response.data()).isNull();

        verify(redisService, never()).setReactive(anyString(), anyString());
    }

    @Test
    void findByUserId_cacheHit_returnsCachedRolesWithoutHittingDb() {
        RoleResponse res1 = RoleResponse.from(createMockRole(1L, "Admin"));
        RoleResponse res2 = RoleResponse.from(createMockRole(2L, "Editor"));
        List<RoleResponse> cachedList = List.of(res1, res2);
        String cachedJson = toJson(cachedList);

        when(redisService.getReactive("roles:user:1")).thenReturn(Uni.createFrom().item(cachedJson));

        ApiResponse<List<RoleResponse>> response = roleQueryService.findByUserId(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Roles found");
        assertThat(response.data()).hasSize(2);
        assertThat(response.data().get(0).getName()).isEqualTo("Admin");

        verify(roleRepository, never()).findUserRoles(anyLong());
    }

    @Test
    void findByUserId_cacheMiss_fetchesFromDbAndCachesResult() {
        Role role1 = createMockRole(1L, "Role1");
        Role role2 = createMockRole(2L, "Role2");
        List<Role> roles = List.of(role1, role2);

        when(redisService.getReactive("roles:user:2")).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findUserRoles(2L)).thenReturn(Uni.createFrom().item(roles));
        when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<List<RoleResponse>> response = roleQueryService.findByUserId(2L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Roles found");
        assertThat(response.data()).hasSize(2);
        assertThat(response.data().get(0).getName()).isEqualTo("Role1");

        verify(roleRepository).findUserRoles(2L);
        verify(redisService).setReactive(anyString(), anyString());
    }

    @Test
    void findByUserId_emptyRoleList_returnsEmptyArray() {
        when(redisService.getReactive("roles:user:999")).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findUserRoles(999L)).thenReturn(Uni.createFrom().item(List.of()));
        when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<List<RoleResponse>> response = roleQueryService.findByUserId(999L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.data()).isEmpty();
        verify(roleRepository).findUserRoles(999L);
        verify(redisService).setReactive(anyString(), anyString());
    }

    @Test
    void findAllPaginated_cacheHit_returnsCachedList() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);
        String cacheKey = "roles:all:1:10:null";

        RoleResponse res1 = RoleResponse.from(createMockRole(1L, "Role1"));
        RoleResponse res2 = RoleResponse.from(createMockRole(2L, "Role2"));
        ApiResponsePagination<List<RoleResponse>> mockResponse = new ApiResponsePagination<>(
                "success", "Roles retrieved successfully", List.of(res1, res2), null);
        String cachedJson = toJson(mockResponse);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().item(cachedJson));

        ApiResponsePagination<List<RoleResponse>> response = roleQueryService.findAllPaginated(req).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.data()).hasSize(2);
        assertThat(response.data().get(0).getName()).isEqualTo("Role1");
        verify(roleRepository, never()).findRoles(any(FindAllRoles.class));
    }

    @Test
    void findAllPaginated_cacheMiss_fetchesFromDbAndCachesResult() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);
        String cacheKey = "roles:all:1:10:null";

        Role role1 = createMockRole(1L, "Role1");
        Role role2 = createMockRole(2L, "Role2");
        PagedResult<Role> pagedResult = new PagedResult<>(List.of(role1, role2), 2);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findRoles(req)).thenReturn(Uni.createFrom().item(pagedResult));
        when(redisService.setWithExpirationReactive(eq(cacheKey), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponsePagination<List<RoleResponse>> response = roleQueryService.findAllPaginated(req).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Roles retrieved successfully");
        assertThat(response.data()).hasSize(2);
        assertThat(response.pagination()).isNotNull();
        assertThat(response.pagination().totalRecords()).isEqualTo(2);
        assertThat(response.pagination().totalPages()).isEqualTo(1);

        verify(roleRepository).findRoles(req);
        verify(redisService).setWithExpirationReactive(anyString(), anyString(), eq(300L));
    }

    @Test
    void findActivePaginated_cacheHit_returnsCachedList() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);
        String cacheKey = "roles:active:1:10:null";

        RoleResponseDeleteAt res1 = RoleResponseDeleteAt.from(createMockRole(1L, "ActiveRole"));
        ApiResponsePagination<List<RoleResponseDeleteAt>> mockResponse = new ApiResponsePagination<>(
                "success", "Active roles retrieved successfully", List.of(res1), null);
        String cachedJson = toJson(mockResponse);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().item(cachedJson));

        ApiResponsePagination<List<RoleResponseDeleteAt>> response = roleQueryService.findActivePaginated(req).await().indefinitely();

        assertThat(response.data()).hasSize(1);
        verify(roleRepository, never()).findActiveRoles(any(FindAllRoles.class));
    }

    @Test
    void findActivePaginated_cacheMiss_fetchesFromDbAndCachesResult() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);
        String cacheKey = "roles:active:1:10:null";

        Role activeRole = createMockRole(1L, "ActiveRole");
        PagedResult<Role> pagedResult = new PagedResult<>(List.of(activeRole), 1);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findActiveRoles(req)).thenReturn(Uni.createFrom().item(pagedResult));
        when(redisService.setWithExpirationReactive(eq(cacheKey), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponsePagination<List<RoleResponseDeleteAt>> response = roleQueryService.findActivePaginated(req).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Active roles retrieved successfully");
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).getName()).isEqualTo("ActiveRole");

        verify(roleRepository).findActiveRoles(req);
        verify(redisService).setWithExpirationReactive(anyString(), anyString(), eq(300L));
    }

    @Test
    void findTrashedPaginated_cacheHit_returnsCachedList() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);
        String cacheKey = "roles:trashed:1:10:null";

        RoleResponseDeleteAt res1 = RoleResponseDeleteAt.from(createMockRole(2L, "TrashedRole"));
        ApiResponsePagination<List<RoleResponseDeleteAt>> mockResponse = new ApiResponsePagination<>(
                "success", "Trashed roles retrieved successfully", List.of(res1), null);
        String cachedJson = toJson(mockResponse);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().item(cachedJson));

        ApiResponsePagination<List<RoleResponseDeleteAt>> response = roleQueryService.findTrashedPaginated(req).await().indefinitely();

        assertThat(response.data()).hasSize(1);
        verify(roleRepository, never()).findTrashedRoles(any(FindAllRoles.class));
    }

    @Test
    void findTrashedPaginated_cacheMiss_fetchesFromDbAndCachesResult() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);
        String cacheKey = "roles:trashed:1:10:null";

        Role trashedRole = createMockRole(2L, "TrashedRole");
        PagedResult<Role> pagedResult = new PagedResult<>(List.of(trashedRole), 1);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findTrashedRoles(req)).thenReturn(Uni.createFrom().item(pagedResult));
        when(redisService.setWithExpirationReactive(eq(cacheKey), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponsePagination<List<RoleResponseDeleteAt>> response = roleQueryService.findTrashedPaginated(req).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Trashed roles retrieved successfully");
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).getName()).isEqualTo("TrashedRole");

        verify(roleRepository).findTrashedRoles(req);
        verify(redisService).setWithExpirationReactive(anyString(), anyString(), eq(300L));
    }

    @Test
    void findAllPaginated_calculatesTotalPagesCorrectlyWhenNotPerfectlyDivisible() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(1);
        req.setPageSize(2);
        req.setSearch(null);
        String cacheKey = "roles:all:1:2:null";

        List<Role> roles = List.of(createMockRole(1L, "r1"), createMockRole(2L, "r2"));
        PagedResult<Role> pagedResult = new PagedResult<>(roles, 5);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findRoles(req)).thenReturn(Uni.createFrom().item(pagedResult));
        when(redisService.setWithExpirationReactive(eq(cacheKey), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponsePagination<List<RoleResponse>> response = roleQueryService.findAllPaginated(req).await().indefinitely();

        assertThat(response.pagination()).isNotNull();
        assertThat(response.pagination().totalPages()).isEqualTo(3);
        assertThat(response.pagination().totalRecords()).isEqualTo(5);
        assertThat(response.pagination().pageSize()).isEqualTo(2);
        assertThat(response.pagination().currentPage()).isEqualTo(1);
    }
}
