package mikrolabs.dev.sisdistribuidos.managers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {
    @BeforeEach
    void isolateConfiguration() {
        Path directory = Path.of("").toAbsolutePath().normalize();
        assertEquals("test-workspace", directory.getFileName().toString(),
                "Run with Surefire's isolated workingDirectory before accessing ConfigManager.");
        assertEquals("target", directory.getParent().getFileName().toString());
        ConfigManager.clearSession();
    }

    @AfterEach
    void clearTestSession() {
        if (Path.of("").toAbsolutePath().getFileName().toString().equals("test-workspace")) {
            ConfigManager.clearSession();
        }
    }

    @Test
    void sessionRequiresBothNonblankValuesWithoutImposingUuidFormat() {
        assertTrue(ConfigManager.hasSession("existing-server-token", "ana"));
        for (String absent : new String[]{null, "", " \t\n"}) {
            assertFalse(ConfigManager.hasSession(absent, "ana"));
            assertFalse(ConfigManager.hasSession("token", absent));
        }
    }

    @Test
    void storedSessionRequiresBothCredentials() {
        assertFalse(ConfigManager.hasSession());
        ConfigManager.saveVariable("Token", "existing-server-token");
        assertFalse(ConfigManager.hasSession());
        ConfigManager.saveVariable("Username", "ana");
        assertTrue(ConfigManager.hasSession());
        ConfigManager.clearToken();
        assertFalse(ConfigManager.hasSession());
    }

    @Test
    void clearSessionPersistsBothRemovalsAndKeepsServerSettings() throws Exception {
        ConfigManager.save("127.0.0.2", "12345");
        ConfigManager.saveVariable("Token", "existing-server-token");
        ConfigManager.saveVariable("Username", "ana");

        ConfigManager.clearSession();

        assertNull(ConfigManager.getToken());
        assertNull(ConfigManager.getUsername());
        assertFalse(ConfigManager.hasSession());
        Properties persisted = new Properties();
        try (InputStream input = Files.newInputStream(Path.of("config.properties"))) {
            persisted.load(input);
        }
        assertFalse(persisted.containsKey("Token"));
        assertFalse(persisted.containsKey("Username"));
        assertEquals("127.0.0.2", persisted.getProperty("ServerIp"));
        assertEquals("12345", persisted.getProperty("ServerPort"));
        ConfigManager.load();
        assertFalse(ConfigManager.hasSession());
    }
}
