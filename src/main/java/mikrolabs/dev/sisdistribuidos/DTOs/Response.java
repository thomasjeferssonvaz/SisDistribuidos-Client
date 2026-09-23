package mikrolabs.dev.sisdistribuidos.DTOs;

import com.google.gson.JsonElement;

public record Response(
        int statusCode,
        String message,
        JsonElement data,
        String error
) {
    public static Response success(String result, JsonElement data) {
        return new Response(200, result, data, null);
    }
    public static Response success(String result) {
        return new Response(200, result, null, null);
    }
    public static Response error(int status, String message) {
        return new Response(status, message, null, null);
    }
}
