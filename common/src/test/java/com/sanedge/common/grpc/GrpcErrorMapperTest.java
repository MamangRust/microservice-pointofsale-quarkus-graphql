package com.sanedge.common.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@DisplayName("GrpcErrorMapper (Fase 15 — no message leak in handler safety net)")
class GrpcErrorMapperTest {

    @Test
    @DisplayName("passthrough StatusRuntimeException unchanged")
    void passthrough_statusRuntimeException() {
        StatusRuntimeException original = Status.UNAVAILABLE.withDescription("downstream").asRuntimeException();

        StatusRuntimeException result = GrpcErrorMapper.toStatusRuntimeException(original);

        assertSame(original, result);
        assertEquals(Status.Code.UNAVAILABLE, result.getStatus().getCode());
    }

    @Test
    @DisplayName("jakarta NotFoundException → NOT_FOUND with domain message")
    void notFound_mapsToNotFound_withDomainMessage() {
        StatusRuntimeException result = GrpcErrorMapper
                .toStatusRuntimeException(new jakarta.ws.rs.NotFoundException("Role not found with id: 5"));

        assertEquals(Status.Code.NOT_FOUND, result.getStatus().getCode());
        assertEquals("Role not found with id: 5", result.getStatus().getDescription());
    }

    @Test
    @DisplayName("jakarta NotFoundException without message → NOT_FOUND (JAX-RS default message)")
    void notFound_withoutMessage_fallsBackToGeneric() {
        StatusRuntimeException result = GrpcErrorMapper.toStatusRuntimeException(new jakarta.ws.rs.NotFoundException());

        assertEquals(Status.Code.NOT_FOUND, result.getStatus().getCode());
        // jakarta.ws.rs.NotFoundException() provides this safe default; no internal detail leaks.
        assertEquals("HTTP 404 Not Found", result.getStatus().getDescription());
    }

    @Test
    @DisplayName("unexpected failure → INTERNAL with generic description (no leak)")
    void unexpectedFailure_mapsToInternal_withGenericDescription() {
        StatusRuntimeException result = GrpcErrorMapper
                .toStatusRuntimeException(new RuntimeException("connect to localhost:5432 failed: password auth"));

        assertEquals(Status.Code.INTERNAL, result.getStatus().getCode());
        // Internal details must NOT leak to the gRPC client.
        assertEquals("An internal server error occurred", result.getStatus().getDescription());
        assertFalse(result.getStatus().getDescription().contains("5432"));
        assertFalse(result.getStatus().getDescription().contains("password"));
    }

    @Test
    @DisplayName("NullPointerException → INTERNAL generic")
    void nullPointerFailure_mapsToInternal_generic() {
        StatusRuntimeException result = GrpcErrorMapper.toStatusRuntimeException(new NullPointerException("boom"));

        assertEquals(Status.Code.INTERNAL, result.getStatus().getCode());
        assertEquals("An internal server error occurred", result.getStatus().getDescription());
        assertTrue(result.getStatus().getDescription().contains("internal"));
    }
}
