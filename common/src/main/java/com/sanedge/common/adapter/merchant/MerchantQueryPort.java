package com.sanedge.common.adapter.merchant;

import com.sanedge.common.adapter.model.Merchant;

import io.smallrye.mutiny.Uni;

/**
 * Port for the merchant query service, replacing direct
 * {@code @GrpcClient("merchant")} usage in consumer services.
 */
public interface MerchantQueryPort {

    Uni<Merchant> findByMerchantId(int merchantId);
}
