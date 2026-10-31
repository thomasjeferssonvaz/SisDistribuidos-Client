package mikrolabs.dev.sisdistribuidos.utils;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import mikrolabs.dev.sisdistribuidos.ClientApplication;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.controllers.DeleteConfirmationController;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NavigationUtils {

    private static final Pattern PATH_TAG_PATTERN = Pattern.compile("<path\\s+([^>]+?)/?>", Pattern.DOTALL);
    private static final Pattern ATTR_PATTERN = Pattern.compile("([a-zA-Z\\-]+)\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern SVG_TOKEN_PATTERN = Pattern.compile("([MmLlHhVvCcZz])|(-?\\d+(?:\\.\\d+)?)");
    private static final String FALLBACK_CLIENT_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256" width="256" height="256">
              <path d="M 68 16 H 188 C 216.7 16 240 39.3 240 68 V 188 C 240 216.7 216.7 240 188 240 H 68 C 39.3 240 16 216.7 16 188 V 68 C 16 39.3 39.3 16 68 16 Z" fill="#18181B" stroke="#2E2E36" stroke-width="4"/>
              <path d="M 70 26 H 186 C 210.3 26 230 45.7 230 70 V 186 C 230 210.3 210.3 230 186 230 H 70 C 45.7 230 26 210.3 26 186 V 70 C 26 45.7 45.7 26 70 26 Z" fill="#202026" stroke="#F59E0B" stroke-width="6"/>
              <path d="M 48 76 V 52 H 72" fill="none" stroke="#38BDF8" stroke-width="5"/>
              <path d="M 208 76 V 52 H 184" fill="none" stroke="#38BDF8" stroke-width="5"/>
              <path d="M 66 54 H 116 C 137 54 150 66 150 84 C 150 102 137 114 116 114 H 90 V 136 H 66 V 54 Z M 90 73 V 95 H 114 C 122 95 127 90.5 127 84 C 127 77.5 122 73 114 73 H 90 Z" fill="#F59E0B"/>
              <path d="M 186 54 L 166 86 H 183 L 173 116 L 203 78 H 185 L 186 54 Z" fill="#38BDF8"/>
              <path d="M 56 176 L 68 144 C 72 134 80 130 92 130 H 164 C 176 130 184 134 188 144 L 200 176 C 206 178 210 184 210 192 V 204 H 192 V 214 H 172 V 204 H 84 V 214 H 64 V 204 H 46 V 192 C 46 184 50 178 56 176 Z" fill="#FAFAFA"/>
              <path d="M 78 168 L 86 144 H 170 L 178 168 H 78 Z" fill="#202026"/>
              <path d="M 58 181 H 82 V 191 H 58 V 181 Z M 174 181 H 198 V 191 H 174 V 181 Z" fill="#F59E0B"/>
              <path d="M 96 183 H 160 V 189 H 96 V 183 Z" fill="#3B82F6"/>
            </svg>
            """;
    private static List<Image> cachedClientIcons;

    private NavigationUtils() {}

    /**
     * Aplica o ícone do Cliente a partir do arquivo SVG ("icon.svg") em múltiplas resoluções
     * para a barra de tarefas do Windows e abas/janelas abertas.
     */
    public static void applyClientIcon(Stage stage) {
        if (stage == null) return;
        if (cachedClientIcons == null || cachedClientIcons.isEmpty()) {
            cachedClientIcons = buildClientIconsFromSvg();
        }
        if (!cachedClientIcons.isEmpty()) {
            stage.getIcons().setAll(cachedClientIcons);
        }
    }

    public static Image getClientHeaderIcon(int size) {
        return renderSvgIconAtSize(loadClientSvgContent(), size);
    }

    private static List<Image> buildClientIconsFromSvg() {
        List<Image> icons = new ArrayList<>();
        String svgContent = loadClientSvgContent();

        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        for (int size : sizes) {
            Image rendered = renderSvgIconAtSize(svgContent, size);
            if (rendered != null) {
                icons.add(rendered);
            }
        }
        return icons;
    }

    private static String loadClientSvgContent() {
        try (InputStream is = ClientApplication.class.getResourceAsStream("icon.svg")) {
            if (is != null) {
                String s = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                if (!s.isBlank()) return s;
            }
        } catch (Exception ignored) {
        }
        try {
            Path localSvg = Path.of("src", "main", "resources", "mikrolabs", "dev", "sisdistribuidos", "icon.svg");
            if (Files.exists(localSvg)) {
                String s = Files.readString(localSvg, StandardCharsets.UTF_8);
                if (!s.isBlank()) return s;
            }
        } catch (Exception ignored) {
        }
        return FALLBACK_CLIENT_SVG;
    }

    private static Image renderSvgIconAtSize(String svgContent, int size) {
        try {
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            double scale = size / 256.0;
            g.scale(scale, scale);

            Matcher pathMatcher = PATH_TAG_PATTERN.matcher(svgContent);
            while (pathMatcher.find()) {
                String attrs = pathMatcher.group(1);
                String d = null;
                String fill = null;
                String stroke = null;
                float strokeWidth = 1.0f;

                Matcher attrMatcher = ATTR_PATTERN.matcher(attrs);
                while (attrMatcher.find()) {
                    String key = attrMatcher.group(1);
                    String val = attrMatcher.group(2);
                    switch (key) {
                        case "d" -> d = val;
                        case "fill" -> fill = val;
                        case "stroke" -> stroke = val;
                        case "stroke-width" -> {
                            try {
                                strokeWidth = Float.parseFloat(val);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }

                if (d != null && !d.isBlank()) {
                    Path2D.Double shape = parseSvgPath(d);
                    if (fill != null && !"none".equalsIgnoreCase(fill)) {
                        g.setColor(java.awt.Color.decode(fill));
                        g.fill(shape);
                    }
                    if (stroke != null && !"none".equalsIgnoreCase(stroke)) {
                        g.setColor(java.awt.Color.decode(stroke));
                        g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g.draw(shape);
                    }
                }
            }
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return new Image(new ByteArrayInputStream(baos.toByteArray()));
        } catch (Exception e) {
            return null;
        }
    }

    private static Path2D.Double parseSvgPath(String d) {
        Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        List<String> tokens = new ArrayList<>();
        Matcher m = SVG_TOKEN_PATTERN.matcher(d);
        while (m.find()) {
            tokens.add(m.group());
        }

        double cx = 0;
        double cy = 0;
        char cmd = 'M';
        int i = 0;
        while (i < tokens.size()) {
            String t = tokens.get(i);
            if (Character.isLetter(t.charAt(0))) {
                cmd = t.charAt(0);
                i++;
                if (cmd == 'Z' || cmd == 'z') {
                    path.closePath();
                    continue;
                }
            }
            switch (cmd) {
                case 'M' -> {
                    cx = Double.parseDouble(tokens.get(i++));
                    cy = Double.parseDouble(tokens.get(i++));
                    path.moveTo(cx, cy);
                    cmd = 'L';
                }
                case 'L' -> {
                    cx = Double.parseDouble(tokens.get(i++));
                    cy = Double.parseDouble(tokens.get(i++));
                    path.lineTo(cx, cy);
                }
                case 'H' -> {
                    cx = Double.parseDouble(tokens.get(i++));
                    path.lineTo(cx, cy);
                }
                case 'V' -> {
                    cy = Double.parseDouble(tokens.get(i++));
                    path.lineTo(cx, cy);
                }
                case 'C' -> {
                    double x1 = Double.parseDouble(tokens.get(i++));
                    double y1 = Double.parseDouble(tokens.get(i++));
                    double x2 = Double.parseDouble(tokens.get(i++));
                    double y2 = Double.parseDouble(tokens.get(i++));
                    cx = Double.parseDouble(tokens.get(i++));
                    cy = Double.parseDouble(tokens.get(i++));
                    path.curveTo(x1, y1, x2, y2, cx, cy);
                }
                default -> i++;
            }
        }
        return path;
    }

    /**
     * Troca a cena do Stage atual carregando o FXML, aplicando a folha de estilos global
     * e mantendo a consistência de tamanho e modo maximizado entre telas.
     */
    public static void navigateTo(Stage stage, String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource(fxmlPath));
        Parent root = loader.load();

        applyClientIcon(stage);
        stage.setMinWidth(960);
        stage.setMinHeight(620);
        stage.setTitle(title);

        var cssResource = ClientApplication.class.getResource("styles.css");
        String cssUrl = cssResource != null ? cssResource.toExternalForm() : null;

        Scene existingScene = stage.getScene();
        if (existingScene == null) {
            Scene scene = new Scene(root, 1280, 760);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl);
            }
            stage.setScene(scene);
            stage.setMaximized(true);
        } else {
            boolean wasMaximized = stage.isMaximized();
            boolean wasFullScreen = stage.isFullScreen();
            double prevWidth = stage.getWidth();
            double prevHeight = stage.getHeight();

            if (cssUrl != null && !existingScene.getStylesheets().contains(cssUrl)) {
                existingScene.getStylesheets().add(cssUrl);
            }
            existingScene.setRoot(root);

            if (wasFullScreen) {
                stage.setFullScreen(true);
            } else if (wasMaximized) {
                stage.setMaximized(true);
            } else if (prevWidth > 200 && prevHeight > 200) {
                stage.setWidth(prevWidth);
                stage.setHeight(prevHeight);
            }
        }
    }

    public static void returnToLogin(Stage source, Response response) {
        if (source == null) return;
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> returnToLogin(source, response));
            return;
        }

        Stage mainStage = findMainStage(source);
        for (Window window : new ArrayList<>(Window.getWindows())) {
            if (window instanceof Stage stage && stage != mainStage
                    && findMainStage(stage) == mainStage) {
                stage.close();
            }
        }

        try {
            navigateTo(mainStage, "views/Login.fxml", "Login");
            if (response != null) {
                Toast.Type type = response.statusCode() == 200 ? Toast.Type.SUCCESS : Toast.Type.ERROR;
                Toast.show(mainStage, response.message(), type);
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar tela de login: " + e.getMessage());
            Toast.show(mainStage, "Não foi possível abrir a tela de login.", Toast.Type.ERROR);
        }
    }

    private static Stage findMainStage(Stage source) {
        Stage mainStage = source;
        while (mainStage.getOwner() instanceof Stage owner) {
            mainStage = owner;
        }
        return mainStage;
    }

    /**
     * Abre a tela de configurações como modal vinculado ao Stage proprietário.
     */
    public static void openConfigModal(Stage ownerStage) {
        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("views/ConfigView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            applyClientIcon(stage);
            stage.setTitle("Configurações");

            Scene scene = new Scene(root);
            var cssResource = ClientApplication.class.getResource("styles.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }

            stage.setScene(scene);
            stage.initOwner(ownerStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.show();
        } catch (IOException e) {
            System.err.println("Erro ao abrir configurações: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void openProfileModal(Stage ownerStage) {
        if (!SessionUtils.ensureSession(ownerStage, ConfigManager.getToken(), ConfigManager.getUsername())) return;
        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("views/ProfileView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            applyClientIcon(stage);
            stage.setTitle("Profile");

            Scene scene = new Scene(root);
            var cssResource = ClientApplication.class.getResource("styles.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }

            stage.setScene(scene);
            stage.initOwner(ownerStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.show();
        } catch (IOException e) {
            System.err.println("Erro ao abrir Profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void openDeleteConfirmationModal(Stage ownerStage) {
        openDeleteConfirmationModal(ownerStage, null, null);
    }

    public static void openDeleteConfirmationModal(
            Stage ownerStage,
            String targetUsername,
            Consumer<Response> onTargetDeleted
    ) {
        if (!SessionUtils.ensureSession(ownerStage, ConfigManager.getToken(), ConfigManager.getUsername())) return;
        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("views/DeleteConfirmationView.fxml"));
            Parent root = loader.load();
            DeleteConfirmationController controller = loader.getController();
            if (targetUsername != null && !targetUsername.isBlank()) {
                controller.configureTargetUser(targetUsername, onTargetDeleted);
            }

            Stage stage = new Stage();
            applyClientIcon(stage);
            stage.setTitle("Delete Confirmation");

            Scene scene = new Scene(root);
            var cssResource = ClientApplication.class.getResource("styles.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }

            stage.setScene(scene);
            stage.initOwner(ownerStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.show();
        } catch (IOException e) {
            System.err.println("Erro ao abrir confirmação de exclusão: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
