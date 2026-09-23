package mikrolabs.dev.sisdistribuidos;

import javafx.application.Application;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import mikrolabs.dev.sisdistribuidos.utils.NavigationUtils;

import java.io.IOException;

public class ClientApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        String token = ConfigManager.getToken();
        boolean isLoggedIn = token != null && !"notloggedin".equals(token);

        String viewPath = isLoggedIn ? "views/MainPage.fxml" : "views/Login.fxml";
        String title = isLoggedIn ? "Client" : "Login";

        NavigationUtils.navigateTo(stage, viewPath, title);
        stage.show();
    }
}
