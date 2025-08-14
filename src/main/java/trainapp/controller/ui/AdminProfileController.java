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
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import trainapp.model.Admin;
import trainapp.service.AdminDataStructureService;
import trainapp.service.AuthService;
import trainapp.service.SessionManager;
import trainapp.dao.TrainDAO;
import trainapp.dao.StationDAO;
import trainapp.util.SceneManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    // Data Structure Demo Controls
    @FXML private ListView<String> recentActivitiesListView; // LinkedList demo
    @FXML private TextField activityTextField;

    @FXML private ListView<String> routeCacheListView; // HashMap demo
    @FXML private TextField routeKeyField;
    @FXML private TextField routeValueField;

    @FXML private ListView<String> userSessionsListView; // HashTable demo
    @FXML private TextField sessionUserField;
    @FXML private Label sessionCountLabel;

    @FXML private ListView<String> fareStructureListView; // TreeMap demo
    @FXML private TextField distanceField;
    @FXML private TextField fareField;

    // Charts
    @FXML private LineChart<String, Number> bookingTrendsChart;
    @FXML private CategoryAxis bookingTrendsXAxis;
    @FXML private NumberAxis bookingTrendsYAxis;
    @FXML private PieChart revenueDistributionChart;
    @FXML private ComboBox<String> chartFilterComboBox;

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
        initializeDataStructures();
        loadCharts();
        updateSystemStatus();
        initializeComboBox();
    }

    /**
     * Initialize Fleet Management Services
     */
    private void initializeFleetServices() {
        try {
            trainDAO = new TrainDAO();
            stationDAO = new StationDAO();
            System.out.println("✅ Fleet Management DAOs initialized successfully");

            // Add initial activity log
            dataStructureService.addActivity("🚄 Fleet Management system initialized");

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize Fleet Management DAOs: " + e.getMessage());
            e.printStackTrace();
            // Don't show error message here as UI might not be ready yet
        }
    }

    /**
     * Initialize ComboBox items in controller (safer approach)
     */
    private void initializeComboBox() {
        if (chartFilterComboBox != null) {
            ObservableList<String> filterOptions = FXCollections.observableArrayList(
                    "Last 7 Days",
                    "Last 30 Days",
                    "Last 90 Days"
            );
            chartFilterComboBox.setItems(filterOptions);
            chartFilterComboBox.setValue("Last 7 Days"); // Set default value
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
     * Enhanced statistics loading with fleet integration
     */
    private void loadStatistics() {
        Task<Void> statsTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Simulate loading statistics
                Thread.sleep(1000);

                Platform.runLater(() -> {
                    // Load existing statistics
                    totalUsersLabel.setText(String.valueOf(dataStructureService.getTotalUsers()));
                    totalBookingsLabel.setText(String.valueOf(dataStructureService.getTotalBookings()));
                    revenueLabel.setText("₹" + dataStructureService.getTotalRevenue());

                    // Load fleet-related statistics
                    if (trainDAO != null) {
                        try {
                            int fleetSize = trainDAO.getAllTrains().size();
                            activeTrainsLabel.setText(String.valueOf(fleetSize));

                            // Update activity log with fleet information
                            if (stationDAO != null) {
                                int stationCount = stationDAO.getAllStations().size();
                                String fleetInfo = String.format("📊 Fleet status: %d trains, %d stations operational",
                                        fleetSize, stationCount);
                                dataStructureService.addActivity(fleetInfo);
                            }
                        } catch (Exception e) {
                            // Fallback to data structure service data
                            activeTrainsLabel.setText(String.valueOf(dataStructureService.getActiveTrains()));
                            System.err.println("Failed to load fleet statistics: " + e.getMessage());
                        }
                    } else {
                        activeTrainsLabel.setText(String.valueOf(dataStructureService.getActiveTrains()));
                    }

                    // Update session count
                    if (sessionCountLabel != null) {
                        sessionCountLabel.setText(dataStructureService.getActiveUserCount() + " Active");
                    }
                });

                return null;
            }
        };

        new Thread(statsTask).start();
    }

    /**
     * Initialize all data structures for demonstration
     */
    private void initializeDataStructures() {
        // Initialize LinkedList demo (Recent Activities)
        initializeRecentActivities();

        // Initialize HashMap demo (Route Cache)
        initializeRouteCache();

        // Initialize HashTable demo (User Sessions)
        initializeUserSessions();

        // Initialize TreeMap demo (Fare Structure)
        initializeFareStructure();
    }

    /**
     * Initialize Recent Activities (LinkedList demonstration)
     */
    private void initializeRecentActivities() {
        LinkedList<String> activities = dataStructureService.getRecentActivities();
        ObservableList<String> activityList = FXCollections.observableArrayList(activities);
        recentActivitiesListView.setItems(activityList);
    }

    /**
     * Initialize Route Cache (HashMap demonstration)
     */
    private void initializeRouteCache() {
        HashMap<String, String> routeCache = dataStructureService.getRouteCache();
        ObservableList<String> cacheList = FXCollections.observableArrayList();

        for (Map.Entry<String, String> entry : routeCache.entrySet()) {
            cacheList.add(entry.getKey() + " → " + entry.getValue());
        }

        routeCacheListView.setItems(cacheList);
    }

    /**
     * Initialize User Sessions (HashTable demonstration)
     */
    private void initializeUserSessions() {
        Hashtable<String, String> userSessions = dataStructureService.getUserSessions();
        ObservableList<String> sessionList = FXCollections.observableArrayList();

        for (Map.Entry<String, String> entry : userSessions.entrySet()) {
            sessionList.add(entry.getKey() + " (Since: " + entry.getValue() + ")");
        }

        userSessionsListView.setItems(sessionList);
    }

    /**
     * Initialize Fare Structure (TreeMap demonstration)
     */
    private void initializeFareStructure() {
        TreeMap<Integer, Double> fareStructure = dataStructureService.getFareStructure();
        ObservableList<String> fareList = FXCollections.observableArrayList();

        for (Map.Entry<Integer, Double> entry : fareStructure.entrySet()) {
            fareList.add(entry.getKey() + " km → ₹" + entry.getValue());
        }

        fareStructureListView.setItems(fareList);
    }

    /**
     * Load charts with sample data
     */
    private void loadCharts() {
        loadBookingTrendsChart();
        loadRevenueDistributionChart();
    }

    private void loadBookingTrendsChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Bookings");

        series.getData().add(new XYChart.Data<>("Jan", 120));
        series.getData().add(new XYChart.Data<>("Feb", 150));
        series.getData().add(new XYChart.Data<>("Mar", 180));
        series.getData().add(new XYChart.Data<>("Apr", 200));
        series.getData().add(new XYChart.Data<>("May", 250));

        bookingTrendsChart.getData().add(series);
    }

    private void loadRevenueDistributionChart() {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("AC First Class", 35),
                new PieChart.Data("AC 2 Tier", 30),
                new PieChart.Data("AC 3 Tier", 25),
                new PieChart.Data("Sleeper", 10)
        );
        revenueDistributionChart.setData(pieChartData);
    }

    // ===================== DATA STRUCTURE DEMO HANDLERS =====================

    /**
     * Add activity to LinkedList
     */
    @FXML
    public void handleAddActivity(ActionEvent event) {
        String activity = activityTextField.getText().trim();
        if (!activity.isEmpty()) {
            dataStructureService.addActivity(activity);

            // Update ListView
            ObservableList<String> currentItems = recentActivitiesListView.getItems();
            currentItems.add(0, activity + " (" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + ")");

            // Keep only last 10 activities
            if (currentItems.size() > 10) {
                currentItems.remove(currentItems.size() - 1);
            }

            activityTextField.clear();
            showSuccessMessage("System event logged successfully!");
        }
    }

    /**
     * Handle refresh activity - reload recent activities
     */
    @FXML
    public void handleRefreshActivity(ActionEvent event) {
        try {
            initializeRecentActivities();
            updateSystemStatus();

            // Refresh fleet data if available
            if (trainDAO != null && stationDAO != null) {
                updateFleetStatisticsInActivityLog();
            }

            showSuccessMessage("Activity monitor refreshed!");
        } catch (Exception e) {
            showErrorMessage("Failed to refresh activities: " + e.getMessage());
        }
    }

    /**
     * Update fleet statistics in activity log
     */
    private void updateFleetStatisticsInActivityLog() {
        try {
            int trainCount = trainDAO.getAllTrains().size();
            int stationCount = stationDAO.getAllStations().size();

            String fleetUpdate = String.format("🔄 Fleet data refreshed: %d trains, %d stations",
                    trainCount, stationCount);
            dataStructureService.addActivity(fleetUpdate);

            // Refresh the display
            initializeRecentActivities();
        } catch (Exception e) {
            System.err.println("Failed to update fleet statistics in activity log: " + e.getMessage());
        }
    }

    /**
     * Cache route in HashMap
     */
    @FXML
    public void handleCacheRoute(ActionEvent event) {
        String key = routeKeyField.getText().trim();
        String value = routeValueField.getText().trim();

        if (!key.isEmpty() && !value.isEmpty()) {
            dataStructureService.cacheRoute(key, value);

            // Update ListView
            ObservableList<String> currentItems = routeCacheListView.getItems();
            currentItems.add(key + " → " + value);

            routeKeyField.clear();
            routeValueField.clear();
            showSuccessMessage("Route cached successfully!");
        } else {
            showErrorMessage("Please enter both route ID and information!");
        }
    }

    /**
     * Simulate user login in HashTable
     */
    @FXML
    public void handleUserLogin(ActionEvent event) {
        String username = sessionUserField.getText().trim();
        if (!username.isEmpty()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            dataStructureService.loginUser(username, timestamp);

            // Update ListView
            ObservableList<String> currentItems = userSessionsListView.getItems();
            currentItems.add(username + " (Since: " + timestamp + ")");

            // Update session count
            if (sessionCountLabel != null) {
                sessionCountLabel.setText(dataStructureService.getActiveUserCount() + " Active");
            }

            sessionUserField.clear();
            showSuccessMessage("User session connected!");
        } else {
            showErrorMessage("Please enter a username!");
        }
    }

    /**
     * Simulate user logout from HashTable
     */
    @FXML
    public void handleUserLogout(ActionEvent event) {
        String username = sessionUserField.getText().trim();
        if (!username.isEmpty()) {
            dataStructureService.logoutUser(username);

            // Update ListView - remove matching entry
            ObservableList<String> currentItems = userSessionsListView.getItems();
            currentItems.removeIf(item -> item.startsWith(username + " "));

            // Update session count
            if (sessionCountLabel != null) {
                sessionCountLabel.setText(dataStructureService.getActiveUserCount() + " Active");
            }

            sessionUserField.clear();
            showSuccessMessage("User session disconnected!");
        } else {
            showErrorMessage("Please enter a username!");
        }
    }

    /**
     * Set fare in TreeMap
     */
    @FXML
    public void handleSetFare(ActionEvent event) {
        try {
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

            dataStructureService.setFare(distance, fare);

            // Update ListView in sorted order (TreeMap benefit)
            initializeFareStructure();

            distanceField.clear();
            fareField.clear();
            showSuccessMessage("Fare updated successfully! (Auto-sorted by distance)");

        } catch (NumberFormatException e) {
            showErrorMessage("Please enter valid numbers for distance and fare!");
        }
    }

    /**
     * Handle optimize pricing button click
     */
    @FXML
    public void handleOptimizePricing(ActionEvent event) {
        try {
            // Add some sample optimized pricing
            dataStructureService.setFare(100, 150.0);
            dataStructureService.setFare(300, 450.0);
            initializeFareStructure();

            // Log activity
            dataStructureService.addActivity("💰 Pricing structure optimized");
            initializeRecentActivities();

            showSuccessMessage("Pricing optimized!");
        } catch (Exception e) {
            showErrorMessage("Failed to optimize pricing: " + e.getMessage());
        }
    }

    // ===================== MANAGEMENT HANDLERS =====================

    @FXML
    public void handleManageUsers(ActionEvent event) {
        int userCount = dataStructureService.getTotalUsers();
        showInfoMessage("User Management: " + userCount + " users in system");

        // Log activity
        dataStructureService.addActivity("👥 User Management accessed - " + userCount + " users");
        initializeRecentActivities();
    }

    @FXML
    public void handleManageTrains(ActionEvent event) {
        if (trainDAO != null) {
            try {
                int trainCount = trainDAO.getAllTrains().size();
                showInfoMessage("Train Management: " + trainCount + " trains in fleet registry");

                // Log activity
                dataStructureService.addActivity("🚂 Train Management accessed - " + trainCount + " trains");
                initializeRecentActivities();
            } catch (Exception e) {
                showInfoMessage("Route Management: " + dataStructureService.getActiveTrains() + " active routes");
            }
        } else {
            showInfoMessage("Route Management: " + dataStructureService.getActiveTrains() + " active routes");
        }
    }

    @FXML
    public void handleViewBookings(ActionEvent event) {
        int bookingCount = dataStructureService.getTotalBookings();
        showInfoMessage("Booking Management: " + bookingCount + " total bookings");

        // Log activity
        dataStructureService.addActivity("🎫 Booking Management accessed - " + bookingCount + " bookings");
        initializeRecentActivities();
    }

    @FXML
    public void handleViewReports(ActionEvent event) {
        String revenue = dataStructureService.getTotalRevenue();
        showInfoMessage("Financial Reports: ₹" + revenue + " total revenue");

        // Log activity
        dataStructureService.addActivity("📊 Financial Reports accessed - ₹" + revenue + " revenue");
        initializeRecentActivities();
    }

    /**
     * Handle Fleet Management - Opens simplified CRUD Fleet Management window
     */
    @FXML
    public void handleTrainManagement(ActionEvent event) {
        try {
            showFleetManagementWindow();
        } catch (Exception e) {
            System.err.println("❌ Failed to open Fleet Management: " + e.getMessage());
            e.printStackTrace();

            // Fallback: Show fleet information using existing DAOs
            showFleetInformationFallback();
        }
    }

    /**
     * Show Fleet Management CRUD window
     */
    private void showFleetManagementWindow() {
        try {
            // Create new stage for Fleet Management
            Stage fleetStage = new Stage();
            fleetStage.setTitle("Tailyatri - Fleet Management System");
            fleetStage.initModality(Modality.APPLICATION_MODAL);
            fleetStage.initOwner(((Stage) adminWelcomeLabel.getScene().getWindow()));

            // Load Fleet Management FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FleetManagement.fxml"));
            Parent root = loader.load();

            System.out.println("🚀 Fleet Management CRUD window opened successfully");

            // Set up scene and show
            Scene scene = new Scene(root, 1200, 800);
            fleetStage.setScene(scene);
            fleetStage.setMinWidth(1000);
            fleetStage.setMinHeight(700);
            fleetStage.setResizable(true);

            // Add activity log before showing
            dataStructureService.addActivity("🚄 Fleet Management CRUD system accessed");
            initializeRecentActivities();

            // Show and wait
            fleetStage.showAndWait();

            // Update admin panel after fleet management window closes
            updateFleetStatisticsAfterClose();

        } catch (IOException e) {
            System.err.println("❌ Failed to load Fleet Management FXML: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Fleet Management FXML not found", e);

        } catch (Exception e) {
            System.err.println("❌ Unexpected error opening Fleet Management: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Fallback fleet information display
     */
    private void showFleetInformationFallback() {
        try {
            if (trainDAO != null && stationDAO != null) {
                int trainCount = trainDAO.getAllTrains().size();
                int stationCount = stationDAO.getAllStations().size();

                String message = String.format(
                        "Fleet Management: %d trains, %d stations in network.\n" +
                                "CRUD interface will be available once FleetManagement.fxml is properly configured!",
                        trainCount, stationCount
                );

                showInfoMessage(message);

                // Log activity
                dataStructureService.addActivity("🚄 Fleet data accessed via fallback - " + trainCount + " trains, " + stationCount + " stations");
                initializeRecentActivities();

            } else {
                showErrorMessage("Fleet Management system is not available. Please contact administrator.");
            }
        } catch (Exception e) {
            showErrorMessage("Failed to access fleet information: " + e.getMessage());
        }
    }

    /**
     * Update fleet statistics after Fleet Management window closes
     */
    private void updateFleetStatisticsAfterClose() {
        try {
            if (trainDAO != null && stationDAO != null) {
                // Reload statistics
                loadStatistics();

                // Add activity to admin panel data structures
                dataStructureService.addActivity("🔄 Fleet Management data synchronized");
                initializeRecentActivities();

                showSuccessMessage("Fleet statistics updated!");
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to update fleet statistics: " + e.getMessage());
        }
    }

    @FXML
    public void handleUserManagement(ActionEvent event) {
        int userCount = dataStructureService.getTotalUsers();
        showInfoMessage("User Management: Advanced user control panel with role-based access for " + userCount + " users will be available in the next update!");

        // Log activity
        dataStructureService.addActivity("👤 User Management panel accessed - " + userCount + " users in system");
        initializeRecentActivities();
    }

    @FXML
    public void handleBookingManagement(ActionEvent event) {
        int bookingCount = dataStructureService.getTotalBookings();
        showInfoMessage("Booking Management: Comprehensive reservation system with real-time seat availability for " + bookingCount + " bookings will be available in the next update!");

        // Log activity
        dataStructureService.addActivity("🎫 Booking Management panel accessed - " + bookingCount + " total bookings");
        initializeRecentActivities();
    }

    @FXML
    public void handleStationManagement(ActionEvent event) {
        try {
            if (stationDAO != null) {
                int stationCount = stationDAO.getAllStations().size();
                showInfoMessage("Station Network: " + stationCount + " stations in network. Advanced station configuration panel coming in next update!");

                // Log activity
                dataStructureService.addActivity("🚉 Station Network accessed - " + stationCount + " stations available");
                initializeRecentActivities();
            } else {
                showInfoMessage("Station Network management will be available in the next update!");
            }
        } catch (Exception e) {
            showErrorMessage("Failed to access station information: " + e.getMessage());
        }
    }

    // ===================== NAVIGATION HANDLERS =====================

    @FXML
    public void handleBackToMenu(ActionEvent event) {
        handleBackToMenu();
    }

    private void handleBackToMenu() {
        try {
            SceneManager.switchScene("/fxml/MainMenu.fxml");
        } catch (Exception e) {
            showErrorMessage("Failed to navigate to main menu.");
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        // Log activity before logout
        dataStructureService.addActivity("🚪 Admin session ended - " + sessionManager.getCurrentAdmin().getUsername());

        authService.logout();

        try {
            SceneManager.switchScene("/fxml/Login.fxml");
        } catch (Exception e) {
            showErrorMessage("Failed to logout properly.");
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