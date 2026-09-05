package com.sanedge.common.exception.mapper;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.TooManyRequestsException;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooManyRequestsExceptionMapperTest {

    private final TooManyRequestsExceptionMapper mapper = new TooManyRequestsExceptionMapper();

    @Test
    void mapsTooManyRequestsToHttp429() {
        Response response = mapper.toResponse(new TooManyRequestsException("Rate limit exceeded"));

        assertEquals(429, response.getStatus());
    }

    @Test
    void responseBodyCarriesErrorMessage() {
        Response response = mapper.toResponse(new TooManyRequestsException("Too many login attempts"));

        ApiResponse<?> body = (ApiResponse<?>) response.getEntity();
        assertNotNull(body);
        assertEquals("error", body.status());
        assertEquals("Too many login attempts", body.message());
    }

    @Test
    void responseIsApplicationJson() {
        Response response = mapper.toResponse(new TooManyRequestsException("Slow down"));

        assertTrue(response.getMediaType().toString().startsWith("application/json"));
    }
}
