package mikrolabs.dev.sisdistribuidos.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import mikrolabs.dev.sisdistribuidos.ClientApplication;
import mikrolabs.dev.sisdistribuidos.DTOs.Request;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import mikrolabs.dev.sisdistribuidos.DTOs.User;
import mikrolabs.dev.sisdistribuidos.DTOs.Vaga;
import mikrolabs.dev.sisdistribuidos.DTOs.Vehicle;
import mikrolabs.dev.sisdistribuidos.exceptions.ServerConnectionError;
import mikrolabs.dev.sisdistribuidos.managers.ConfigManager;
import mikrolabs.dev.sisdistribuidos.managers.SocketManager;
import mikrolabs.dev.sisdistribuidos.utils.FieldValidation;
import mikrolabs.dev.sisdistribuidos.utils.NavigationUtils;
import mikrolabs.dev.sisdistribuidos.utils.ProfileResponseValidation;
import mikrolabs.dev.sisdistribuidos.utils.SessionUtils;
import mikrolabs.dev.sisdistribuidos.utils.Toast;
import mikrolabs.dev.sisdistribuidos.utils.VagaResponseValidation;
import mikrolabs.dev.sisdistribuidos.utils.VehicleResponseValidation;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainPageController extends BaseController {

    @FXML
    private Node navbar;

    // Header de sessão e navegação entre módulos
    @FXML
    public Label sessionUserLabel;
    @FXML
    public Label sessionRoleBadge;
    @FXML
    public Button syncRoleBtn;
    @FXML
    public Button tabVagasBtn;
    @FXML
    public Button tabVehiclesBtn;
    @FXML
    public Button tabUsersAdminBtn;

    @FXML
    public VBox vagasPane;
    @FXML
    public VBox vehiclesPane;
    @FXML
    public VBox usersAdminPane;

    // ==================== ABA 1: MAPA VISUAL DE VAGAS ====================
    @FXML
    public TextField findVehiclePlateField;
    @FXML
    public Button findVehicleBtn;
    @FXML
    public Label findVehicleResultLabel;
    @FXML
    public Label vagasFilterSubtitleLabel;
    @FXML
    public ToggleButton toggleAllExceptOthersBtn;
    @FXML
    public ToggleButton toggleOtherUsersSpotsBtn;
    @FXML
    public Button refreshVagasBtn;
    @FXML
    public Button openCreateVagaModalBtn;
    @FXML
    public VBox vagasVisualContainer;

    // ==================== ABA 2: GALERIA VISUAL DE VEÍCULOS ====================
    @FXML
    public TextField searchVehiclePlateField;
    @FXML
    public Button searchVehicleBtn;
    @FXML
    public Button refreshVehiclesBtn;
    @FXML
    public Button openCreateVehicleModalBtn;
    @FXML
    public Label vehiclesGalleryTitleLabel;
    @FXML
    public Label vehiclesGallerySubtitleLabel;
    @FXML
    public FlowPane vehiclesVisualPane;

    // ==================== ABA 3: GESTÃO VISUAL DE USUÁRIOS (ADMIN) ====================
    @FXML
    public TextField targetSearchUsernameField;
    @FXML
    public Button searchTargetUserBtn;
    @FXML
    public Button openCreateUserModalBtn;
    @FXML
    public VBox userAdminResultContainer;

    private final List<Vaga> allVagasList = new ArrayList<>();
    private final ObservableList<Vaga> vagasList = FXCollections.observableArrayList();
    private final ObservableList<Vehicle> vehiclesList = FXCollections.observableArrayList();

    private String highlightedSpotName = null;
    private ProfileResponseValidation.ProfileData loadedTargetUser = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        if (toggleAllExceptOthersBtn != null) {
            toggleAllExceptOthersBtn.setSelected(true);
        }
        if (toggleOtherUsersSpotsBtn != null) {
            toggleOtherUsersSpotsBtn.setSelected(true);
        }

        applyRoleVisibility();
        showVagasTab();
        aplicarFiltroVagas();
        renderVehiclesVisualGrid();
        renderUserAdminResultCard();

        Platform.runLater(() -> {
            setupKeyboardShortcuts();
            carregarDadosIniciais();
        });
    }

    private void carregarDadosIniciais() {
        String token = ConfigManager.getToken();
        String username = ConfigManager.getUsername();
        if (!ConfigManager.hasSession(token, username) || ConfigManager.getRole() == null) {
            return;
        }
        if (ConfigManager.isUser()) {
            carregarMeusVeiculosSilenciosoEAtualizarVagas();
        } else {
            carregarVagas();
        }
    }

    private void carregarMeusVeiculosSilenciosoEAtualizarVagas() {
        String token = ConfigManager.getToken();
        if (token == null || token.isBlank()) return;
        new Thread(() -> {
            try {
                Response vehResp = SocketManager.sendRequest(Vehicle.getVehiclesRequest(token));
                if (vehResp.statusCode() == 200) {
                    List<Vehicle> vehicles = VehicleResponseValidation.readVehicles(vehResp.data());
                    Platform.runLater(() -> {
                        vehiclesList.setAll(vehicles);
                        renderVehiclesVisualGrid();
                        aplicarFiltroVagas();
                    });
                }
            } catch (Exception ignored) {
            } finally {
                Platform.runLater(this::carregarVagas);
            }
        }).start();
    }

    private void setupKeyboardShortcuts() {
        if (vagasPane == null || vagasPane.getScene() == null || vagasPane.getScene().getRoot() == null) return;
        vagasPane.getScene().getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() || event.isAltDown()) {
                if (event.getCode() == KeyCode.DIGIT1 || event.getCode() == KeyCode.NUMPAD1) {
                    showVagasTab();
                    event.consume();
                    return;
                }
                if (event.getCode() == KeyCode.DIGIT2 || event.getCode() == KeyCode.NUMPAD2) {
                    showVehiclesTab();
                    event.consume();
                    return;
                }
                if (event.getCode() == KeyCode.DIGIT3 || event.getCode() == KeyCode.NUMPAD3) {
                    if (ConfigManager.isAdmin()) {
                        showUsersAdminTab();
                    }
                    event.consume();
                    return;
                }
                if (event.getCode() == KeyCode.N) {
                    if (vagasPane.isVisible() && ConfigManager.isAdmin()) {
                        abrirModalCriarVaga();
                        event.consume();
                        return;
                    } else if (vehiclesPane != null && vehiclesPane.isVisible() && ConfigManager.isUser()) {
                        abrirModalCriarVeiculo();
                        event.consume();
                        return;
                    } else if (usersAdminPane != null && usersAdminPane.isVisible() && ConfigManager.isAdmin()) {
                        abrirModalCriarUsuarioAdmin();
                        event.consume();
                        return;
                    }
                }
            }
            if (event.getCode() == KeyCode.F5 || (event.isControlDown() && event.getCode() == KeyCode.R)) {
                if (vagasPane.isVisible()) {
                    carregarVagas();
                    event.consume();
                } else if (vehiclesPane != null && vehiclesPane.isVisible() && ConfigManager.isUser()) {
                    carregarMeusVeiculos();
                    event.consume();
                }
                return;
            }
            if (event.isControlDown() && event.getCode() == KeyCode.F) {
                if (vagasPane.isVisible() && findVehiclePlateField != null) {
                    findVehiclePlateField.requestFocus();
                    findVehiclePlateField.selectAll();
                    event.consume();
                } else if (vehiclesPane != null && vehiclesPane.isVisible() && searchVehiclePlateField != null) {
                    searchVehiclePlateField.requestFocus();
                    searchVehiclePlateField.selectAll();
                    event.consume();
                } else if (usersAdminPane != null && usersAdminPane.isVisible() && targetSearchUsernameField != null) {
                    targetSearchUsernameField.requestFocus();
                    targetSearchUsernameField.selectAll();
                    event.consume();
                }
            }
        });
    }

    public void applyRoleVisibility() {
        String username = ConfigManager.getUsername();
        String role = ConfigManager.getRole();
        boolean isAdmin = "admin".equals(role);
        boolean isUser = "user".equals(role);

        if (sessionUserLabel != null) {
            sessionUserLabel.setText(username != null ? username : "—");
        }
        if (sessionRoleBadge != null) {
            sessionRoleBadge.setText(role != null ? role.toUpperCase() : "SINCRONIZAR");
        }

        if (tabUsersAdminBtn != null) {
            tabUsersAdminBtn.setVisible(isAdmin);
            tabUsersAdminBtn.setManaged(isAdmin);
        }
        if (openCreateVagaModalBtn != null) {
            openCreateVagaModalBtn.setVisible(isAdmin);
            openCreateVagaModalBtn.setManaged(isAdmin);
        }
        if (refreshVehiclesBtn != null) {
            refreshVehiclesBtn.setVisible(isUser);
            refreshVehiclesBtn.setManaged(isUser);
        }
        if (openCreateVehicleModalBtn != null) {
            openCreateVehicleModalBtn.setVisible(isUser);
            openCreateVehicleModalBtn.setManaged(isUser);
        }
        if (vehiclesGalleryTitleLabel != null && vehiclesGallerySubtitleLabel != null) {
            if (isAdmin) {
                vehiclesGalleryTitleLabel.setText("Consulta Administrativa de Veículos");
                vehiclesGallerySubtitleLabel.setText("Digite qualquer placa no campo acima e clique em 'Abrir Ficha da Placa' para consultar ou excluir.");
            } else {
                vehiclesGalleryTitleLabel.setText("Minha Garagem de Veículos (clique em um cartão para ajustar)");
                vehiclesGallerySubtitleLabel.setText("Clique em qualquer veículo abaixo para alterar dados ou remover, ou use '+ Cadastrar Veículo'.");
            }
        }
    }

    @FXML
    public void showVagasTab() {
        setVisiblePane(vagasPane, tabVagasBtn);
    }

    @FXML
    public void showVehiclesTab() {
        setVisiblePane(vehiclesPane, tabVehiclesBtn);
        if (ConfigManager.isUser() && vehiclesList.isEmpty()) {
            carregarMeusVeiculos();
        } else {
            renderVehiclesVisualGrid();
        }
    }

    @FXML
    public void showUsersAdminTab() {
        if (!ConfigManager.isAdmin()) {
            Stage stage = getMainStage();
            if (stage != null) {
                Toast.show(stage, "Acesso restrito a administradores.", Toast.Type.ERROR);
            }
            return;
        }
        setVisiblePane(usersAdminPane, tabUsersAdminBtn);
    }

    private void setVisiblePane(VBox activePane, Button activeTabBtn) {
        VBox[] panes = new VBox[]{vagasPane, vehiclesPane, usersAdminPane};
        for (VBox pane : panes) {
            if (pane != null) {
                boolean active = pane == activePane;
                pane.setVisible(active);
                pane.setManaged(active);
            }
        }
        Button[] buttons = new Button[]{tabVagasBtn, tabVehiclesBtn, tabUsersAdminBtn};
        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-tab-active");
                if (btn == activeTabBtn && !btn.getStyleClass().contains("nav-tab-active")) {
                    btn.getStyleClass().add("nav-tab-active");
                }
            }
        }
    }

    // ==================== SINCRONIZAÇÃO DE PERFIL PRÓPRIO ====================
    @FXML
    public void sincronizarPerfil() {
        Stage stage = getMainStage();
        String token = ConfigManager.getToken();
        String username = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(stage, token, username)) return;

        setButtonBusy(syncRoleBtn, true);
        new Thread(() -> {
            try {
                Response response = SocketManager.sendRequest(User.getUserRequest(token, username));

                var profileData = response.statusCode() == 200
                        ? ProfileResponseValidation.readData(response.data()) : null;

                Platform.runLater(() -> {
                    setButtonBusy(syncRoleBtn, false);
                    if (stage == null || !stage.isShowing() || SessionUtils.handleExpiredSession(stage, response)) {
                        return;
                    }
                    if (response.statusCode() == 200 && profileData != null) {
                        ConfigManager.saveRole(profileData.role());
                        applyRoleVisibility();
                        carregarDadosIniciais();
                        Toast.show(stage, "Sessão sincronizada (" + (profileData.role() != null ? profileData.role() : "user") + ").", Toast.Type.SUCCESS);
                    } else {
                        Toast.show(stage, response.message(), Toast.Type.ERROR);
                    }
                });
            } catch (ServerConnectionError e) {
                Platform.runLater(() -> {
                    setButtonBusy(syncRoleBtn, false);
                    Toast.show(stage, e.getMessage(), Toast.Type.ERROR);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setButtonBusy(syncRoleBtn, false);
                    Toast.show(stage, "Erro inesperado ao sincronizar sessão.", Toast.Type.ERROR);
                });
            }
        }).start();
    }

    // ==================== MAPA VISUAL DE VAGAS & FILTROS ====================
    @FXML
    public void carregarVagas() {
        Stage stage = getMainStage();
        String token = ConfigManager.getToken();
        String username = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(stage, token, username)) return;

        setButtonBusy(refreshVagasBtn, true);
        new Thread(() -> {
            try {
                Response response = SocketManager.sendRequest(Vaga.getVagasRequest(token));

                Platform.runLater(() -> {
                    setButtonBusy(refreshVagasBtn, false);
                    if (stage == null || !stage.isShowing() || SessionUtils.handleExpiredSession(stage, response)) {
                        return;
                    }
                    if (response.statusCode() != 200) {
                        Toast.show(stage, response.message(), Toast.Type.ERROR);
                        return;
                    }
                    try {
                        List<Vaga> vagas = VagaResponseValidation.readVagas(response.data());
                        allVagasList.clear();
                        allVagasList.addAll(vagas);
                        aplicarFiltroVagas();
                        renderVehiclesVisualGrid();
                    } catch (IllegalArgumentException e) {
                        Toast.show(stage, e.getMessage(), Toast.Type.ERROR);
                    }
                });
            } catch (ServerConnectionError e) {
                Platform.runLater(() -> {
                    setButtonBusy(refreshVagasBtn, false);
                    Toast.show(stage, e.getMessage(), Toast.Type.ERROR);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setButtonBusy(refreshVagasBtn, false);
                    Toast.show(stage, "Erro inesperado ao listar vagas.", Toast.Type.ERROR);
                });
            }
        }).start();
    }

    @FXML
    public void alternarFiltroSemTerceiros() {
        boolean active = toggleAllExceptOthersBtn != null && toggleAllExceptOthersBtn.isSelected();
        aplicarFiltroVagas();
        Stage stage = getMainStage();
        if (stage != null) {
            if (active) {
                Toast.show(stage, "Filtro ativo: exibindo também vagas fora de serviço.", Toast.Type.INFO);
            } else {
                Toast.show(stage, "Vagas fora de serviço ocultadas do mapa.", Toast.Type.INFO);
            }
        }
    }

    @FXML
    public void alternarFiltroOcupadasOutros() {
        boolean active = toggleOtherUsersSpotsBtn != null && toggleOtherUsersSpotsBtn.isSelected();
        aplicarFiltroVagas();
        Stage stage = getMainStage();
        if (stage != null) {
            if (active) {
                Toast.show(stage, "Vagas ocupadas por outros usuários liberadas no mapa.", Toast.Type.INFO);
            } else {
                Toast.show(stage, "Vagas ocupadas por outros usuários ocultadas.", Toast.Type.INFO);
            }
        }
    }

    @FXML
    public void aplicarFiltroVagas() {
        boolean showAllExceptOthers = toggleAllExceptOthersBtn != null && toggleAllExceptOthersBtn.isSelected();
        boolean showOtherUsersSpots = toggleOtherUsersSpotsBtn != null && toggleOtherUsersSpotsBtn.isSelected();

        List<Vaga> filtered = new ArrayList<>();
        for (Vaga vaga : allVagasList) {
            if (shouldDisplayVaga(vaga, showAllExceptOthers, showOtherUsersSpots)) {
                filtered.add(vaga);
            }
        }

        vagasList.setAll(filtered);
        updateVagasFilterSubtitle(showAllExceptOthers, showOtherUsersSpots);
        renderVagasVisualGrid();
    }

    private boolean shouldDisplayVaga(Vaga vaga, boolean showAllExceptOthers, boolean showOtherUsersSpots) {
        if (vaga == null || vaga.status() == null) return false;
        return switch (vaga.status()) {
            case "disponivel" -> true;
            case "ocupada" -> isUserOwnPlate(vaga.licensePlate()) || showOtherUsersSpots;
            default -> showAllExceptOthers;
        };
    }

    private boolean isThirdPartyOccupiedSpot(Vaga vaga) {
        return vaga != null && "ocupada".equals(vaga.status()) && !isUserOwnPlate(vaga.licensePlate());
    }

    private boolean isUserOwnPlate(String licensePlate) {
        if (licensePlate == null || licensePlate.isBlank()) return false;
        String normalized = licensePlate.trim();
        for (Vehicle v : vehiclesList) {
            if (v != null && v.licensePlate() != null && v.licensePlate().trim().equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    private void updateVagasFilterSubtitle(boolean showAllExceptOthers, boolean showOtherUsersSpots) {
        if (vagasFilterSubtitleLabel == null) return;
        long hiddenThirdPartyCount = showOtherUsersSpots
                ? 0
                : allVagasList.stream().filter(this::isThirdPartyOccupiedSpot).count();
        String hiddenSuffix = hiddenThirdPartyCount > 0
                ? " (" + hiddenThirdPartyCount + (hiddenThirdPartyCount == 1 ? " vaga de terceiro oculta" : " vagas de terceiros ocultas") + ")"
                : "";

        if (showAllExceptOthers && showOtherUsersSpots) {
            vagasFilterSubtitleLabel.setText("Exibindo todas as vagas do estacionamento (incluindo fora de serviço e terceiros)");
        } else if (showAllExceptOthers) {
            vagasFilterSubtitleLabel.setText("Exibindo todas as vagas (disponíveis, fora de serviço e seus veículos), sem terceiros" + hiddenSuffix);
        } else if (showOtherUsersSpots) {
            vagasFilterSubtitleLabel.setText("Exibindo vagas disponíveis e todas as vagas ocupadas (próprias e de outros usuários)");
        } else {
            vagasFilterSubtitleLabel.setText("Exibindo apenas vagas disponíveis e ocupadas pelos seus veículos" + hiddenSuffix);
        }
    }

    private void renderVagasVisualGrid() {
        if (vagasVisualContainer == null) return;
        vagasVisualContainer.getChildren().clear();

        boolean showAllExceptOthers = toggleAllExceptOthersBtn != null && toggleAllExceptOthersBtn.isSelected();
        boolean showOtherUsersSpots = toggleOtherUsersSpotsBtn != null && toggleOtherUsersSpotsBtn.isSelected();

        if (vagasList.isEmpty() && (!showAllExceptOthers || allVagasList.isEmpty())) {
            VBox emptyBox = new VBox(8);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40, 20, 40, 20));
            Label emptyTitle = new Label("Nenhuma vaga para exibir com o filtro atual");
            emptyTitle.getStyleClass().add("section-header-label");
            Label emptySub = new Label("Use os filtros acima ou clique em 'Atualizar Vagas' para carregar o pátio.");
            emptySub.getStyleClass().add("card-subtitle");
            emptyBox.getChildren().addAll(emptyTitle, emptySub);
            vagasVisualContainer.getChildren().add(emptyBox);
            return;
        }

        List<Vaga> sortedVisible = new ArrayList<>(vagasList);
        sortedVisible.sort(Comparator.comparingInt(Vaga::floor).thenComparing(Vaga::spotName, String.CASE_INSENSITIVE_ORDER));

        Map<Integer, List<Vaga>> visibleByFloor = new LinkedHashMap<>();
        for (Vaga vaga : sortedVisible) {
            visibleByFloor.computeIfAbsent(vaga.floor(), _ -> new ArrayList<>()).add(vaga);
        }

        Map<Integer, List<Vaga>> hiddenThirdPartyByFloor = new LinkedHashMap<>();
        if (!showOtherUsersSpots) {
            List<Vaga> allSorted = new ArrayList<>(allVagasList);
            allSorted.sort(Comparator.comparingInt(Vaga::floor).thenComparing(Vaga::spotName, String.CASE_INSENSITIVE_ORDER));
            for (Vaga vaga : allSorted) {
                if (isThirdPartyOccupiedSpot(vaga)) {
                    hiddenThirdPartyByFloor.computeIfAbsent(vaga.floor(), _ -> new ArrayList<>()).add(vaga);
                    if (showAllExceptOthers) {
                        visibleByFloor.computeIfAbsent(vaga.floor(), _ -> new ArrayList<>());
                    }
                }
            }
        }

        for (Map.Entry<Integer, List<Vaga>> entry : visibleByFloor.entrySet()) {
            int floor = entry.getKey();
            List<Vaga> spotsOnFloor = entry.getValue();
            List<Vaga> hiddenOnFloor = hiddenThirdPartyByFloor.getOrDefault(floor, List.of());

            VBox floorCard = new VBox(12);
            floorCard.getStyleClass().add("floor-section-card");

            HBox floorHeader = new HBox(10);
            floorHeader.setAlignment(Pos.CENTER_LEFT);
            Label floorBadge = new Label("Andar " + floor);
            floorBadge.getStyleClass().add("floor-badge");

            long freeCount = spotsOnFloor.stream().filter(v -> "disponivel".equals(v.status())).count();
            long outCount = spotsOnFloor.stream().filter(v -> "fora_de_servico".equals(v.status())).count();
            StringBuilder summaryText = new StringBuilder();
            summaryText.append(spotsOnFloor.size()).append(spotsOnFloor.size() == 1 ? " vaga visível" : " vagas visíveis")
                    .append(" • ").append(freeCount).append(freeCount == 1 ? " disponível" : " disponíveis");
            if (outCount > 0) {
                summaryText.append(" • ").append(outCount).append(" fora de serviço");
            }
            if (!hiddenOnFloor.isEmpty()) {
                summaryText.append(" • ").append(hiddenOnFloor.size())
                        .append(hiddenOnFloor.size() == 1 ? " ocupada por outro (oculta)" : " ocupadas por outros (ocultas)");
            }

            Label floorSummary = new Label(summaryText.toString());
            floorSummary.getStyleClass().add("card-subtitle");

            floorHeader.getChildren().addAll(floorBadge, floorSummary);

            FlowPane spotsFlow = new FlowPane(12, 12);
            for (Vaga vaga : spotsOnFloor) {
                spotsFlow.getChildren().add(createSpotCard(vaga));
            }
            if (showAllExceptOthers && !showOtherUsersSpots && !hiddenOnFloor.isEmpty()) {
                spotsFlow.getChildren().add(createHiddenThirdPartyCard(hiddenOnFloor));
            }

            floorCard.getChildren().addAll(floorHeader, spotsFlow);
            vagasVisualContainer.getChildren().add(floorCard);
        }
    }

    private VBox createHiddenThirdPartyCard(List<Vaga> hiddenSpots) {
        VBox tile = new VBox(8);
        tile.setPrefWidth(215);
        tile.setMinWidth(195);
        tile.setMaxWidth(235);
        tile.getStyleClass().addAll("spot-tile", "spot-tile-out");
        tile.setFocusTraversable(true);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        String countTitle = hiddenSpots.size() == 1 ? "1 Vaga Oculta" : hiddenSpots.size() + " Vagas Ocultas";
        Label titleLbl = new Label(countTitle);
        titleLbl.getStyleClass().add("spot-code-label");
        titleLbl.setStyle("-fx-font-size: 14.5px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label("TERCEIROS");
        badge.getStyleClass().add("spot-status-pill-out");
        topRow.getChildren().addAll(titleLbl, spacer, badge);

        StringBuilder codes = new StringBuilder("Baias: ");
        for (int i = 0; i < hiddenSpots.size(); i++) {
            if (i > 0) codes.append(", ");
            codes.append(hiddenSpots.get(i).spotName());
        }
        Label spotsCodesLbl = new Label(codes.toString());
        spotsCodesLbl.getStyleClass().add("type-pill-eletrico");

        Label hintLbl = new Label("Clique para revelar vagas de outros");
        hintLbl.getStyleClass().add("card-subtitle");

        tile.getChildren().addAll(topRow, spotsCodesLbl, hintLbl);
        Runnable revealAction = () -> {
            if (toggleOtherUsersSpotsBtn != null) {
                toggleOtherUsersSpotsBtn.setSelected(true);
                alternarFiltroOcupadasOutros();
            }
        };
        tile.setOnMouseClicked(_ -> revealAction.run());
        tile.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                revealAction.run();
                e.consume();
            }
        });
        return tile;
    }

    private VBox createSpotCard(Vaga vaga) {
        VBox tile = new VBox(8);
        tile.setPrefWidth(205);
        tile.setMinWidth(190);
        tile.setMaxWidth(220);
        tile.getStyleClass().add("spot-tile");
        tile.setFocusTraversable(true);

        boolean ownCar = "ocupada".equals(vaga.status()) && isUserOwnPlate(vaga.licensePlate());
        String statusText;
        String statusPillClass;

        if ("disponivel".equals(vaga.status())) {
            tile.getStyleClass().add("spot-tile-available");
            statusText = "LIVRE";
            statusPillClass = "spot-status-pill-available";
        } else if (ownCar) {
            tile.getStyleClass().add("spot-tile-own");
            statusText = "SEU VEÍCULO";
            statusPillClass = "spot-status-pill-own";
        } else if ("ocupada".equals(vaga.status())) {
            tile.getStyleClass().add("spot-tile-occupied");
            statusText = "OCUPADA";
            statusPillClass = "spot-status-pill-occupied";
        } else {
            tile.getStyleClass().add("spot-tile-out");
            statusText = "FORA DE SERVIÇO";
            statusPillClass = "spot-status-pill-out";
        }

        if (highlightedSpotName != null && highlightedSpotName.equalsIgnoreCase(vaga.spotName())) {
            tile.getStyleClass().add("spot-tile-highlighted");
        }

        // Top Row: Spot Name + Status Badge
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label spotCode = new Label(vaga.spotName());
        spotCode.getStyleClass().add("spot-code-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label statusPill = new Label(statusText);
        statusPill.getStyleClass().add(statusPillClass);
        topRow.getChildren().addAll(spotCode, spacer, statusPill);

        // Middle Row: Type Pill
        boolean isElectric = "eletrico".equalsIgnoreCase(vaga.type());
        Label typePill = new Label(isElectric ? "⚡ Elétrico" : "⛽ Combustão");
        typePill.getStyleClass().add(isElectric ? "type-pill-eletrico" : "type-pill-combustao");

        // Bottom Row: Plate or Action Hint
        HBox bottomRow = new HBox(6);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        if (vaga.licensePlate() != null && !vaga.licensePlate().isBlank()) {
            Label plateLabel = new Label(vaga.licensePlate());
            plateLabel.getStyleClass().add("plate-badge");
            bottomRow.getChildren().add(plateLabel);
        } else {
            Label actionHint = new Label("Clique para estacionar / ajustar");
            actionHint.getStyleClass().add("card-subtitle");
            bottomRow.getChildren().add(actionHint);
        }

        tile.getChildren().addAll(topRow, typePill, bottomRow);

        tile.setOnMouseClicked(_ -> abrirModalAjustarVaga(vaga));
        tile.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                abrirModalAjustarVaga(vaga);
                e.consume();
            }
        });

        return tile;
    }

    // ==================== MODAIS MENORES DE VAGA (CRIAR / AJUSTAR / EXCLUIR) ====================
    @FXML
    public void abrirModalCriarVaga() {
        Stage owner = getMainStage();
        String token = ConfigManager.getToken();
        String username = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(owner, token, username)) return;

        Stage modal = createStyledModal(owner, "Nova Vaga", 420);
        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");

        Label title = new Label("Cadastrar Nova Vaga");
        title.getStyleClass().add("card-title");
        Label subtitle = new Label("Defina o identificador (1–4 caracteres), andar e tipo. O status inicial será 'disponivel'.");
        subtitle.getStyleClass().add("card-subtitle");
        subtitle.setWrapText(true);

        TextField spotNameField = new TextField();
        spotNameField.setPromptText("Ex.: A1 (1 a 4 caracteres)");

        TextField floorField = new TextField();
        floorField.setPromptText("Ex.: 1");

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("combustao", "eletrico"));
        typeCombo.setValue("combustao");
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("disponivel"));
        statusCombo.setValue("disponivel");
        statusCombo.setDisable(true);
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        Button cancelBtn = new Button("Cancelar");
        cancelBtn.getStyleClass().add("secondary-button");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(_ -> modal.close());

        Button saveBtn = new Button("Criar Vaga");
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setDefaultButton(true);

        Runnable submitAction = () -> {
            String spotName = textOf(spotNameField).trim();
            String floorText = textOf(floorField).trim();
            String type = typeCombo.getValue();
            String status = statusCombo.getValue();

            if (!FieldValidation.validSpotName(spotName)) {
                Toast.show(modal, "Informe o identificador da vaga (1 a 4 caracteres).", Toast.Type.INFO);
                return;
            }
            Integer floor = FieldValidation.parseFloor(floorText);
            if (floor == null) {
                Toast.show(modal, "O andar deve ser um número inteiro válido.", Toast.Type.INFO);
                return;
            }
            if (!FieldValidation.validType(type) || !FieldValidation.validVagaStatus(status)) {
                Toast.show(modal, "Selecione tipo e status válidos.", Toast.Type.INFO);
                return;
            }

            setButtonBusy(saveBtn, true);
            new Thread(() -> {
                try {
                    Response response = SocketManager.sendRequest(Vaga.createVagaRequest(token, spotName, floor, type));

                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        if (!modal.isShowing() || SessionUtils.handleExpiredSession(owner, response)) {
                            return;
                        }
                        if (response.statusCode() == 201) {
                            modal.close();
                            Toast.show(owner, response.message(), Toast.Type.SUCCESS);
                            carregarVagas();
                        } else {
                            Toast.show(modal, response.message(), Toast.Type.ERROR);
                        }
                    });
                } catch (ServerConnectionError ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        Toast.show(modal, ex.getMessage(), Toast.Type.ERROR);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        Toast.show(modal, "Erro inesperado ao criar vaga.", Toast.Type.ERROR);
                    });
                }
            }).start();
        };

        saveBtn.setOnAction(_ -> submitAction.run());
        spotNameField.setOnAction(_ -> submitAction.run());
        floorField.setOnAction(_ -> submitAction.run());

        HBox actions = new HBox(10, cancelBtn, createSpacer(), saveBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(
                new VBox(4, title, subtitle),
                labeledControl("Identificador da Vaga", spotNameField),
                labeledControl("Andar", floorField),
                labeledControl("Tipo da Vaga", typeCombo),
                labeledControl("Status Inicial", statusCombo),
                new Separator(),
                actions
        );

        showModalWithCard(modal, card);
        Platform.runLater(spotNameField::requestFocus);
    }

    public void abrirModalAjustarVaga(Vaga vaga) {
        if (vaga == null) return;
        Stage owner = getMainStage();
        String token = ConfigManager.getToken();
        String username = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(owner, token, username)) return;

        boolean isAdmin = ConfigManager.isAdmin();
        if (!isAdmin && "fora_de_servico".equals(vaga.status())) {
            if (owner != null) {
                Toast.show(owner, "Esta vaga está fora de serviço e só pode ser alterada por um administrador.", Toast.Type.ERROR);
            }
            return;
        }
        Stage modal = createStyledModal(owner, "Ajustar Vaga — " + vaga.spotName(), 450);

        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");

        Label title = new Label("Vaga " + vaga.spotName());
        title.getStyleClass().add("card-title");
        String tipoLabel = "eletrico".equalsIgnoreCase(vaga.type()) ? "Elétrico ⚡" : "Combustão ⛽";
        Label subtitle = new Label("Andar " + vaga.floor() + " • Tipo: " + tipoLabel);
        subtitle.getStyleClass().add("card-subtitle");

        TextField newSpotNameField = new TextField(vaga.spotName());
        newSpotNameField.setPromptText("Novo código da vaga (1–4 chars)");
        newSpotNameField.setDisable("ocupada".equals(vaga.status()));

        TextField floorField = new TextField(String.valueOf(vaga.floor()));
        floorField.setPromptText("Andar");
        floorField.setDisable("ocupada".equals(vaga.status()));

        ObservableList<String> statusOptions = isAdmin
                ? ("ocupada".equals(vaga.status())
                        ? FXCollections.observableArrayList("disponivel", "ocupada")
                        : FXCollections.observableArrayList("disponivel", "fora_de_servico"))
                : FXCollections.observableArrayList("disponivel", "ocupada");
        ComboBox<String> statusCombo = new ComboBox<>(statusOptions);
        statusCombo.setValue(vaga.status());
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> vehicleCombo = new ComboBox<>();
        vehicleCombo.setMaxWidth(Double.MAX_VALUE);
        vehicleCombo.setCellFactory(_ -> createVehicleComboCell());
        vehicleCombo.setButtonCell(createVehicleComboCell());
        vehicleCombo.setEditable(false);
        vehicleCombo.setPromptText("Selecione um veículo disponível");

        List<String> availablePlates = getAvailableUserPlatesForSpot(vaga);
        vehicleCombo.setItems(FXCollections.observableArrayList(availablePlates));
        if (vaga.licensePlate() != null && !vaga.licensePlate().isBlank()) {
            vehicleCombo.setValue(vaga.licensePlate());
        }

        CheckBox clearPlateCheck = new CheckBox(isAdmin
                ? "Liberar veículo e desocupar vaga"
                : "Liberar veículo desta vaga (limpar placa)");

        vehicleCombo.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null && !newVal.isBlank() && "disponivel".equals(statusCombo.getValue())) {
                statusCombo.setValue("ocupada");
            }
        });

        clearPlateCheck.selectedProperty().addListener((_, _, selected) -> {
            if (selected) {
                vehicleCombo.setValue(null);
                if (vehicleCombo.getEditor() != null) {
                    vehicleCombo.getEditor().clear();
                }
                if ("ocupada".equals(statusCombo.getValue())) {
                    statusCombo.setValue("disponivel");
                }
            }
            vehicleCombo.setDisable(selected);
        });

        Button cancelBtn = new Button("Fechar");
        cancelBtn.getStyleClass().add("secondary-button");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(_ -> modal.close());

        Button deleteBtn = new Button("Excluir Vaga");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setVisible(isAdmin);
        deleteBtn.setManaged(isAdmin);

        Button saveBtn = new Button("Salvar Alterações");
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setDefaultButton(true);

        Runnable saveAction = () -> {
            String newSpotName = null;
            Integer floor = null;

            if (isAdmin) {
                String candidateName = textOf(newSpotNameField).trim();
                if (!candidateName.isEmpty() && !Objects.equals(candidateName, vaga.spotName())) {
                    newSpotName = candidateName;
                }
                String floorText = textOf(floorField).trim();
                if (!floorText.isEmpty()) {
                    floor = FieldValidation.parseFloor(floorText);
                    if (floor == null) {
                        Toast.show(modal, "O andar deve ser um número inteiro válido.", Toast.Type.INFO);
                        return;
                    }
                    if (floor == vaga.floor()) {
                        floor = null;
                    }
                }
            }

            String status = statusCombo.getValue();
            if (status != null && !FieldValidation.validVagaStatus(status)) {
                Toast.show(modal, "Status da vaga inválido.", Toast.Type.INFO);
                return;
            }

            boolean clearPlate = clearPlateCheck.isSelected();
            String typedPlate = "";
            if (vehicleCombo.isEditable() && vehicleCombo.getEditor() != null
                    && !vehicleCombo.getEditor().getText().isBlank()) {
                typedPlate = vehicleCombo.getEditor().getText().trim();
            } else if (vehicleCombo.getValue() != null) {
                typedPlate = vehicleCombo.getValue().trim();
            }

            boolean includePlate = false;
            String licensePlateToSend = null;

            if (isAdmin) {
                if (clearPlate || "disponivel".equals(status) || "fora_de_servico".equals(status)) {
                    if (vaga.licensePlate() != null || clearPlate) {
                        includePlate = true;
                    }
                }
            } else {
                if (clearPlate || "disponivel".equals(status) || "fora_de_servico".equals(status)) {
                    if (vaga.licensePlate() != null || clearPlate) {
                        includePlate = true;
                    }
                } else if (!typedPlate.isEmpty()) {
                    if (!Objects.equals(typedPlate, vaga.licensePlate())) {
                        includePlate = true;
                        licensePlateToSend = typedPlate;
                    }
                }

                if ("ocupada".equals(status)) {
                    String effectivePlate = includePlate ? licensePlateToSend : vaga.licensePlate();
                    if (effectivePlate == null || effectivePlate.isBlank()) {
                        Toast.show(modal, "Para ocupar a vaga, selecione um veículo da sua conta.", Toast.Type.INFO);
                        return;
                    }
                    if (!FieldValidation.validLicensePlate(effectivePlate)) {
                        Toast.show(modal, "A placa do veículo deve ter exatamente 7 caracteres.", Toast.Type.INFO);
                        return;
                    }
                }
            }

            if (includePlate && licensePlateToSend != null) {
                for (Vehicle v : vehiclesList) {
                    if (v != null && v.licensePlate() != null
                            && v.licensePlate().trim().equalsIgnoreCase(licensePlateToSend)
                            && !Objects.equals(v.type(), vaga.type())) {
                        Toast.show(
                                modal,
                                "Tipo do veículo (" + v.type() + ") incompatível com o tipo da vaga (" + vaga.type() + ").",
                                Toast.Type.ERROR
                        );
                        return;
                    }
                }
            }

            if (Objects.equals(status, vaga.status()) && (newSpotName != null || floor != null || includePlate)) {
                status = null;
            }

            if (newSpotName == null && floor == null && status == null && !includePlate) {
                Toast.show(modal, "Altere pelo menos um campo para salvar.", Toast.Type.INFO);
                return;
            }

            Request request = isAdmin
                    ? Vaga.updateVagaRequest(token, vaga.spotName(), newSpotName, floor, status, includePlate, licensePlateToSend)
                    : Vaga.updateVagaUserRequest(token, vaga.spotName(), status, includePlate, licensePlateToSend);

            setButtonBusy(saveBtn, true);
            new Thread(() -> {
                try {
                    Response response = SocketManager.sendRequest(request);

                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        if (!modal.isShowing() || SessionUtils.handleExpiredSession(owner, response)) {
                            return;
                        }
                        if (response.statusCode() == 200) {
                            modal.close();
                            Toast.show(owner, response.message(), Toast.Type.SUCCESS);
                            carregarVagas();
                        } else {
                            Toast.show(modal, response.message(), Toast.Type.ERROR);
                        }
                    });
                } catch (ServerConnectionError ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        Toast.show(modal, ex.getMessage(), Toast.Type.ERROR);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        Toast.show(modal, "Erro inesperado ao atualizar vaga.", Toast.Type.ERROR);
                    });
                }
            }).start();
        };

        deleteBtn.setOnAction(_ -> {
            setButtonBusy(deleteBtn, true);
            new Thread(() -> {
                try {
                    Response response = SocketManager.sendRequest(Vaga.deleteVagaRequest(token, vaga.spotName()));

                    Platform.runLater(() -> {
                        setButtonBusy(deleteBtn, false);
                        if (!modal.isShowing() || SessionUtils.handleExpiredSession(owner, response)) {
                            return;
                        }
                        if (response.statusCode() == 200) {
                            modal.close();
                            Toast.show(owner, response.message(), Toast.Type.SUCCESS);
                            carregarVagas();
                        } else {
                            Toast.show(modal, response.message(), Toast.Type.ERROR);
                        }
                    });
                } catch (ServerConnectionError ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(deleteBtn, false);
                        Toast.show(modal, ex.getMessage(), Toast.Type.ERROR);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(deleteBtn, false);
                        Toast.show(modal, "Erro inesperado ao remover vaga.", Toast.Type.ERROR);
                    });
                }
            }).start();
        });

        saveBtn.setOnAction(_ -> saveAction.run());
        newSpotNameField.setOnAction(_ -> saveAction.run());
        floorField.setOnAction(_ -> saveAction.run());

        card.getChildren().add(new VBox(4, title, subtitle));
        if (isAdmin) {
            HBox adminRow = new HBox(10);
            VBox nameBox = labeledControl("Novo Identificador", newSpotNameField);
            HBox.setHgrow(nameBox, Priority.ALWAYS);
            VBox floorBox = labeledControl("Andar", floorField);
            floorBox.setPrefWidth(110);
            adminRow.getChildren().addAll(nameBox, floorBox);
            card.getChildren().add(adminRow);
        }

        VBox plateBox;
        if (isAdmin) {
            plateBox = new VBox(6);
            if (vaga.licensePlate() != null && !vaga.licensePlate().isBlank()) {
                Label currentVehicleLabel = new Label("Veículo estacionado: " + vaga.licensePlate());
                currentVehicleLabel.getStyleClass().add("field-label");
                plateBox.getChildren().addAll(currentVehicleLabel, clearPlateCheck);
            } else {
                plateBox.setVisible(false);
                plateBox.setManaged(false);
            }
        } else {
            plateBox = labeledControl("Veículo na Vaga (disponíveis da sua conta)", vehicleCombo);
            plateBox.getChildren().add(clearPlateCheck);
        }

        HBox actions = new HBox(10, cancelBtn, deleteBtn, createSpacer(), saveBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(
                labeledControl("Status de Ocupação", statusCombo),
                plateBox,
                new Separator(),
                actions
        );

        showModalWithCard(modal, card);
    }

    private ListCell<String> createVehicleComboCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String plate, boolean empty) {
                super.updateItem(plate, empty);
                if (empty || plate == null || plate.isBlank()) {
                    setText(null);
                    return;
                }
                for (Vehicle v : vehiclesList) {
                    if (v != null && v.licensePlate() != null && v.licensePlate().equalsIgnoreCase(plate.trim())) {
                        String modelPart = (v.model() != null && !v.model().isBlank()) ? " " + v.model() : "";
                        setText(v.licensePlate() + " — " + v.brand() + modelPart + " (" + v.type() + ")");
                        return;
                    }
                }
                setText(plate);
            }
        };
    }

    private List<String> getAvailableUserPlatesForSpot(Vaga currentSpot) {
        String currentSpotName = currentSpot != null ? currentSpot.spotName() : null;
        String currentSpotPlate = currentSpot != null ? currentSpot.licensePlate() : null;

        List<String> availablePlates = new ArrayList<>();
        for (Vehicle v : vehiclesList) {
            if (v == null || v.licensePlate() == null) continue;
            String plate = v.licensePlate().trim();
            boolean parkedInAnotherSpot = false;
            for (Vaga spot : allVagasList) {
                if (spot != null && "ocupada".equals(spot.status()) && spot.licensePlate() != null
                        && spot.licensePlate().trim().equalsIgnoreCase(plate)
                        && (currentSpotName == null || !Objects.equals(spot.spotName(), currentSpotName))) {
                    parkedInAnotherSpot = true;
                    break;
                }
            }
            if (!parkedInAnotherSpot) {
                availablePlates.add(plate);
            }
        }

        if (currentSpotPlate != null && !currentSpotPlate.isBlank()
                && availablePlates.stream().noneMatch(p -> p.equalsIgnoreCase(currentSpotPlate.trim()))) {
            if (ConfigManager.isAdmin() || isUserOwnPlate(currentSpotPlate)) {
                availablePlates.addFirst(currentSpotPlate.trim());
            }
        }
        return availablePlates;
    }

    // ==================== LOCALIZAR VEÍCULO POR PLACA ====================
    @FXML
    public void localizarVeiculo() {
        Stage stage = getMainStage();
        String token = ConfigManager.getToken();
        String username = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(stage, token, username)) return;

        String plate = textOf(findVehiclePlateField).trim();
        if (!FieldValidation.validLicensePlate(plate)) {
            highlightedSpotName = null;
            renderVagasVisualGrid();
            if (findVehicleResultLabel != null) {
                findVehicleResultLabel.setText("Informe uma placa válida para localizar.");
            }
            Toast.show(stage, "Informe a placa do veículo para busca.", Toast.Type.INFO);
            return;
        }

        if (findVehicleResultLabel != null) {
            findVehicleResultLabel.setText("Buscando localização para a placa " + plate + "...");
        }
        setButtonBusy(findVehicleBtn, true);

        new Thread(() -> {
            try {
                Response response = SocketManager.sendRequest(Vaga.findVehicleRequest(token, plate));

                Platform.runLater(() -> {
                    setButtonBusy(findVehicleBtn, false);
                    if (stage == null || !stage.isShowing() || SessionUtils.handleExpiredSession(stage, response)) {
                        return;
                    }
                    if (response.statusCode() == 200) {
                        try {
                            Vaga.Location location = VagaResponseValidation.readLocation(response.data());
                            highlightedSpotName = location.spotName();
                            if (findVehicleResultLabel != null) {
                                findVehicleResultLabel.setText(String.format(
                                        "Placa %s localizada → Vaga %s (Andar %d • %s)",
                                        plate.toUpperCase(), location.spotName(), location.floor(), location.status()));
                            }
                            renderVagasVisualGrid();
                            Toast.show(stage, response.message(), Toast.Type.SUCCESS);
                        } catch (IllegalArgumentException e) {
                            highlightedSpotName = null;
                            renderVagasVisualGrid();
                            if (findVehicleResultLabel != null) {
                                findVehicleResultLabel.setText("Resposta de localização inválida.");
                            }
                            Toast.show(stage, e.getMessage(), Toast.Type.ERROR);
                        }
                    } else {
                        highlightedSpotName = null;
                        renderVagasVisualGrid();
                        if (findVehicleResultLabel != null) {
                            findVehicleResultLabel.setText("Nenhuma vaga localizada para " + plate.toUpperCase() + " (" + response.message() + ")");
                        }
                        Toast.show(stage, response.message(), Toast.Type.ERROR);
                    }
                });
            } catch (ServerConnectionError e) {
                Platform.runLater(() -> {
                    setButtonBusy(findVehicleBtn, false);
                    highlightedSpotName = null;
                    renderVagasVisualGrid();
                    if (findVehicleResultLabel != null) {
                        findVehicleResultLabel.setText("Falha de comunicação ao consultar placa " + plate + ".");
                    }
                    Toast.show(stage, e.getMessage(), Toast.Type.ERROR);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setButtonBusy(findVehicleBtn, false);
                    highlightedSpotName = null;
                    renderVagasVisualGrid();
                    if (findVehicleResultLabel != null) {
                        findVehicleResultLabel.setText("Erro inesperado na busca.");
                    }
                    Toast.show(stage, "Erro inesperado ao localizar veículo.", Toast.Type.ERROR);
                });
            }
        }).start();
    }

    // ==================== GALERIA VISUAL DE VEÍCULOS & MODAIS MENORES ====================
    @FXML
    public void carregarMeusVeiculos() {
        Stage stage = getMainStage();
        String token = ConfigManager.getToken();
        String username = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(stage, token, username)) return;

        setButtonBusy(refreshVehiclesBtn, true);
        new Thread(() -> {
            try {
                Response response = SocketManager.sendRequest(Vehicle.getVehiclesRequest(token));

                Platform.runLater(() -> {
                    setButtonBusy(refreshVehiclesBtn, false);
                    if (stage == null || !stage.isShowing() || SessionUtils.handleExpiredSession(stage, response)) {
                        return;
                    }
                    if (response.statusCode() != 200) {
                        Toast.show(stage, response.message(), Toast.Type.ERROR);
                        return;
                    }
                    try {
                        List<Vehicle> vehicles = VehicleResponseValidation.readVehicles(response.data());
                        vehiclesList.setAll(vehicles);
                        renderVehiclesVisualGrid();
                        aplicarFiltroVagas();
                    } catch (IllegalArgumentException e) {
                        Toast.show(stage, e.getMessage(), Toast.Type.ERROR);
                    }
                });
            } catch (ServerConnectionError e) {
                Platform.runLater(() -> {
                    setButtonBusy(refreshVehiclesBtn, false);
                    Toast.show(stage, e.getMessage(), Toast.Type.ERROR);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setButtonBusy(refreshVehiclesBtn, false);
                    Toast.show(stage, "Erro inesperado ao listar veículos.", Toast.Type.ERROR);
                });
            }
        }).start();
    }

    private void renderVehiclesVisualGrid() {
        if (vehiclesVisualPane == null) return;
        vehiclesVisualPane.getChildren().clear();

        if (ConfigManager.isAdmin()) {
            VBox adminHint = new VBox(8);
            adminHint.setPadding(new Insets(24));
            Label title = new Label("Modo Administrador: pesquise qualquer veículo pela placa acima");
            title.getStyleClass().add("section-header-label");
            Label sub = new Label("Ao pesquisar uma placa no campo superior, uma janela menor será aberta com os dados do veículo para consulta ou exclusão.");
            sub.getStyleClass().add("card-subtitle");
            adminHint.getChildren().addAll(title, sub);
            vehiclesVisualPane.getChildren().add(adminHint);
            return;
        }

        if (vehiclesList.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setPadding(new Insets(28));
            Label emptyTitle = new Label("Sua garagem ainda está vazia");
            emptyTitle.getStyleClass().add("section-header-label");
            Label emptySub = new Label("Clique em '+ Cadastrar Veículo' no canto superior direito para adicionar seu primeiro veículo.");
            emptySub.getStyleClass().add("card-subtitle");
            emptyBox.getChildren().addAll(emptyTitle, emptySub);
            vehiclesVisualPane.getChildren().add(emptyBox);
            return;
        }

        for (Vehicle vehicle : vehiclesList) {
            vehiclesVisualPane.getChildren().add(createVehicleCard(vehicle));
        }
    }

    private VBox createVehicleCard(Vehicle vehicle) {
        VBox card = new VBox(10);
        card.setPrefWidth(250);
        card.setMinWidth(230);
        card.setMaxWidth(270);
        card.getStyleClass().add("vehicle-tile");
        card.setFocusTraversable(true);

        // Mercosul-style plate header
        HBox plateBox = new HBox();
        plateBox.setAlignment(Pos.CENTER);
        plateBox.getStyleClass().add("mercosul-plate-box");
        Label plateLabel = new Label(vehicle.licensePlate());
        plateLabel.getStyleClass().add("mercosul-plate-text");
        plateBox.getChildren().add(plateLabel);

        // Brand & Model
        String modelText = (vehicle.model() != null && !vehicle.model().isBlank()) ? " " + vehicle.model() : "";
        Label brandModelLabel = new Label(vehicle.brand() + modelText);
        brandModelLabel.getStyleClass().add("vehicle-brand-title");
        brandModelLabel.setWrapText(true);

        // Type & Parking Status Row
        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        boolean isElectric = "eletrico".equalsIgnoreCase(vehicle.type());
        Label typePill = new Label(isElectric ? "⚡ Elétrico" : "⛽ Combustão");
        typePill.getStyleClass().add(isElectric ? "type-pill-eletrico" : "type-pill-combustao");

        String parkedSpot = findParkedSpotForPlate(vehicle.licensePlate());
        Label parkBadge = new Label(parkedSpot != null ? "Na vaga " + parkedSpot : "Fora do pátio");
        parkBadge.getStyleClass().add(parkedSpot != null ? "spot-status-pill-own" : "card-subtitle");
        metaRow.getChildren().addAll(typePill, createSpacer(), parkBadge);

        card.getChildren().addAll(plateBox, brandModelLabel, metaRow);

        card.setOnMouseClicked(_ -> abrirModalAjustarVeiculo(vehicle));
        card.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                abrirModalAjustarVeiculo(vehicle);
                e.consume();
            }
        });

        return card;
    }

    private String findParkedSpotForPlate(String plate) {
        if (plate == null || plate.isBlank()) return null;
        for (Vaga v : allVagasList) {
            if (v != null && "ocupada".equals(v.status()) && v.licensePlate() != null
                    && v.licensePlate().trim().equalsIgnoreCase(plate.trim())) {
                return v.spotName() + " (Andar " + v.floor() + ")";
            }
        }
        return null;
    }

    @FXML
    public void abrirModalCriarVeiculo() {
        Stage owner = getMainStage();
        String token = ConfigManager.getToken();
        String username = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(owner, token, username)) return;

        Stage modal = createStyledModal(owner, "Cadastrar Veículo", 430);
        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");

        Label title = new Label("Novo Veículo");
        title.getStyleClass().add("card-title");
        Label subtitle = new Label("Informe a placa, marca, modelo (opcional) e motorização.");
        subtitle.getStyleClass().add("card-subtitle");

        TextField plateField = new TextField();
        plateField.setPromptText("Ex.: ABC1234");

        TextField brandField = new TextField();
        brandField.setPromptText("Ex.: Chevrolet");

        TextField modelField = new TextField();
        modelField.setPromptText("Ex.: Onix (opcional)");

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("combustao", "eletrico"));
        typeCombo.setValue("combustao");
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        Button cancelBtn = new Button("Cancelar");
        cancelBtn.getStyleClass().add("secondary-button");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(_ -> modal.close());

        Button saveBtn = new Button("Cadastrar Veículo");
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setDefaultButton(true);

        Runnable submitAction = () -> {
            String plate = textOf(plateField).trim();
            String brand = textOf(brandField).trim();
            String rawModel = textOf(modelField).trim();
            String model = rawModel.isEmpty() ? null : rawModel;
            String type = typeCombo.getValue();

            if (!FieldValidation.validLicensePlate(plate)
                    || !FieldValidation.validBrand(brand)
                    || !FieldValidation.validModel(model)
                    || !FieldValidation.validType(type)) {
                Toast.show(modal, "Preencha placa (7 caracteres), marca (1–20), modelo (máx. 20) e motorização válidos.", Toast.Type.INFO);
                return;
            }

            setButtonBusy(saveBtn, true);
            new Thread(() -> {
                try {
                    Response response = SocketManager.sendRequest(
                            Vehicle.createVehicleRequest(token, brand, model, type, plate));

                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        if (!modal.isShowing() || SessionUtils.handleExpiredSession(owner, response)) {
                            return;
                        }
                        if (response.statusCode() == 201) {
                            modal.close();
                            Toast.show(owner, response.message(), Toast.Type.SUCCESS);
                            if (ConfigManager.isUser()) {
                                carregarMeusVeiculos();
                            }
                        } else {
                            Toast.show(modal, response.message(), Toast.Type.ERROR);
                        }
                    });
                } catch (ServerConnectionError ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        Toast.show(modal, ex.getMessage(), Toast.Type.ERROR);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        Toast.show(modal, "Erro inesperado ao cadastrar veículo.", Toast.Type.ERROR);
                    });
                }
            }).start();
        };

        saveBtn.setOnAction(_ -> submitAction.run());
        plateField.setOnAction(_ -> submitAction.run());
        brandField.setOnAction(_ -> submitAction.run());
        modelField.setOnAction(_ -> submitAction.run());

        HBox actions = new HBox(10, cancelBtn, createSpacer(), saveBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(
                new VBox(4, title, subtitle),
                labeledControl("Placa", plateField),
                labeledControl("Marca", brandField),
                labeledControl("Modelo (opcional)", modelField),
                labeledControl("Motorização", typeCombo),
                new Separator(),
                actions
        );

        showModalWithCard(modal, card);
        Platform.runLater(plateField::requestFocus);
    }

    @FXML
    public void consultarVeiculoPorPlaca() {
        Stage stage = getMainStage();
        String token = ConfigManager.getToken();
        String username = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(stage, token, username)) return;

        String plate = textOf(searchVehiclePlateField).trim();
        if (!FieldValidation.validLicensePlate(plate)) {
            Toast.show(stage, "Informe a placa do veículo para consulta.", Toast.Type.INFO);
            return;
        }

        setButtonBusy(searchVehicleBtn, true);
        new Thread(() -> {
            try {
                Response response = SocketManager.sendRequest(Vehicle.getVehicleRequest(token, plate));

                Platform.runLater(() -> {
                    setButtonBusy(searchVehicleBtn, false);
                    if (stage == null || !stage.isShowing() || SessionUtils.handleExpiredSession(stage, response)) {
                        return;
                    }
                    if (response.statusCode() == 200) {
                        try {
                            Vehicle vehicle = VehicleResponseValidation.readVehicle(response.data());
                            Toast.show(stage, response.message(), Toast.Type.SUCCESS);
                            abrirModalAjustarVeiculo(vehicle);
                        } catch (IllegalArgumentException e) {
                            Toast.show(stage, e.getMessage(), Toast.Type.ERROR);
                        }
                    } else {
                        Toast.show(stage, response.message(), Toast.Type.ERROR);
                    }
                });
            } catch (ServerConnectionError e) {
                Platform.runLater(() -> {
                    setButtonBusy(searchVehicleBtn, false);
                    Toast.show(stage, e.getMessage(), Toast.Type.ERROR);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setButtonBusy(searchVehicleBtn, false);
                    Toast.show(stage, "Erro inesperado ao consultar veículo.", Toast.Type.ERROR);
                });
            }
        }).start();
    }

    public void abrirModalAjustarVeiculo(Vehicle vehicle) {
        if (vehicle == null) return;
        Stage owner = getMainStage();
        String token = ConfigManager.getToken();
        String username = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(owner, token, username)) return;

        boolean isAdmin = ConfigManager.isAdmin();
        Stage modal = createStyledModal(owner, "Ficha do Veículo — " + vehicle.licensePlate(), 440);
        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");

        Label title = new Label("Veículo " + vehicle.licensePlate());
        title.getStyleClass().add("card-title");
        String parkedInfo = findParkedSpotForPlate(vehicle.licensePlate());
        Label subtitle = new Label(parkedInfo != null
                ? "Atualmente estacionado na vaga " + parkedInfo
                : (isAdmin ? "Visualize os dados cadastrais ou remova o veículo." : "Edite os dados cadastrais ou remova o veículo."));
        subtitle.getStyleClass().add("card-subtitle");

        TextField brandField = new TextField(vehicle.brand() != null ? vehicle.brand() : "");
        brandField.setPromptText("Marca do veículo");
        brandField.setDisable(isAdmin);

        TextField modelField = new TextField(vehicle.model() != null ? vehicle.model() : "");
        modelField.setPromptText("Modelo do veículo");
        modelField.setDisable(isAdmin);

        CheckBox clearModelCheck = new CheckBox("Remover modelo (deixar sem modelo)");
        clearModelCheck.setVisible(!isAdmin);
        clearModelCheck.setManaged(!isAdmin);
        clearModelCheck.selectedProperty().addListener((_, _, selected) -> {
            if (selected) modelField.clear();
            modelField.setDisable(selected);
        });

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("combustao", "eletrico"));
        typeCombo.setValue(vehicle.type());
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo.setDisable(isAdmin);

        Button cancelBtn = new Button("Fechar");
        cancelBtn.getStyleClass().add("secondary-button");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(_ -> modal.close());

        Button deleteBtn = new Button("Excluir Veículo");
        deleteBtn.getStyleClass().add("danger-button");

        Button saveBtn = new Button("Salvar Alterações");
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setDefaultButton(!isAdmin);
        saveBtn.setVisible(!isAdmin);
        saveBtn.setManaged(!isAdmin);

        Runnable saveAction = () -> {
            String brandText = textOf(brandField).trim();
            String brandToSend = null;
            if (!brandText.isEmpty() && !Objects.equals(brandText, vehicle.brand())) {
                if (!FieldValidation.validBrand(brandText)) {
                    Toast.show(modal, "Marca deve ter entre 1 e 20 caracteres.", Toast.Type.INFO);
                    return;
                }
                brandToSend = brandText;
            }

            boolean clearModel = clearModelCheck.isSelected();
            String modelText = textOf(modelField).trim();
            boolean includeModel = false;
            String modelToSend = null;
            if (clearModel) {
                includeModel = true;
            } else if (!modelText.isEmpty() && !Objects.equals(modelText, vehicle.model())) {
                if (!FieldValidation.validModel(modelText)) {
                    Toast.show(modal, "Modelo deve ter no máximo 20 caracteres.", Toast.Type.INFO);
                    return;
                }
                includeModel = true;
                modelToSend = modelText;
            }

            String selectedType = typeCombo.getValue();
            String typeToSend = null;
            if (selectedType != null && !Objects.equals(selectedType, vehicle.type())) {
                if (!FieldValidation.validType(selectedType)) {
                    Toast.show(modal, "Motorização inválida.", Toast.Type.INFO);
                    return;
                }
                typeToSend = selectedType;
            }

            if (brandToSend == null && !includeModel && typeToSend == null) {
                Toast.show(modal, "Altere marca, modelo ou motorização antes de salvar.", Toast.Type.INFO);
                return;
            }

            Request request = Vehicle.updateVehicleRequest(
                    token, vehicle.licensePlate(), brandToSend, includeModel, modelToSend, typeToSend);
            setButtonBusy(saveBtn, true);
            new Thread(() -> {
                try {
                    Response response = SocketManager.sendRequest(request);

                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        if (!modal.isShowing() || SessionUtils.handleExpiredSession(owner, response)) {
                            return;
                        }
                        if (response.statusCode() == 200) {
                            modal.close();
                            Toast.show(owner, response.message(), Toast.Type.SUCCESS);
                            if (ConfigManager.isUser()) {
                                carregarMeusVeiculos();
                            }
                        } else {
                            Toast.show(modal, response.message(), Toast.Type.ERROR);
                        }
                    });
                } catch (ServerConnectionError ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        Toast.show(modal, ex.getMessage(), Toast.Type.ERROR);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        Toast.show(modal, "Erro inesperado ao atualizar veículo.", Toast.Type.ERROR);
                    });
                }
            }).start();
        };

        deleteBtn.setOnAction(_ -> {
            setButtonBusy(deleteBtn, true);
            new Thread(() -> {
                try {
                    Response response = SocketManager.sendRequest(Vehicle.deleteVehicleRequest(token, vehicle.licensePlate()));

                    Platform.runLater(() -> {
                        setButtonBusy(deleteBtn, false);
                        if (!modal.isShowing() || SessionUtils.handleExpiredSession(owner, response)) {
                            return;
                        }
                        if (response.statusCode() == 200) {
                            modal.close();
                            Toast.show(owner, response.message(), Toast.Type.SUCCESS);
                            if (ConfigManager.isUser()) {
                                carregarMeusVeiculos();
                            }
                        } else {
                            Toast.show(modal, response.message(), Toast.Type.ERROR);
                        }
                    });
                } catch (ServerConnectionError ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(deleteBtn, false);
                        Toast.show(modal, ex.getMessage(), Toast.Type.ERROR);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(deleteBtn, false);
                        Toast.show(modal, "Erro inesperado ao excluir veículo.", Toast.Type.ERROR);
                    });
                }
            }).start();
        });

        saveBtn.setOnAction(_ -> saveAction.run());
        brandField.setOnAction(_ -> saveAction.run());
        modelField.setOnAction(_ -> saveAction.run());

        VBox modelBox = labeledControl("Modelo", modelField);
        modelBox.getChildren().add(clearModelCheck);

        HBox actions = new HBox(10, cancelBtn, deleteBtn, createSpacer(), saveBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(
                new VBox(4, title, subtitle),
                labeledControl("Marca", brandField),
                modelBox,
                labeledControl("Motorização", typeCombo),
                new Separator(),
                actions
        );

        showModalWithCard(modal, card);
    }

    // ==================== GESTÃO VISUAL DE USUÁRIOS (ADMIN) & MODAIS MENORES ====================
    private void renderUserAdminResultCard() {
        if (userAdminResultContainer == null) return;
        userAdminResultContainer.getChildren().clear();

        if (loadedTargetUser == null || loadedTargetUser.username() == null) {
            VBox emptyState = new VBox(8);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(40));
            Label title = new Label("Nenhum usuário selecionado");
            title.getStyleClass().add("section-header-label");
            Label sub = new Label("Pesquise um username na barra superior ou clique em '+ Novo Usuário / Admin' para cadastrar uma conta.");
            sub.getStyleClass().add("card-subtitle");
            emptyState.getChildren().addAll(title, sub);
            userAdminResultContainer.getChildren().add(emptyState);
            return;
        }

        VBox userCard = new VBox(16);
        userCard.setMaxWidth(480);
        userCard.getStyleClass().add("auth-card");

        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(loadedTargetUser.name() != null ? loadedTargetUser.name() : "—");
        nameLabel.getStyleClass().add("card-title");
        Label rolePill = new Label(loadedTargetUser.role() != null ? loadedTargetUser.role().toUpperCase() : "USER");
        rolePill.getStyleClass().add("role-badge");
        headerRow.getChildren().addAll(nameLabel, createSpacer(), rolePill);

        Label usernameTag = new Label("@" + loadedTargetUser.username());
        usernameTag.getStyleClass().add("session-user-pill");

        Button editNameBtn = new Button("Editar Nome");
        editNameBtn.getStyleClass().add("primary-button");
        editNameBtn.setOnAction(_ -> abrirModalEditarUsuarioAlvo(loadedTargetUser));

        Button deleteUserBtn = new Button("Excluir Conta");
        deleteUserBtn.getStyleClass().add("danger-button");
        deleteUserBtn.setOnAction(_ -> {
            Stage stage = getMainStage();
            String target = loadedTargetUser.username();
            NavigationUtils.openDeleteConfirmationModal(stage, target, deleteResponse -> {
                loadedTargetUser = null;
                renderUserAdminResultCard();
                Toast.show(stage, deleteResponse.message(), Toast.Type.SUCCESS);
            });
        });

        HBox actions = new HBox(10, editNameBtn, deleteUserBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        userCard.getChildren().addAll(headerRow, usernameTag, new Separator(), actions);
        userAdminResultContainer.getChildren().add(userCard);
    }

    @FXML
    public void buscarUsuarioAlvo() {
        Stage stage = getMainStage();
        String token = ConfigManager.getToken();
        String sessionUsername = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(stage, token, sessionUsername)) return;

        String targetUsername = textOf(targetSearchUsernameField).trim();
        if (!FieldValidation.validUsername(targetUsername)) {
            Toast.show(stage, "Informe um username válido (3–20 caracteres) para pesquisa.", Toast.Type.INFO);
            return;
        }

        setButtonBusy(searchTargetUserBtn, true);
        new Thread(() -> {
            try {
                Response response = SocketManager.sendRequest(User.getUserRequest(token, targetUsername));

                var profileData = response.statusCode() == 200
                        ? ProfileResponseValidation.readData(response.data()) : null;

                Platform.runLater(() -> {
                    setButtonBusy(searchTargetUserBtn, false);
                    if (stage == null || !stage.isShowing() || SessionUtils.handleExpiredSession(stage, response)) {
                        return;
                    }
                    if (response.statusCode() == 200 && profileData != null && profileData.username() != null) {
                        loadedTargetUser = profileData;
                        renderUserAdminResultCard();
                        Toast.show(stage, response.message(), Toast.Type.SUCCESS);
                    } else {
                        loadedTargetUser = null;
                        renderUserAdminResultCard();
                        Toast.show(stage, response.message(), Toast.Type.ERROR);
                    }
                });
            } catch (ServerConnectionError e) {
                Platform.runLater(() -> {
                    setButtonBusy(searchTargetUserBtn, false);
                    Toast.show(stage, e.getMessage(), Toast.Type.ERROR);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setButtonBusy(searchTargetUserBtn, false);
                    Toast.show(stage, "Erro inesperado ao buscar usuário.", Toast.Type.ERROR);
                });
            }
        }).start();
    }

    @FXML
    public void abrirModalCriarUsuarioAdmin() {
        Stage owner = getMainStage();
        String token = ConfigManager.getToken();
        String sessionUsername = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(owner, token, sessionUsername)) return;

        Stage modal = createStyledModal(owner, "Novo Usuário / Admin", 440);
        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");

        Label title = new Label("Cadastrar Conta");
        title.getStyleClass().add("card-title");
        Label subtitle = new Label("Crie um novo usuário definindo seu perfil de acesso.");
        subtitle.getStyleClass().add("card-subtitle");

        TextField nameField = new TextField();
        nameField.setPromptText("Apenas letras e espaços (1–60)");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Minúsculas, números, ponto ou sublinhado (3–20)");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("8–20 caracteres com maiúscula, minúscula, número e símbolo");

        ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList("user", "admin"));
        roleCombo.setValue("user");
        roleCombo.setMaxWidth(Double.MAX_VALUE);

        Button cancelBtn = new Button("Cancelar");
        cancelBtn.getStyleClass().add("secondary-button");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(_ -> modal.close());

        Button saveBtn = new Button("Criar Conta");
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setDefaultButton(true);

        Runnable submitAction = () -> {
            String name = textOf(nameField);
            String username = textOf(usernameField);
            String password = textOf(passwordField);
            String role = roleCombo.getValue();

            if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.show(modal, "Preencha nome, username e senha.", Toast.Type.INFO);
                return;
            }
            if (!FieldValidation.validName(name)) {
                Toast.show(modal, "Nome: use apenas letras e espaços, entre 1 e 60 caracteres.", Toast.Type.INFO);
                return;
            }
            if (!FieldValidation.validUsername(username)) {
                Toast.show(modal, "Username: use 3–20 caracteres (minúsculas, números, ponto ou sublinhado).", Toast.Type.INFO);
                return;
            }
            if (!FieldValidation.validPassword(password)) {
                Toast.show(modal, "Senha: 8–20 caracteres com maiúscula, minúscula, número e símbolo permitido.", Toast.Type.INFO);
                return;
            }

            setButtonBusy(saveBtn, true);
            new Thread(() -> {
                try {
                    Response response = SocketManager.sendRequest(
                            User.registerWithRoleRequest(name, username, password, role, token));

                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        if (!modal.isShowing() || SessionUtils.handleExpiredSession(owner, response)) {
                            return;
                        }
                        if (response.statusCode() == 201) {
                            modal.close();
                            Toast.show(owner, response.message(), Toast.Type.SUCCESS);
                        } else {
                            Toast.show(modal, response.message(), Toast.Type.ERROR);
                        }
                    });
                } catch (ServerConnectionError ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        Toast.show(modal, ex.getMessage(), Toast.Type.ERROR);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        Toast.show(modal, "Erro inesperado ao cadastrar usuário.", Toast.Type.ERROR);
                    });
                }
            }).start();
        };

        saveBtn.setOnAction(_ -> submitAction.run());
        nameField.setOnAction(_ -> submitAction.run());
        usernameField.setOnAction(_ -> submitAction.run());
        passwordField.setOnAction(_ -> submitAction.run());

        HBox actions = new HBox(10, cancelBtn, createSpacer(), saveBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(
                new VBox(4, title, subtitle),
                labeledControl("Nome Completo", nameField),
                labeledControl("Nome de Usuário (username)", usernameField),
                labeledControl("Senha", passwordField),
                labeledControl("Perfil de Acesso", roleCombo),
                new Separator(),
                actions
        );

        showModalWithCard(modal, card);
        Platform.runLater(nameField::requestFocus);
    }

    private void abrirModalEditarUsuarioAlvo(ProfileResponseValidation.ProfileData targetUser) {
        if (targetUser == null || targetUser.username() == null) return;
        Stage owner = getMainStage();
        String token = ConfigManager.getToken();
        String sessionUsername = ConfigManager.getUsername();
        if (!SessionUtils.ensureSession(owner, token, sessionUsername)) return;

        Stage modal = createStyledModal(owner, "Editar Nome — @" + targetUser.username(), 420);
        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");

        Label title = new Label("Editar Usuário");
        title.getStyleClass().add("card-title");
        Label subtitle = new Label("Conta: @" + targetUser.username() + " (" + (targetUser.role() != null ? targetUser.role() : "user") + ")");
        subtitle.getStyleClass().add("card-subtitle");

        TextField nameField = new TextField(targetUser.name() != null ? targetUser.name() : "");
        nameField.setPromptText("Novo nome completo");

        Button cancelBtn = new Button("Cancelar");
        cancelBtn.getStyleClass().add("secondary-button");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(_ -> modal.close());

        Button saveBtn = new Button("Salvar Nome");
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setDefaultButton(true);

        Runnable saveAction = () -> {
            String newName = textOf(nameField);
            if (!FieldValidation.validName(newName)) {
                Toast.show(modal, "Nome: use apenas letras e espaços, entre 1 e 60 caracteres.", Toast.Type.INFO);
                return;
            }

            setButtonBusy(saveBtn, true);
            new Thread(() -> {
                try {
                    Response response = SocketManager.sendRequest(
                            User.updateUserNameRequest(token, targetUser.username(), newName));

                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        if (!modal.isShowing() || SessionUtils.handleExpiredSession(owner, response)) {
                            return;
                        }
                        if (response.statusCode() == 200) {
                            modal.close();
                            Toast.show(owner, response.message(), Toast.Type.SUCCESS);
                            targetSearchUsernameField.setText(targetUser.username());
                            buscarUsuarioAlvo();
                        } else {
                            Toast.show(modal, response.message(), Toast.Type.ERROR);
                        }
                    });
                } catch (ServerConnectionError ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        Toast.show(modal, ex.getMessage(), Toast.Type.ERROR);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        setButtonBusy(saveBtn, false);
                        Toast.show(modal, "Erro inesperado ao atualizar nome.", Toast.Type.ERROR);
                    });
                }
            }).start();
        };

        saveBtn.setOnAction(_ -> saveAction.run());
        nameField.setOnAction(_ -> saveAction.run());

        HBox actions = new HBox(10, cancelBtn, createSpacer(), saveBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(
                new VBox(4, title, subtitle),
                labeledControl("Nome Completo", nameField),
                new Separator(),
                actions
        );

        showModalWithCard(modal, card);
        Platform.runLater(nameField::requestFocus);
    }

    // ==================== UTILITÁRIOS DE CONSTRUÇÃO DE MODAIS ====================
    private Stage createStyledModal(Stage owner, String title, double width) {
        Stage modal = new Stage();
        NavigationUtils.applyClientIcon(modal);
        modal.setTitle(title);
        if (owner != null) {
            modal.initOwner(owner);
            modal.initModality(Modality.WINDOW_MODAL);
        }
        modal.setWidth(width);
        modal.setResizable(false);
        return modal;
    }

    private void showModalWithCard(Stage modal, VBox card) {
        AnchorPane root = new AnchorPane();
        root.getStyleClass().add("main-background");
        root.setPadding(new Insets(18));

        AnchorPane.setTopAnchor(card, 0.0);
        AnchorPane.setBottomAnchor(card, 0.0);
        AnchorPane.setLeftAnchor(card, 0.0);
        AnchorPane.setRightAnchor(card, 0.0);
        root.getChildren().add(card);

        Scene scene = new Scene(root);
        URL cssUrl = ClientApplication.class.getResource("styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                modal.close();
                event.consume();
            }
        });

        modal.setScene(scene);
        modal.sizeToScene();
        modal.show();
    }

    private VBox labeledControl(String labelText, Node control) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("field-label");
        return new VBox(5, lbl, control);
    }

    private Region createSpacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private Stage getMainStage() {
        if (sessionUserLabel != null && sessionUserLabel.getScene() != null
                && sessionUserLabel.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        if (navbar != null && navbar.getScene() != null
                && navbar.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    private static String textOf(TextField field) {
        return field != null && field.getText() != null ? field.getText() : "";
    }

    private static void setButtonBusy(Button button, boolean busy) {
        if (button != null) {
            button.setDisable(busy);
        }
    }
}
