package trainapp.controller.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
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
import trainapp.controller.ui.TrainBookingController;
import trainapp.service.AuthService;
import trainapp.service.SessionManager;
import trainapp.util.SceneManager;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label usernameError;
    @FXML private Label passwordError;
    @FXML private Label errorMessage;
    @FXML private Button loginButton;
    @FXML private Label messageLabel;

    private final AuthService authService = new AuthService();
    private final SessionManager sessionManager = SessionManager.getInstance();

    // Instance variables for redirect handling
    private String redirectAfterLogin = null;
    private String loginMessage = null;

    @FXML
    public void initialize() {
        setupAutoErrorClear();

        // Set initial message if provided
        if (loginMessage != null) {
            setLoginMessage(loginMessage);
        }
    }

    private void setupAutoErrorClear() {
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (usernameError != null && usernameError.isVisible()) {
                clearUsernameError();
            }
            if (errorMessage != null && errorMessage.isVisible()) {
                hideGeneralError();
            }
        });

        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (passwordError != null && passwordError.isVisible()) {
                clearPasswordError();
            }
            if (errorMessage != null && errorMessage.isVisible()) {
                hideGeneralError();
            }
        });
    }

    /**
     * Set login message (e.g., "You need to login to book")
     */
    public void setLoginMessage(String message) {
        this.loginMessage = message;
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
            messageLabel.getStyleClass().removeAll("success-message", "general-error");
            messageLabel.getStyleClass().add("warning-message");
        } else if (errorMessage != null) {
            // Fallback to errorMessage if messageLabel not available
            errorMessage.setText(message);
            errorMessage.setVisible(true);
            errorMessage.setManaged(true);
            errorMessage.getStyleClass().removeAll("success-message", "general-error");
            errorMessage.getStyleClass().add("warning-message");
        }
    }

    /**
     * Set redirect path after login - SINGLE METHOD ONLY
     */
    public void setRedirectAfterLogin(String redirectPath) {
        this.redirectAfterLogin = redirectPath;
        System.out.println("LoginController: Redirect after login set to: " + redirectPath);
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String nameOrUsername = usernameField.getText().trim();
        String password = passwordField.getText();

        clearAllErrors();

        if (nameOrUsername.isEmpty()) {
            showUsernameError("Name/Username is required");
            return;
        }

        if (password.isEmpty()) {
            showPasswordError("Password is required");
            return;
        }

        setLoginButtonLoading(true);

        Task<AuthService.AuthResult> loginTask = new Task<AuthService.AuthResult>() {
            @Override
            protected AuthService.AuthResult call() throws Exception {
                Thread.sleep(500);
                return authService.login(nameOrUsername, password);
            }
        };

        loginTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                AuthService.AuthResult result = loginTask.getValue();
                setLoginButtonLoading(false);

                if (result.isSuccess()) {
                    showSuccessMessage("Login successful! Welcome back.");

                    PauseTransition delay = new PauseTransition(Duration.millis(1000));
                    delay.setOnFinished(ev -> navigateAfterLogin());
                    delay.play();
                } else {
                    showGeneralError(result.getMessage());
                }
            });
        });

        loginTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                setLoginButtonLoading(false);
                showGeneralError("Login failed due to system error. Please try again.");

                Throwable exception = loginTask.getException();
                if (exception != null) {
                    exception.printStackTrace();
                }
            });
        });

        new Thread(loginTask).start();
    }

    private void setLoginButtonLoading(boolean loading) {
        if (loginButton != null) {
            loginButton.setText(loading ? "Signing In..." : "Sign In");
            loginButton.setDisable(loading);
        }
    }

    /**
     * Navigate after successful login based on redirect or user type
     */
    private void navigateAfterLogin() {
        try {
            if (!sessionManager.isLoggedIn()) {
                showGeneralError("Session error. Please try logging in again.");
                return;
            }

            System.out.println("LoginController: Navigating after login...");
            System.out.println("LoginController: Redirect path: " + redirectAfterLogin);
            System.out.println("LoginController: Has pending booking: " + sessionManager.hasPendingBooking());

            // Handle booking-specific redirect
            if (redirectAfterLogin != null && redirectAfterLogin.equals("/fxml/TrainBooking.fxml")) {
                redirectToBooking();
                return;
            }

            // Handle other specific redirects
            if (redirectAfterLogin != null) {
                String redirect = redirectAfterLogin;
                redirectAfterLogin = null; // Clear after use

                // Validate user permissions for the redirect
                if (redirect.equals("/fxml/UserProfile.fxml") && sessionManager.isAdmin()) {
                    // Admin trying to access user profile, redirect to admin panel instead
                    SceneManager.switchScene("/fxml/AdminProfile.fxml");
                } else if (redirect.equals("/fxml/AdminProfile.fxml") && sessionManager.isUser()) {
                    // Regular user trying to access admin panel, redirect to user profile instead
                    SceneManager.switchScene("/fxml/UserProfile.fxml");
                } else {
                    // Use the redirect as intended
                    SceneManager.switchScene(redirect);
                }
                return;
            }

            // Default navigation based on user role
            navigateBasedOnUserType();

        } catch (Exception e) {
            e.printStackTrace();
            showGeneralError("Navigation error. Please try again.");
        }
    }

    /**
     * Redirect to booking page with pending booking data
     */
    private void redirectToBooking() {
        try {
            System.out.println("LoginController: Redirecting to booking page");

            if (sessionManager.hasPendingBooking()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TrainBooking.fxml"));
                Parent root = loader.load();

                TrainBookingController bookingController = loader.getController();
                bookingController.setPendingBookingData(
                        sessionManager.getPendingTrainId(),
                        sessionManager.getPendingFromStation(),
                        sessionManager.getPendingToStation(),
                        sessionManager.getPendingJourneyDate()
                );

                // Clear pending booking after setting it
                sessionManager.clearPendingBooking();

                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.centerOnScreen();

                System.out.println("LoginController: Successfully redirected to booking page");
            } else {
                // No pending booking, go to main menu
                System.out.println("LoginController: No pending booking, going to main menu");
                SceneManager.switchScene("/fxml/MainMenu.fxml");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showGeneralError("Failed to load booking page: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showGeneralError("An error occurred while loading booking: " + e.getMessage());
        }
    }

    /**
     * Default navigation based on user type
     */
    private void navigateBasedOnUserType() {
        try {
            if (sessionManager.isAdmin()) {
                SceneManager.switchScene("/fxml/AdminProfile.fxml");
            } else if (sessionManager.isUser()) {
                SceneManager.switchScene("/fxml/UserProfile.fxml");
            } else {
                showGeneralError("Unknown user type. Please contact support.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showGeneralError("Failed to navigate to profile page.");
        }
    }

    @FXML
    public void handleSignup(ActionEvent event) {
        try {
            SceneManager.switchScene("/fxml/Register.fxml");
        } catch (Exception e) {
            showGeneralError("Failed to load registration page.");
        }
    }

    @FXML
    public void handleForgotPassword(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/ForgotPassword.fxml"));
            DialogPane forgotPasswordPane = loader.load();

            Dialog<ButtonType> forgotPasswordDialog = new Dialog<>();
            forgotPasswordDialog.setDialogPane(forgotPasswordPane);
            forgotPasswordDialog.initModality(Modality.APPLICATION_MODAL);
            forgotPasswordDialog.setTitle("Reset Password");

            forgotPasswordDialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showGeneralError("Failed to open password reset dialog.");
        }
    }

    @FXML
    public void handleBackToHome(ActionEvent event) {
        try {
            // Clear any pending booking when going back to home
            if (sessionManager.hasPendingBooking()) {
                sessionManager.clearPendingBooking();
                System.out.println("LoginController: Cleared pending booking on back to home");
            }

            SceneManager.switchScene("/fxml/MainMenu.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            showGeneralError("Failed to go back to home page.");
        }
    }

    // ===================== ERROR HANDLING METHODS =====================

    private void showUsernameError(String message) {
        if (usernameError != null) {
            usernameError.setText(message);
            usernameError.setVisible(true);
            usernameError.setManaged(true);
        }
    }

    private void showPasswordError(String message) {
        if (passwordError != null) {
            passwordError.setText(message);
            passwordError.setVisible(true);
            passwordError.setManaged(true);
        }
    }

    private void showGeneralError(String message) {
        if (errorMessage != null) {
            errorMessage.setText(message);
            errorMessage.setVisible(true);
            errorMessage.setManaged(true);
            errorMessage.getStyleClass().removeAll("success-message", "warning-message");
            errorMessage.getStyleClass().add("general-error");
        }
    }

    private void showSuccessMessage(String message) {
        if (errorMessage != null) {
            errorMessage.setText(message);
            errorMessage.setVisible(true);
            errorMessage.setManaged(true);
            errorMessage.getStyleClass().removeAll("general-error", "warning-message");
            errorMessage.getStyleClass().add("success-message");
        }
    }

    private void clearUsernameError() {
        if (usernameError != null) {
            usernameError.setVisible(false);
            usernameError.setManaged(false);
        }
    }

    private void clearPasswordError() {
        if (passwordError != null) {
            passwordError.setVisible(false);
            passwordError.setManaged(false);
        }
    }

    private void clearAllErrors() {
        clearUsernameError();
        clearPasswordError();
        hideGeneralError();
    }

    private void hideGeneralError() {
        if (errorMessage != null) {
            errorMessage.setVisible(false);
            errorMessage.setManaged(false);
        }
    }

    // ===================== UTILITY METHODS =====================

    /**
     * Clear all pending states
     */
    public void clearAllStates() {
        redirectAfterLogin = null;
        loginMessage = null;
        clearAllErrors();

        if (messageLabel != null) {
            messageLabel.setVisible(false);
            messageLabel.setManaged(false);
        }
    }

    /**
     * Check if login is for booking purposes
     */
    public boolean isBookingLogin() {
        return sessionManager.hasPendingBooking() ||
                (redirectAfterLogin != null && redirectAfterLogin.equals("/fxml/TrainBooking.fxml"));
    }
}