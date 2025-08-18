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

/**
 * LoginController manages user authentication and post-login navigation routing.
 * Provides secure login functionality with intelligent redirect handling and booking preservation.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Secure user and admin authentication with password verification</li>
 *   <li>Intelligent post-login navigation based on user roles and context</li>
 *   <li>Booking context preservation during authentication flow</li>
 *   <li>Real-time form validation with contextual error messages</li>
 *   <li>Asynchronous authentication with loading states</li>
 *   <li>Password reset and registration navigation</li>
 * </ul>
 *
 * <p>Advanced Navigation Features:
 * <ul>
 *   <li>Context-aware redirects for booking flows</li>
 *   <li>Role-based access control and appropriate redirection</li>
 *   <li>Pending booking data preservation across authentication</li>
 *   <li>Fallback navigation for unexpected scenarios</li>
 *   <li>Session state management and cleanup</li>
 * </ul>
 *
 * <p>User Experience Features:
 * <ul>
 *   <li>Auto-clearing error messages on input change</li>
 *   <li>Loading states with visual feedback</li>
 *   <li>Success messages with automatic progression</li>
 *   <li>Contextual messaging for different login scenarios</li>
 *   <li>Graceful error handling with user-friendly messages</li>
 * </ul>
 */
public class LoginController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label usernameError;
    @FXML private Label passwordError;
    @FXML private Label errorMessage;
    @FXML private Button loginButton;
    @FXML private Label messageLabel;

    // -------------------------------------------------------------------------
    // Services and State Management
    // -------------------------------------------------------------------------

    private final AuthService authService = new AuthService();
    private final SessionManager sessionManager = SessionManager.getInstance();

    // Navigation and context state
    private String redirectAfterLogin = null;
    private String loginMessage = null;

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the login interface with auto-error clearing and initial message setup.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        setupAutoErrorClear();

        if (loginMessage != null) {
            setLoginMessage(loginMessage);
        }
    }

    /**
     * Sets up automatic error clearing when users start typing.
     * Provides immediate visual feedback and reduces form clutter.
     */
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

    // -------------------------------------------------------------------------
    // Context and Redirect Management
    // -------------------------------------------------------------------------

    /**
     * Sets a contextual login message for user guidance.
     * Used for scenarios like "Please login to continue booking".
     *
     * @param message the contextual message to display
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
     * Sets the redirect path for post-login navigation.
     * Enables context-aware routing after successful authentication.
     *
     * @param redirectPath the FXML path to redirect to after login
     */
    public void setRedirectAfterLogin(String redirectPath) {
        this.redirectAfterLogin = redirectPath;
        System.out.println("LoginController: Redirect after login set to: " + redirectPath);
    }

    /**
     * Checks if the current login session is for booking purposes.
     * Used to determine appropriate navigation flow and context.
     *
     * @return true if login is part of a booking flow
     */
    public boolean isBookingLogin() {
        return sessionManager.hasPendingBooking() ||
                (redirectAfterLogin != null && redirectAfterLogin.equals("/fxml/TrainBooking.fxml"));
    }

    /**
     * Clears all context states and error messages.
     * Used for resetting the login form to initial state.
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

    // -------------------------------------------------------------------------
    // Authentication Handling
    // -------------------------------------------------------------------------

    /**
     * Handles the login authentication process asynchronously.
     * Validates input, performs authentication, and manages post-login navigation.
     *
     * @param event ActionEvent from login button click
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        String nameOrUsername = usernameField.getText().trim();
        String password = passwordField.getText();

        clearAllErrors();

        if (!validateLoginInput(nameOrUsername, password)) {
            return;
        }

        performAsyncLogin(nameOrUsername, password);
    }

    /**
     * Validates login input fields and shows appropriate error messages.
     *
     * @param nameOrUsername the username or name input
     * @param password the password input
     * @return true if input is valid
     */
    private boolean validateLoginInput(String nameOrUsername, String password) {
        if (nameOrUsername.isEmpty()) {
            showUsernameError("Name/Username is required");
            return false;
        }

        if (password.isEmpty()) {
            showPasswordError("Password is required");
            return false;
        }

        return true;
    }

    /**
     * Performs asynchronous login authentication with loading states.
     *
     * @param nameOrUsername the username or name for authentication
     * @param password the password for authentication
     */
    private void performAsyncLogin(String nameOrUsername, String password) {
        setLoginButtonLoading(true);

        Task<AuthService.AuthResult> loginTask = new Task<AuthService.AuthResult>() {
            @Override
            protected AuthService.AuthResult call() throws Exception {
                Thread.sleep(500); // Brief delay for UX
                return authService.login(nameOrUsername, password);
            }
        };

        loginTask.setOnSucceeded(e -> Platform.runLater(() -> handleLoginSuccess(loginTask.getValue())));
        loginTask.setOnFailed(e -> Platform.runLater(() -> handleLoginFailure(loginTask.getException())));

        new Thread(loginTask).start();
    }

    /**
     * Handles successful login authentication and initiates navigation.
     *
     * @param result the authentication result
     */
    private void handleLoginSuccess(AuthService.AuthResult result) {
        setLoginButtonLoading(false);

        if (result.isSuccess()) {
            showSuccessMessage("Login successful! Welcome back.");

            PauseTransition delay = new PauseTransition(Duration.millis(1000));
            delay.setOnFinished(ev -> navigateAfterLogin());
            delay.play();
        } else {
            showGeneralError(result.getMessage());
        }
    }

    /**
     * Handles login authentication failures with appropriate error messages.
     *
     * @param exception the exception that occurred during login
     */
    private void handleLoginFailure(Throwable exception) {
        setLoginButtonLoading(false);
        showGeneralError("Login failed due to system error. Please try again.");

        if (exception != null) {
            exception.printStackTrace();
        }
    }

    /**
     * Sets the login button loading state with visual feedback.
     *
     * @param loading true to show loading state, false to reset
     */
    private void setLoginButtonLoading(boolean loading) {
        if (loginButton != null) {
            loginButton.setText(loading ? "Signing In..." : "Sign In");
            loginButton.setDisable(loading);
        }
    }

    // -------------------------------------------------------------------------
    // Post-Login Navigation Management
    // -------------------------------------------------------------------------

    /**
     * Manages navigation after successful login based on context and user role.
     * Handles booking redirects, role-based routing, and fallback navigation.
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
            if (isBookingRedirect()) {
                redirectToBooking();
                return;
            }

            // Handle other specific redirects
            if (redirectAfterLogin != null) {
                handleSpecificRedirect();
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
     * Checks if the current redirect is for booking purposes.
     *
     * @return true if redirect is for booking
     */
    private boolean isBookingRedirect() {
        return redirectAfterLogin != null && redirectAfterLogin.equals("/fxml/TrainBooking.fxml");
    }

    /**
     * Handles specific redirect scenarios with role validation.
     */
    private void handleSpecificRedirect() {
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
    }

    /**
     * Redirects to booking page with pending booking data preservation.
     * Maintains booking context across the authentication flow.
     */
    private void redirectToBooking() {
        try {
            System.out.println("LoginController: Redirecting to booking page");

            if (sessionManager.hasPendingBooking()) {
                loadBookingPageWithContext();
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
     * Loads the booking page with preserved context and data.
     *
     * @throws IOException if FXML loading fails
     */
    private void loadBookingPageWithContext() throws IOException {
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
    }

    /**
     * Performs default navigation based on authenticated user type.
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

    // -------------------------------------------------------------------------
    // Navigation Action Handlers
    // -------------------------------------------------------------------------

    /**
     * Handles navigation to the registration/signup page.
     *
     * @param event ActionEvent from signup button
     */
    @FXML
    public void handleSignup(ActionEvent event) {
        try {
            SceneManager.switchScene("/fxml/Register.fxml");
        } catch (Exception e) {
            showGeneralError("Failed to load registration page.");
        }
    }

    /**
     * Handles opening the forgot password dialog.
     * Opens a modal dialog for password reset functionality.
     *
     * @param event ActionEvent from forgot password link
     */
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

    /**
     * Handles navigation back to the main menu/home page.
     * Clears any pending booking data when navigating away.
     *
     * @param event ActionEvent from back to home button
     */
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

    // -------------------------------------------------------------------------
    // Error Handling and Messaging
    // -------------------------------------------------------------------------

    /**
     * Displays username-specific error message.
     *
     * @param message the error message to display
     */
    private void showUsernameError(String message) {
        if (usernameError != null) {
            usernameError.setText(message);
            usernameError.setVisible(true);
            usernameError.setManaged(true);
        }
    }

    /**
     * Displays password-specific error message.
     *
     * @param message the error message to display
     */
    private void showPasswordError(String message) {
        if (passwordError != null) {
            passwordError.setText(message);
            passwordError.setVisible(true);
            passwordError.setManaged(true);
        }
    }

    /**
     * Displays general error message with appropriate styling.
     *
     * @param message the error message to display
     */
    private void showGeneralError(String message) {
        if (errorMessage != null) {
            errorMessage.setText(message);
            errorMessage.setVisible(true);
            errorMessage.setManaged(true);
            errorMessage.getStyleClass().removeAll("success-message", "warning-message");
            errorMessage.getStyleClass().add("general-error");
        }
    }

    /**
     * Displays success message with appropriate styling.
     *
     * @param message the success message to display
     */
    private void showSuccessMessage(String message) {
        if (errorMessage != null) {
            errorMessage.setText(message);
            errorMessage.setVisible(true);
            errorMessage.setManaged(true);
            errorMessage.getStyleClass().removeAll("general-error", "warning-message");
            errorMessage.getStyleClass().add("success-message");
        }
    }

    /**
     * Clears username error message and hides the error label.
     */
    private void clearUsernameError() {
        if (usernameError != null) {
            usernameError.setVisible(false);
            usernameError.setManaged(false);
        }
    }

    /**
     * Clears password error message and hides the error label.
     */
    private void clearPasswordError() {
        if (passwordError != null) {
            passwordError.setVisible(false);
            passwordError.setManaged(false);
        }
    }

    /**
     * Clears all error messages and hides all error labels.
     */
    private void clearAllErrors() {
        clearUsernameError();
        clearPasswordError();
        hideGeneralError();
    }

    /**
     * Hides the general error message label.
     */
    private void hideGeneralError() {
        if (errorMessage != null) {
            errorMessage.setVisible(false);
            errorMessage.setManaged(false);
        }
    }
}