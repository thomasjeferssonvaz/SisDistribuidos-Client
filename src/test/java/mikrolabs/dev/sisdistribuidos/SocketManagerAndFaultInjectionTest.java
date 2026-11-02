package mikrolabs.dev.sisdistribuidos;

import com.google.gson.JsonObject;
import mikrolabs.dev.sisdistribuidos.DTOs.Request;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.DTOs.User;
import mikrolabs.dev.sisdistribuidos.exceptions.ServerConnectionError;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import mikrolabs.dev.sisdistribuidos.managers.SocketManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SocketManagerAndFaultInjectionTest {

    private String originalIp;
    private int originalPort;

    @BeforeEach
    void setUp() {
        originalIp = ConfigManager.getServerIp();
        originalPort = ConfigManager.getServerPort();
    }

    @AfterEach
    void tearDown() {
        ConfigManager.save(originalIp, String.valueOf(originalPort));
    }

    @Test
    void testConnectionRefusedThrowsServerConnectionError() throws Exception {
        int closedPort;
        try (ServerSocket ss = new ServerSocket(0)) {
            closedPort = ss.getLocalPort();
        }
        ConfigManager.save("127.0.0.1", String.valueOf(closedPort));

        Request req = User.loginRequest("admin", "Admin@123");
        ServerConnectionError ex = assertThrows(ServerConnectionError.class, () -> SocketManager.sendRequest(req));
        assertTrue(ex.getMessage().contains("Não foi possível comunicar com o servidor"));
    }

    @Test
    void testEmptyServerResponseOnAbruptCloseThrowsServerConnectionError() throws Exception {
        try (ServerSocket ss = new ServerSocket(0)) {
            ConfigManager.save("127.0.0.1", String.valueOf(ss.getLocalPort()));
            Thread serverThread = new Thread(() -> {
                try (Socket client = ss.accept()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                    in.readLine();
                    // Close immediately without writing anything
                } catch (Exception ignored) {
                }
            });
            serverThread.start();

            ServerConnectionError ex = assertThrows(
                    ServerConnectionError.class,
                    () -> SocketManager.sendRequest(User.loginRequest("admin", "Admin@123"))
            );
            assertEquals("O servidor encerrou a conexão sem uma resposta válida.", ex.getMessage());
            serverThread.join(2000);
        }
    }

    @Test
    void testMalformedAndNonIntegerStatusCodeResponsesThrowServerConnectionError() throws Exception {
        // 1. Corrupted JSON
        try (ServerSocket ss = new ServerSocket(0)) {
            ConfigManager.save("127.0.0.1", String.valueOf(ss.getLocalPort()));
            Thread serverThread = new Thread(() -> {
                try (Socket client = ss.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                     OutputStream out = client.getOutputStream()) {
                    in.readLine();
                    out.write("{broken_json\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } catch (Exception ignored) {
                }
            });
            serverThread.start();

            ServerConnectionError ex = assertThrows(
                    ServerConnectionError.class,
                    () -> SocketManager.sendRequest(User.loginRequest("admin", "Admin@123"))
            );
            assertEquals("O servidor enviou uma resposta JSON inválida.", ex.getMessage());
            serverThread.join(2000);
        }

        // 2. Float statusCode (e.g. 200.5) must be rejected
        try (ServerSocket ss = new ServerSocket(0)) {
            ConfigManager.save("127.0.0.1", String.valueOf(ss.getLocalPort()));
            Thread serverThread = new Thread(() -> {
                try (Socket client = ss.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                     OutputStream out = client.getOutputStream()) {
                    in.readLine();
                    out.write("{\"statusCode\":200.5,\"message\":\"OK\",\"data\":null}\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } catch (Exception ignored) {
                }
            });
            serverThread.start();

            ServerConnectionError ex = assertThrows(
                    ServerConnectionError.class,
                    () -> SocketManager.sendRequest(User.loginRequest("admin", "Admin@123"))
            );
            assertEquals("O servidor enviou uma resposta JSON inválida.", ex.getMessage());
            serverThread.join(2000);
        }
    }

    @Test
    void testChunkedTcpResponseIsReassembledCorrectly() throws Exception {
        try (ServerSocket ss = new ServerSocket(0)) {
            ConfigManager.save("127.0.0.1", String.valueOf(ss.getLocalPort()));
            Thread serverThread = new Thread(() -> {
                try (Socket client = ss.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                     OutputStream out = client.getOutputStream()) {
                    in.readLine();
                    String part1 = "{\"statusCode\":200,";
                    String part2 = "\"message\":\"Sucesso\",";
                    String part3 = "\"data\":{\"floor\":1,\"spotName\":\"A1\",\"status\":\"ocupada\"}}\n";
                    out.write(part1.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    Thread.sleep(25);
                    out.write(part2.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    Thread.sleep(25);
                    out.write(part3.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } catch (Exception ignored) {
                }
            });
            serverThread.start();

            Response res = SocketManager.sendRequest( new Request("findvehicle", new JsonObject()));
            assertEquals(200, res.statusCode());
            assertEquals("Sucesso", res.message());
            assertNotNull(res.data());
            assertEquals("A1", res.data().getAsJsonObject().get("spotName").getAsString());
            serverThread.join(2000);
        }
    }
}

