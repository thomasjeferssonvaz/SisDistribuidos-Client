package mikrolabs.dev.sisdistribuidos.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.DTOs.User;
import mikrolabs.dev.sisdistribuidos.exceptions.ServerConnectionError;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import mikrolabs.dev.sisdistribuidos.managers.SocketManager;
import mikrolabs.dev.sisdistribuidos.utils.FieldValidation;
import mikrolabs.dev.sisdistribuidos.utils.NavigationUtils;
import mikrolabs.dev.sisdistribuidos.utils.ProfileResponseValidation;
import mikrolabs.dev.sisdistribuidos.utils.SessionUtils;
import mikrolabs.dev.sisdistribuidos.utils.Toast;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController extends BaseController implements Initializable {

    @FXML
    public Button logoutButton;
    public Button deleteAccountButton;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        usernameField.setText(ConfigManager.getUsername());
        loadUserData();
        Platform.runLater(() -> {
            if (logoutButton != null && logoutButton.getScene() != null) {
                logoutButton.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ESCAPE && logoutButton.getScene().getWindow() instanceof Stage stage) {
                        stage.close();
                        event.consume();
                    }
                });
            }
        });
    }

    private void loadUserData() {
        String token = ConfigManager.getToken();
        String username = ConfigManager.getUsername();
        if (!ConfigManager.hasSession(token, username)) {
            Platform.runLater(() -> SessionUtils.ensureSession(getProfileStage(), token, username));
            return;
        }

        new Thread(() -> {
            try {
                Response getUserResponse = SocketManager.sendRequest(User.getUserRequest(token, username));

                if (getUserResponse == null) {
                    throw new ServerConnectionError();
                }

                var profileData = getUserResponse.statusCode() == 200
                        ? ProfileResponseValidation.readData(getUserResponse.data()) : null;

                Platform.runLater(() -> {
                    Stage toast = getProfileStage();
                    if (!toast.isShowing() || SessionUtils.handleExpiredSession(toast, getUserResponse)) {
                        return;
                    }

                    if (getUserResponse.statusCode() == 200) {
                        if (profileData == null) {
                            Toast.show(toast, "Não foi possível carregar os dados do perfil.", Toast.Type.INFO);
                            return;
                        }
                        if (profileData.name() != null) {
                            nameField.setText(profileData.name());
                        }
                        if (profileData.username() != null) {
                            usernameField.setText(profileData.username());
                        }
                        if (profileData.role() != null) {
                            ConfigManager.saveRole(profileData.role());
                        }
                    } else {
                        Toast.show(toast, getUserResponse.message(), Toast.Type.ERROR);
                    }
                });
            } catch (ServerConnectionError serverConnectionError) {
                Platform.runLater(() -> {
                    Stage toast = getProfileStage();
                    Toast.show(toast, serverConnectionError.getMessage(), Toast.Type.ERROR);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.out.println(e.getMessage());
                    Stage toast = getProfileStage();
                    Toast.show(toast, "Erro inesperado na comunicação ao buscar usuário.", Toast.Type.ERROR);
                });
            }
        }).start();
    }

    @FXML
    private void salvarPerfil() {
        Stage toast = getProfileStage();
        String token = ConfigManager.getToken();
        String username = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(toast, token, username)) return;
        String novoNome = nameField.getText();

        if (!FieldValidation.validName(novoNome)) {
            Toast.show(toast, "Nome: use apenas letras e espaços, entre 1 e 60 caracteres.", Toast.Type.INFO);
            return;
        }

        new Thread(() -> {
            try {
                Response updateUserNameResponse = SocketManager.sendRequest(User.updateUserNameRequest(token, username, novoNome));
                if (updateUserNameResponse == null) throw new ServerConnectionError();

                Platform.runLater(() -> {
                    if (!toast.isShowing() || SessionUtils.handleExpiredSession(toast, updateUserNameResponse)) {
                        return;
                    }
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
        Stage toast = getProfileStage();
        String token = ConfigManager.getToken();
        String username = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(toast, token, username)) return;
        String senhaAtual = currentPasswordField.getText();
        String novaSenha = newPasswordField.getText();
        String confirmaNovaSenha = confirmNewPasswordField.getText();

        if (senhaAtual.isEmpty() || novaSenha.isEmpty() || confirmaNovaSenha.isEmpty()) {
            Toast.show(toast, "Preencha todos os campos da seção Alterar Senha.", Toast.Type.INFO);
            return;
        }

        if (!FieldValidation.validPassword(senhaAtual) || !FieldValidation.validPassword(novaSenha)) {
            Toast.show(toast, "Senha: use 8–20 caracteres, incluindo maiúscula, minúscula, número e símbolo permitido (# . * & % $ @ ! ( ) - _ = +).", Toast.Type.INFO);
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

        new Thread(() -> {
            try {
                Response updateUserPasswordResponse = SocketManager.sendRequest(
                        User.updateUserPasswordRequest(token, username, senhaAtual, novaSenha));
                if (updateUserPasswordResponse == null) throw new ServerConnectionError();

                Platform.runLater(() -> {
                    if (!toast.isShowing() || SessionUtils.handleExpiredSession(toast, updateUserPasswordResponse)) {
                        return;
                    }
                    boolean isSuccess = updateUserPasswordResponse.statusCode() == 200;

                    if (isSuccess) {
                        Toast.show(toast, "Senha atualizado com sucesso!", Toast.Type.SUCCESS);
                        loadUserData();

                        currentPasswordField.clear();
                        newPasswordField.clear();
                        confirmNewPasswordField.clear();
                    } else {
                        Toast.show(toast, updateUserPasswordResponse.message(), Toast.Type.ERROR);
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
    public void deslogar(ActionEvent mouseEvent) {
        if (logoutButton.isDisabled()) return;
        Stage profileStage = getProfileStage();
        String token = ConfigManager.getToken();
        String username = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(profileStage, token, username)) return;
        logoutButton.setDisable(true);
        new Thread(() -> {
            try {
                Response response = SocketManager.sendRequest(User.logoutRequest(token));
                Platform.runLater(() -> {
                    try {
                        if (!profileStage.isShowing()) return;
                        if (response.statusCode() == 200 || response.statusCode() == 401) {
                            ConfigManager.clearSession();
                            NavigationUtils.returnToLogin(profileStage, response);
                        } else {
                            Toast.show(profileStage, response.message(), Toast.Type.ERROR);
                        }
                    } finally {
                        logoutButton.setDisable(false);
                    }
                });
            } catch (ServerConnectionError e) {
                Platform.runLater(() -> {
                    logoutButton.setDisable(false);
                    Toast.show(profileStage, e.getMessage(), Toast.Type.ERROR);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    logoutButton.setDisable(false);
                    Toast.show(profileStage, "Erro inesperado na comunicação ao deslogar.", Toast.Type.ERROR);
                });
            }
        }).start();
    }

    private Stage getProfileStage() {
        return (Stage) logoutButton.getScene().getWindow();
    }

    @FXML
    private void telaConfirmacaoDelete(ActionEvent event) {
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        NavigationUtils.openDeleteConfirmationModal(currentStage);
    }
}
