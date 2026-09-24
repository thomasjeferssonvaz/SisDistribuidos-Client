package mikrolabs.dev.sisdistribuidos.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.DTOs.Request;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import mikrolabs.dev.sisdistribuidos.managers.SocketManager;
import mikrolabs.dev.sisdistribuidos.utils.NavigationUtils;
import mikrolabs.dev.sisdistribuidos.utils.Toast;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class ConfigController extends BaseController implements Initializable {


    public Button logout;
    public TextField serverIpField;
    public TextField serverPortField;
    Gson gson = new Gson();


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load saved preferences into the fields if they exist in this view
        if (serverIpField != null && serverPortField != null) {
            serverIpField.setText(ConfigManager.getServerIp());
            serverPortField.setText(String.valueOf(ConfigManager.getServerPort()));
        }


        String token = ConfigManager.getToken();
        boolean isLoggedIn = token != null && !"notloggedin".equals(token);
        if (token != null) {
            logout.setVisible(isLoggedIn);
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

    @FXML
    public void deslogar(MouseEvent mouseEvent) {
        JsonElement data = gson.toJsonTree(Map.of(
                "token", ConfigManager.getToken()
        ));
        Response logoutResponse = SocketManager.sendRequest(new Request("logout", data));
        ConfigManager.clearToken();

        Stage modalStage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        Stage ownerStage = (Stage) modalStage.getOwner();

        if(logoutResponse.statusCode() == 200) {
            Toast.show(ownerStage != null ? ownerStage : modalStage, logoutResponse.message(), Toast.Type.SUCCESS);
        } else {
            Toast.show(ownerStage != null ? ownerStage : modalStage, logoutResponse.message(), Toast.Type.ERROR);
        }


        modalStage.close();

        try {
            NavigationUtils.navigateTo(ownerStage, "views/Login.fxml", "Login");
        } catch (IOException e) {
            System.err.println("Erro ao carregar tela de login: " + e.getMessage());
        }
    }
}
