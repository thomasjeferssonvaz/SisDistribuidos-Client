package mikrolabs.dev.sisdistribuidos.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.DTOs.Request;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.exceptions.ServerConnectionError;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import mikrolabs.dev.sisdistribuidos.managers.SocketManager;
import mikrolabs.dev.sisdistribuidos.utils.NavigationUtils;
import mikrolabs.dev.sisdistribuidos.utils.Toast;
import mikrolabs.dev.sisdistribuidos.utils.FieldValidation;
import mikrolabs.dev.sisdistribuidos.utils.LoginResponseValidation;

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
        Platform.runLater(() -> {
            if (loginBox.getScene() != null) {
                loginBox.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        // Verifica qual card está visível no momento
                        if (loginBox.isVisible()) {
                            logar();
                        } else if (registerBox.isVisible()) {
                            registrar();
                        }
                        event.consume(); // Previne comportamentos indesejados
                    }
                });
            }
        });
        isRegister = false;
        registerBox.setVisible(false);
        registerBox.setManaged(false);

    }

    public void logar() {
        if (enviarBtn.isDisabled()) return;
        String username =  usernameTextBox.getText();
        String password = passwordTextBox.getText();
        Stage toast = (Stage) enviarBtn.getScene().getWindow();

        if(username.isEmpty() || password.isEmpty()) {
            Toast.show(toast, "Preencha todos os campos!", Toast.Type.INFO);
            return;
        }

        if (!validCredentials(toast, username, password)) return;
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
                    try {
                        if (loginResponse.statusCode() != 200) {
                            Toast.show(toast, loginResponse.message(), Toast.Type.ERROR);
                            return;
                        }
                        String token = LoginResponseValidation.extractToken(loginResponse);
                        ConfigManager.saveVariable("Token", token);
                        ConfigManager.saveVariable("Username", username);
                        mudarTela();
                        Toast.show(toast, loginResponse.message(), Toast.Type.SUCCESS);
                    } catch (IllegalArgumentException e) {
                        Toast.show(toast, "Resposta de login inválida: token ausente ou fora do formato UUID.", Toast.Type.ERROR);
                    } catch (IOException e) {
                        Toast.show(toast, "Não foi possível abrir a tela principal.", Toast.Type.ERROR);
                    } finally {
                        enviarBtn.setDisable(false);
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
        if (enviarRegisterBtn.isDisabled()) return;
        String name = nameRegisterTextBox.getText();
        String username =  usernameRegisterTextBox.getText();
        String password = passwordRegisterTextBox.getText();
        Stage toast = (Stage) enviarRegisterBtn.getScene().getWindow();

        if(username.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.show(toast, "Preencha todos os campos!", Toast.Type.INFO);
            return;
        }
        if (!FieldValidation.validName(name)) {
            Toast.show(toast, "Nome: use apenas letras e espaços, entre 1 e 60 caracteres.", Toast.Type.INFO);
            return;
        }
        if (!validCredentials(toast, username, password)) return;
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

                    boolean isSuccess = registerResponse.statusCode() == 201;

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


    private boolean validCredentials(Stage stage, String username, String password) {
        if (!FieldValidation.validUsername(username)) {
            Toast.show(stage, "Username: use 3–20 caracteres, com letras minúsculas, números, ponto ou sublinhado.", Toast.Type.INFO);
            return false;
        }
        if (!FieldValidation.validPassword(password)) {
            Toast.show(stage, "Senha: use 8–20 caracteres, incluindo maiúscula, minúscula, número e símbolo permitido (# . * & % $ @ ! ( ) - _ = +).", Toast.Type.INFO);
            return false;
        }
        return true;
    }
}
