package mikrolabs.dev.sisdistribuidos;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mikrolabs.dev.sisdistribuidos.DTOs.Request;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.DTOs.User;
import mikrolabs.dev.sisdistribuidos.DTOs.Vaga;
import mikrolabs.dev.sisdistribuidos.DTOs.Vehicle;
import mikrolabs.dev.sisdistribuidos.utils.FieldValidation;
import mikrolabs.dev.sisdistribuidos.utils.LoginResponseValidation;
import mikrolabs.dev.sisdistribuidos.utils.ProfileResponseValidation;
import mikrolabs.dev.sisdistribuidos.utils.VagaResponseValidation;
import mikrolabs.dev.sisdistribuidos.utils.VehicleResponseValidation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolSerializationAndValidationTest {

    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    @Test
    void testUserRequestBuildersConformToProtocol() {
        Request login = User.loginRequest("alice", "Senha@123");
        assertEquals("login", login.method());
        assertEquals("alice", login.data().getAsJsonObject().get("username").getAsString());
        assertEquals("Senha@123", login.data().getAsJsonObject().get("password").getAsString());

        // Public register omits role and token
        Request regPublic = User.registerRequest("Alice Silva", "alice", "Senha@123");
        assertEquals("register", regPublic.method());
        JsonObject regData = regPublic.data().getAsJsonObject();
        assertEquals("Alice Silva", regData.get("name").getAsString());
        assertFalse(regData.has("role"));
        assertFalse(regData.has("token"));

        // Admin register includes role and token
        Request regAdmin = User.registerWithRoleRequest("Novo Admin", "admin2", "Senha@123", "admin", "tok-123");
        JsonObject regAdminData = regAdmin.data().getAsJsonObject();
        assertEquals("admin", regAdminData.get("role").getAsString());
        assertEquals("tok-123", regAdminData.get("token").getAsString());

        Request getUser = User.getUserRequest("tok-123", "alice");
        assertEquals("getuser", getUser.method());
        assertEquals("alice", getUser.data().getAsJsonObject().get("username").getAsString());

        Request updName = User.updateUserNameRequest("tok-123", "alice", "Alice Nova");
        assertEquals("updateusername", updName.method());
        assertEquals("Alice Nova", updName.data().getAsJsonObject().get("name").getAsString());

        Request updPass = User.updateUserPasswordRequest("tok-123", "alice", "Senha@123", "Nova@1234");
        assertEquals("updateuserpassword", updPass.method());
        assertEquals("Nova@1234", updPass.data().getAsJsonObject().get("newPassword").getAsString());

        Request delUser = User.deleteUserRequest("tok-123", "alice");
        assertEquals("deleteuser", delUser.method());
        assertEquals("alice", delUser.data().getAsJsonObject().get("username").getAsString());
    }

    @Test
    void testVagaAndVehicleRequestBuildersAndNullSemantics() {
        // CreateVaga
        Request createVaga = Vaga.createVagaRequest("tok", "A1", 2, "disponivel", "eletrico");
        assertEquals("createvaga", createVaga.method());
        JsonObject cv = createVaga.data().getAsJsonObject();
        assertEquals("A1", cv.get("spotName").getAsString());
        assertEquals(2, cv.get("floor").getAsInt());
        assertEquals("disponivel", cv.get("status").getAsString());
        assertEquals("eletrico", cv.get("type").getAsString());

        // GetVagas (D3)
        Request getVagas = Vaga.getVagasRequest("tok");
        assertEquals("getvagas", getVagas.method());

        // UpdateVaga for user releasing spot sends explicit licensePlate: null
        Request releaseSpot = Vaga.updateVagaUserRequest("tok", "A1", "disponivel", true, null);
        assertEquals("updatevaga", releaseSpot.method());
        JsonObject rs = releaseSpot.data().getAsJsonObject();
        assertTrue(rs.has("licensePlate"));
        assertTrue(rs.get("licensePlate").isJsonNull());
        assertFalse(rs.has("newSpotName"));
        assertFalse(rs.has("floor"));

        // FindVehicle (D5 / P3: licensePlate only, no ticketId)
        Request findVeh = Vaga.findVehicleRequest("tok", "ABC1D23");
        assertEquals("findvehicle", findVeh.method());
        assertEquals("ABC1D23", findVeh.data().getAsJsonObject().get("licensePlate").getAsString());
        assertFalse(findVeh.data().getAsJsonObject().has("ticketId"));

        // CreateVehicle with model = null (P6: key present with JsonNull)
        Request createVehNullModel = Vehicle.createVehicleRequest("tok", "BYD", null, "eletrico", "BYD1E11");
        assertEquals("createvehicle", createVehNullModel.method());
        JsonObject cvm = createVehNullModel.data().getAsJsonObject();
        assertTrue(cvm.has("model"));
        assertTrue(cvm.get("model").isJsonNull());
        assertFalse(cvm.has("ownerUsername"));

        // GetVehicle (D1: "getvehicle")
        Request getVeh = Vehicle.getVehicleRequest("tok", "BYD1E11");
        assertEquals("getvehicle", getVeh.method());

        // UpdateVehicle clearing model vs omitting model
        Request clearModel = Vehicle.updateVehicleRequest("tok", "BYD1E11", "BYD", true, null, null);
        JsonObject cm = clearModel.data().getAsJsonObject();
        assertTrue(cm.has("model") && cm.get("model").isJsonNull());
        assertFalse(cm.has("type"));
    }

    @Test
    void testFieldValidationRules() {
        assertTrue(FieldValidation.validName("Maria Silva"));
        assertFalse(FieldValidation.validName("Maria 123"));
        assertFalse(FieldValidation.validName(""));
        assertFalse(FieldValidation.validName(null));

        assertTrue(FieldValidation.validUsername("maria.silva_1"));
        assertFalse(FieldValidation.validUsername("Maria"));
        assertFalse(FieldValidation.validUsername("ab"));

        assertTrue(FieldValidation.validPassword("Senha@123"));
        assertFalse(FieldValidation.validPassword("senha@123"));
        assertFalse(FieldValidation.validPassword("SENHA@123"));
        assertFalse(FieldValidation.validPassword("Senhaaaaa"));
        assertFalse(FieldValidation.validPassword("Senha1234"));

        assertTrue(FieldValidation.validRole("admin"));
        assertTrue(FieldValidation.validRole("user"));
        assertFalse(FieldValidation.validRole("guest"));

        assertTrue(FieldValidation.validFloor("-1"));
        assertEquals(-1, FieldValidation.parseFloor("-1"));
        assertFalse(FieldValidation.validFloor("1.5"));
        assertNull(FieldValidation.parseFloor("abc"));

        assertTrue(FieldValidation.validVagaStatus("disponivel"));
        assertTrue(FieldValidation.validVagaStatus("ocupada"));
        assertTrue(FieldValidation.validVagaStatus("fora_de_servico"));
        assertFalse(FieldValidation.validVagaStatus("manutencao"));

        assertTrue(FieldValidation.validType("eletrico"));
        assertTrue(FieldValidation.validType("combustao"));
        assertFalse(FieldValidation.validType("diesel"));

        assertTrue(FieldValidation.validPort("34345"));
        assertFalse(FieldValidation.validPort("0"));
        assertFalse(FieldValidation.validPort("70000"));
    }

    @Test
    void testResponseValidatorsAcceptValidAndRejectMalformedPayloads() {
        // LoginResponseValidation
        String uuid = UUID.randomUUID().toString();
        JsonObject loginData = new JsonObject();
        loginData.addProperty("token", uuid);
        assertEquals(uuid, LoginResponseValidation.extractToken(new Response(200, "OK", loginData)));

        JsonObject badLoginData = new JsonObject();
        badLoginData.addProperty("token", "not-a-uuid");
        assertThrows(IllegalArgumentException.class,
                () -> LoginResponseValidation.extractToken(new Response(200, "OK", badLoginData)));

        // ProfileResponseValidation
        JsonObject profileJson = JsonParser.parseString("{\"name\":\"Alice\",\"username\":\"alice\",\"role\":\"user\"}").getAsJsonObject();
        ProfileResponseValidation.ProfileData profile = ProfileResponseValidation.readData(profileJson);
        assertNotNull(profile);
        assertEquals("Alice", profile.name());
        assertEquals("alice", profile.username());
        assertEquals("user", profile.role());

        // VagaResponseValidation
        JsonArray vagasJson = JsonParser.parseString(
                "[{\"floor\":1,\"spotName\":\"A1\",\"status\":\"disponivel\",\"licensePlate\":null,\"type\":\"combustao\"}," +
                "{\"floor\":2,\"spotName\":\"E1\",\"status\":\"ocupada\",\"licensePlate\":\"ABC1D23\",\"type\":\"eletrico\"}]"
        ).getAsJsonArray();
        List<Vaga> vagas = VagaResponseValidation.readVagas(vagasJson);
        assertEquals(2, vagas.size());
        assertNull(vagas.get(0).licensePlate());
        assertEquals("ABC1D23", vagas.get(1).licensePlate());

        // Rejects non-integer floor or invalid status
        JsonElement floatFloorVaga = JsonParser.parseString(
                "[{\"floor\":1.5,\"spotName\":\"A1\",\"status\":\"disponivel\",\"licensePlate\":null,\"type\":\"combustao\"}]"
        );
        assertThrows(IllegalArgumentException.class, () -> VagaResponseValidation.readVagas(floatFloorVaga));

        // Location validation
        JsonElement locJson = JsonParser.parseString("{\"floor\":2,\"spotName\":\"E1\",\"status\":\"ocupada\"}");
        Vaga.Location loc = VagaResponseValidation.readLocation(locJson);
        assertEquals(2, loc.floor());
        assertEquals("E1", loc.spotName());
        assertEquals("ocupada", loc.status());

        // VehicleResponseValidation
        JsonArray vehiclesJson = JsonParser.parseString(
                "[{\"brand\":\"BYD\",\"model\":null,\"type\":\"eletrico\",\"licensePlate\":\"BYD1E11\"}]"
        ).getAsJsonArray();
        List<Vehicle> vehicles = VehicleResponseValidation.readVehicles(vehiclesJson);
        assertEquals(1, vehicles.size());
        assertNull(vehicles.get(0).model());
        assertEquals("BYD1E11", vehicles.get(0).licensePlate());

        JsonElement invalidVehicle = JsonParser.parseString(
                "{\"brand\":\"BYD\",\"model\":null,\"type\":\"nuclear\",\"licensePlate\":\"BYD1E11\"}"
        );
        assertThrows(IllegalArgumentException.class, () -> VehicleResponseValidation.readVehicle(invalidVehicle));
    }
}
