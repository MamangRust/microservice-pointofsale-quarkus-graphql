package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.MerchantDocumentDto;
import io.smallrye.mutiny.Uni;

public interface MerchantDocumentService {
    Uni<MerchantDocumentDto.ApiResponsePaginationMerchantDocument> listMerchantDocuments(int page, int size, String search);
    Uni<MerchantDocumentDto.ApiResponsePaginationMerchantDocumentDeleteAt> activeMerchantDocuments(int page, int size, String search);
    Uni<MerchantDocumentDto.ApiResponsePaginationMerchantDocumentDeleteAt> trashedMerchantDocuments(int page, int size, String search);
    Uni<MerchantDocumentDto.ApiResponseMerchantDocument> getMerchantDocument(int id);
    Uni<MerchantDocumentDto.ApiResponseMerchantDocument> createMerchantDocument(MerchantDocumentDto.CreateMerchantDocumentRequest body);
    Uni<MerchantDocumentDto.ApiResponseMerchantDocument> updateMerchantDocument(int id, MerchantDocumentDto.UpdateMerchantDocumentRequest body);
    Uni<MerchantDocumentDto.ApiResponseMerchantDocumentDeleteAt> deleteMerchantDocument(int id);
    Uni<MerchantDocumentDto.ApiResponseMerchantDocumentDeleteAt> restoreMerchantDocument(int id);
    Uni<MerchantDocumentDto.ApiResponseMerchantDocumentDelete> deleteMerchantDocumentPermanent(int id);
    Uni<MerchantDocumentDto.ApiResponseMerchantDocumentAll> restoreAllMerchantDocuments();
    Uni<MerchantDocumentDto.ApiResponseMerchantDocumentAll> deleteAllMerchantDocumentsPermanent();
}
