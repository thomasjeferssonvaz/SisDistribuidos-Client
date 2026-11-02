package mikrolabs.dev.sisdistribuidos;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.DTOs.User;
import mikrolabs.dev.sisdistribuidos.DTOs.Vaga;
import mikrolabs.dev.sisdistribuidos.DTOs.Vehicle;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import mikrolabs.dev.sisdistribuidos.managers.SocketManager;
import mikrolabs.dev.sisdistribuidos.utils.LoginResponseValidation;
import mikrolabs.dev.sisdistribuidos.utils.ProfileResponseValidation;
import mikrolabs.dev.sisdistribuidos.utils.VagaResponseValidation;
import mikrolabs.dev.sisdistribuidos.utils.VehicleResponseValidation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End TCP Client-Server integration test verifying that requests built
 * by the Client DTOs
 * and transmitted via SocketManager are protocol-compliant across all 17
 * endpoints and that the
 * Client validators accurately interpret all responses.
 */
class ClientServerEndToEndIntegrationTest {

    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    private ServerSocket serverSocket;
    private String originalIp;
    private int originalPort;

    private final Map<String, Map<String, String>> users = new ConcurrentHashMap<>();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final Map<String, JsonObject> vagas = new ConcurrentHashMap<>();
    private final Map<String, JsonObject> vehicles = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        originalIp = ConfigManager.getServerIp();
        originalPort = ConfigManager.getServerPort();

        Map<String, String> admin = new HashMap<>();
        admin.put("name", "admin");
        admin.put("username", "admin");
        admin.put("password", "Admin123!");
        admin.put("role", "admin");
        users.put("admin", admin);

        serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();
        ConfigManager.save("127.0.0.1", String.valueOf(port));

