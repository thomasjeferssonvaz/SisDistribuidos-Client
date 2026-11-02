package mikrolabs.dev.sisdistribuidos.DTOs;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public record Vaga(
        String spotName,
        int floor,
        String status,
        String type,
        String licensePlate
) {
    public record Location(
            int floor,
            String spotName,
            String status
    ) {}

    public static Request createVagaRequest(String token, String spotName, int floor, String type) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        data.addProperty("spotName", spotName);
        data.addProperty("floor", floor);
        data.addProperty("type", type);
        return new Request("createvaga", data);
    }

    public static Request getVagasRequest(String token) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        return new Request("getvagas", data);
    }

    public static Request updateVagaRequest(
            String token,
            String spotName,
            String newSpotName,
            Integer floor,
            String status,
            boolean includeLicensePlate,
            String licensePlate
    ) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        data.addProperty("spotName", spotName);
        if (newSpotName != null) {
            data.addProperty("newSpotName", newSpotName);
        }
        if (floor != null) {
            data.addProperty("floor", floor);
        }
        if (status != null) {
            data.addProperty("status", status);
        }
        if (includeLicensePlate) {
            if (licensePlate == null) {
                data.add("licensePlate", JsonNull.INSTANCE);
            } else {
                data.addProperty("licensePlate", licensePlate);
            }
        }
        return new Request("updatevaga", data);
    }

    public static Request updateVagaAdminRequest(String token, String spotName, String newSpotName, Integer floor) {
        return updateVagaRequest(token, spotName, newSpotName, floor, null, false, null);
    }

    public static Request updateVagaUserRequest(
            String token,
            String spotName,
            String status,
            boolean includeLicensePlate,
            String licensePlate
    ) {
        return updateVagaRequest(token, spotName, null, null, status, includeLicensePlate, licensePlate);
    }

    public static Request deleteVagaRequest(String token, String spotName) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        data.addProperty("spotName", spotName);
        return new Request("deletevaga", data);
    }

    public static Request findVehicleRequest(String token, String licensePlate) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        data.addProperty("licensePlate", licensePlate);
        return new Request("findvehicle", data);
    }
}
