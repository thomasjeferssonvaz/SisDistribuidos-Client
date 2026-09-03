package mikrolabs.dev.sisdistribuidos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;

import java.io.IOException;

public class ClientApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        if (ConfigManager.getToken() != "notloggedin" ) {
            System.out.println("Token: " + ConfigManager.getToken());
            FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("views/MainPage.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 900, 600);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            stage.setTitle("Client");
            stage.setScene(scene);
            stage.show();
        } else {
            FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("views/Login.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 900, 600);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            stage.setTitle("Login");
            stage.setScene(scene);
            stage.show();
        }
    }
}
