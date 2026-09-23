package mikrolabs.dev.sisdistribuidos.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.ClientApplication;

import java.io.IOException;

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
        }
    }
}