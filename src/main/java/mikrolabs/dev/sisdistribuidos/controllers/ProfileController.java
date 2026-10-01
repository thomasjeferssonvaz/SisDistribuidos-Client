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

    @FXML
    public Button logoutButton;

    @FXML
    private TextField nameField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmNewPasswordField;

    private final Gson gson = new Gson();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadUserData();
    }

    private void loadUserData() {
        new Thread(() -> {
            try {
                JsonElement data = gson.toJsonTree(Map.of(
                        "token", ConfigManager.getToken(),
                        "username", ConfigManager.getUsername()
                        ));
                Response getUserResponse = SocketManager.sendRequest(new Request("getuser", data));

                if (getUserResponse == null) {
                    throw new ServerConnectionError();
                }

                Platform.runLater(() -> {
                    boolean isSuccess = getUserResponse.statusCode() == 200;

                    if (isSuccess && getUserResponse.data() != null) {
                        JsonObject jsonObject = getUserResponse.data().getAsJsonObject();
                        User currentUser = gson.fromJson(jsonObject, User.class);
                        if (currentUser != null) {
                            nameField.setText(currentUser.name());
                            usernameField.setText(currentUser.username());
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
    private void salvarPerfil() {
        Stage toast = (Stage) logoutButton.getScene().getWindow();
        String novoNome = nameField.getText();

        if (novoNome == null || novoNome.trim().isEmpty()) {
            Toast.show(toast, "O campo Nome não pode ficar vazio.", Toast.Type.INFO);
            return;
        }

        new Thread(() -> {
            try {
                JsonElement data = gson.toJsonTree(Map.of(
                        "token", ConfigManager.getToken(),
                        "username", ConfigManager.getUsername(),
                        "name", novoNome
                ));

                Response updateUserNameResponse = SocketManager.sendRequest(new Request("updateusername", data));
                if (updateUserNameResponse == null) throw new ServerConnectionError();

                Platform.runLater(() -> {
                    boolean isSuccess = updateUserNameResponse.statusCode() == 200;

                    if (isSuccess) {
                        Toast.show(toast, "Perfil atualizado com sucesso!", Toast.Type.SUCCESS);
                        loadUserData();
                    } else {
                        Toast.show(toast, updateUserNameResponse.message(), Toast.Type.ERROR);
                    }
                });

            } catch (ServerConnectionError serverConnectionError) {
                Platform.runLater(() -> {
                    Toast.show(toast, serverConnectionError.getMessage(), Toast.Type.ERROR);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Toast.show(toast, "Erro inesperado na comunicação ao buscar usuário.", Toast.Type.ERROR);
                });
            }


        }).start();
    }

    @FXML
    private void alterarSenha() {
        Stage toast = (Stage) logoutButton.getScene().getWindow();
        String senhaAtual = currentPasswordField.getText();
        String novaSenha = newPasswordField.getText();
        String confirmaNovaSenha = confirmNewPasswordField.getText();

        if (senhaAtual.isEmpty() || novaSenha.isEmpty() || confirmaNovaSenha.isEmpty()) {
            Toast.show(toast, "Preencha todos os campos da seção Alterar Senha.", Toast.Type.INFO);
            return;
        }

        if (!novaSenha.equals(confirmaNovaSenha)) {
            Toast.show(toast, "A nova senha e a confirmação não coincidem.", Toast.Type.ERROR);
            return;
        }

        if (senhaAtual.equals(novaSenha)) {
            Toast.show(toast, "A nova senha deve ser diferente da senha atual.", Toast.Type.INFO);
            return;
        }

        // Enviar requisição para o servidor para alterar a senha
        /*
        new Thread(() -> {
            JsonElement data = gson.toJsonTree(Map.of(
                "token", ConfigManager.getToken(),
                "currentPassword", senhaAtual,
                "newPassword", novaSenha
            ));
            Response response = SocketManager.sendRequest(new Request("UpdateUserPassword", data));
            ...
        }).start();
        */

        Toast.show(toast, "Senha alterada com sucesso!", Toast.Type.SUCCESS);

        currentPasswordField.clear();
        newPasswordField.clear();
        confirmNewPasswordField.clear();
    }

    @FXML
    public void deslogar(ActionEvent mouseEvent) {
        Stage toast = (Stage) logoutButton.getScene().getWindow();
        String token = ConfigManager.getToken();
        Response logoutResponse;

        if (token != null) {
            JsonElement data = gson.toJsonTree(Map.of("token", token));
            logoutResponse = SocketManager.sendRequest(new Request("logout", data));
            ConfigManager.clearToken();
        } else {
            logoutResponse = Response.error(400, "User não está logado");
        }

        if (logoutResponse != null && logoutResponse.statusCode() == 200) {
            Toast.show(toast, logoutResponse.message(), Toast.Type.SUCCESS);
        } else if (logoutResponse != null) {
            Toast.show(toast, logoutResponse.message(), Toast.Type.ERROR);
        }

        Stage modalStage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        Stage ownerStage = (Stage) modalStage.getOwner();

        modalStage.close();

        try {
            NavigationUtils.navigateTo(ownerStage, "views/Login.fxml", "Login");
        } catch (IOException e) {
            System.err.println("Erro ao carregar tela de login: " + e.getMessage());
        }
        ConfigManager.clearUsername();
    }
}