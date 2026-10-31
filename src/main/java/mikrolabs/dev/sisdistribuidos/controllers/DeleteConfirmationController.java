package mikrolabs.dev.sisdistribuidos.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.DTOs.User;
import mikrolabs.dev.sisdistribuidos.exceptions.ServerConnectionError;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import mikrolabs.dev.sisdistribuidos.managers.SocketManager;
import mikrolabs.dev.sisdistribuidos.utils.NavigationUtils;
import mikrolabs.dev.sisdistribuidos.utils.SessionUtils;
import mikrolabs.dev.sisdistribuidos.utils.Toast;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class DeleteConfirmationController implements Initializable {
    public Label accountUsernameLabel;
    public TextField confirmationUsernameField;
    public Label confirmationFeedbackLabel;
    public Button cancelButton;
    public Button confirmDeleteButton;

    private boolean exclusaoEmAndamento = false;
    private String customTargetUsername = null;
    private Consumer<Response> onCustomTargetDeleted = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String target = resolveTargetUsername();

        accountUsernameLabel.setText(target);
        confirmationFeedbackLabel.setVisible(false);
        confirmationFeedbackLabel.setManaged(false);
        confirmDeleteButton.setDisable(true);

        confirmationUsernameField.textProperty().addListener(
                (observable, oldValue, newValue) -> {
                    String currentTarget = resolveTargetUsername();
                    boolean matches = ConfigManager.hasSession()
                            && currentTarget != null
                            && !currentTarget.isBlank()
                            && currentTarget.equals(newValue);

                    confirmDeleteButton.setDisable(exclusaoEmAndamento || !matches);

                    boolean showError = !newValue.isEmpty() && !matches;
                    confirmationFeedbackLabel.setVisible(showError);
                    confirmationFeedbackLabel.setManaged(showError);
                }
        );

        Platform.runLater(() -> {
            if (confirmDeleteButton.getScene() != null
                    && confirmDeleteButton.getScene().getWindow() instanceof Stage stage
                    && stage.isShowing()) {
                SessionUtils.ensureSession(stage, ConfigManager.getToken(), ConfigManager.getUsername());
            }
        });
    }

    public void configureTargetUser(String targetUsername, Consumer<Response> onDeletedCallback) {
        this.customTargetUsername = targetUsername;
        this.onCustomTargetDeleted = onDeletedCallback;
        accountUsernameLabel.setText(targetUsername);
        confirmationUsernameField.clear();
        confirmDeleteButton.setDisable(true);
    }

    private String resolveTargetUsername() {
        return customTargetUsername != null ? customTargetUsername : ConfigManager.getUsername();
    }

    @FXML
    private void deleteAccount(ActionEvent mouseEvent) {
        Stage toast = (Stage) confirmDeleteButton.getScene().getWindow();

        if (exclusaoEmAndamento) {
            return;
        }

        String token = ConfigManager.getToken();
        String sessionUsername = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(toast, token, sessionUsername)) {
            return;
        }

        String targetUsername = resolveTargetUsername();
        if (confirmationUsernameField.getText().isEmpty()
                || !Objects.equals(confirmationUsernameField.getText(), targetUsername)) {
            confirmationFeedbackLabel.setVisible(true);
            confirmationFeedbackLabel.setManaged(true);
            return;
        }

        boolean deletingAnotherAccount = customTargetUsername != null
                && !Objects.equals(customTargetUsername, sessionUsername);

        exclusaoEmAndamento = true;
        confirmDeleteButton.setDisable(true);
        cancelButton.setDisable(true);

        new Thread(() -> {
            try {
                Response deleteUser = SocketManager.sendRequest(User.deleteUserRequest(token, targetUsername));

                if (deleteUser == null) {
                    throw new ServerConnectionError();
                }

                Platform.runLater(() -> {
                    if (!toast.isShowing()) {
                        return;
                    }
                    if (SessionUtils.handleExpiredSession(toast, deleteUser)) {
                        return;
                    }

                    boolean isSuccess = deleteUser.statusCode() == 200;

                    if (isSuccess) {
                        if (deletingAnotherAccount) {
                            toast.close();
                            if (onCustomTargetDeleted != null) {
                                onCustomTargetDeleted.accept(deleteUser);
                            }
                        } else {
                            ConfigManager.clearSession();
                            NavigationUtils.returnToLogin(toast, deleteUser);
                        }
                    } else {
                        Toast.show(toast, deleteUser.message(), Toast.Type.ERROR);
                        finalizarRequisicao();
                    }
                });
            } catch (ServerConnectionError serverConnectionError) {
                Platform.runLater(() -> {
                    if (!toast.isShowing()) {
                        return;
                    }
                    Toast.show(toast, serverConnectionError.getMessage(), Toast.Type.ERROR);
                    finalizarRequisicao();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (!toast.isShowing()) {
                        return;
                    }
                    System.out.println(e.getMessage());
                    Toast.show(toast, "Erro inesperado na comunicação ao excluir usuário.", Toast.Type.ERROR);
                    finalizarRequisicao();
                });
            }
        }).start();
    }

    private void finalizarRequisicao() {
        exclusaoEmAndamento = false;
        cancelButton.setDisable(false);

        String targetUsername = resolveTargetUsername();
        boolean matches = ConfigManager.hasSession()
                && targetUsername != null
                && !targetUsername.isBlank()
                && targetUsername.equals(confirmationUsernameField.getText());

        confirmDeleteButton.setDisable(!matches);
    }

    @FXML
    private void cancelar() {
        if (exclusaoEmAndamento) {
            return;
        }

        Stage confirmationStage = (Stage) cancelButton.getScene().getWindow();
        confirmationStage.close();
    }
}
