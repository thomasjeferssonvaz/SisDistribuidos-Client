package mikrolabs.dev.sisdistribuidos.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class ProfileResponseValidation {
    private ProfileResponseValidation() {}

    public record ProfileData(String name, String username) {}

    public static ProfileData readData(JsonElement data) {
        if (data == null || !data.isJsonObject()) return null;

        JsonObject object = data.getAsJsonObject();
        String name = readText(object.get("name"));
        String username = readText(object.get("username"));
        return name == null && username == null ? null : new ProfileData(name, username);
    }

    private static String readText(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) return null;
        String text = value.getAsString();
        return text.isBlank() ? null : text;
    }
}
