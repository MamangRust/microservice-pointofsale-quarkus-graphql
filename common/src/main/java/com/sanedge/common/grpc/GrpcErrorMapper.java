package com.sanedge.common.grpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Centralizes the gRPC handler "safety net" conversion of unexpected failures.
 *
 * <p>Handlers previously converted every failure with
 * {@code Status.INTERNAL.withDescription(e.getMessage())}, which leaked
 * internal exception messages (SQL, stack context, host names) to the gRPC
 * client — and from there into HTTP 500 bodies at the gateway. This helper
 * logs the full cause server-side and returns a generic description.
 */
public final class GrpcErrorMapper {

    private static final Logger log = LoggerFactory.getLogger(GrpcErrorMapper.class);

    private static final String INTERNAL_DESCRIPTION = "An internal server error occurred";
    private static final String NOT_FOUND_DESCRIPTION = "Resource not found";

    private GrpcErrorMapper() {
    }

    /**
     * Converts a failure into the {@link StatusRuntimeException} sent over gRPC:
     *
     * <ul>
     *   <li>{@link StatusRuntimeException} (e.g. from a downstream gRPC call or
     *       chaos injection) → passed through unchanged.</li>
     *   <li>{@link jakarta.ws.rs.NotFoundException} → {@code NOT_FOUND} with a
     *       generic description.</li>
     *   <li>Anything else → logged server-side and converted to
     *       {@code INTERNAL} with a generic description (no message leak).</li>
     * </ul>
     */
    public static StatusRuntimeException toStatusRuntimeException(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException sre) {
            return sre;
        }
        if (throwable instanceof jakarta.ws.rs.NotFoundException nfe) {
            // Domain message (e.g. "Role not found with id: 5") is written by
            // the service itself and safe to surface; internal details do not
            // reach this exception type.
            String description = nfe.getMessage() != null ? nfe.getMessage() : NOT_FOUND_DESCRIPTION;
            return Status.NOT_FOUND.withDescription(description).asRuntimeException();
        }
        log.error("Unhandled gRPC failure", throwable);
        return Status.INTERNAL.withDescription(INTERNAL_DESCRIPTION).asRuntimeException();
    }
}
