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

public class UserProfileController {

    // Header Controls
    @FXML private Button backButton;
    @FXML private Button logoutButton;

    // Profile Information
    @FXML private Label welcomeLabel;
    @FXML private Label userNameLabel;
    @FXML private Label nameDisplay;
    @FXML private Label emailDisplay;
    @FXML private Label phoneDisplay;
    @FXML private Label memberSinceDisplay;
    @FXML private Label lastLoginDisplay;

    // Statistics
    @FXML private Label totalBookingsLabel;
    @FXML private Label completedTripsLabel;
    @FXML private Label cancelledBookingsLabel;

    // Action Buttons
    @FXML private Button editProfileButton;
    @FXML private Button bookTicketButton;
    @FXML private Button viewBookingsButton;
    @FXML private Button changePasswordButton;

    // Recent Activity
    @FXML private VBox recentActivityContainer;

    // Message
    @FXML private Label messageLabel;

    // Services
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final AuthService authService = new AuthService();
    private final UserProfileService profileService = new UserProfileService();

    @FXML
    public void initialize() {
        loadUserProfile();
        loadUserStatistics();
        loadRecentActivity();
    }

    /**
     * Load user profile information
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
     * Load user statistics
     */
    private void loadUserStatistics() {
        Task<UserProfileService.UserStatistics> statsTask = new Task<UserProfileService.UserStatistics>() {
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
     * Load recent activity
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

    /**
     * Populate recent activity section
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

    /**
     * Set welcome message based on time of day
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

    /**
     * Handle back to main menu
     */
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

    /**
     * Handle logout
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        authService.logout();

        try {
            SceneManager.switchScene("/fxml/Login.fxml");
        } catch (Exception e) {
            showErrorMessage("Failed to logout properly.");
        }
    }

    /**
     * Handle edit profile
     */
    @FXML
    public void handleEditProfile(ActionEvent event) {
        try {
            SceneManager.switchScene("/fxml/EditProfile.fxml");
        } catch (Exception e) {
            showErrorMessage("Failed to load profile edit page.");
        }
    }

    /**
     * Handle book ticket
     */
    @FXML
    public void handleBookTicket(ActionEvent event) {
        try {
            SceneManager.switchScene("/fxml/TrainSearch.fxml");
        } catch (Exception e) {
            showErrorMessage("Failed to load ticket booking page.");
        }
    }

    /**
     * Handle view bookings
     */
    @FXML
    public void handleViewBookings(ActionEvent event) {
        try {
            SceneManager.switchScene("/fxml/MyBookings.fxml");
        } catch (Exception e) {
            showErrorMessage("Failed to load bookings page.");
        }
    }

    /**
     * Handle change password
     */
    @FXML
    public void handleChangePassword(ActionEvent event) {
        // You can implement this as a dialog or separate scene
        showInfoMessage("Change password functionality will be implemented soon.");
    }

    /**
     * Handle download data
     */
    @FXML
    public void handleDownloadData(ActionEvent event) {
        showInfoMessage("Profile data download will be implemented soon.");
    }

    /**
     * Handle account settings
     */
    @FXML
    public void handleAccountSettings(ActionEvent event) {
        showInfoMessage("Account settings will be implemented soon.");
    }

    /**
     * Handle delete account
     */
    @FXML
    public void handleDeleteAccount(ActionEvent event) {
        // Show confirmation dialog before deletion
        showInfoMessage("Account deletion requires confirmation. This will be implemented soon.");
    }

    /**
     * Show success message
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
     * Show error message
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
     * Show info message
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
