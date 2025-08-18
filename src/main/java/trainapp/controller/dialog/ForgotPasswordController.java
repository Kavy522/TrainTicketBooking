package trainapp.controller.dialog;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import trainapp.service.PasswordResetService;

import java.io.IOException;
import java.util.Optional;

/**
 * ForgotPasswordController manages secure password recovery workflow with multi-step verification.
 * Provides comprehensive password reset functionality with OTP verification and validation.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Multi-step password recovery workflow with email verification</li>
 *   <li>OTP-based verification system for enhanced security</li>
 *   <li>Real-time validation with comprehensive input checking</li>
 *   <li>Animated transitions between workflow steps</li>
 *   <li>Asynchronous operations with loading indicators</li>
 *   <li>Comprehensive error handling and user feedback</li>
 * </ul>
 *
 * <p>Security Features:
 * <ul>
 *   <li>Email format validation with regex pattern matching</li>
 *   <li>Password strength validation with minimum requirements</li>
 *   <li>Password confirmation matching with real-time feedback</li>
 *   <li>OTP verification through secure dialog integration</li>
 *   <li>Session-based email verification tracking</li>
 * </ul>
 *
 * <p>User Experience Features:
 * <ul>
 *   <li>Smooth animated transitions between workflow steps</li>
 *   <li>Real-time field validation with immediate feedback</li>
 *   <li>Loading states with contextual progress messages</li>
 *   <li>Clear error messaging with field-specific guidance</li>
 *   <li>Success feedback with automatic dialog closure</li>
 * </ul>
 */
