package com.sanedge.common.adapter.cashier;

import com.sanedge.common.adapter.model.Cashier;

import io.smallrye.mutiny.Uni;

/**
 * Port for the cashier query service, replacing direct
 * {@code @GrpcClient("cashier")} usage in consumer services.
 */
public interface CashierQueryPort {

    Uni<Cashier> findById(int cashierId);
}
