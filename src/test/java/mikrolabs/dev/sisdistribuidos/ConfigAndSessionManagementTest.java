package mikrolabs.dev.sisdistribuidos;

import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import mikrolabs.dev.sisdistribuidos.utils.SessionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigAndSessionManagementTest {

    private String originalIp;
    private int originalPort;

    @BeforeEach
    void setUp() {
        originalIp = ConfigManager.getServerIp();
        originalPort = ConfigManager.getServerPort();
        ConfigManager.clearSession();
    }

    @AfterEach
    void tearDown() {
        ConfigManager.clearSession();
        ConfigManager.save(originalIp, String.valueOf(originalPort));
    }

    @Test
    void testConfigManagerSessionRoleAndEndpointChanges() {
        assertFalse(ConfigManager.hasSession());
        assertNull(ConfigManager.getRole());

        ConfigManager.save("127.0.0.1", "34345");
        ConfigManager.saveVariable("Token", "uuid-token-1");
        ConfigManager.saveVariable("Username", "alice");
        ConfigManager.saveRole("admin");

        assertTrue(ConfigManager.hasSession());
        assertEquals("admin", ConfigManager.getRole());
        assertTrue(ConfigManager.isAdmin());
        assertFalse(ConfigManager.isUser());

        // Invalid role clears role instead of storing corrupted value
        ConfigManager.saveRole("invalid_role");
        assertNull(ConfigManager.getRole());

        ConfigManager.saveRole("user");
        assertTrue(ConfigManager.isUser());

        // Changing server port or IP clears cached Role so stale privileges are never kept across servers
        ConfigManager.save("127.0.0.1", "45000");
        assertNull(ConfigManager.getRole());
        assertEquals(45000, ConfigManager.getServerPort());

        ConfigManager.clearSession();
        assertFalse(ConfigManager.hasSession());
        assertNull(ConfigManager.getToken());
        assertNull(ConfigManager.getUsername());
        assertNull(ConfigManager.getRole());
    }

    @Test
    void testSessionUtilsDistinguishesExpiredSessionFromPermissionErrors() {
        // True session expiration (401 "Sessão expirada ou encerrada.") -> true
        assertTrue(SessionUtils.isExpiredSession(Response.error(401, "Sessão expirada ou encerrada.")));
        assertTrue(SessionUtils.isExpiredSession(Response.error(401, "Sessao encerrada pelo servidor")));

        // 401 permission errors MUST NOT be treated as expired session (user stays logged in!)
        assertFalse(SessionUtils.isExpiredSession(Response.error(401, "Operação não autorizada.")));
        assertFalse(SessionUtils.isExpiredSession(Response.error(
                401,
                "Operação não autorizada. Apenas usuários com perfil user podem gerenciar seus veículos."
        )));
        assertFalse(SessionUtils.isExpiredSession(Response.error(
                401,
                "Operação não autorizada. Apenas administradores podem cadastrar administradores."
        )));
        assertFalse(SessionUtils.isExpiredSession(Response.error(401, "Credenciais incorretas.")));
        assertFalse(SessionUtils.isExpiredSession(Response.error(401, "Senha atual incorreta")));
        assertFalse(SessionUtils.isExpiredSession(Response.error(400, "Sessão expirada ou encerrada.")));
    }
}