public class ForgotPasswordController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // Email Step Controls
    @FXML private VBox emailStep;
    @FXML private TextField emailField;
    @FXML private Label emailError;
    @FXML private Button sendOtpButton;

    // Password Step Controls
    @FXML private VBox passwordStep;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Button resetPasswordButton;

    // Common Controls
    @FXML private Label messageLabel;
    @FXML private HBox loadingBox;

    // -------------------------------------------------------------------------
    // Services and State Management
    // -------------------------------------------------------------------------

    private final PasswordResetService passwordResetService = new PasswordResetService();
    private String verifiedEmail;

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the forgot password dialog with field validation setup.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        setupFieldValidation();
    }

    /**
     * Sets up comprehensive field validation with real-time feedback.
     */
    private void setupFieldValidation() {
        // Email field validation
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            clearEmailError();
            clearMessage();
        });

        // Password fields validation
        newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            clearPasswordErrors();
            validatePasswords();
        });

        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            clearPasswordErrors();
            validatePasswords();
        });
    }

    // -------------------------------------------------------------------------
    // OTP Request and Verification Flow
    // -------------------------------------------------------------------------

    /**
     * Handles OTP sending with email validation and asynchronous processing.
     *
     * @param event ActionEvent from send OTP button
     */
    @FXML
    public void handleSendOtp(ActionEvent event) {
        String email = emailField.getText().trim();

        if (!validateEmail(email)) {
            return;
        }

        // Show loading
        showLoading(true, "Sending verification code...");
        sendOtpButton.setDisable(true);

        // Send OTP in background
        Task<PasswordResetService.PasswordResetResult> sendOtpTask = new Task<PasswordResetService.PasswordResetResult>() {
            @Override
            protected PasswordResetService.PasswordResetResult call() throws Exception {
                return passwordResetService.sendOtp(email);
            }
        };

        sendOtpTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                showLoading(false, "");
                sendOtpButton.setDisable(false);

                PasswordResetService.PasswordResetResult result = sendOtpTask.getValue();

                if (result.isSuccess()) {
                    showSuccessMessage(result.getMessage());

                    // Open OTP verification dialog
                    openOtpVerificationDialog(email);
                } else {
                    showErrorMessage(result.getMessage());
                }
            });
        });

        sendOtpTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                showLoading(false, "");
                sendOtpButton.setDisable(false);
                showErrorMessage("Failed to send verification code. Please try again.");
            });
        });

        new Thread(sendOtpTask).start();
    }

    /**
     * Opens OTP verification dialog with modal display and result handling.
     *
     * @param email the email address for OTP verification
     */
    private void openOtpVerificationDialog(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/OtpVerify.fxml"));
            DialogPane otpDialogPane = loader.load();

            OtpVerifyController otpController = loader.getController();
            otpController.setEmail(email);

            Dialog<ButtonType> otpDialog = new Dialog<>();
            otpDialog.setDialogPane(otpDialogPane);
            otpDialog.initModality(Modality.APPLICATION_MODAL);
            otpDialog.setTitle("Verify OTP");

            Optional<ButtonType> result = otpDialog.showAndWait();

            // Check if OTP was verified successfully
            if (otpController.isOtpVerified()) {
                verifiedEmail = email;
                showPasswordStep();
                showSuccessMessage("OTP verified! Please enter your new password.");
            }

        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage("Failed to open OTP verification dialog.");
        }
    }

    // -------------------------------------------------------------------------
    // Password Reset Processing
    // -------------------------------------------------------------------------

    /**
     * Handles password reset with validation and asynchronous processing.
     *
     * @param event ActionEvent from reset password button
     */
    @FXML
    public void handleResetPassword(ActionEvent event) {
        if (!validateNewPassword()) {
            return;
        }

        String newPassword = newPasswordField.getText();

        // Show loading
        showLoading(true, "Resetting password...");
        resetPasswordButton.setDisable(true);

        // Reset password in background
        Task<PasswordResetService.PasswordResetResult> resetTask = new Task<PasswordResetService.PasswordResetResult>() {
            @Override
            protected PasswordResetService.PasswordResetResult call() throws Exception {
                return passwordResetService.resetPassword(verifiedEmail, newPassword);
            }
        };

        resetTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                showLoading(false, "");
                resetPasswordButton.setDisable(false);

                PasswordResetService.PasswordResetResult result = resetTask.getValue();

                if (result.isSuccess()) {
                    showSuccessMessage(result.getMessage());

                    // Close dialog after success
                    Platform.runLater(() -> {
                        try {
                            Thread.sleep(2000);
                            closeDialog();
                        } catch (InterruptedException ignored) {
                        }
                    });
                } else {
                    showErrorMessage(result.getMessage());
                }
            });
        });

        resetTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                showLoading(false, "");
                resetPasswordButton.setDisable(false);
                showErrorMessage("Failed to reset password. Please try again.");
            });
        });

        new Thread(resetTask).start();
    }

    // -------------------------------------------------------------------------
    // UI Step Transition Management
    // -------------------------------------------------------------------------

    /**
     * Animates transition from email step to password step with smooth fade effects.
     */
    private void showPasswordStep() {
        // Fade out email step
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), emailStep);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            emailStep.setVisible(false);
            emailStep.setManaged(false);

            // Fade in password step
            passwordStep.setVisible(true);
            passwordStep.setManaged(true);
            passwordStep.setOpacity(0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), passwordStep);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }

    // -------------------------------------------------------------------------
    // Input Validation Methods
    // -------------------------------------------------------------------------

    /**
     * Validates email format with comprehensive pattern matching.
     *
     * @param email the email address to validate
     * @return true if email is valid
     */
    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            showEmailError("Email is required");
            return false;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            showEmailError("Please enter a valid email address");
            return false;
        }

        return true;
    }

    /**
     * Validates new password with strength requirements and confirmation matching.
     *
     * @return true if password validation passes
     */
    private boolean validateNewPassword() {
        String password = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (password.isEmpty()) {
            showPasswordError("New password is required");
            return false;
        }

        if (password.length() < 6) {
            showPasswordError("Password must be at least 6 characters");
            return false;
        }

        if (confirmPassword.isEmpty()) {
            showConfirmPasswordError("Please confirm your password");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            showConfirmPasswordError("Passwords do not match");
            return false;
        }

        return true;
    }

    /**
     * Validates password confirmation matching in real-time.
     */
    private void validatePasswords() {
        String password = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!password.isEmpty() && !confirmPassword.isEmpty() && !password.equals(confirmPassword)) {
            showConfirmPasswordError("Passwords do not match");
        }
    }

    // -------------------------------------------------------------------------
    // UI Helper Methods for Error Display
    // -------------------------------------------------------------------------

    /**
     * Displays email validation error with appropriate styling.
     *
     * @param message the error message to display
     */
    private void showEmailError(String message) {
        emailError.setText(message);
        emailError.setVisible(true);
        emailError.setManaged(true);
    }

    /**
     * Clears email validation error display.
     */
    private void clearEmailError() {
        emailError.setVisible(false);
        emailError.setManaged(false);
    }

    /**
     * Displays password validation error with appropriate styling.
     *
     * @param message the error message to display
     */
    private void showPasswordError(String message) {
        passwordError.setText(message);
        passwordError.setVisible(true);
        passwordError.setManaged(true);
    }

    /**
     * Displays password confirmation error with appropriate styling.
     *
     * @param message the error message to display
     */
    private void showConfirmPasswordError(String message) {
        confirmPasswordError.setText(message);
        confirmPasswordError.setVisible(true);
        confirmPasswordError.setManaged(true);
    }

    /**
     * Clears all password validation error displays.
     */
    private void clearPasswordErrors() {
        passwordError.setVisible(false);
        passwordError.setManaged(false);
        confirmPasswordError.setVisible(false);
        confirmPasswordError.setManaged(false);
    }

    // -------------------------------------------------------------------------
    // UI Helper Methods for Messaging and Loading
    // -------------------------------------------------------------------------

    /**
     * Displays success message with appropriate styling.
     *
     * @param message the success message to display
     */
    private void showSuccessMessage(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("error-message");
        messageLabel.getStyleClass().add("success-message");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    /**
     * Displays error message with appropriate styling.
     *
     * @param message the error message to display
     */
    private void showErrorMessage(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("success-message");
        messageLabel.getStyleClass().add("error-message");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    /**
     * Clears all displayed messages.
     */
    private void clearMessage() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    /**
     * Shows or hides loading indicator with contextual message.
     *
     * @param show true to show loading indicator
     * @param message the loading message to display
     */
    private void showLoading(boolean show, String message) {
        loadingBox.setVisible(show);
        loadingBox.setManaged(show);

        if (show && loadingBox.getChildren().size() > 1) {
            Label loadingLabel = (Label) loadingBox.getChildren().get(1);
            loadingLabel.setText(message);
        }
    }

    /**
     * Closes the forgot password dialog.
     */
    private void closeDialog() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }
}