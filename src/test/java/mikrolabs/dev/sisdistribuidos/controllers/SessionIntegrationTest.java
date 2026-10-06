package mikrolabs.dev.sisdistribuidos.controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import mikrolabs.dev.sisdistribuidos.ClientApplication;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class SessionIntegrationTest {
    private static final String TOKEN = "existing-server-token";
    private static final String USERNAME = "ana";
    private static final String PROFILE = response(200, "ok", "{\"name\":\"Ana\",\"username\":\"ana\"}");
    private static final String EXPIRED = response(401, "Sessão expirada ou encerrada.", "null");

    @BeforeAll
    static void startJavaFxInIsolatedDirectory() throws Exception {
        assertIsolatedDirectory();
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                started.countDown();
            });
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(() -> {
                Platform.setImplicitExit(false);
                started.countDown();
            });
        }
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX did not start.");
    }

    @BeforeEach
    void createSession() {
        assertIsolatedDirectory();
        ConfigManager.clearSession();
        ConfigManager.saveVariable("Token", TOKEN);
        ConfigManager.saveVariable("Username", USERNAME);
    }

    @AfterEach
    void closeWindowsAndClearSession() throws Exception {
        fx(() -> {
            for (Window window : new ArrayList<>(Window.getWindows())) window.hide();
            return null;
        });
        ConfigManager.clearSession();
    }

    @Test
    void startupKeepsCompleteExistingSessionWithoutRevalidatingTokenAsUuid() throws Exception {
        Stage main = startApplication();
        fx(() -> {
            assertEquals("Client", main.getTitle());
            assertNull(main.getScene().lookup("#usernameTextBox"));
            assertTrue(ConfigManager.hasSession());
            assertEquals(TOKEN, ConfigManager.getToken());
            return null;
        });
    }

    @Test
    void startupClearsIncompleteSessionsAndOpensLogin() throws Exception {
        for (String[] credentials : new String[][]{{TOKEN, null}, {null, USERNAME}, {null, null}}) {
            ConfigManager.saveVariable("Token", credentials[0]);
            ConfigManager.saveVariable("Username", credentials[1]);
            Stage main = startApplication();
            assertLogin(main);
            fx(() -> { main.close(); return null; });
        }
    }

    @Test
    void loginSendsEmptyAndOutOfPolicyPasswordsExactlyAsEntered() throws Exception {
        String message = "Usuario ou senhas inválidos";
        String rejected = response(401, message, "null");
        ConfigManager.clearSession();
        try (Peer peer = new Peer(rejected, rejected, rejected)) {
            Stage main = startApplication();
            for (String password : new String[]{"", "  á?  ", "x".repeat(21)}) {
                fx(() -> {
                    hidePopups();
                    field(main, "usernameTextBox").setText(USERNAME);
                    field(main, "passwordTextBox").setText(password);
                    Button submit = (Button) main.getScene().lookup("#enviarBtn");
                    submit.fire();
                    assertTrue(submit.isDisabled());
                    return null;
                });
                JsonObject request = peer.requests.poll(5, TimeUnit.SECONDS);
                assertNotNull(request, "Login must send the entered password, including an empty value.");
                assertEquals("login", request.get("method").getAsString());
                JsonObject data = request.getAsJsonObject("data");
                assertEquals(USERNAME, data.get("username").getAsString());
                assertEquals(password, data.get("password").getAsString());
                awaitFx(() -> !((Button) main.getScene().lookup("#enviarBtn")).isDisabled()
                        && hasToast("toast-error"));
                fx(() -> {
                    assertTrue(Window.getWindows().stream().filter(PopupWindow.class::isInstance)
                            .anyMatch(window -> window.getScene().lookup(".toast-error") instanceof Label label
                                    && message.equals(label.getText())));
                    return null;
                });
                assertLogin(main);
            }
        }
    }

    @Test
    void loginStillRejectsInvalidUsernamesAndRegistrationStillValidatesPasswords() throws Exception {
        ConfigManager.clearSession();
        try (Peer peer = new Peer()) {
            Stage main = startApplication();
            fx(() -> {
                field(main, "passwordTextBox").setText("Abcdef1!");
                Button login = (Button) main.getScene().lookup("#enviarBtn");
                for (String username : new String[]{"", "ab", "Ana"}) {
                    field(main, "usernameTextBox").setText(username);
                    login.fire();
                    assertFalse(login.isDisabled());
                }
                ((Button) main.getScene().lookup("#switchModeLoginButton")).fire();
                field(main, "nameRegisterTextBox").setText("Ana");
                field(main, "usernameRegisterTextBox").setText(USERNAME);
                Button register = (Button) main.getScene().lookup("#enviarRegisterBtn");
                for (String password : new String[]{"", "  á?  ", "x".repeat(21)}) {
                    field(main, "passwordRegisterTextBox").setText(password);
                    register.fire();
                    assertFalse(register.isDisabled());
                }
                assertTrue(main.getScene().lookup("#registerBox").isVisible());
                assertTrue(hasToast("toast-info"));
                assertFalse(ConfigManager.hasSession());
                return null;
            });
            peer.assertNoFurtherRequest();
        }
    }

    @Test
    void partialProfileDataUpdatesOnlyUsableFieldsAndKeepsLocalUsername() throws Exception {
        try (Peer peer = new Peer(
                response(200, "ok", "{\"name\":\"Ana Maria\"}"),
                response(200, "ok", "{\"name\":42,\"username\":\"bea\"}"))) {
            Loaded<ProfileController> profile = openProfile(startApplication());
            peer.nextRequest("getuser");
            awaitFx(() -> text(profile.stage(), "nameField").equals("Ana Maria"));
            fx(() -> {
                assertEquals(USERNAME, text(profile.stage(), "usernameField"));
                invoke(profile.controller(), "loadUserData");
                return null;
            });
            peer.nextRequest("getuser");
            awaitFx(() -> text(profile.stage(), "usernameField").equals("bea"));
            fx(() -> {
                assertEquals("Ana Maria", text(profile.stage(), "nameField"));
                assertFalse(field(profile.stage(), "usernameField").isEditable());
                assertEquals(USERNAME, ConfigManager.getUsername());
                return null;
            });
        }
    }

    @Test
    void unusableProfileDataPreservesDisplayedValuesAndShowsInformation() throws Exception {
        try (Peer peer = new Peer(PROFILE,
                response(200, "ok", "null"), response(200, "ok", "[]"),
                response(200, "ok", "\"invalid\""),
                response(200, "ok", "{\"name\":[],\"username\":false}"))) {
            Loaded<ProfileController> profile = openProfile(startApplication());
            peer.nextRequest("getuser");
            awaitFx(() -> text(profile.stage(), "nameField").equals("Ana"));
            for (int i = 0; i < 4; i++) {
                fx(() -> {
                    hidePopups();
                    invoke(profile.controller(), "loadUserData");
                    return null;
                });
                peer.nextRequest("getuser");
                awaitFx(() -> hasToast("toast-info"));
                fx(() -> {
                    assertEquals("Ana", text(profile.stage(), "nameField"));
                    assertEquals(USERNAME, text(profile.stage(), "usernameField"));
                    assertTrue(profile.stage().isShowing());
                    assertTrue(ConfigManager.hasSession());
                    return null;
                });
            }
        }
    }

    @Test
    void expiredProfileQueryClearsSessionAndClosesRelatedWindows() throws Exception {
        try (Peer peer = new Peer(EXPIRED)) {
            Stage main = startApplication();
            Loaded<ProfileController> profile = openProfile(main);
            peer.nextRequest("getuser");
            awaitLogin(main);
            fx(() -> { assertFalse(profile.stage().isShowing()); return null; });
        }
    }

    @Test
    void expiredNameUpdateReturnsToLogin() throws Exception {
        expiredProfileUpdate("salvarPerfil", "updateusername");
    }

    @Test
    void expiredPasswordUpdateReturnsToLogin() throws Exception {
        expiredProfileUpdate("alterarSenha", "updateuserpassword");
    }

    @Test
    void expiredDeleteClearsSessionAndClosesBothModals() throws Exception {
        deleteEndsSession(EXPIRED);
    }

    @Test
    void successfulDeleteClearsSessionAndClosesBothModals() throws Exception {
        deleteEndsSession(response(200, "Usuário excluído com sucesso.", "null"));
    }

    @Test
    void unauthorizedDeleteKeepsSessionAndAllowsRetryOrCancellation() throws Exception {
        try (Peer peer = new Peer(PROFILE, response(401, "Operação não autorizada", "null"))) {
            Stage main = startApplication();
            Loaded<ProfileController> profile = openProfile(main);
            peer.nextRequest("getuser");
            awaitFx(() -> text(profile.stage(), "nameField").equals("Ana"));
            Loaded<DeleteConfirmationController> deletion = openDelete(profile.stage());
            confirmDeletion(deletion);
            peer.nextRequest("deleteuser");
            awaitFx(() -> !deletion.controller().cancelButton.isDisabled());
            fx(() -> {
                assertTrue(ConfigManager.hasSession());
                assertEquals(TOKEN, ConfigManager.getToken());
                assertEquals(USERNAME, ConfigManager.getUsername());
                assertTrue(profile.stage().isShowing());
                assertTrue(deletion.stage().isShowing());
                assertFalse(deletion.controller().confirmDeleteButton.isDisabled());
                assertEquals("Client", main.getTitle());
                assertTrue(hasToast("toast-error"));
                deletion.controller().cancelButton.fire();
                assertFalse(deletion.stage().isShowing());
                assertTrue(profile.stage().isShowing());
                return null;
            });
        }
    }

    @Test
    void incompleteSessionBlocksProtectedActionsBeforeSendingAnyRequest() throws Exception {
        for (String action : new String[]{"salvarPerfil", "alterarSenha", "deleteAccount", "loadUserData"}) {
            createSession();
            try (Peer peer = new Peer(PROFILE)) {
                Stage main = startApplication();
                Loaded<ProfileController> profile = openProfile(main);
                peer.nextRequest("getuser");
                awaitFx(() -> text(profile.stage(), "nameField").equals("Ana"));
                Loaded<DeleteConfirmationController> deletion = action.equals("deleteAccount")
                        ? openDelete(profile.stage()) : null;
                fx(() -> {
                    ConfigManager.clearUsername();
                    if (deletion != null) {
                        invoke(deletion.controller(), "deleteAccount", new javafx.event.ActionEvent());
                    } else {
                        invoke(profile.controller(), action);
                    }
                    return null;
                });
                awaitLogin(main);
                peer.assertNoFurtherRequest();
                fx(() -> { main.close(); return null; });
            }
        }
    }

    private void expiredProfileUpdate(String action, String method) throws Exception {
        try (Peer peer = new Peer(PROFILE, EXPIRED)) {
            Stage main = startApplication();
            Loaded<ProfileController> profile = openProfile(main);
            peer.nextRequest("getuser");
            awaitFx(() -> text(profile.stage(), "nameField").equals("Ana"));
            Loaded<DeleteConfirmationController> sibling = openDelete(profile.stage());
            fx(() -> {
                if (action.equals("alterarSenha")) {
                    field(profile.stage(), "currentPasswordField").setText("Abcdef1!");
                    field(profile.stage(), "newPasswordField").setText("Abcdef2!");
                    field(profile.stage(), "confirmNewPasswordField").setText("Abcdef2!");
                } else {
                    field(profile.stage(), "nameField").setText("Ana Maria");
                }
                invoke(profile.controller(), action);
                return null;
            });
            JsonObject data = peer.nextRequest(method).getAsJsonObject("data");
            if (method.equals("updateusername")) {
                assertEquals("Ana Maria", data.get("name").getAsString());
            } else {
                assertEquals("Abcdef1!", data.get("oldPassword").getAsString());
                assertEquals("Abcdef2!", data.get("newPassword").getAsString());
            }
            awaitLogin(main);
            fx(() -> {
                assertFalse(profile.stage().isShowing());
                assertFalse(sibling.stage().isShowing());
                return null;
            });
        }
    }

    private void deleteEndsSession(String reply) throws Exception {
        try (Peer peer = new Peer(PROFILE, reply)) {
            Stage main = startApplication();
            Loaded<ProfileController> profile = openProfile(main);
            peer.nextRequest("getuser");
            awaitFx(() -> text(profile.stage(), "nameField").equals("Ana"));
            Loaded<DeleteConfirmationController> deletion = openDelete(profile.stage());
            confirmDeletion(deletion);
            peer.nextRequest("deleteuser");
            awaitLogin(main);
            fx(() -> {
                assertFalse(profile.stage().isShowing());
                assertFalse(deletion.stage().isShowing());
                return null;
            });
        }
    }

    private static void confirmDeletion(Loaded<DeleteConfirmationController> deletion) throws Exception {
        fx(() -> {
            deletion.controller().confirmationUsernameField.setText(USERNAME);
            assertFalse(deletion.controller().confirmDeleteButton.isDisabled());
            deletion.controller().confirmDeleteButton.fire();
            assertTrue(deletion.controller().cancelButton.isDisabled());
            assertTrue(deletion.controller().confirmDeleteButton.isDisabled());
            return null;
        });
    }

    private static Stage startApplication() throws Exception {
        return fx(() -> {
            Stage stage = new Stage();
            new ClientApplication().start(stage);
            return stage;
        });
    }

    private static Loaded<ProfileController> openProfile(Stage main) throws Exception {
        return loadModal(main, "views/ProfileView.fxml");
    }

    private static Loaded<DeleteConfirmationController> openDelete(Stage owner) throws Exception {
        return loadModal(owner, "views/DeleteConfirmationView.fxml");
    }

    private static <T> Loaded<T> loadModal(Stage owner, String resource) throws Exception {
        return fx(() -> {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource(resource));
            Scene scene = new Scene(loader.load());
            Stage modal = new Stage();
            modal.initOwner(owner);
            modal.setScene(scene);
            modal.show();
            return new Loaded<>(modal, loader.getController());
        });
    }

    private static TextField field(Stage stage, String id) {
        return (TextField) stage.getScene().lookup("#" + id);
    }

    private static String text(Stage stage, String id) {
        return field(stage, id).getText();
    }

    private static void invoke(Object controller, String action, Object... args) throws Exception {
        Class<?>[] types = args.length == 0 ? new Class<?>[0] : new Class<?>[]{javafx.event.ActionEvent.class};
        Method method = controller.getClass().getDeclaredMethod(action, types);
        method.setAccessible(true);
        method.invoke(controller, args);
    }

    private static void assertLogin(Stage main) throws Exception {
        fx(() -> {
            assertEquals("Login", main.getTitle());
            assertNotNull(main.getScene().lookup("#usernameTextBox"));
            assertTrue(main.isShowing());
            assertNull(ConfigManager.getToken());
            assertNull(ConfigManager.getUsername());
            return null;
        });
    }

    private static void awaitLogin(Stage main) throws Exception {
        awaitFx(() -> main.getScene().lookup("#usernameTextBox") != null);
        assertLogin(main);
    }

    private static boolean hasToast(String style) {
        return Window.getWindows().stream().filter(PopupWindow.class::isInstance)
                .anyMatch(window -> window.getScene().lookup("." + style) != null);
    }

    private static void hidePopups() {
        for (Window window : new ArrayList<>(Window.getWindows())) {
            if (window instanceof PopupWindow) window.hide();
        }
    }

    private static <T> T fx(Callable<T> task) throws Exception {
        FutureTask<T> future = new FutureTask<>(task);
        Platform.runLater(future);
        return future.get(10, TimeUnit.SECONDS);
    }

    private static void awaitFx(Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (fx(condition)) return;
            Thread.sleep(20);
        }
        fail("Expected JavaFX state was not reached within five seconds.");
    }

    private static void assertIsolatedDirectory() {
        Path directory = Path.of("").toAbsolutePath().normalize();
        assertEquals("test-workspace", directory.getFileName().toString(),
                "Run with Surefire's isolated workingDirectory before accessing ConfigManager.");
        assertEquals("target", directory.getParent().getFileName().toString());
    }

    private static String response(int status, String message, String data) {
        JsonObject object = new JsonObject();
        object.addProperty("statusCode", status);
        object.addProperty("message", message);
        object.add("data", JsonParser.parseString(data));
        return object.toString();
    }

    private record Loaded<T>(Stage stage, T controller) {}

    private static final class Peer implements AutoCloseable {
        private final ServerSocket server;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final BlockingQueue<JsonObject> requests = new LinkedBlockingQueue<>();
        private final Future<?> finished;

        private Peer(String... replies) throws Exception {
            server = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
            server.setSoTimeout(5000);
            ConfigManager.save("127.0.0.1", Integer.toString(server.getLocalPort()));
            finished = executor.submit(() -> {
                try {
                    for (String reply : replies) {
                        try (Socket socket = server.accept()) {
                            socket.setSoTimeout(5000);
                            BufferedReader input = new BufferedReader(new InputStreamReader(
                                    socket.getInputStream(), StandardCharsets.UTF_8));
                            requests.add(JsonParser.parseString(input.readLine()).getAsJsonObject());
                            new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8).println(reply);
                        }
                    }
                } catch (Exception failure) {
                    throw new RuntimeException(failure);
                }
            });
        }

        private JsonObject nextRequest(String method) throws Exception {
            JsonObject request = requests.poll(5, TimeUnit.SECONDS);
            assertNotNull(request, "Expected request: " + method);
            assertEquals(method, request.get("method").getAsString());
            JsonObject data = request.getAsJsonObject("data");
            assertEquals(TOKEN, data.get("token").getAsString());
            assertEquals(USERNAME, data.get("username").getAsString());
            return request;
        }

        private void assertNoFurtherRequest() throws Exception {
            finished.get(5, TimeUnit.SECONDS);
            server.setSoTimeout(250);
            assertThrows(java.net.SocketTimeoutException.class, () -> {
                try (Socket unexpected = server.accept()) {
                    fail("A protected request was sent with an incomplete session.");
                }
            });
            assertTrue(requests.isEmpty());
        }

        @Override
        public void close() throws Exception {
            try {
                finished.get(6, TimeUnit.SECONDS);
            } finally {
                server.close();
                executor.shutdownNow();
            }
        }
    }
}
