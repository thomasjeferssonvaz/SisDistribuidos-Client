package mikrolabs.dev.sisdistribuidos.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.utils.NavigationUtils;

public class BaseController {
    @FXML
    private void abrirConfiguracoes(ActionEvent event) {
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        NavigationUtils.openConfigModal(currentStage);
    }
}
