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

/**
 * AdminProfileController manages the main administrative dashboard and controls.
 * Provides access to system statistics, fare management, and administrative operations.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Real-time system statistics display (users, trains, bookings, revenue)</li>
 *   <li>Dynamic fare management with TreeMap-based pricing engine</li>
 *   <li>Navigation to various administrative modules</li>
 *   <li>Fleet management operations and CRUD interfaces</li>
 *   <li>Session management and authentication controls</li>
 * </ul>
 *
 * <p>Administrative Functions:
 * <ul>
 *   <li>User management and account operations</li>
 *   <li>Train fleet management with CRUD operations</li>
 *   <li>Station network management</li>
 *   <li>Booking and reservation oversight</li>
 *   <li>Dynamic pricing and fare optimization</li>
 * </ul>
 */
public class AdminProfileController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // Header Controls
    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private Label adminWelcomeLabel;

    // Statistics Display
    @FXML private Label totalUsersLabel;
    @FXML private Label activeTrainsLabel;
    @FXML private Label totalBookingsLabel;
    @FXML private Label revenueLabel;

    // Dynamic Pricing Engine Controls
    @FXML private ListView<String> fareStructureListView;
    @FXML private TextField distanceField;
    @FXML private TextField fareField;
    @FXML private ComboBox<String> classComboBox;

    // Status and Messaging
    @FXML private Label lastUpdateLabel;
    @FXML private Label messageLabel;

    // -------------------------------------------------------------------------
    // Services and Data Access
    // -------------------------------------------------------------------------

    private final SessionManager sessionManager = SessionManager.getInstance();
    private final AuthService authService = new AuthService();
    private final AdminDataStructureService dataStructureService = new AdminDataStructureService();

    private TrainDAO trainDAO;
    private StationDAO stationDAO;

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the admin dashboard with profile information, statistics, and pricing engine.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        loadAdminProfile();
        initializeFleetServices();
        loadStatistics();
        initializePricingEngine();
        updateSystemStatus();
    }

    /**
     * Loads and displays the current admin's profile information.
     * Redirects to menu if no admin session is found.
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
     * Initializes fleet management DAOs for train and station operations.
     * Provides safe fallback if initialization fails.
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
     * Initializes the dynamic pricing engine with available train classes.
     * Sets up fare structure display and class selection controls.
     */
    private void initializePricingEngine() {
        if (classComboBox != null) {
            ObservableList<String> classes = FXCollections.observableArrayList("SL", "3A", "2A", "1A");
            classComboBox.setItems(classes);
            classComboBox.setValue("SL");
        }
        initializeFareStructure();
    }

    // -------------------------------------------------------------------------
    // Data Loading and Statistics
    // -------------------------------------------------------------------------

    /**
     * Loads system statistics asynchronously and updates dashboard display.
     * Shows real-time data for users, trains, bookings, and revenue.
     * Provides fallback values if database access fails.
     */
    private void loadStatistics() {
        Task<TravelStatistics> statsTask = new Task<>() {
            @Override
            protected TravelStatistics call() throws Exception {
                return dataStructureService.getTravelStatistics();
            }
        };

        statsTask.setOnSucceeded(e -> Platform.runLater(() -> {
            TravelStatistics stats = statsTask.getValue();
            totalUsersLabel.setText(String.valueOf(stats.getTotalUsers()));
            activeTrainsLabel.setText(String.valueOf(stats.getActiveTrains()));
            totalBookingsLabel.setText(String.valueOf(stats.getTotalBookings()));
            revenueLabel.setText("₹" + String.format("%.2f", stats.getTotalRevenue()));
        }));

        statsTask.setOnFailed(e -> Platform.runLater(() -> {
            showErrorMessage("Failed to load statistics from database.");
            // Fallback to zero values
            totalUsersLabel.setText("0");
            activeTrainsLabel.setText("0");
            totalBookingsLabel.setText("0");
            revenueLabel.setText("₹0.00");
        }));

        new Thread(statsTask).start();
    }

    /**
     * Initializes and displays the current fare structure using TreeMap data.
     * Shows auto-sorted fare information for all train classes.
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

    // -------------------------------------------------------------------------
    // Dynamic Pricing Management
    // -------------------------------------------------------------------------

    /**
     * Handles setting custom fare for a specific class and distance.
     * Updates the TreeMap-based pricing structure and refreshes display.
     *
     * @param event ActionEvent from the set fare button
     */
    @FXML
    public void handleSetFare(ActionEvent event) {
        String classCode = classComboBox.getValue();
        String distanceText = distanceField.getText().trim();
        String fareText = fareField.getText().trim();

        if (classCode == null) {
            showErrorMessage("Please select a class!");
            return;
        }
        if (distanceText.isEmpty() || fareText.isEmpty()) {
            showErrorMessage("Please enter both distance and fare!");
            return;
        }

        try {
            TrainClass trainClass = TrainClass.fromString(classCode);
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

    /**
     * Handles automatic fare optimization for all train classes.
     * Applies pre-defined optimized pricing structure across all classes.
     *
     * @param event ActionEvent from the optimize pricing button
     */
    @FXML
    public void handleOptimizePricing(ActionEvent event) {
        try {
            // Apply optimized fares for all classes
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

    // -------------------------------------------------------------------------
    // Administrative Module Navigation
    // -------------------------------------------------------------------------

    /**
     * Opens the user management interface for admin operations.
     *
     * @param event ActionEvent from user management button
     */
    @FXML
    public void handleUserManagement(ActionEvent event) {
        SceneManager.switchScene("/fxml/UserManagement.fxml");
    }

    /**
     * Opens the booking and reservation management interface.
     *
     * @param event ActionEvent from booking management button
     */
    @FXML
    public void handleBookingManagement(ActionEvent event) {
        SceneManager.switchScene("/fxml/Reservations.fxml");
    }

    /**
     * Opens the station network management interface.
     *
     * @param event ActionEvent from station management button
     */
    @FXML
    public void handleStationManagement(ActionEvent event) {
        SceneManager.switchScene("/fxml/StationNetwork.fxml");
    }

    /**
     * Opens the comprehensive fleet management interface with CRUD operations.
     * Displays train count and launches modal window for detailed management.
     *
     * @param event ActionEvent from train management button
     */
    @FXML
    public void handleTrainManagement(ActionEvent event) {
        try {
            showFleetManagementWindow();
        } catch (Exception e) {
            showErrorMessage("Failed to open Fleet Management: " + e.getMessage());
        }
    }

    /**
     * Displays basic train information when legacy manage trains button is used.
     *
     * @param event ActionEvent from manage trains button
     */
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

    // -------------------------------------------------------------------------
    // Window Management
    // -------------------------------------------------------------------------

    /**
     * Opens the Fleet Management CRUD interface in a modal window.
     * Provides comprehensive train and route management capabilities.
     * Refreshes statistics after window closes.
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
            loadStatistics(); // Refresh stats after close
        } catch (IOException e) {
            showErrorMessage("Fleet Management FXML not found.");
        } catch (Exception e) {
            showErrorMessage("Error opening Fleet Management.");
        }
    }

    // -------------------------------------------------------------------------
    // Session and Navigation Management
    // -------------------------------------------------------------------------

    /**
     * Handles navigation back to the main menu.
     */
    @FXML
    public void handleBackToMenu() {
        try {
            SceneManager.switchScene("/fxml/MainMenu.fxml");
        } catch (Exception e) {
            showErrorMessage("Failed to navigate to main menu.");
        }
    }

    /**
     * Handles admin logout and returns to login screen.
     * Clears current session and redirects appropriately.
     *
     * @param event ActionEvent from logout button
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        authService.logout();
        try {
            SceneManager.switchScene("/fxml/Login.fxml");
        } catch (Exception e) {
            showErrorMessage("Failed to logout.");
        }
    }

    // -------------------------------------------------------------------------
    // Utility and Status Management
    // -------------------------------------------------------------------------

    /**
     * Updates the system status display with current timestamp.
     */
    private void updateSystemStatus() {
        if (lastUpdateLabel != null) {
            lastUpdateLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
        }
    }

    /**
     * Displays a success message with green styling and auto-hide after 3 seconds.
     *
     * @param message success message to display
     */
    private void showSuccessMessage(String message) {
        showMessage(message, "success", 3);
    }

    /**
     * Displays an error message with red styling and auto-hide after 5 seconds.
     *
     * @param message error message to display
     */
    private void showErrorMessage(String message) {
        showMessage(message, "error", 5);
    }

    /**
     * Displays an informational message with default styling and auto-hide after 4 seconds.
     *
     * @param message info message to display
     */
    private void showInfoMessage(String message) {
        showMessage(message, "", 4);
    }

    /**
     * Unified message display method with styling and auto-hide functionality.
     * Consolidates message handling logic to reduce code duplication.
     *
     * @param message text to display
     * @param styleClass CSS class for styling ("success", "error", or "")
     * @param hideDelaySeconds seconds before auto-hiding the message
     */
    private void showMessage(String message, String styleClass, int hideDelaySeconds) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.getStyleClass().removeAll("success", "error");
            if (!styleClass.isEmpty()) {
                messageLabel.getStyleClass().add(styleClass);
            }
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);

            PauseTransition hideDelay = new PauseTransition(Duration.seconds(hideDelaySeconds));
            hideDelay.setOnFinished(e -> {
                messageLabel.setVisible(false);
                messageLabel.setManaged(false);
            });
            hideDelay.play();
        }
    }
}