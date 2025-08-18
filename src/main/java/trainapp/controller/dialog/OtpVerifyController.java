package trainapp.controller.dialog;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import trainapp.service.PasswordResetService;

/**
 * OtpVerifyController manages secure OTP (One-Time Password) verification for password recovery.
 * Provides comprehensive OTP input handling with timer management and security features.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Interactive 6-digit OTP input with automatic field navigation</li>
 *   <li>Countdown timer with expiration handling and visual feedback</li>
 *   <li>Secure OTP verification with asynchronous processing</li>
 *   <li>OTP resend functionality with rate limiting</li>
 *   <li>Email masking for privacy protection</li>
 *   <li>Comprehensive error handling and user feedback</li>
 * </ul>
 *
 * <p>Security Features:
 * <ul>
 *   <li>Time-limited OTP validity with automatic expiration (5 minutes)</li>
 *   <li>Email address masking to protect user privacy</li>
 *   <li>Single-digit input validation with character restrictions</li>
 *   <li>Secure OTP transmission and verification</li>
 *   <li>Rate limiting on resend functionality</li>
 * </ul>
 *
 * <p>User Experience Features:
 * <ul>
 *   <li>Automatic focus navigation between OTP input fields</li>
 *   <li>Backspace handling for intuitive field clearing</li>
 *   <li>Real-time countdown timer with expiration warnings</li>
 *   <li>Loading states during verification and resend operations</li>
 *   <li>Clear success and error messaging with appropriate styling</li>
 * </ul>
 */
