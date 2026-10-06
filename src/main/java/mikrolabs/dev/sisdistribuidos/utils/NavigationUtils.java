package mikrolabs.dev.sisdistribuidos.utils;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import mikrolabs.dev.sisdistribuidos.ClientApplication;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;

import java.io.IOException;
import java.util.ArrayList;

public final class NavigationUtils {

    private NavigationUtils() {}

    /**
     * Troca a cena do Stage atual carregando o FXML e aplicando a folha de estilos global.
     */
    public static void navigateTo(Stage stage, String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource(fxmlPath));
        Scene scene = new Scene(loader.load(), 900, 600);

        var cssResource = ClientApplication.class.getResource("styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        stage.setTitle(title);
        stage.setScene(scene);
    }

    public static void returnToLogin(Stage source, Response response) {
        if (source == null) return;
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> returnToLogin(source, response));
            return;
        }

        Stage mainStage = findMainStage(source);
        for (Window window : new ArrayList<>(Window.getWindows())) {
            if (window instanceof Stage stage && stage != mainStage
                    && findMainStage(stage) == mainStage) {
                stage.close();
            }
        }

        try {
            navigateTo(mainStage, "views/Login.fxml", "Login");
            if (response != null) {
                Toast.Type type = response.statusCode() == 200 ? Toast.Type.SUCCESS : Toast.Type.ERROR;
                Toast.show(mainStage, response.message(), type);
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar tela de login: " + e.getMessage());
            Toast.show(mainStage, "Não foi possível abrir a tela de login.", Toast.Type.ERROR);
        }
    }

    private static Stage findMainStage(Stage source) {
        Stage mainStage = source;
        while (mainStage.getOwner() instanceof Stage owner) {
            mainStage = owner;
        }
        return mainStage;
    }

    /**
     * Abre a tela de configurações como modal vinculado ao Stage proprietário.
     */
    public static void openConfigModal(Stage ownerStage) {
        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("views/ConfigView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Configurações");

            Scene scene = new Scene(root);
            var cssResource = ClientApplication.class.getResource("styles.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }

            stage.setScene(scene);
            stage.initOwner(ownerStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.show();
        } catch (IOException e) {
            System.err.println("Erro ao abrir configurações: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void openProfileModal(Stage ownerStage) {
        if (!SessionUtils.ensureSession(ownerStage, ConfigManager.getToken(), ConfigManager.getUsername())) return;
        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("views/ProfileView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Profile");

            Scene scene = new Scene(root);
            var cssResource = ClientApplication.class.getResource("styles.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }

            stage.setScene(scene);
            stage.initOwner(ownerStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.show();
        } catch (IOException e) {
            System.err.println("Erro ao abrir Profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void openDeleteConfirmationModal(Stage ownerStage) {
        if (!SessionUtils.ensureSession(ownerStage, ConfigManager.getToken(), ConfigManager.getUsername())) return;
        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("views/DeleteConfirmationView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Delete Confirmation");

            Scene scene = new Scene(root);
            var cssResource = ClientApplication.class.getResource("styles.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }

            stage.setScene(scene);
            stage.initOwner(ownerStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.show();
        } catch (IOException e) {
            System.err.println("Erro ao abrir Profile: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
