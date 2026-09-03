package mikrolabs.dev.sisdistribuidos.exceptions;

import mikrolabs.dev.sisdistribuidos.DTOs.Response;

public class ServerConnectionError extends RuntimeException {
    public ServerConnectionError() {
        super("Server connection error");
    }

    public Response toResponse() {
        return Response.error(500, "Server connection error");
    }
}
