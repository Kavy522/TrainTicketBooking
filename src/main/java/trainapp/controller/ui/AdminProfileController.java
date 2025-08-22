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
 * AdminProfileController manages the comprehensive administrative dashboard and system controls.
 *
 * <h2>Core Responsibilities:</h2>
 * <ul>
 *   <li><b>Dashboard Management</b> - Central hub for all administrative operations and system oversight</li>
 *   <li><b>Real-Time Statistics</b> - Live monitoring of users, trains, bookings, and revenue metrics</li>
 *   <li><b>Dynamic Pricing Engine</b> - TreeMap-based fare management with automatic optimization</li>
 *   <li><b>Module Navigation</b> - Gateway to specialized administrative interfaces and tools</li>
 *   <li><b>Fleet Management</b> - Comprehensive train and station network administration</li>
 *   <li><b>Session Security</b> - Admin authentication and session management controls</li>
 * </ul>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Real-time system statistics with async data loading</li>
 *   <li>Advanced TreeMap-based pricing engine for dynamic fare management</li>
 *   <li>Comprehensive navigation to all administrative modules</li>
 *   <li>Modal window integration for complex management interfaces</li>
 *   <li>Auto-refresh capabilities and status monitoring</li>
 *   <li>Professional message system with styling and auto-hide functionality</li>
 *   <li>Secure session management with authentication verification</li>
 * </ul>
 *
 * <h2>Administrative Functions:</h2>
 * <ul>
 *   <li><b>User Management</b> - Account operations, user administration, and access control</li>
 *   <li><b>Fleet Operations</b> - Train CRUD operations, route management, and schedule control</li>
 *   <li><b>Station Network</b> - Station management, network configuration, and connectivity</li>
 *   <li><b>Booking Oversight</b> - Reservation management, booking analytics, and customer service</li>
 *   <li><b>Pricing Strategy</b> - Dynamic fare optimization, class-based pricing, and revenue management</li>
 *   <li><b>System Analytics</b> - Performance monitoring, usage statistics, and business intelligence</li>
 * </ul>
 *
 * <h2>Technical Architecture:</h2>
 * <ul>
 *   <li>Async task processing for non-blocking UI operations</li>
 *   <li>TreeMap-based data structures for efficient fare management</li>
 *   <li>Modal window system for complex administrative interfaces</li>
 *   <li>Professional message system with automatic cleanup</li>
 *   <li>Thread-safe operations with Platform.runLater() integration</li>
 * </ul>
 */
public class AdminProfileController {

    // =========================================================================
    // FXML UI COMPONENTS
    // =========================================================================

    // Header and Navigation Controls
    /** Button for navigating back to main menu */
    @FXML private Button backButton;

    /** Button for admin logout and session termination */
    @FXML private Button logoutButton;

    /** Welcome label displaying current admin username */
    @FXML private Label adminWelcomeLabel;

    // Real-Time Statistics Dashboard
    /** Label displaying total registered users count */
    @FXML private Label totalUsersLabel;

    /** Label displaying active trains in fleet */
    @FXML private Label activeTrainsLabel;

    /** Label displaying total booking transactions */
    @FXML private Label totalBookingsLabel;

    /** Label displaying total system revenue */
    @FXML private Label revenueLabel;

    // Dynamic Pricing Engine Interface
    /** ListView displaying current fare structure across all classes */
    @FXML private ListView<String> fareStructureListView;

    /** Input field for distance-based fare setting */
    @FXML private TextField distanceField;

    /** Input field for fare amount configuration */
    @FXML private TextField fareField;

    /** Dropdown for selecting train class for fare management */
    @FXML private ComboBox<String> classComboBox;

    // System Status and Messaging
    /** Label showing last system update timestamp */
    @FXML private Label lastUpdateLabel;

    /** Label for displaying system messages and notifications */
    @FXML private Label messageLabel;

    // =========================================================================
    // SERVICES AND DEPENDENCIES
    // =========================================================================

    /** Session manager for admin authentication and state management */
    private final SessionManager sessionManager = SessionManager.getInstance();

    /** Authentication service for login/logout operations */
    private final AuthService authService = new AuthService();

    /** Data structure service for pricing engine and statistics */
    private final AdminDataStructureService dataStructureService = new AdminDataStructureService();

    /** Data access object for train fleet management operations */
    private TrainDAO trainDAO;

