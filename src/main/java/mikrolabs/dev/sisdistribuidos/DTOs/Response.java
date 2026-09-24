package mikrolabs.dev.sisdistribuidos.DTOs;

import com.google.gson.JsonElement;

public record Response(
        int statusCode,
        String message,
        JsonElement data
) {
    public static Response error(int status, String message) {
        return new Response(status, message, null);
    }
}