public class OtpVerifyController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // OTP Display and Input
    @FXML private Label otpSubtitle;
    @FXML private TextField otp1, otp2, otp3, otp4, otp5, otp6;
    @FXML private Label otpError;

    // Timer and Controls
    @FXML private Label timerLabel;
    @FXML private Button resendButton;
    @FXML private Button verifyButton;

    // Status and Loading
    @FXML private Label messageLabel;
    @FXML private HBox loadingBox;

    // -------------------------------------------------------------------------
    // Services and State Management
    // -------------------------------------------------------------------------

    private final PasswordResetService passwordResetService = new PasswordResetService();
    private TextField[] otpFields;
    private String email;
    private boolean otpVerified = false;
    private Timeline timer;
    private int timeLeft = 300; // 5 minutes in seconds

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the OTP verification dialog with field setup and timer activation.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        setupOtpFields();
        startTimer();
    }

    /**
     * Sets up OTP input fields with automatic navigation and validation.
     */
    private void setupOtpFields() {
        otpFields = new TextField[]{otp1, otp2, otp3, otp4, otp5, otp6};

        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;
            TextField field = otpFields[i];

            // Limit to single digit
            field.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.length() > 1) {
                    field.setText(newVal.substring(0, 1));
                }

                // Move to next field
                if (newVal.length() == 1 && index < otpFields.length - 1) {
                    otpFields[index + 1].requestFocus();
                }

                // Clear error when typing
                clearOtpError();
            });

            // Handle backspace
            field.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("BACK_SPACE") &&
                        field.getText().isEmpty() && index > 0) {
                    otpFields[index - 1].requestFocus();
                }
            });
        }
    }

    // -------------------------------------------------------------------------
    // Email Configuration and Display
    // -------------------------------------------------------------------------

    /**
     * Sets the email address and updates display with masked email for privacy.
     *
     * @param email the email address where OTP was sent
     */
    public void setEmail(String email) {
        this.email = email;
        otpSubtitle.setText("Enter the 6-digit code sent to " + maskEmail(email));
    }

    /**
     * Masks email address for privacy protection while maintaining readability.
     *
     * @param email the email address to mask
     * @return masked email address with asterisks
     */
    private String maskEmail(String email) {
        if (email.length() <= 4) return email;

        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;

        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    // -------------------------------------------------------------------------
    // OTP Verification Processing
    // -------------------------------------------------------------------------

    /**
     * Handles OTP verification with validation and asynchronous processing.
     *
     * @param event ActionEvent from verify button
     */
    @FXML
    public void handleVerifyOtp(ActionEvent event) {
        String otpCode = getEnteredOtp();

        if (otpCode.length() != 6) {
            showOtpError("Please enter the complete 6-digit code");
            return;
        }

        // Show loading
        showLoading(true);
        verifyButton.setDisable(true);

        // Verify OTP in background
        Task<PasswordResetService.PasswordResetResult> verifyTask = new Task<PasswordResetService.PasswordResetResult>() {
            @Override
            protected PasswordResetService.PasswordResetResult call() throws Exception {
                return passwordResetService.verifyOtp(email, otpCode);
            }
        };

        verifyTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                showLoading(false);
                verifyButton.setDisable(false);

                PasswordResetService.PasswordResetResult result = verifyTask.getValue();

                if (result.isSuccess()) {
                    otpVerified = true;
                    showSuccessMessage("OTP verified successfully!");
                    timer.stop();

                    // Close dialog after short delay
                    Platform.runLater(() -> {
                        try {
                            Thread.sleep(1500);
                            closeDialog();
                        } catch (InterruptedException ignored) {}
                    });
                } else {
                    showOtpError(result.getMessage());
                    clearOtpFields();
                }
            });
        });

        verifyTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                showLoading(false);
                verifyButton.setDisable(false);
                showOtpError("Failed to verify OTP. Please try again.");
            });
        });

        new Thread(verifyTask).start();
    }

    /**
     * Handles OTP resend functionality with asynchronous processing and rate limiting.
     *
     * @param event ActionEvent from resend button
     */
    @FXML
    public void handleResendOtp(ActionEvent event) {
        // Show loading
        showLoading(true);
        resendButton.setDisable(true);

        Task<PasswordResetService.PasswordResetResult> resendTask = new Task<PasswordResetService.PasswordResetResult>() {
            @Override
            protected PasswordResetService.PasswordResetResult call() throws Exception {
                return passwordResetService.sendOtp(email);
            }
        };

        resendTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                showLoading(false);

                PasswordResetService.PasswordResetResult result = resendTask.getValue();

                if (result.isSuccess()) {
                    showSuccessMessage("New verification code sent!");
                    clearOtpFields();
                    resetTimer();
                } else {
                    showErrorMessage(result.getMessage());
                    resendButton.setDisable(false);
                }
            });
        });

        resendTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                showLoading(false);
                resendButton.setDisable(false);
                showErrorMessage("Failed to resend code. Please try again.");
            });
        });

        new Thread(resendTask).start();
    }

    // -------------------------------------------------------------------------
    // Timer Management and Display
    // -------------------------------------------------------------------------

    /**
     * Starts countdown timer for OTP expiration with visual feedback.
     */
    private void startTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            updateTimerDisplay();

            if (timeLeft <= 0) {
                timer.stop();
                timerLabel.setText("Code expired");
                timerLabel.getStyleClass().add("expired");
                resendButton.setDisable(false);
                verifyButton.setDisable(true);
            }
        }));

        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    /**
     * Resets countdown timer for new OTP with fresh expiration period.
     */
    private void resetTimer() {
        if (timer != null) {
            timer.stop();
        }

        timeLeft = 300; // Reset to 5 minutes
        timerLabel.getStyleClass().remove("expired");
        resendButton.setDisable(true);
        verifyButton.setDisable(false);
        startTimer();
    }

    /**
     * Updates timer display with formatted countdown showing minutes and seconds.
     */
    private void updateTimerDisplay() {
        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        timerLabel.setText(String.format("Code expires in %02d:%02d", minutes, seconds));
    }

    // -------------------------------------------------------------------------
    // OTP Input Management
    // -------------------------------------------------------------------------

    /**
     * Retrieves entered OTP code by concatenating all field values.
     *
     * @return complete OTP string from all input fields
     */
    private String getEnteredOtp() {
        StringBuilder otp = new StringBuilder();
        for (TextField field : otpFields) {
            otp.append(field.getText());
        }
        return otp.toString();
    }

    /**
     * Clears all OTP input fields and returns focus to first field.
     */
    private void clearOtpFields() {
        for (TextField field : otpFields) {
            field.clear();
        }
        otpFields[0].requestFocus();
    }

    // -------------------------------------------------------------------------
    // Verification Status Access
    // -------------------------------------------------------------------------

    /**
     * Returns the verification status of the OTP.
     *
     * @return true if OTP was successfully verified
     */
    public boolean isOtpVerified() {
        return otpVerified;
    }

    // -------------------------------------------------------------------------
    // UI Helper Methods for Error Display
    // -------------------------------------------------------------------------

    /**
     * Displays OTP validation error with appropriate styling.
     *
     * @param message the error message to display
     */
    private void showOtpError(String message) {
        otpError.setText(message);
        otpError.setVisible(true);
        otpError.setManaged(true);
    }

    /**
     * Clears OTP validation error display.
     */
    private void clearOtpError() {
        otpError.setVisible(false);
        otpError.setManaged(false);
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
     * Shows or hides loading indicator during processing operations.
     *
     * @param show true to show loading indicator
     */
    private void showLoading(boolean show) {
        loadingBox.setVisible(show);
        loadingBox.setManaged(show);
    }

    /**
     * Closes the OTP verification dialog.
     */
    private void closeDialog() {
        Stage stage = (Stage) otp1.getScene().getWindow();
        stage.close();
    }
}