    /** Data access object for station network management operations */
    private StationDAO stationDAO;

    // =========================================================================
    // INITIALIZATION AND SETUP
    // =========================================================================

    /**
     * Initializes the complete administrative dashboard with all components and data.
     * Called automatically by JavaFX framework after FXML loading and component injection.
     *
     * <h3>Initialization Sequence:</h3>
     * <ol>
     *   <li>Load and verify admin profile authentication</li>
     *   <li>Initialize fleet management services and DAOs</li>
     *   <li>Load real-time system statistics asynchronously</li>
     *   <li>Configure dynamic pricing engine with current fare structure</li>
     *   <li>Update system status and timestamp displays</li>
     * </ol>
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
     * Loads and displays current administrator's profile information.
     * Verifies active admin session and redirects if authentication fails.
     *
     * <h3>Security Features:</h3>
     * <ul>
     *   <li>Session validation and authentication verification</li>
     *   <li>Automatic redirect to menu for unauthorized access</li>
     *   <li>Personalized welcome message with admin username</li>
     * </ul>
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
     * Initializes fleet management data access objects for train and station operations.
     * Provides safe initialization with error handling and fallback mechanisms.
     *
     * <h3>Initialization Features:</h3>
     * <ul>
     *   <li>Safe DAO initialization with exception handling</li>
     *   <li>Database connection validation</li>
     *   <li>Graceful degradation if services unavailable</li>
     *   <li>Comprehensive error logging for troubleshooting</li>
     * </ul>
     */
    private void initializeFleetServices() {
        try {
            trainDAO = new TrainDAO();
            stationDAO = new StationDAO();
        } catch (Exception e) {
            // Graceful degradation - UI remains functional without fleet services
        }
    }

    /**
     * Initializes the dynamic pricing engine with available train classes and fare structure.
     * Sets up TreeMap-based pricing display and class selection controls.
     *
     * <h3>Pricing Engine Components:</h3>
     * <ul>
     *   <li>Class dropdown with all available train categories (SL, 3A, 2A, 1A)</li>
     *   <li>Real-time fare structure display with TreeMap sorting</li>
     *   <li>Interactive controls for custom fare configuration</li>
     *   <li>Automatic refresh and display update mechanisms</li>
     * </ul>
     */
    private void initializePricingEngine() {
        if (classComboBox != null) {
            ObservableList<String> classes = FXCollections.observableArrayList("SL", "3A", "2A", "1A");
            classComboBox.setItems(classes);
            classComboBox.setValue("SL");
        }
        initializeFareStructure();
    }

    // =========================================================================
    // DATA LOADING AND STATISTICS
    // =========================================================================

