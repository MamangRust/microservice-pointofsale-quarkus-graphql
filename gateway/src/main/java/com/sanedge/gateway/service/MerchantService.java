package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.MerchantDto;
import io.smallrye.mutiny.Uni;

public interface MerchantService {
    Uni<MerchantDto.ApiResponsePaginationMerchant> listMerchants(int page, int size, String search);
    Uni<MerchantDto.ApiResponsePaginationMerchantDeleteAt> activeMerchants(int page, int size, String search);
    Uni<MerchantDto.ApiResponsePaginationMerchantDeleteAt> trashedMerchants(int page, int size, String search);
    Uni<MerchantDto.ApiResponseMerchant> getMerchant(int id);
    Uni<MerchantDto.ApiResponseMerchant> getMerchantByApiKey(String apiKey);
    Uni<MerchantDto.ApiResponsesMerchant> getMerchantsByUserId(int userId);
    Uni<MerchantDto.ApiResponseMerchant> createMerchant(MerchantDto.CreateMerchantRequest body);
    Uni<MerchantDto.ApiResponseMerchant> updateMerchant(int id, MerchantDto.UpdateMerchantRequest body);
    Uni<MerchantDto.ApiResponseMerchantDeleteAt> deleteMerchant(int id);
    Uni<MerchantDto.ApiResponseMerchantDeleteAt> restoreMerchant(int id);
    Uni<MerchantDto.ApiResponseMerchantDelete> deleteMerchantPermanent(int id);
    Uni<MerchantDto.ApiResponseMerchantAll> restoreAllMerchants();
    Uni<MerchantDto.ApiResponseMerchantAll> deleteAllMerchantsPermanent();
}
