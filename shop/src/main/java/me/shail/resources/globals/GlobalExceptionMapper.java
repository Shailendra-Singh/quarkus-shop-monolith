package me.shail.resources.globals;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import javax.naming.OperationNotSupportedException;
import java.time.Instant;
import java.util.Map;

@SuppressWarnings("unused")
public class GlobalExceptionMapper {

    // Catch EntityNotFoundException and convert it into HTTP 404
    @ServerExceptionMapper(EntityNotFoundException.class)
    public Response handleNotFound(EntityNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", Response.Status.NOT_FOUND.getStatusCode(),
                        "error", "Not Found",
                        "message", exception.getMessage()
                ))
                .build();
    }

    // Catch IllegalStateException and convert it into HTTP 404
    @ServerExceptionMapper(IllegalStateException.class)
    public Response handleBadRequest(IllegalStateException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", Response.Status.BAD_REQUEST.getStatusCode(),
                        "error", "Bad Request",
                        "message", exception.getMessage()
                ))
                .build();
    }

    // Catch OperationNotSupportedException and convert it into HTTP 404
    @ServerExceptionMapper(OperationNotSupportedException.class)
    public Response handleBadRequest(OperationNotSupportedException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", Response.Status.FORBIDDEN.getStatusCode(),
                        "error", "Not Permitted",
                        "message", exception.getMessage()
                ))
                .build();
    }
}
