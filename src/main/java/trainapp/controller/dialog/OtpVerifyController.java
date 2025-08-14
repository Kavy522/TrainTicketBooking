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

public class OtpVerifyController {

    @FXML private Label otpSubtitle;
    @FXML private TextField otp1, otp2, otp3, otp4, otp5, otp6;
    @FXML private Label otpError;
    @FXML private Label timerLabel;
    @FXML private Button resendButton;
    @FXML private Button verifyButton;
    @FXML private Label messageLabel;
    @FXML private HBox loadingBox;

    private final PasswordResetService passwordResetService = new PasswordResetService();
    private TextField[] otpFields;
    private String email;
    private boolean otpVerified = false;
    private Timeline timer;
    private int timeLeft = 300; // 5 minutes in seconds

    @FXML
    public void initialize() {
        setupOtpFields();
        startTimer();
    }

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

    public void setEmail(String email) {
        this.email = email;
        otpSubtitle.setText("Enter the 6-digit code sent to " + maskEmail(email));
    }

    private String maskEmail(String email) {
        if (email.length() <= 4) return email;

        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;

        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

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

    private void updateTimerDisplay() {
        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        timerLabel.setText(String.format("Code expires in %02d:%02d", minutes, seconds));
    }

    private String getEnteredOtp() {
        StringBuilder otp = new StringBuilder();
        for (TextField field : otpFields) {
            otp.append(field.getText());
        }
        return otp.toString();
    }

    private void clearOtpFields() {
        for (TextField field : otpFields) {
            field.clear();
        }
        otpFields[0].requestFocus();
    }

    public boolean isOtpVerified() {
        return otpVerified;
    }

    // UI Helper Methods
    private void showOtpError(String message) {
        otpError.setText(message);
        otpError.setVisible(true);
        otpError.setManaged(true);
    }

    private void clearOtpError() {
        otpError.setVisible(false);
        otpError.setManaged(false);
    }

    private void showSuccessMessage(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("error-message");
        messageLabel.getStyleClass().add("success-message");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showErrorMessage(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("success-message");
        messageLabel.getStyleClass().add("error-message");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showLoading(boolean show) {
        loadingBox.setVisible(show);
        loadingBox.setManaged(show);
    }

    private void closeDialog() {
        Stage stage = (Stage) otp1.getScene().getWindow();
        stage.close();
    }
}
