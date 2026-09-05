package com.sanedge.gateway.exception;

import com.sanedge.common.domain.response.ApiResponse;
import io.quarkus.logging.Log;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GeneralExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException) {
            WebApplicationException webAppException = (WebApplicationException) exception;
            Response response = webAppException.getResponse();
            
            // If the response already has an entity, return it
            if (response.hasEntity()) {
                return response;
            }
            
            int status = response.getStatus();
            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(ApiResponse.error(webAppException.getMessage()))
                    .build();
        }

        Log.error("An unexpected gateway error occurred", exception);

        ApiResponse<Void> errorResponse = ApiResponse.error("An internal gateway error occurred: " + exception.getMessage());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorResponse)
                .build();
    }
}
