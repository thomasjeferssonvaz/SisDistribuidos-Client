package mikrolabs.dev.sisdistribuidos.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import mikrolabs.dev.sisdistribuidos.DTOs.Vehicle;

public final class VehicleResponseValidation {
    private VehicleResponseValidation() {}

    public static List<Vehicle> readVehicles(JsonElement data) {
        if (data == null || !data.isJsonArray()) {
            throw new IllegalArgumentException("Resposta de veículos inválida: esperado array JSON.");
        }
        JsonArray array = data.getAsJsonArray();
        List<Vehicle> vehicles = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            vehicles.add(readVehicle(element));
        }
        return vehicles;
    }

    public static Vehicle readVehicle(JsonElement data) {
        if (data == null || !data.isJsonObject()) {
            throw new IllegalArgumentException("Dados de veículo inválidos.");
        }
        JsonObject obj = data.getAsJsonObject();
        String brand = readRequiredString(obj.get("brand"), "brand");
        String model = readNullableString(obj.get("model"), "model");
        String type = readRequiredString(obj.get("type"), "type");
        if (!FieldValidation.validType(type)) {
            throw new IllegalArgumentException("Tipo de veículo inválido: " + type);
        }
        String licensePlate = readRequiredString(obj.get("licensePlate"), "licensePlate");
        return new Vehicle(brand, model, type, licensePlate);
    }

    private static String readRequiredString(JsonElement value, String field) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Campo obrigatório inválido: " + field);
        }
        String text = value.getAsString();
        if (text.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório em branco: " + field);
        }
        return text;
    }

    private static String readNullableString(JsonElement value, String field) {
        if (value == null || value.isJsonNull()) return null;
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Campo inválido: " + field);
        }
        String text = value.getAsString();
        return text.isBlank() ? null : text;
    }
}

