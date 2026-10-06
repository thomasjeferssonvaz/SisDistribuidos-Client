package mikrolabs.dev.sisdistribuidos.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.DTOs.Request;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.exceptions.ServerConnectionError;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import mikrolabs.dev.sisdistribuidos.managers.SocketManager;
import mikrolabs.dev.sisdistribuidos.utils.Toast;

import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

public class DeleteConfirmationController implements Initializable {
    public Label accountUsernameLabel;
    public TextField confirmationUsernameField;
    public Label confirmationFeedbackLabel;
    public Button cancelButton;
    public Button confirmDeleteButton;

    private boolean exclusaoEmAndamento = false;


    private final Gson gson = new Gson();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String username = ConfigManager.getUsername();

        accountUsernameLabel.setText(username);
        confirmationFeedbackLabel.setVisible(false);
        confirmationFeedbackLabel.setManaged(false);
        confirmDeleteButton.setDisable(true);

        confirmationUsernameField.textProperty().addListener(
                (observable, oldValue, newValue) -> {
                    boolean matches = username != null
                            && !username.isBlank()
                            && username.equals(newValue);

                    confirmDeleteButton.setDisable(exclusaoEmAndamento || !matches);

                    boolean showError = !newValue.isEmpty() && !matches;
                    confirmationFeedbackLabel.setVisible(showError);
                    confirmationFeedbackLabel.setManaged(showError);
                }
        );
    }


    @FXML
    private void deleteAccount(ActionEvent mouseEvent) {
        Stage toast = (Stage) confirmDeleteButton.getScene().getWindow();

        if (exclusaoEmAndamento) {
            return;
        }

        if (confirmationUsernameField.getText().isEmpty() || !Objects.equals(confirmationUsernameField.getText(), ConfigManager.getUsername())) {
            confirmationFeedbackLabel.setVisible(true);
            confirmationFeedbackLabel.setManaged(true);
            return;
        }

        exclusaoEmAndamento = true;
        confirmDeleteButton.setDisable(true);
        cancelButton.setDisable(true);

        new Thread(() -> {
            try {

                JsonElement data = gson.toJsonTree(Map.of(
                        "token", ConfigManager.getToken(),
                        "username", ConfigManager.getUsername()
                ));
                Response deleteUser = SocketManager.sendRequest(new Request("deleteuser", data));

                if (deleteUser == null) {
                    throw new ServerConnectionError();
                }

                Platform.runLater(() -> {

                    boolean isSuccess = deleteUser.statusCode() == 200;

                    if (isSuccess) {
                        Stage confirmationStage =
                                (Stage) confirmDeleteButton.getScene().getWindow();
                        Stage profileStage = (Stage) confirmationStage.getOwner();
                        Stage mainStage = (Stage) profileStage.getOwner();


                        ConfigManager.clearToken();
                        ConfigManager.clearUsername();

                        confirmationStage.close();
                        profileStage.close();

                        ProfileController.sendToLoginScreen(mainStage, deleteUser);
                    } else {
                        Toast.show(toast, deleteUser.message(), Toast.Type.ERROR);
                        finalizarRequisicao();
                    }
                });
            } catch (ServerConnectionError serverConnectionError) {
                Platform.runLater(() -> {
                    Toast.show(toast, serverConnectionError.getMessage(), Toast.Type.ERROR);
                    finalizarRequisicao();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.out.println(e.getMessage());
                    Toast.show(toast, "Erro inesperado na comunicação ao buscar usuário.", Toast.Type.ERROR);
                    finalizarRequisicao();
                });
            }
        }).start();
    }

    private void finalizarRequisicao() {
        exclusaoEmAndamento = false;
        cancelButton.setDisable(false);

        String username = ConfigManager.getUsername();
        boolean matches = username != null
                && !username.isBlank()
                && username.equals(confirmationUsernameField.getText());

        confirmDeleteButton.setDisable(!matches);
    }

    @FXML
    private void cancelar() {
        if (exclusaoEmAndamento) {
            return;
        }

        Stage confirmationStage =
                (Stage) cancelButton.getScene().getWindow();

        confirmationStage.close();
    }




}
