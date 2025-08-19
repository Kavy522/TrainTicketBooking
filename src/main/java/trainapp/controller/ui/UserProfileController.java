package trainapp.controller.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import trainapp.model.User;
import trainapp.service.AuthService;
import trainapp.service.SessionManager;
import trainapp.service.UserProfileService;
import trainapp.util.SceneManager;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * UserProfileController manages comprehensive user profile display and account operations.
 * Provides personalized user information, statistics, activity tracking, and profile management.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Complete user profile information display with formatted dates</li>
 *   <li>Real-time user statistics including bookings and trip analytics</li>
 *   <li>Recent activity tracking with chronological activity feed</li>
 *   <li>Time-sensitive personalized greeting system</li>
 *   <li>Comprehensive profile management actions</li>
 *   <li>Seamless navigation to related user functions</li>
 * </ul>
 *
 * <p>Profile Display Features:
 * <ul>
 *   <li>User information display with formatted member since and last login dates</li>
 *   <li>Dynamic greeting based on current time of day</li>
 *   <li>Real-time statistics loading with booking counts and trip history</li>
 *   <li>Activity feed with chronological display of recent user actions</li>
 *   <li>Responsive layout adapting to user data availability</li>
 * </ul>
 *
 * <p>Account Management Features:
 * <ul>
 *   <li>Profile editing navigation with seamless transitions</li>
 *   <li>Password change functionality (placeholder for future implementation)</li>
 *   <li>Account settings access for comprehensive user control</li>
 *   <li>Data download capabilities for user data portability</li>
 *   <li>Account deletion with confirmation workflows</li>
 *   <li>Secure logout with session cleanup</li>
 * </ul>
 */
