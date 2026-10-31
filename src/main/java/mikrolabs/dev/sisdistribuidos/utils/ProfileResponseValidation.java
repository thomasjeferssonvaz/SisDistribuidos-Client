package mikrolabs.dev.sisdistribuidos.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class ProfileResponseValidation {
    private ProfileResponseValidation() {}

    public record ProfileData(String name, String username, String role) {
        public ProfileData(String name, String username) {
            this(name, username, null);
        }
    }

    public static ProfileData readData(JsonElement data) {
        if (data == null || !data.isJsonObject()) return null;

        JsonObject object = data.getAsJsonObject();
        String name = readText(object.get("name"));
        String username = readText(object.get("username"));
        String role = readRole(object.get("role"));
        return name == null && username == null && role == null ? null : new ProfileData(name, username, role);
    }

    private static String readText(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) return null;
        String text = value.getAsString();
        return text.isBlank() ? null : text;
    }

    private static String readRole(JsonElement value) {
        String text = readText(value);
        if (text == null) return null;
        String trimmed = text.trim();
        return FieldValidation.validRole(trimmed) ? trimmed : null;
    }
}
