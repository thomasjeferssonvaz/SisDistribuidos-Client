package mikrolabs.dev.sisdistribuidos.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import mikrolabs.dev.sisdistribuidos.DTOs.Vaga;

public final class VagaResponseValidation {
    private VagaResponseValidation() {}

    public static List<Vaga> readVagas(JsonElement data) {
        if (data == null || !data.isJsonArray()) {
            throw new IllegalArgumentException("Resposta de vagas inválida: esperado array JSON.");
        }
        JsonArray array = data.getAsJsonArray();
        List<Vaga> vagas = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            vagas.add(readVaga(element));
        }
        return vagas;
    }

    public static Vaga readVaga(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("Item de vaga inválido.");
        }
        JsonObject obj = element.getAsJsonObject();
        String spotName = readRequiredString(obj.get("spotName"), "spotName");
        int floor = readRequiredInt(obj.get("floor"), "floor");
        String status = readRequiredString(obj.get("status"), "status");
        if (!FieldValidation.validVagaStatus(status)) {
            throw new IllegalArgumentException("Status de vaga inválido: " + status);
        }
        String type = readRequiredString(obj.get("type"), "type");
        if (!FieldValidation.validType(type)) {
            throw new IllegalArgumentException("Tipo de vaga inválido: " + type);
        }
        String licensePlate = readNullableString(obj.get("licensePlate"), "licensePlate");
        return new Vaga(spotName, floor, status, type, licensePlate);
    }

    public static Vaga.Location readLocation(JsonElement data) {
        if (data == null || !data.isJsonObject()) {
            throw new IllegalArgumentException("Resposta de localização inválida.");
        }
        JsonObject obj = data.getAsJsonObject();
        int floor = readRequiredInt(obj.get("floor"), "floor");
        String spotName = readRequiredString(obj.get("spotName"), "spotName");
        String status = readRequiredString(obj.get("status"), "status");
        if (!FieldValidation.validVagaStatus(status)) {
            throw new IllegalArgumentException("Status da localização inválido: " + status);
        }
        return new Vaga.Location(floor, spotName, status);
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

    private static int readRequiredInt(JsonElement value, String field) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Campo numérico inválido: " + field);
        }
        try {
            return new BigDecimal(value.getAsString()).intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Campo deve ser inteiro: " + field, e);
        }
    }
}

