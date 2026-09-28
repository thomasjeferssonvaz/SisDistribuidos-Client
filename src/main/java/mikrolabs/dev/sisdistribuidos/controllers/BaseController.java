package mikrolabs.dev.sisdistribuidos.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import mikrolabs.dev.sisdistribuidos.utils.NavigationUtils;

import java.net.URL;
import java.util.ResourceBundle;

public class BaseController implements Initializable {
    public Button configBtn;
    public Button profileBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (profileBtn != null) {
            String token = ConfigManager.getToken();
            boolean isLoggedIn = (token != null);

            profileBtn.setVisible(isLoggedIn);
            profileBtn.setManaged(isLoggedIn);
        }
    }


    @FXML
    private void abrirConfiguracoes(ActionEvent event) {
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        NavigationUtils.openConfigModal(currentStage);
    }

    @FXML
    private void abrirProfile(ActionEvent event) {
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        NavigationUtils.openProfileModal(currentStage);
    }
}
