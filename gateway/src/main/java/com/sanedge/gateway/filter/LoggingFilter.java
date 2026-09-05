package com.sanedge.gateway.filter;

import io.quarkus.logging.Log;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;

@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String START_TIME = "start-time";
    private static final String CORRELATION_ID = "correlation-id";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        long startTime = System.currentTimeMillis();
        requestContext.setProperty(START_TIME, startTime);

        String correlationId = requestContext.getHeaderString("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        requestContext.setProperty(CORRELATION_ID, correlationId);
        MDC.put("CorrelationId", correlationId);

        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();
        Log.infof("Incoming request: %s %s [Correlation ID: %s]", method, path, correlationId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        Long startTime = (Long) requestContext.getProperty(START_TIME);
        String correlationId = (String) requestContext.getProperty(CORRELATION_ID);
        
        long duration = startTime != null ? (System.currentTimeMillis() - startTime) : 0;
        int status = responseContext.getStatus();
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();

        Log.infof("Outgoing response: %s %s -> Status: %d [Duration: %dms, Correlation ID: %s]",
                method, path, status, duration, correlationId);

        responseContext.getHeaders().putSingle("X-Correlation-ID", correlationId);
        MDC.clear();
    }
}