public class UserProfileController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // Header Controls
    @FXML private Button backButton;
    @FXML private Button logoutButton;

    // Profile Information Display
    @FXML private Label welcomeLabel;
    @FXML private Label userNameLabel;
    @FXML private Label nameDisplay;
    @FXML private Label emailDisplay;
    @FXML private Label phoneDisplay;
    @FXML private Label memberSinceDisplay;
    @FXML private Label lastLoginDisplay;

    // Statistics Display
    @FXML private Label totalBookingsLabel;
    @FXML private Label completedTripsLabel;
    @FXML private Label cancelledBookingsLabel;

    // Action Buttons
    @FXML private Button editProfileButton;
    @FXML private Button bookTicketButton;
    @FXML private Button viewBookingsButton;
    @FXML private Button changePasswordButton;

    // Activity Feed
    @FXML private VBox recentActivityContainer;

    // Status and Messaging
    @FXML private Label messageLabel;

    // -------------------------------------------------------------------------
    // Services and Dependencies
    // -------------------------------------------------------------------------

    // Services
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final AuthService authService = new AuthService();
    private final UserProfileService profileService = new UserProfileService();

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the user profile interface with complete data loading.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        loadUserProfile();
        loadUserStatistics();
        loadRecentActivity();
    }

    // -------------------------------------------------------------------------
    // Profile Data Loading
    // -------------------------------------------------------------------------

    /**
     * Loads comprehensive user profile information with formatted display.
     * Handles user authentication validation and displays complete profile data.
     */
    private void loadUserProfile() {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) {
            handleBackToMenu();
            return;
        }

        // Set user information
        userNameLabel.setText(currentUser.getName());
        nameDisplay.setText(currentUser.getName());
        emailDisplay.setText(currentUser.getEmail());
        phoneDisplay.setText(currentUser.getPhone());

        // Format dates
        if (currentUser.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
            memberSinceDisplay.setText(currentUser.getCreatedAt().format(formatter));
        }

        if (currentUser.getLastLogin() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd 'at' h:mm a");
            lastLoginDisplay.setText(currentUser.getLastLogin().format(formatter));
        } else {
            lastLoginDisplay.setText("Never");
        }

        // Set welcome message based on time
        setWelcomeMessage(currentUser.getName());
    }

    /**
     * Loads user statistics asynchronously with error handling and fallback display.
     */
    private void loadUserStatistics() {
        Task<UserProfileService.UserStatistics> statsTask = new Task<>() {
            @Override
            protected UserProfileService.UserStatistics call() throws Exception {
                User currentUser = sessionManager.getCurrentUser();
                return profileService.getUserStatistics(currentUser.getUserId());
            }
        };

        statsTask.setOnSucceeded(e -> {
            UserProfileService.UserStatistics stats = statsTask.getValue();
            Platform.runLater(() -> {
                totalBookingsLabel.setText(String.valueOf(stats.getTotalBookings()));
                completedTripsLabel.setText(String.valueOf(stats.getCompletedTrips()));
                cancelledBookingsLabel.setText(String.valueOf(stats.getCancelledBookings()));
            });
        });

        statsTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                totalBookingsLabel.setText("0");
                completedTripsLabel.setText("0");
                cancelledBookingsLabel.setText("0");
            });
        });

        new Thread(statsTask).start();
    }

    /**
     * Loads recent user activity with chronological display and error handling.
     */
    private void loadRecentActivity() {
        Task<List<UserProfileService.ActivityItem>> activityTask = new Task<List<UserProfileService.ActivityItem>>() {
            @Override
            protected List<UserProfileService.ActivityItem> call() throws Exception {
                User currentUser = sessionManager.getCurrentUser();
                return profileService.getRecentActivity(currentUser.getUserId(), 5);
            }
        };

        activityTask.setOnSucceeded(e -> {
            List<UserProfileService.ActivityItem> activities = activityTask.getValue();
            Platform.runLater(() -> populateRecentActivity(activities));
        });

        activityTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                Label noActivity = new Label("No recent activity");
                noActivity.getStyleClass().add("activity-text");
                recentActivityContainer.getChildren().add(noActivity);
            });
        });

        new Thread(activityTask).start();
    }

    // -------------------------------------------------------------------------
    // Activity Feed Management
    // -------------------------------------------------------------------------

    /**
     * Populates recent activity section with chronological activity display.
     *
     * @param activities list of recent user activities
     */
    private void populateRecentActivity(List<UserProfileService.ActivityItem> activities) {
        recentActivityContainer.getChildren().clear();

        if (activities.isEmpty()) {
            Label noActivity = new Label("No recent activity");
            noActivity.getStyleClass().add("activity-text");
            recentActivityContainer.getChildren().add(noActivity);
            return;
        }

        for (UserProfileService.ActivityItem activity : activities) {
            VBox activityItem = new VBox(2);
            activityItem.getStyleClass().add("activity-item");

            Label activityText = new Label(activity.getDescription());
            activityText.getStyleClass().add("activity-text");

            Label activityTime = new Label(activity.getFormattedTime());
            activityTime.getStyleClass().add("activity-time");

            activityItem.getChildren().addAll(activityText, activityTime);
            recentActivityContainer.getChildren().add(activityItem);
        }
    }

    // -------------------------------------------------------------------------
    // Personalization Features
    // -------------------------------------------------------------------------

    /**
     * Sets personalized welcome message based on current time of day.
     *
     * @param userName the user's name for personalization
     */
    private void setWelcomeMessage(String userName) {
        int hour = java.time.LocalTime.now().getHour();
        String greeting;

        if (hour < 12) {
            greeting = "Good Morning!";
        } else if (hour < 17) {
            greeting = "Good Afternoon!";
        } else {
            greeting = "Good Evening!";
        }

        welcomeLabel.setText(greeting);
    }

    // -------------------------------------------------------------------------
    // Navigation Action Handlers
    // -------------------------------------------------------------------------

    /**
     * Handles navigation back to main menu.
     */
    @FXML
    public void handleBackToMenu() {
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    /**
     * Handles user logout with session cleanup.
     *
     * @param event ActionEvent from logout button
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        authService.logout();
        SceneManager.switchScene("/fxml/Login.fxml");
    }

    /**
     * Handles navigation to profile editing interface.
     *
     * @param event ActionEvent from edit profile button
     */
    @FXML
    public void handleEditProfile(ActionEvent event) {
        showInfoMessage("HandleEditProfile functionality will be implemented soon.");
    }

    /**
     * Handles navigation to ticket booking interface.
     *
     * @param event ActionEvent from book ticket button
     */
    @FXML
    public void handleBookTicket(ActionEvent event) {
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    /**
     * Handles navigation to user bookings interface.
     *
     * @param event ActionEvent from view bookings button
     */
    @FXML
    public void handleViewBookings(ActionEvent event) {
        SceneManager.switchScene("/fxml/MyBookings.fxml");
    }

    // -------------------------------------------------------------------------
    // Account Management Action Handlers
    // -------------------------------------------------------------------------

    /**
     * Handles password change functionality.
     * Currently shows placeholder message for future implementation.
     *
     * @param event ActionEvent from change password button
     */
    @FXML
    public void handleChangePassword(ActionEvent event) {
        // You can implement this as a dialog or separate scene
        showInfoMessage("Change password functionality will be implemented soon.");
    }

    /**
     * Handles user data download functionality.
     * Currently shows placeholder message for future implementation.
     *
     * @param event ActionEvent from download data button
     */
    @FXML
    public void handleDownloadData(ActionEvent event) {
        showInfoMessage("Profile data download will be implemented soon.");
    }

    /**
     * Handles navigation to account settings.
     * Currently shows placeholder message for future implementation.
     *
     * @param event ActionEvent from account settings button
     */
    @FXML
    public void handleAccountSettings(ActionEvent event) {
        showInfoMessage("Account settings will be implemented soon.");
    }

    /**
     * Handles account deletion with confirmation workflow.
     * Currently shows placeholder message for future implementation.
     *
     * @param event ActionEvent from delete account button
     */
    @FXML
    public void handleDeleteAccount(ActionEvent event) {
        // Show confirmation dialog before deletion
        showInfoMessage("Account deletion requires confirmation. This will be implemented soon.");
    }

    // -------------------------------------------------------------------------
    // UI Messaging and Feedback
    // -------------------------------------------------------------------------

    /**
     * Displays success message with automatic hiding and appropriate styling.
     *
     * @param message the success message to display
     */
    private void showSuccessMessage(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("error-message");
        messageLabel.getStyleClass().add("success-message");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);

        // Auto-hide after 3 seconds
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            messageLabel.setVisible(false);
            messageLabel.setManaged(false);
        }));
        timeline.play();
    }

    /**
     * Displays error message with automatic hiding and appropriate styling.
     *
     * @param message the error message to display
     */
    private void showErrorMessage(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("success-message");
        messageLabel.getStyleClass().add("error-message");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);

        // Auto-hide after 5 seconds
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            messageLabel.setVisible(false);
            messageLabel.setManaged(false);
        }));
        timeline.play();
    }

    /**
     * Displays informational message with automatic hiding.
     *
     * @param message the informational message to display
     */
    private void showInfoMessage(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("success-message", "error-message");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);

        // Auto-hide after 3 seconds
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            messageLabel.setVisible(false);
            messageLabel.setManaged(false);
        }));
        timeline.play();
    }
}