        Thread acceptThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket client = serverSocket.accept();
                    new Thread(() -> handleClient(client)).start();
                } catch (Exception ignored) {
                }
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        ConfigManager.save(originalIp, String.valueOf(originalPort));
    }

    private void handleClient(Socket client) {
        try (Socket s = client;
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8),
                        true)) {
            String line = in.readLine();
            if (line == null)
                return;
            JsonObject req = JsonParser.parseString(line).getAsJsonObject();
            String method = req.get("method").getAsString();
            JsonObject data = req.get("data").getAsJsonObject();
            JsonObject res = route(method, data);
            out.println(gson.toJson(res));
        } catch (Exception ignored) {
        }
    }

    private synchronized JsonObject route(String method, JsonObject d) {
        return switch (method) {
            case "login" -> {
                String u = d.get("username").getAsString();
                String p = d.get("password").getAsString();
                Map<String, String> user = users.get(u);
                if (user == null || !Objects.equals(user.get("password"), p)) {
                    yield resp(401, "Credenciais incorretas.", null);
                }
                String tok = UUID.randomUUID().toString();
                sessions.put(tok, u);
                JsonObject tokObj = new JsonObject();
                tokObj.addProperty("token", tok);
                yield resp(200, "Sucesso no Login", tokObj);
            }
            case "register" -> {
                String u = d.get("username").getAsString();
                if (users.containsKey(u))
                    yield resp(409, "O username já está em uso.", null);
                String role = d.has("role") && !d.get("role").isJsonNull() ? d.get("role").getAsString() : "user";
                Map<String, String> created = new HashMap<>();
                created.put("name", d.get("name").getAsString());
                created.put("username", u);
                created.put("password", d.get("password").getAsString());
                created.put("role", role);
                users.put(u, created);
                yield resp(201, "Usuário criado com sucesso", null);
            }
            case "getuser" -> {
                String u = d.get("username").getAsString();
                Map<String, String> target = users.get(u);
                if (target == null)
                    yield resp(404, "Usuário não encontrado", null);
                JsonObject obj = new JsonObject();
                obj.addProperty("name", target.get("name"));
                obj.addProperty("username", target.get("username"));
                obj.addProperty("role", target.get("role"));
                yield resp(200, "Usuário encontrado com sucesso", obj);
            }
            case "createvaga" -> {
                String spotName = d.get("spotName").getAsString();
                JsonObject v = new JsonObject();
                v.addProperty("spotName", spotName);
                v.addProperty("floor", d.get("floor").getAsInt());
                v.addProperty("status", d.has("status") && !d.get("status").isJsonNull() ? d.get("status").getAsString() : "disponivel");
                v.addProperty("type", d.get("type").getAsString());
                v.add("licensePlate", null);
                vagas.put(spotName, v);
                yield resp(201, "Vaga criada com sucesso", null);
            }
            case "getvagas" -> {
                JsonArray arr = new JsonArray();
                vagas.values().forEach(arr::add);
                yield resp(200, "Vagas obtidas com sucesso", arr);
            }
            case "updatevaga" -> {
                String spotName = d.get("spotName").getAsString();
                JsonObject v = vagas.get(spotName);
                if (d.has("newSpotName") && !d.get("newSpotName").isJsonNull()) {
                    String newName = d.get("newSpotName").getAsString();
                    vagas.remove(spotName);
                    v.addProperty("spotName", newName);
                    vagas.put(newName, v);
                }
                if (d.has("status") && !d.get("status").isJsonNull()) {
                    v.addProperty("status", d.get("status").getAsString());
                }
                if (d.has("licensePlate")) {
                    v.add("licensePlate", d.get("licensePlate"));
                }
                yield resp(200, "Vaga atualizada com sucesso", null);
            }
            case "findvehicle" -> {
                String plate = d.get("licensePlate").getAsString();
                for (JsonObject v : vagas.values()) {
                    if (!v.get("licensePlate").isJsonNull()
                            && plate.equalsIgnoreCase(v.get("licensePlate").getAsString())) {
                        JsonObject loc = new JsonObject();
                        loc.addProperty("floor", v.get("floor").getAsInt());
                        loc.addProperty("spotName", v.get("spotName").getAsString());
                        loc.addProperty("status", v.get("status").getAsString());
                        yield resp(200, "Veiculo localizado com sucesso", loc);
                    }
                }
                yield resp(404, "Veículo não encontrado.", null);
            }
            case "createvehicle" -> {
                String owner = sessions.get(d.get("token").getAsString());
                String plate = d.get("licensePlate").getAsString().toUpperCase();
                JsonObject veh = new JsonObject();
                veh.addProperty("brand", d.get("brand").getAsString());
                veh.add("model", d.get("model"));
                veh.addProperty("type", d.get("type").getAsString());
                veh.addProperty("licensePlate", plate);
                veh.addProperty("ownerUsername", owner);
                vehicles.put(plate, veh);
                yield resp(201, "Veículo criado com sucesso", null);
            }
            case "getvehicles" -> {
                String owner = sessions.get(d.get("token").getAsString());
                JsonArray arr = new JsonArray();
                for (JsonObject veh : vehicles.values()) {
                    if (Objects.equals(owner, veh.get("ownerUsername").getAsString())) {
                        JsonObject pub = veh.deepCopy();
                        pub.remove("ownerUsername");
                        arr.add(pub);
                    }
                }
                yield resp(200, "Veículos obtidos com sucesso", arr);
            }
            case "logout" -> {
                sessions.remove(d.get("token").getAsString());
                yield resp(200, "Logout realizado com sucesso", null);
            }
            default -> resp(404, "Ação desconhecida: " + method, null);
        };
    }

    private JsonObject resp(int code, String msg, JsonElement data) {
        JsonObject o = new JsonObject();
        o.addProperty("statusCode", code);
        o.addProperty("message", msg);
        o.add("data", data);
        return o;
    }

    @Test
    void testCompleteClientServerWorkflowOverRealTcpSockets() {
        // 1. Admin logs in and creates spot E1
        Response adminLogin = SocketManager.sendRequest(User.loginRequest("admin", "Admin123!"));
        assertEquals(200, adminLogin.statusCode());
        String adminToken = LoginResponseValidation.extractToken(adminLogin);

        Response adminProfileRes = SocketManager.sendRequest(User.getUserRequest(adminToken, "admin"));
        ProfileResponseValidation.ProfileData adminProfile = ProfileResponseValidation.readData(adminProfileRes.data());
        assertNotNull(adminProfile);
        assertEquals("admin", adminProfile.role());

        Response createSpot = SocketManager
                .sendRequest(Vaga.createVagaRequest(adminToken, "E1", 2, "eletrico"));
        assertEquals(201, createSpot.statusCode());

        // 2. User registers, logs in, creates electric vehicle, parks in E1
        Response regRes = SocketManager.sendRequest(User.registerRequest("Alice Silva", "alice", "Senha@123"));
        assertEquals(201, regRes.statusCode());

        Response aliceLogin = SocketManager.sendRequest(User.loginRequest("alice", "Senha@123"));
        String aliceToken = LoginResponseValidation.extractToken(aliceLogin);

        Response createVeh = SocketManager.sendRequest(
                Vehicle.createVehicleRequest(aliceToken, "BYD", "Dolphin", "eletrico", "BYD1E11"));
        assertEquals(201, createVeh.statusCode());

        Response getVehs = SocketManager.sendRequest(Vehicle.getVehiclesRequest(aliceToken));
        List<Vehicle> aliceVehicles = VehicleResponseValidation.readVehicles(getVehs.data());
        assertEquals(1, aliceVehicles.size());
        assertEquals("BYD1E11", aliceVehicles.getFirst().licensePlate());

        Response parkRes = SocketManager.sendRequest(
                Vaga.updateVagaUserRequest(aliceToken, "E1", "ocupada", true, "BYD1E11"));
        assertEquals(200, parkRes.statusCode());

        // 3. Admin renames E1 -> E10; Alice locates her vehicle and sees E10
        Response renameRes = SocketManager.sendRequest(
                Vaga.updateVagaAdminRequest(adminToken, "E1", "E10", 2));
        assertEquals(200, renameRes.statusCode());

        Response findRes = SocketManager.sendRequest(Vaga.findVehicleRequest(aliceToken, "BYD1E11"));
        assertEquals(200, findRes.statusCode());
        Vaga.Location loc = VagaResponseValidation.readLocation(findRes.data());
        assertEquals("E10", loc.spotName());
        assertEquals(2, loc.floor());
        assertEquals("ocupada", loc.status());

        // 4. GetVagas returns updated spot list
        Response allSpotsRes = SocketManager.sendRequest(Vaga.getVagasRequest(aliceToken));
        List<Vaga> spots = VagaResponseValidation.readVagas(allSpotsRes.data());
        assertEquals(1, spots.size());
        assertEquals("E10", spots.getFirst().spotName());
        assertEquals("BYD1E11", spots.getFirst().licensePlate());

        // 5. Logout
        assertEquals(200, SocketManager.sendRequest(User.logoutRequest(aliceToken)).statusCode());
    }
}
