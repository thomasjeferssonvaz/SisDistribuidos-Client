package mikrolabs.dev.sisdistribuidos.utils;

import javafx.application.Platform;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;

import java.text.Normalizer;
import java.util.Locale;

public final class SessionUtils {
    private SessionUtils() {}

    public static boolean ensureSession(Stage source, String token, String username) {
        if (ConfigManager.hasSession(token, username)) return true;
        finishSession(source, Response.error(401, "Sessão incompleta. Faça login novamente."));
        return false;
    }

    public static boolean isExpiredSession(Response response) {
        if (response == null || response.statusCode() != 401 || response.message() == null) return false;

        String message = Normalizer.normalize(response.message(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .strip();
        return message.contains("sessao expirada") || message.contains("sessao encerrada");
    }

    public static boolean handleExpiredSession(Stage source, Response response) {
        if (!isExpiredSession(response)) return false;
        finishSession(source, response);
        return true;
    }

    private static void finishSession(Stage source, Response response) {
        Runnable finish = () -> {
            ConfigManager.clearSession();
            NavigationUtils.returnToLogin(source, response);
        };
        if (Platform.isFxApplicationThread()) {
            finish.run();
        } else {
            Platform.runLater(finish);
        }
    }
}
