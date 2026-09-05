package com.sanedge.gateway.resource;

import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.sanedge.gateway.dto.MerchantDocumentDto;
import com.sanedge.gateway.service.MerchantDocumentService;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

@GraphQLApi
public class MerchantDocumentResource {

        @Inject
        MerchantDocumentService merchantDocumentService;

        @Query("merchantDocuments")
        @Description("List all merchant documents")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<MerchantDocumentDto.ApiResponsePaginationMerchantDocument> listMerchantDocuments(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return merchantDocumentService.listMerchantDocuments(page, size, search);
        }

        @Query("activeMerchantDocuments")
        @Description("List active merchant documents")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<MerchantDocumentDto.ApiResponsePaginationMerchantDocumentDeleteAt> activeMerchantDocuments(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return merchantDocumentService.activeMerchantDocuments(page, size, search);
        }

        @Query("trashedMerchantDocuments")
        @Description("List trashed merchant documents")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<MerchantDocumentDto.ApiResponsePaginationMerchantDocumentDeleteAt> trashedMerchantDocuments(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return merchantDocumentService.trashedMerchantDocuments(page, size, search);
        }

        @Query("merchantDocument")
        @Description("Get merchant document by ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<MerchantDocumentDto.ApiResponseMerchantDocument> getMerchantDocument(@Name("id") int id) {
                return merchantDocumentService.getMerchantDocument(id);
        }

        @Mutation("createMerchantDocument")
        @Description("Create a new merchant document")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<MerchantDocumentDto.ApiResponseMerchantDocument> createMerchantDocument(
                        @Name("body") MerchantDocumentDto.CreateMerchantDocumentRequest body) {
                return merchantDocumentService.createMerchantDocument(body);
        }

        @Mutation("updateMerchantDocument")
        @Description("Update merchant document")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<MerchantDocumentDto.ApiResponseMerchantDocument> updateMerchantDocument(@Name("id") int id,
                        @Name("body") MerchantDocumentDto.UpdateMerchantDocumentRequest body) {
                return merchantDocumentService.updateMerchantDocument(id, body);
        }

        @Mutation("deleteMerchantDocument")
        @Description("Soft-delete a merchant document")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<MerchantDocumentDto.ApiResponseMerchantDocumentDeleteAt> deleteMerchantDocument(@Name("id") int id) {
                return merchantDocumentService.deleteMerchantDocument(id);
        }

        @Mutation("restoreMerchantDocument")
        @Description("Restore a soft-deleted merchant document")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<MerchantDocumentDto.ApiResponseMerchantDocumentDeleteAt> restoreMerchantDocument(@Name("id") int id) {
                return merchantDocumentService.restoreMerchantDocument(id);
        }

        @Mutation("deleteMerchantDocumentPermanent")
        @Description("Permanently delete a merchant document")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<MerchantDocumentDto.ApiResponseMerchantDocumentDelete> deleteMerchantDocumentPermanent(
                        @Name("id") int id) {
                return merchantDocumentService.deleteMerchantDocumentPermanent(id);
        }

        @Mutation("restoreAllMerchantDocuments")
        @Description("Restore all soft-deleted merchant documents")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<MerchantDocumentDto.ApiResponseMerchantDocumentAll> restoreAllMerchantDocuments() {
                return merchantDocumentService.restoreAllMerchantDocuments();
        }

        @Mutation("deleteAllMerchantDocumentsPermanent")
        @Description("Permanently delete all soft-deleted merchant documents")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<MerchantDocumentDto.ApiResponseMerchantDocumentAll> deleteAllMerchantDocumentsPermanent() {
                return merchantDocumentService.deleteAllMerchantDocumentsPermanent();
        }
}
