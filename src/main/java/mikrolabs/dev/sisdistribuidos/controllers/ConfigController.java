package mikrolabs.dev.sisdistribuidos.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import mikrolabs.dev.sisdistribuidos.utils.FieldValidation;
import mikrolabs.dev.sisdistribuidos.utils.Toast;

import java.net.URL;
import java.util.ResourceBundle;

public class ConfigController extends BaseController {

    public TextField serverIpField;
    public TextField serverPortField;
    boolean isLoggedIn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (serverIpField != null && serverPortField != null) {
            serverIpField.setText(ConfigManager.getServerIp());
            serverPortField.setText(String.valueOf(ConfigManager.getServerPort()));
        }

        String token = ConfigManager.getToken();
        isLoggedIn = token != null && !token.isBlank();

        Platform.runLater(() -> {
            if (serverIpField != null && serverIpField.getScene() != null) {
                serverIpField.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ESCAPE && serverIpField.getScene().getWindow() instanceof Stage stage) {
                        stage.close();
                        event.consume();
                    }
                });
            }
        });
    }

    @FXML
    private void salvarAction(ActionEvent event) {
        if (event.getSource() instanceof Node node && node.getScene() != null
                && node.getScene().getWindow() instanceof Stage stage) {
            executarSalvar(stage);
        }
    }

    @FXML
    private void salvar(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        executarSalvar(stage);
    }

    private void executarSalvar(Stage stage) {
        String ip = serverIpField.getText() != null ? serverIpField.getText().trim() : "";
        String port = serverPortField.getText() != null ? serverPortField.getText().trim() : "";

        if (!FieldValidation.validIp(ip)) {
            Toast.show(stage, "Informe um endereço IP válido.", Toast.Type.INFO);
            return;
        }
        if (!FieldValidation.validPort(port)) {
            Toast.show(stage, "A porta deve ser um número inteiro entre 1 e 65535.", Toast.Type.INFO);
            return;
        }

        ConfigManager.save(ip, port);
        stage.close();
    }
}
