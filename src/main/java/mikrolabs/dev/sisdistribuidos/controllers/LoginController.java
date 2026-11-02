package mikrolabs.dev.sisdistribuidos.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.DTOs.User;
import mikrolabs.dev.sisdistribuidos.exceptions.ServerConnectionError;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import mikrolabs.dev.sisdistribuidos.managers.SocketManager;
import mikrolabs.dev.sisdistribuidos.utils.FieldValidation;
import mikrolabs.dev.sisdistribuidos.utils.LoginResponseValidation;
import mikrolabs.dev.sisdistribuidos.utils.NavigationUtils;
import mikrolabs.dev.sisdistribuidos.utils.ProfileResponseValidation;
import mikrolabs.dev.sisdistribuidos.utils.Toast;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController extends BaseController {

    public Label returnLabel;
    public Button enviarBtn;
    public TextField usernameTextBox;
    public PasswordField passwordTextBox;
    public Button enviarRegisterBtn;
    public PasswordField passwordRegisterTextBox;
    public TextField usernameRegisterTextBox;
    public VBox registerBox;
    public Label returnRegisterLabel;
    public VBox loginBox;
    public Button switchModeRegisterButton;
    public Button switchModeLoginButton;
    public TextField nameRegisterTextBox;
    @FXML
    public ImageView welcomeLogoImage;
    @FXML
    public ImageView registerLogoImage;
    boolean isRegister;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        Image heroIcon = NavigationUtils.getClientHeaderIcon(136);
        if (welcomeLogoImage != null && heroIcon != null) {
            welcomeLogoImage.setImage(heroIcon);
        }
        if (registerLogoImage != null && heroIcon != null) {
            registerLogoImage.setImage(heroIcon);
        }
        Platform.runLater(() -> {
            Node rootNode = loginBox != null && loginBox.getScene() != null ? loginBox.getScene().getRoot() : loginBox;
            if (rootNode != null) {
                rootNode.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        if (loginBox.isVisible()) {
                            logar();
                        } else if (registerBox.isVisible()) {
                            registrar();
                        }
                        event.consume();
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
        String username = usernameTextBox.getText();
        String password = passwordTextBox.getText();
        Stage toast = (Stage) enviarBtn.getScene().getWindow();

        if (username.isEmpty()) {
            Toast.show(toast, "Informe o nome de usuário!", Toast.Type.INFO);
            return;
        }

        if (isInvalidUsername(toast, username)) return;
        enviarBtn.setDisable(true);

        new Thread(() -> {
            try {
                Response loginResponse = SocketManager.sendRequest(User.loginRequest(username, password));

                String extractedToken = null;
                String fetchedRole = null;
                IllegalArgumentException tokenError = null;

                if (loginResponse.statusCode() == 200) {
                    try {
                        extractedToken = LoginResponseValidation.extractToken(loginResponse);
                        try {
                            Response getUserResponse = SocketManager.sendRequest(User.getUserRequest(extractedToken, username));
                            if (getUserResponse.statusCode() == 200) {
                                var profile = ProfileResponseValidation.readData(getUserResponse.data());
                                if (profile != null && profile.role() != null) {
                                    fetchedRole = profile.role();
                                }
                            }
                        } catch (Exception ignored) {
                            // Role can also be fetched later in MainPage/Profile
                        }
                    } catch (IllegalArgumentException e) {
                        tokenError = e;
                    }
                }

                final String finalToken = extractedToken;
                final String finalRole = fetchedRole;
                final IllegalArgumentException finalTokenError = tokenError;

                Platform.runLater(() -> {
                    try {
                        if (loginResponse.statusCode() != 200) {
                            Toast.show(toast, loginResponse.message(), Toast.Type.ERROR);
                            return;
                        }
                        if (finalTokenError != null) {
                            throw finalTokenError;
                        }
                        ConfigManager.saveVariable("Token", finalToken);
                        ConfigManager.saveVariable("Username", username);
                        ConfigManager.saveRole(finalRole);
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
        String username = usernameRegisterTextBox.getText();
        String password = passwordRegisterTextBox.getText();
        Stage toast = (Stage) enviarRegisterBtn.getScene().getWindow();

        if (username.isEmpty() || password.isEmpty() || name.isEmpty()) {
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
                Response registerResponse = SocketManager.sendRequest(User.registerRequest(name, username, password));
                System.out.println("Received: " + registerResponse);

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

    private boolean isInvalidUsername(Stage stage, String username) {
        if (!FieldValidation.validUsername(username)) {
            Toast.show(stage, "Username: use 3–20 caracteres, com letras minúsculas, números, ponto ou sublinhado.", Toast.Type.INFO);
            return true;
        }
        return false;
    }

    private boolean validCredentials(Stage stage, String username, String password) {
        if (isInvalidUsername(stage, username)) return false;
        if (!FieldValidation.validPassword(password)) {
            Toast.show(stage, "Senha: use 8–20 caracteres, incluindo maiúscula, minúscula, número e símbolo permitido (# . * & % $ @ ! ( ) - _ = +).", Toast.Type.INFO);
            return false;
        }
        return true;
    }
}
