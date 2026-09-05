package com.sanedge.gateway.resource;

import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.sanedge.gateway.dto.MerchantDto;
import com.sanedge.gateway.service.MerchantService;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

@GraphQLApi
public class MerchantResource {

        @Inject
        MerchantService merchantService;

        @Query("merchants")
        @Description("List all merchants")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<MerchantDto.ApiResponsePaginationMerchant> listMerchants(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return merchantService.listMerchants(page, size, search);
        }

        @Query("activeMerchants")
        @Description("List active merchants")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<MerchantDto.ApiResponsePaginationMerchantDeleteAt> activeMerchants(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return merchantService.activeMerchants(page, size, search);
        }

        @Query("trashedMerchants")
        @Description("List trashed merchants")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<MerchantDto.ApiResponsePaginationMerchantDeleteAt> trashedMerchants(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return merchantService.trashedMerchants(page, size, search);
        }

        @Query("merchant")
        @Description("Get merchant by ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<MerchantDto.ApiResponseMerchant> getMerchant(@Name("id") int id) {
                return merchantService.getMerchant(id);
        }

        @Query("merchantByApiKey")
        @Description("Get merchant by API key")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<MerchantDto.ApiResponseMerchant> getMerchantByApiKey(@Name("apiKey") String apiKey) {
                return merchantService.getMerchantByApiKey(apiKey);
        }

        @Query("merchantsByUserId")
        @Description("Get merchants by user ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<MerchantDto.ApiResponsesMerchant> getMerchantsByUserId(@Name("userId") int userId) {
                return merchantService.getMerchantsByUserId(userId);
        }

        @Mutation("createMerchant")
        @Description("Create a new merchant")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<MerchantDto.ApiResponseMerchant> createMerchant(@Name("body") MerchantDto.CreateMerchantRequest body) {
                return merchantService.createMerchant(body);
        }

        @Mutation("updateMerchant")
        @Description("Update merchant")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<MerchantDto.ApiResponseMerchant> updateMerchant(@Name("id") int id,
                        @Name("body") MerchantDto.UpdateMerchantRequest body) {
                return merchantService.updateMerchant(id, body);
        }

        @Mutation("deleteMerchant")
        @Description("Soft-delete a merchant")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<MerchantDto.ApiResponseMerchantDeleteAt> deleteMerchant(@Name("id") int id) {
                return merchantService.deleteMerchant(id);
        }

        @Mutation("restoreMerchant")
        @Description("Restore a soft-deleted merchant")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<MerchantDto.ApiResponseMerchantDeleteAt> restoreMerchant(@Name("id") int id) {
                return merchantService.restoreMerchant(id);
        }

        @Mutation("deleteMerchantPermanent")
        @Description("Permanently delete a merchant")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<MerchantDto.ApiResponseMerchantDelete> deleteMerchantPermanent(@Name("id") int id) {
                return merchantService.deleteMerchantPermanent(id);
        }

        @Mutation("restoreAllMerchants")
        @Description("Restore all soft-deleted merchants")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<MerchantDto.ApiResponseMerchantAll> restoreAllMerchants() {
                return merchantService.restoreAllMerchants();
        }

        @Mutation("deleteAllMerchantsPermanent")
        @Description("Permanently delete all soft-deleted merchants")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<MerchantDto.ApiResponseMerchantAll> deleteAllMerchantsPermanent() {
                return merchantService.deleteAllMerchantsPermanent();
        }
}
