package com.sanedge.common.adapter.cashier;

import com.sanedge.common.adapter.model.Cashier;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.cashier.Cashier.FindByIdCashierRequest;
import pb.cashier.Cashier.CashierResponse;
import pb.cashier.CashierService;

@ApplicationScoped
public class CashierAdapter implements CashierQueryPort {

    @GrpcClient("cashier")
    CashierService query;

    @Override
    public Uni<Cashier> findById(int cashierId) {
        return query.findById(FindByIdCashierRequest.newBuilder().setId(cashierId).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Cashier not found: " + cashierId);
                    }
                    return toCashier(resp.getData());
                });
    }

    private static Cashier toCashier(CashierResponse c) {
        if (c == null) {
            return null;
        }
        return new Cashier(c.getId(), c.getMerchantId(), c.getName(),
                ProtoTime.parse(c.getCreatedAt()), ProtoTime.parse(c.getUpdatedAt()));
    }
}
