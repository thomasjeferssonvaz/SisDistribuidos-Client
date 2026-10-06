package mikrolabs.dev.sisdistribuidos.managers;

import com.google.gson.JsonParseException;
import mikrolabs.dev.sisdistribuidos.DTOs.Request;
import mikrolabs.dev.sisdistribuidos.exceptions.ServerConnectionError;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class SocketManagerTest {
    @Test void responseSchemaAndServerErrors() {
        assertEquals(500, SocketManager.parseResponse("{\"statusCode\":500,\"message\":\"erro\"}").statusCode());
        assertNull(SocketManager.parseResponse("{\"statusCode\":201,\"message\":\"ok\"}").data());
        assertNull(SocketManager.parseResponse("{\"statusCode\":401,\"message\":\"erro\",\"data\":null}").data());
        for (String value : new String[]{"null", "[]", "{}", "{\"statusCode\":\"200\",\"message\":\"ok\"}", "{\"statusCode\":200.5,\"message\":\"ok\"}", "{\"statusCode\":200,\"message\":123}", "invalid"})
            assertThrows(JsonParseException.class, () -> SocketManager.parseResponse(value), value);
    }

    @Test void connectionsCloseAndTransportFailuresStaySeparate() throws Exception {
        var field = ConfigManager.class.getDeclaredField("properties");
        field.setAccessible(true);
        Properties properties = (Properties) field.get(null);
        Properties original = new Properties();
        original.putAll(properties);
        try (ServerSocket server = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(10000);
            properties.setProperty("ServerIp", "127.0.0.1");
            properties.setProperty("ServerPort", Integer.toString(server.getLocalPort()));
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                Future<?> peer = executor.submit(() -> {
                    try {
                        for (int i = 0; i < 5; i++) {
                            try (Socket socket = server.accept()) {
                                socket.setSoTimeout(10000);
                                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                                assertNotNull(reader.readLine());
                                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                                if (i < 2) writer.println("{\"statusCode\":500,\"message\":\"erro\"}");
                                if (i == 2) writer.println("invalid");
                                if (i == 3) continue;
                                if (i == 4) Thread.sleep(5500);
                                assertEquals(-1, reader.read());
                            }
                        }
                    } catch (Exception e) { throw new RuntimeException(e); }
                });
                Request request = new Request("getuser", null);
                assertEquals(500, SocketManager.sendRequest(request).statusCode());
                assertEquals(500, SocketManager.sendRequest(request).statusCode());
                assertThrows(ServerConnectionError.class, () -> SocketManager.sendRequest(request));
                assertThrows(ServerConnectionError.class, () -> SocketManager.sendRequest(request));
                assertTrue(assertThrows(ServerConnectionError.class, () -> SocketManager.sendRequest(request)).getCause() instanceof SocketTimeoutException);
                peer.get(15, TimeUnit.SECONDS);
            }
        } finally {
            properties.clear();
            properties.putAll(original);
        }
    }
}
