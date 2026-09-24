package mikrolabs.dev.sisdistribuidos.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
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

public class LoginController extends BaseController implements Initializable {

    public Label returnLabel;
    public Button enviarBtn;
    public TextField usernameTextBox;
    public PasswordField passwordTextBox;
    public Button configBtn;
    public Button enviarRegisterBtn;
    public PasswordField passwordRegisterTextBox;
    public TextField usernameRegisterTextBox;
    public VBox registerBox;
    public Label returnRegisterLabel;
    public VBox loginBox;
    public Button switchModeRegisterButton;
    public Button switchModeLoginButton;
    public TextField nameRegisterTextBox;
    boolean isRegister;
    Gson gson = new Gson();
    @FXML
    private Node navbar;
    @FXML
    private Node footer;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        isRegister = false;
        registerBox.setVisible(false);
        registerBox.setManaged(false);
    }

    public void logar() {
        String username =  usernameTextBox.getText().trim();
        String password = passwordTextBox.getText();
        Stage toast = (Stage) enviarBtn.getScene().getWindow();



        if(username.isEmpty() || password.isEmpty()) {
            Toast.show(toast, "Preencha todos os campos!", Toast.Type.INFO);
            return;
        }

        enviarBtn.setDisable(true);


        new Thread(() -> {
            try {
                JsonElement data = gson.toJsonTree(Map.of(
                        "username", username,
                        "password", password
                ));

                Response loginResponse = SocketManager.sendRequest(new Request("login", data));
                if (loginResponse == null) {
                    throw new ServerConnectionError();
                }

                Platform.runLater(() -> {
                    enviarBtn.setDisable(false);
                    if (loginResponse.data() == null) {
                        Toast.show(toast, loginResponse.message(), Toast.Type.ERROR);
                    }

                    boolean isSuccess = loginResponse.statusCode() == 200;

                    if (isSuccess && loginResponse.data() != null) {
                        try {
                            System.out.println("Received pré Gson: " + loginResponse);
                            JsonObject jsonObject = loginResponse.data().getAsJsonObject();
                            String token = jsonObject.get("token").getAsString();
                            User user = new User("", "", token);

                            if (user.token() != null) {
                                Toast.show(toast, loginResponse.message(), Toast.Type.SUCCESS);
                                ConfigManager.saveToken(user.token());
                                mudarTela();
                            }
                        } catch (IOException e) {
                            Toast.show(toast, "Erro ao processar dados da sessão.", Toast.Type.INFO);
                        }
                    } else {
                        Toast.show(toast, loginResponse.message(), Toast.Type.ERROR);
                    }
                });
            } catch (ServerConnectionError serverConnectionError) {
                Platform.runLater(() -> {
                    Toast.show(toast, serverConnectionError.getMessage(), Toast.Type.ERROR);
                    enviarBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.out.println(e.getMessage());
                    Toast.show(toast, "Erro inesperado na comunicação.", Toast.Type.ERROR);
                    enviarBtn.setDisable(false);
                });
            }
        }).start();

    }

    public void switchMode() {
        isRegister = !isRegister;
        atualizarVisibilidade();
    }

    private void atualizarVisibilidade() {
        loginBox.setVisible(!isRegister);
        loginBox.setManaged(!isRegister);

        registerBox.setVisible(isRegister);
        registerBox.setManaged(isRegister);

        returnLabel.setText("");
        returnRegisterLabel.setText("");
    }

    private void mudarTela() throws IOException {
        Stage stage = (Stage) enviarBtn.getScene().getWindow();
        NavigationUtils.navigateTo(stage, "views/MainPage.fxml", "Client");
    }

    public void registrar() {
        String name = nameRegisterTextBox.getText().trim();
        String username =  usernameRegisterTextBox.getText().trim();
        String password = passwordRegisterTextBox.getText();
        Stage toast = (Stage) enviarRegisterBtn.getScene().getWindow();

        if(username.isEmpty() || password.isEmpty()) {
            Toast.show(toast, "Preencha todos os campos!", Toast.Type.INFO);
            return;
        }
        enviarRegisterBtn.setDisable(true);

        new Thread(() -> {
            try {
                JsonElement data = gson.toJsonTree(Map.of(
                        "name", name,
                        "username", username,
                        "password", password
                ));
                Response registerResponse = SocketManager.sendRequest(new Request("register", data));
                System.out.println("Received: " + registerResponse);
                if (registerResponse == null) {
                    throw new ServerConnectionError();
                }


                Platform.runLater(() -> {
                    enviarRegisterBtn.setDisable(false);

                    boolean isSuccess = "Usuário criado com sucesso".equalsIgnoreCase(registerResponse.message());

                    if (isSuccess) {
                        try {
                            Toast.show(toast, registerResponse.message(), Toast.Type.SUCCESS);
                            switchMode();
                        } catch (Exception e) {
                            Toast.show(toast, registerResponse.message(), Toast.Type.ERROR);
                        }
                    } else {
                        Toast.show(toast, registerResponse.message(), Toast.Type.ERROR);
                    }
                });

            } catch (ServerConnectionError serverConnectionError) {
                Platform.runLater(() -> {
                    Toast.show(toast, serverConnectionError.getMessage(), Toast.Type.ERROR);
                    enviarRegisterBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Toast.show(toast, "Erro inesperado na comunicação.", Toast.Type.ERROR);
                    enviarRegisterBtn.setDisable(false);
                });
            }
        }).start();
    }


}
