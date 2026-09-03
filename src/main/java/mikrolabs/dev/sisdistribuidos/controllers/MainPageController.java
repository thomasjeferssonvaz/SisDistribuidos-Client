package mikrolabs.dev.sisdistribuidos.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.ClientApplication;

import java.io.IOException;

public class MainPageController {

    @FXML
    private void abrirConfiguracoes(ActionEvent event) {
        try {
            var cssResource = ClientApplication.class.getResource("styles.css");
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("views/ConfigView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();

            stage.setTitle("Configurações");

            Scene scene = new Scene(root);

            if( cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.err.println("Css não encontrado");
            }

            stage.setScene(scene);

            // Optional: Block interactions with the main window until closed
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.initOwner(currentStage);
            stage.initModality(Modality.WINDOW_MODAL);

            stage.show();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
