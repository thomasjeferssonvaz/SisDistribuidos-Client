package mikrolabs.dev.sisdistribuidos.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.DTOs.Request;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.DTOs.User;
import mikrolabs.dev.sisdistribuidos.exceptions.ServerConnectionError;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import mikrolabs.dev.sisdistribuidos.managers.SocketManager;
import mikrolabs.dev.sisdistribuidos.utils.NavigationUtils;
import mikrolabs.dev.sisdistribuidos.utils.Toast;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class ProfileController extends BaseController implements Initializable {

    public Button logoutButton;
    @FXML
    private TextField nameField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    private User currentUser;

    Gson gson = new Gson();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new Thread(() -> {
            try {
                String token = ConfigManager.getToken();
                JsonElement data = gson.toJsonTree(Map.of(
                        "token", token
                ));
                Response getUserResponse = SocketManager.sendRequest(new Request("getuser", data));
                if (getUserResponse == null) {
                    throw new ServerConnectionError();
                }

                Platform.runLater(() -> {
                    boolean isSuccess = getUserResponse.statusCode() == 200;

                    if (isSuccess && getUserResponse.data() != null) {
                        System.out.println("Received pré Gson: " + getUserResponse);
                        JsonObject jsonObject = getUserResponse.data().getAsJsonObject();
                        this.currentUser = gson.fromJson(jsonObject, User.class);
                        if (currentUser != null) {
                            nameField.setText(currentUser.name());
                            usernameField.setText(currentUser.username());
                            // O username fica explicitamente bloqueado/não editável
                            usernameField.setEditable(false);
                        }
                    } else {
                        Stage toast = (Stage) logoutButton.getScene().getWindow();
                        Toast.show(toast, getUserResponse.message(), Toast.Type.ERROR);
                    }
                });
            } catch (ServerConnectionError serverConnectionError) {
                Platform.runLater(() -> {
                    Stage toast = (Stage) logoutButton.getScene().getWindow();
                    Toast.show(toast, serverConnectionError.getMessage(), Toast.Type.ERROR);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.out.println(e.getMessage());
                    Stage toast = (Stage) logoutButton.getScene().getWindow();
                    Toast.show(toast, "Erro inesperado na comunicação ao buscar usuário.", Toast.Type.ERROR);
                });
            }
        }).start();
    }


    @FXML
    private void salvar() {
        Stage toast = (Stage) logoutButton.getScene().getWindow();
        String novoNome = nameField.getText();
        String senhaAtual = currentPasswordField.getText();
        String novaSenha = newPasswordField.getText();

        if (novoNome == null || novoNome.trim().isEmpty()) {
            Toast.show(toast, "O campo Nome não pode ficar vazio.",  Toast.Type.INFO);
            return;
        }

        boolean alterouSenha = false;

        // Se o usuário preencheu os campos de senha, valida o fluxo de reset
        if (!senhaAtual.isEmpty() || !novaSenha.isEmpty()) {
            if (senhaAtual.isEmpty() || novaSenha.isEmpty()) {
                Toast.show(toast, "Para alterar a senha, preencha tanto a senha atual quanto a nova senha.",  Toast.Type.INFO);
                return;
            }
            // Aqui você validaria se a senha atual confere e aplicaria a nova
            alterouSenha = true;
        }

        // Chamar o serviço/client para enviar a atualização para o servidor
        // Exemplo: ClientService.getInstance().updateProfile(currentUser.username(), novoNome, alterouSenha ? novaSenha : null);

        Toast.show(toast, "Alterações salvas com sucesso!",  Toast.Type.SUCCESS);


        // Limpa os campos de senha após salvar
        currentPasswordField.clear();
        newPasswordField.clear();
    }

    public void deslogar(ActionEvent mouseEvent) {
        Stage toast = (Stage) logoutButton.getScene().getWindow();
        String token = ConfigManager.getToken();
        Response logoutResponse;

        if (token != null) {
            JsonElement data = gson.toJsonTree(Map.of(
                    "token", token
            ));
            logoutResponse = SocketManager.sendRequest(new Request("logout", data));
            ConfigManager.clearToken();
        } else {
            logoutResponse = Response.error(400, "User não está logado");
        }

        if(logoutResponse.statusCode() == 200) {
            Toast.show(toast, logoutResponse.message(),  Toast.Type.SUCCESS);
        } else {
            Toast.show(toast, logoutResponse.message(),  Toast.Type.ERROR);
        }

        Stage modalStage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        Stage ownerStage = (Stage) modalStage.getOwner();


        modalStage.close();

        try {
            NavigationUtils.navigateTo(ownerStage, "views/Login.fxml", "Login");
        } catch (IOException e) {
            System.err.println("Erro ao carregar tela de login: " + e.getMessage());
        }
    }


}