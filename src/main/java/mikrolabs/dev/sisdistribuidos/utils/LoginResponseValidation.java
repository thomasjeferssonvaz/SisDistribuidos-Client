package mikrolabs.dev.sisdistribuidos.utils;

import com.google.gson.JsonElement;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import java.util.UUID;

public final class LoginResponseValidation {
    private LoginResponseValidation() {}
    public static String extractToken(Response response) {
        JsonElement data = response.data();
        if (data == null || !data.isJsonObject()) {
            throw new IllegalArgumentException("Resposta de login sem dados válidos.");
        }
        JsonElement element = data.getAsJsonObject().get("token");
        if (element == null || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Resposta de login sem token válido.");
        }
        String token = element.getAsString();
        if (!UUID.fromString(token).toString().equalsIgnoreCase(token)) {
            throw new IllegalArgumentException("Token fora do formato UUID.");
        }
        return token;
    }
}
