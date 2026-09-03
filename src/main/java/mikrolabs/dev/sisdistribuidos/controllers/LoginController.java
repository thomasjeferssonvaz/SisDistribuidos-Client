package mikrolabs.dev.sisdistribuidos.controllers;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.ClientApplication;
import mikrolabs.dev.sisdistribuidos.DTOs.Request;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.DTOs.User;
import mikrolabs.dev.sisdistribuidos.exceptions.ServerConnectionError;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import mikrolabs.dev.sisdistribuidos.managers.SocketManager;

import java.io.IOException;
import java.util.Map;

public class LoginController {

    public Label returnLabel;
    public Button enviarBtn;
    public TextField usernameTextBox;
    public TextField passwordTextBox;
    public Button configBtn;

    public void logar() {
        Gson gson = new Gson();
        String username =  usernameTextBox.getText().trim();
        String password = passwordTextBox.getText();

        if(username.isEmpty() || password.isEmpty()) {
            returnLabel.setText("Preencha todos os campos");
            return;
        }

        enviarBtn.setDisable(true);
        returnLabel.setText("Autenticando...");
        new Thread(() -> {
            try {
                String data = gson.toJson(Map.of(
                        "username", username,
                        "password", password
                ));

                Response loginResponse = SocketManager.sendRequest(new Request("login", data));

                Platform.runLater(() -> {
                    User user = gson.fromJson(loginResponse.data(), User.class);
                    ConfigManager.saveToken(user.token());
                    returnLabel.setText(loginResponse.result());
                    enviarBtn.setDisable(false);
                });
            } catch (ServerConnectionError serverConnectionError) {
                Platform.runLater(() -> {
                    returnLabel.setText(serverConnectionError.getMessage());
                    enviarBtn.setDisable(false);
                });
            }
        }).start();

    }

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
