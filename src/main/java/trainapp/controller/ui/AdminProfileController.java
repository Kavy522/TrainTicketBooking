package trainapp.controller.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import trainapp.model.Admin;
import trainapp.model.TravelStatistics;
import trainapp.model.TrainClass;
import trainapp.service.AdminDataStructureService;
import trainapp.service.AuthService;
import trainapp.service.SessionManager;
import trainapp.dao.TrainDAO;
import trainapp.dao.StationDAO;
import trainapp.util.SceneManager;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

public class AdminProfileController {

    // Header Controls
    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private Label adminWelcomeLabel;

    // Statistics Labels
    @FXML private Label totalUsersLabel;
    @FXML private Label activeTrainsLabel;
    @FXML private Label totalBookingsLabel;
    @FXML private Label revenueLabel;

    // Pricing Engine Controls (Kept)
    @FXML private ListView fareStructureListView;
    @FXML private TextField distanceField;
    @FXML private TextField fareField;
    @FXML private ComboBox<String> classComboBox;

    // Status and Messages
    @FXML private Label lastUpdateLabel;
    @FXML private Label messageLabel;

    // Services
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final AuthService authService = new AuthService();
    private final AdminDataStructureService dataStructureService = new AdminDataStructureService();

    // Fleet Management DAOs
    private TrainDAO trainDAO;
    private StationDAO stationDAO;

    @FXML
    public void initialize() {
        loadAdminProfile();

        // Initialize Fleet Management DAOs
        initializeFleetServices();
        loadStatistics();
        initializePricingEngine();
        updateSystemStatus();
    }

    /**
     * Initialize Fleet Management Services
     */
    private void initializeFleetServices() {
        try {
            trainDAO = new TrainDAO();
            stationDAO = new StationDAO();
            System.out.println("✅ Fleet Management DAOs initialized successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize Fleet Management DAOs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load admin profile information
     */
    private void loadAdminProfile() {
        Admin currentAdmin = sessionManager.getCurrentAdmin();
        if (currentAdmin == null) {
            handleBackToMenu();
            return;
        }

        adminWelcomeLabel.setText("Welcome, " + currentAdmin.getUsername() + "!");
    }

    /**
     * Load statistics from database
     */
    private void loadStatistics() {
        Task<TravelStatistics> statsTask = new Task<>() {
            @Override
            protected TravelStatistics call() throws Exception {
                return dataStructureService.getTravelStatistics();
            }
        };

        statsTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                TravelStatistics stats = statsTask.getValue();
                totalUsersLabel.setText(String.valueOf(stats.getTotalUsers()));
                activeTrainsLabel.setText(String.valueOf(stats.getActiveTrains()));
                totalBookingsLabel.setText(String.valueOf(stats.getTotalBookings()));
                revenueLabel.setText("₹" + String.format("%.2f", stats.getTotalRevenue()));
            });
        });

        statsTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                showErrorMessage("Failed to load statistics from database.");
                // Fallback to zero values
                totalUsersLabel.setText("0");
                activeTrainsLabel.setText("0");
                totalBookingsLabel.setText("0");
                revenueLabel.setText("₹0.00");
            });
        });

        new Thread(statsTask).start();
    }

    /**
     * Initialize Pricing Engine (TreeMap demonstration)
     */
    private void initializePricingEngine() {
        // Initialize class combo for pricing
        if (classComboBox != null) {
            ObservableList<String> classes = FXCollections.observableArrayList("SL", "3A", "2A", "1A");
            classComboBox.setItems(classes);
            classComboBox.setValue("SL");  // Default
        }
        initializeFareStructure();
    }

    /**
     * Initialize Fare Structure (TreeMap demonstration)
     */
    private void initializeFareStructure() {
        Map<TrainClass, TreeMap<Integer, Double>> allFares = dataStructureService.getAllFareStructures();
        ObservableList<String> fareList = FXCollections.observableArrayList();
        for (Map.Entry<TrainClass, TreeMap<Integer, Double>> classEntry : allFares.entrySet()) {
            String classCode = classEntry.getKey().name().replace("_", "");
            for (Map.Entry<Integer, Double> entry : classEntry.getValue().entrySet()) {
                fareList.add(classCode + ": " + entry.getKey() + " km → ₹" + entry.getValue());
            }
        }
        fareStructureListView.setItems(fareList);
    }

    // ===================== PRICING HANDLERS =====================

    @FXML
    public void handleSetFare(ActionEvent event) {
        try {
            String classCode = classComboBox.getValue();
            if (classCode == null) {
                showErrorMessage("Please select a class!");
                return;
            }

            TrainClass trainClass = TrainClass.fromString(classCode);

            String distanceText = distanceField.getText().trim();
            String fareText = fareField.getText().trim();
            if (distanceText.isEmpty() || fareText.isEmpty()) {
                showErrorMessage("Please enter both distance and fare!");
                return;
            }

            int distance = Integer.parseInt(distanceText);
            double fare = Double.parseDouble(fareText);
            if (distance <= 0 || fare <= 0) {
                showErrorMessage("Distance and fare must be positive values!");
                return;
            }

            dataStructureService.setFare(trainClass, distance, fare);
            initializeFareStructure();
            distanceField.clear();
            fareField.clear();
            showSuccessMessage("Fare updated successfully for " + classCode + "! (Auto-sorted by distance)");
        } catch (NumberFormatException e) {
            showErrorMessage("Please enter valid numbers for distance and fare!");
        } catch (IllegalArgumentException e) {
            showErrorMessage("Invalid class selected!");
        }
    }

    @FXML
    public void handleOptimizePricing(ActionEvent event) {
        try {
            // Sample optimization - add/update fares for all classes
            dataStructureService.setFare(TrainClass.SL, 100, 150.0);
            dataStructureService.setFare(TrainClass.SL, 300, 450.0);
            dataStructureService.setFare(TrainClass._3A, 100, 225.0);
            dataStructureService.setFare(TrainClass._3A, 300, 675.0);
            dataStructureService.setFare(TrainClass._2A, 100, 300.0);
            dataStructureService.setFare(TrainClass._2A, 300, 900.0);
            dataStructureService.setFare(TrainClass._1A, 100, 450.0);
            dataStructureService.setFare(TrainClass._1A, 300, 1350.0);
            initializeFareStructure();
            showSuccessMessage("Pricing optimized for all classes!");
        } catch (Exception e) {
            showErrorMessage("Failed to optimize pricing: " + e.getMessage());
        }
    }

    // ===================== MANAGEMENT HANDLERS =====================

    @FXML
    public void handleUserManagement(ActionEvent event) {
        SceneManager.switchScene("/fxml/UserManagement.fxml");
    }

    @FXML
    public void handleManageTrains(ActionEvent event) {
        if (trainDAO != null) {
            try {
                int trainCount = trainDAO.getAllTrains().size();
                showInfoMessage("Train Management: " + trainCount + " trains in fleet.");
            } catch (Exception e) {
                showErrorMessage("Failed to load train data.");
            }
        } else {
            showInfoMessage("Train Management: Coming soon!");
        }
    }

    @FXML
    public void handleBookingManagement(ActionEvent event) {
        SceneManager.switchScene("/fxml/Reservations.fxml");
    }

    @FXML
    public void handleStationManagement(ActionEvent event) {
        SceneManager.switchScene("/fxml/StationNetwork.fxml");
    }

    @FXML
    public void handleTrainManagement(ActionEvent event) {
        try {
            showFleetManagementWindow();
        } catch (Exception e) {
            showErrorMessage("Failed to open Fleet Management: " + e.getMessage());
        }
    }

    /**
     * Show Fleet Management CRUD window
     */
    private void showFleetManagementWindow() {
        try {
            Stage fleetStage = new Stage();
            fleetStage.setTitle("Tailyatri - Fleet Management System");
            fleetStage.initModality(Modality.APPLICATION_MODAL);
            fleetStage.initOwner(((Stage) adminWelcomeLabel.getScene().getWindow()));

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FleetManagement.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1200, 800);
            fleetStage.setScene(scene);
            fleetStage.setMinWidth(1000);
            fleetStage.setMinHeight(700);
            fleetStage.setResizable(true);

            fleetStage.showAndWait();

            loadStatistics();  // Refresh stats after close
        } catch (IOException e) {
            showErrorMessage("Fleet Management FXML not found.");
        } catch (Exception e) {
            showErrorMessage("Error opening Fleet Management.");
        }
    }

    // ===================== NAVIGATION HANDLERS =====================

    @FXML
    public void handleBackToMenu() {
        try {
            SceneManager.switchScene("/fxml/MainMenu.fxml");
        } catch (Exception e) {
            showErrorMessage("Failed to navigate to main menu.");
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        authService.logout();
        try {
            SceneManager.switchScene("/fxml/Login.fxml");
        } catch (Exception e) {
            showErrorMessage("Failed to logout.");
        }
    }

    // ===================== UTILITY METHODS =====================

    private void updateSystemStatus() {
        if (lastUpdateLabel != null) {
            lastUpdateLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
        }
    }

    private void showSuccessMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.getStyleClass().removeAll("error");
            messageLabel.getStyleClass().add("success");
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
            PauseTransition hideDelay = new PauseTransition(Duration.seconds(3));
            hideDelay.setOnFinished(e -> {
                messageLabel.setVisible(false);
                messageLabel.setManaged(false);
            });
            hideDelay.play();
        }
    }

    private void showErrorMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.getStyleClass().removeAll("success");
            messageLabel.getStyleClass().add("error");
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
            PauseTransition hideDelay = new PauseTransition(Duration.seconds(5));
            hideDelay.setOnFinished(e -> {
                messageLabel.setVisible(false);
                messageLabel.setManaged(false);
            });
            hideDelay.play();
        }
    }

    private void showInfoMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.getStyleClass().removeAll("success", "error");
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
            PauseTransition hideDelay = new PauseTransition(Duration.seconds(4));
            hideDelay.setOnFinished(e -> {
                messageLabel.setVisible(false);
                messageLabel.setManaged(false);
            });
            hideDelay.play();
        }
    }
}