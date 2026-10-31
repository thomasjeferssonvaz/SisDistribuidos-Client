package mikrolabs.dev.sisdistribuidos.DTOs;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public record Vehicle(
        String brand,
        String model,
        String type,
        String licensePlate
) {
    public static Request createVehicleRequest(String token, String brand, String model, String type, String licensePlate) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        data.addProperty("brand", brand);
        if (model == null) {
            data.add("model", JsonNull.INSTANCE);
        } else {
            data.addProperty("model", model);
        }
        data.addProperty("type", type);
        data.addProperty("licensePlate", licensePlate);
        return new Request("createvehicle", data);
    }

    public static Request getVehiclesRequest(String token) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        return new Request("getvehicles", data);
    }

    public static Request getVehicleRequest(String token, String licensePlate) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        data.addProperty("licensePlate", licensePlate);
        return new Request("getvehicle", data);
    }

    public static Request updateVehicleRequest(
            String token,
            String licensePlate,
            String brand,
            boolean includeModel,
            String model,
            String type
    ) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        data.addProperty("licensePlate", licensePlate);
        if (brand != null) {
            data.addProperty("brand", brand);
        }
        if (includeModel) {
            if (model == null) {
                data.add("model", JsonNull.INSTANCE);
            } else {
                data.addProperty("model", model);
            }
        }
        if (type != null) {
            data.addProperty("type", type);
        }
        return new Request("updatevehicle", data);
    }

    public static Request deleteVehicleRequest(String token, String licensePlate) {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        data.addProperty("licensePlate", licensePlate);
        return new Request("deletevehicle", data);
    }
}

