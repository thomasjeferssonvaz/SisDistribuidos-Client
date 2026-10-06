package mikrolabs.dev.sisdistribuidos.utils;

import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionUtilsTest {
    @Test
    void recognizesExpiredSessionsDespiteMessageFormatting() {
        for (String message : new String[]{
                "Sessão expirada ou encerrada.",
                "Sua SESSÃO EXPIRADA! Faça login novamente.",
                "sessao encerrada",
                "Erro: Sessão - ENCERRADA; faça login.",
                "  Sessão: expirada.  "
        }) {
            assertTrue(SessionUtils.isExpiredSession(Response.error(401, message)), message);
        }
    }

    @Test
    void otherUnauthorizedResponsesDoNotExpireTheSession() {
        for (String message : new String[]{
                "Operação não autorizada",
                "Usuario ou senhas inválidos",
                "Token de autenticação não fornecido.",
                "Sessão ativa.",
                "",
                "   "
        }) {
            Response response = Response.error(401, message);
            assertFalse(SessionUtils.isExpiredSession(response), message);
            assertFalse(SessionUtils.handleExpiredSession(null, response), message);
        }
        assertFalse(SessionUtils.isExpiredSession(null));
        assertFalse(SessionUtils.isExpiredSession(Response.error(401, null)));
        assertFalse(SessionUtils.isExpiredSession(Response.error(200, "Sessão encerrada.")));
        assertFalse(SessionUtils.isExpiredSession(Response.error(500, "Sessão expirada.")));
    }

    @Test
    void completeLocalSessionDoesNotRequireUuidOrUiNavigation() {
        assertTrue(SessionUtils.ensureSession(null, "token-do-colega", "Usuario"));
    }
}
