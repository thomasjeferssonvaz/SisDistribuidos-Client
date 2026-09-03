package mikrolabs.dev.sisdistribuidos.DTOs;

public record Response(
        int status,
        String result,
        String data,
        String error
) {
    public static Response success(String result, String data) {
        return new Response(200, result, data, null);
    }
    public static Response success(String result) {
        return new Response(200, result, null, null);
    }
    public static Response error(int status, String message) {
        return new Response(status, message, null, null);
    }
}
