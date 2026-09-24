package mikrolabs.dev.sisdistribuidos.exceptions;

import mikrolabs.dev.sisdistribuidos.DTOs.Response;

public class ServerConnectionError extends RuntimeException {
    public ServerConnectionError() {
        super("Server connection error");
    }

    public Response toResponse() {
        System.out.println(getMessage());
        return Response.error(500, getMessage());
    }

    public Response toResponseTimeOut() {
        System.out.println("Server Timed Out");
        return Response.error(500, "Server Timed Out");
    }
    public Response toResponseGsonError() {
        System.out.println("Gson formatting error");
        return Response.error(500, "Gson formatting error");
    }
}