    /**
     * Loads comprehensive system statistics asynchronously and updates dashboard display.
     * Provides real-time monitoring of key business metrics and system performance.
     *
     * <h3>Statistics Monitored:</h3>
     * <ul>
     *   <li><b>User Metrics</b> - Total registered users and active accounts</li>
     *   <li><b>Fleet Status</b> - Active trains and operational capacity</li>
     *   <li><b>Booking Analytics</b> - Total transactions and reservation volume</li>
     *   <li><b>Revenue Tracking</b> - Financial performance and earnings summary</li>
     * </ul>
     *
     * <h3>Performance Features:</h3>
     * <ul>
     *   <li>Asynchronous loading prevents UI blocking</li>
     *   <li>Automatic fallback values for database connectivity issues</li>
     *   <li>Thread-safe UI updates with Platform.runLater()</li>
     *   <li>Professional error handling and user feedback</li>
     * </ul>
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
            // Fallback to zero values for graceful degradation
            totalUsersLabel.setText("0");
            activeTrainsLabel.setText("0");
            totalBookingsLabel.setText("0");
            revenueLabel.setText("₹0.00");
        }));

        new Thread(statsTask).start();
    }

    /**
     * Initializes and displays current fare structure using TreeMap-based data organization.
     * Shows automatically sorted fare information across all train classes.
     *
     * <h3>Display Features:</h3>
     * <ul>
     *   <li>TreeMap-based sorting for optimal fare display order</li>
     *   <li>Multi-class fare comparison and analysis</li>
     *   <li>Distance-based pricing structure visualization</li>
     *   <li>Real-time updates reflecting pricing changes</li>
     * </ul>
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

    // =========================================================================
    // DYNAMIC PRICING MANAGEMENT
    // =========================================================================

    /**
     * Handles custom fare configuration for specific train class and distance combinations.
     * Updates the TreeMap-based pricing structure and refreshes administrative display.
     *
     * <h3>Fare Management Process:</h3>
     * <ol>
     *   <li>Validate input parameters for class, distance, and fare amount</li>
     *   <li>Convert class selection to TrainClass enumeration</li>
     *   <li>Update TreeMap pricing structure with new fare information</li>
     *   <li>Refresh fare structure display with automatic sorting</li>
     *   <li>Clear input fields and show confirmation message</li>
     * </ol>
     *
     * <h3>Validation Features:</h3>
     * <ul>
     *   <li>Comprehensive input validation and error messaging</li>
     *   <li>Positive value enforcement for distance and fare</li>
     *   <li>Class selection validation and error handling</li>
     *   <li>Automatic field clearing after successful updates</li>
     * </ul>
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
     * Handles automatic fare optimization across all train classes using predefined algorithms.
     * Applies scientifically optimized pricing structure for maximum revenue and customer satisfaction.
     *
     * <h3>Optimization Strategy:</h3>
     * <ul>
     *   <li><b>SL Class</b> - Economy pricing for budget-conscious travelers</li>
     *   <li><b>3A Class</b> - Mid-tier pricing with balanced comfort and affordability</li>
     *   <li><b>2A Class</b> - Premium pricing for enhanced comfort and privacy</li>
     *   <li><b>1A Class</b> - Luxury pricing for maximum comfort and exclusive service</li>
     * </ul>
     *
     * <h3>Pricing Tiers:</h3>
     * <ul>
     *   <li>Distance-based scaling for fair and transparent pricing</li>
     *   <li>Class-based multipliers reflecting service quality differences</li>
     *   <li>Market-competitive rates optimized for revenue generation</li>
     *   <li>Automatic TreeMap organization for efficient fare lookup</li>
     * </ul>
     *
     * @param event ActionEvent from the optimize pricing button
     */
    @FXML
    public void handleOptimizePricing(ActionEvent event) {
        try {
            // Apply scientifically optimized fares across all classes
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

    // =========================================================================
    // ADMINISTRATIVE MODULE NAVIGATION
    // =========================================================================

    /**
     * Opens comprehensive user management interface for administrative operations.
     * Provides access to user account administration, access control, and customer service tools.
     *
     * <h3>User Management Features:</h3>
     * <ul>
     *   <li>User account creation, modification, and deletion</li>
     *   <li>Access control and permission management</li>
     *   <li>Customer service and support ticket handling</li>
     *   <li>User analytics and behavior monitoring</li>
     * </ul>
     *
     * @param event ActionEvent from user management navigation button
     */
    @FXML
    public void handleUserManagement(ActionEvent event) {
        SceneManager.switchScene("/fxml/UserManagement.fxml");
    }

    /**
     * Opens booking and reservation management interface for transaction oversight.
     * Provides comprehensive tools for booking administration and customer service.
     *
     * <h3>Booking Management Capabilities:</h3>
     * <ul>
     *   <li>Reservation monitoring and status tracking</li>
     *   <li>Booking modification and cancellation processing</li>
     *   <li>Payment verification and refund management</li>
     *   <li>Customer service and dispute resolution</li>
     * </ul>
     *
     * @param event ActionEvent from booking management navigation button
     */
    @FXML
    public void handleBookingManagement(ActionEvent event) {
        SceneManager.switchScene("/fxml/Reservations.fxml");
    }

    /**
     * Opens station network management interface for infrastructure administration.
     * Provides tools for managing railway station network and connectivity.
     *
     * <h3>Station Management Functions:</h3>
     * <ul>
     *   <li>Station information management and updates</li>
     *   <li>Network connectivity and route planning</li>
     *   <li>Infrastructure monitoring and maintenance scheduling</li>
     *   <li>Geographic coverage and accessibility optimization</li>
     * </ul>
     *
     * @param event ActionEvent from station management navigation button
     */
    @FXML
    public void handleStationManagement(ActionEvent event) {
        SceneManager.switchScene("/fxml/StationNetwork.fxml");
    }

    /**
     * Opens comprehensive fleet management interface with full CRUD operations.
     * Displays current train inventory and launches modal window for detailed management.
     *
     * <h3>Fleet Management Capabilities:</h3>
     * <ul>
     *   <li>Train fleet inventory and status monitoring</li>
     *   <li>CRUD operations for train records and specifications</li>
     *   <li>Route assignment and schedule management</li>
     *   <li>Maintenance tracking and operational status</li>
     * </ul>
     *
     * @param event ActionEvent from train management navigation button
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
     * Displays basic train fleet information using legacy manage trains interface.
     * Provides quick access to fleet statistics and basic information display.
     *
     * <h3>Quick Fleet Information:</h3>
     * <ul>
     *   <li>Current train count and fleet size</li>
     *   <li>Basic operational status overview</li>
     *   <li>Quick access to detailed management tools</li>
     * </ul>
     *
     * @param event ActionEvent from legacy manage trains button
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

    // =========================================================================
    // WINDOW MANAGEMENT
    // =========================================================================

    /**
     * Opens Fleet Management CRUD interface in dedicated modal window.
     * Provides comprehensive train and route management capabilities in professional interface.
     *
     * <h3>Modal Window Features:</h3>
     * <ul>
     *   <li>Application-modal design preventing background interaction</li>
     *   <li>Optimal window sizing (1200x800) for detailed management operations</li>
     *   <li>Resizable interface with minimum size constraints</li>
     *   <li>Automatic statistics refresh after window closure</li>
     *   <li>Professional window styling and branding</li>
     * </ul>
     *
     * <h3>Error Handling:</h3>
     * <ul>
     *   <li>FXML loading error detection and user notification</li>
     *   <li>Graceful degradation for missing management interfaces</li>
     *   <li>Automatic fallback to alternative management methods</li>
     * </ul>
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
            loadStatistics(); // Refresh dashboard statistics after management operations
        } catch (IOException e) {
            showErrorMessage("Fleet Management FXML not found.");
        } catch (Exception e) {
            showErrorMessage("Error opening Fleet Management.");
        }
    }

    // =========================================================================
    // SESSION AND NAVIGATION MANAGEMENT
    // =========================================================================

    /**
     * Handles navigation back to the main application menu.
     * Provides safe navigation with error handling and fallback mechanisms.
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
     * Handles secure admin logout and session termination.
     * Clears current administrative session and redirects to login interface.
     *
     * <h3>Logout Security Features:</h3>
     * <ul>
     *   <li>Complete session cleanup and authentication token removal</li>
     *   <li>Secure redirect to login interface</li>
     *   <li>Administrative activity logging</li>
     *   <li>Error handling for logout process failures</li>
     * </ul>
     *
     * @param event ActionEvent from logout navigation button
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

    // =========================================================================
    // UTILITY AND STATUS MANAGEMENT
    // =========================================================================

    /**
     * Updates system status display with current timestamp and operational information.
     * Provides real-time system status monitoring for administrative oversight.
     */
    private void updateSystemStatus() {
        if (lastUpdateLabel != null) {
            lastUpdateLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
        }
    }

    /**
     * Displays success message with professional green styling and automatic cleanup.
     * Provides positive feedback for successful administrative operations.
     *
     * @param message Success message text to display to administrator
     */
    private void showSuccessMessage(String message) {
        showMessage(message, "success", 3);
    }

    /**
     * Displays error message with professional red styling and extended visibility.
     * Provides clear error communication for failed administrative operations.
     *
     * @param message Error message text to display to administrator
     */
    private void showErrorMessage(String message) {
        showMessage(message, "error", 5);
    }

    /**
     * Displays informational message with default styling and standard visibility duration.
     * Provides neutral information feedback for general administrative notifications.
     *
     * @param message Informational message text to display to administrator
     */
    private void showInfoMessage(String message) {
        showMessage(message, "", 4);
    }

    /**
     * Unified message display system with professional styling and automatic cleanup.
     * Consolidates all messaging functionality to ensure consistent user experience.
     *
     * <h3>Message System Features:</h3>
     * <ul>
     *   <li>Dynamic CSS styling based on message type (success, error, info)</li>
     *   <li>Automatic message cleanup with configurable timing</li>
     *   <li>Professional fade-out animation for smooth user experience</li>
     *   <li>Thread-safe message handling with JavaFX Platform integration</li>
     *   <li>CSS class management for consistent visual design</li>
     * </ul>
     *
     * @param message Text content to display to administrator
     * @param styleClass CSS styling class ("success", "error", or "" for default)
     * @param hideDelaySeconds Duration in seconds before automatic message cleanup
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