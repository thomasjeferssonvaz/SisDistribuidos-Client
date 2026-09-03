package mikrolabs.dev.sisdistribuidos.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;

import java.net.URL;
import java.util.ResourceBundle;

public class ConfigController implements Initializable {


    public Button logout;
    public TextField serverIpField;
    public TextField serverPortField;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load saved preferences into the fields if they exist in this view
        if (serverIpField != null && serverPortField != null) {
            serverIpField.setText(ConfigManager.getServerIp());
            serverPortField.setText(String.valueOf(ConfigManager.getServerPort()));
        }
        if (ConfigManager.getToken() != null) {
            logout.setDisable(false);
        } else  {
            logout.setDisable(true);
        }
    }



    @FXML
    private void salvar(MouseEvent event) {
        String ip = serverIpField.getText().trim();
        String port = serverPortField.getText().trim();
        ConfigManager.save(ip, port);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    public void deslogar(MouseEvent mouseEvent) {
        ConfigManager.clearToken();
        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
    }
}
