package trainapp.controller.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import trainapp.service.AuthService;
import trainapp.service.SessionManager;
import trainapp.util.SceneManager;

public class RegisterController {

    // Form Fields
    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;

    // Error Labels
    @FXML
    private Label nameError;
    @FXML
    private Label emailError;
    @FXML
    private Label phoneError;
    @FXML
    private Label passwordError;
    @FXML
    private Label confirmPasswordError;
    @FXML
    private Label errorMessage;

    // Buttons and Links
    @FXML
    private Button registerButton;
    @FXML
    private Hyperlink loginLink;

    // Services
    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        setupFieldValidation();
    }

    private void setupFieldValidation() {
        nameField.textProperty().addListener((observable, oldValue, newValue) ->
                validateName(newValue, false));

        emailField.textProperty().addListener((observable, oldValue, newValue) ->
                validateEmail(newValue, false));

        phoneField.textProperty().addListener((observable, oldValue, newValue) ->
                validatePhone(newValue, false));

        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            validatePassword(newValue, false);
            if (!confirmPasswordField.getText().isEmpty()) {
                validateConfirmPassword(confirmPasswordField.getText(), false);
            }
        });

        confirmPasswordField.textProperty().addListener((observable, oldValue, newValue) ->
                validateConfirmPassword(newValue, false));
    }


    @FXML
    public void handleRegister(ActionEvent event) {
        clearAllErrors();

        if (!validateAllFields(true)) {
            return;
        }

        registerButton.setText("Creating Account...");

        Task<AuthService.AuthResult> registrationTask = new Task<AuthService.AuthResult>() {
            @Override
            protected AuthService.AuthResult call() throws Exception {
                Thread.sleep(1000);

                return authService.registerUser(
                        nameField.getText().trim(),
                        emailField.getText().trim(),
                        phoneField.getText().trim(),
                        passwordField.getText(),
                        confirmPasswordField.getText()
                );
            }
        };

        registrationTask.setOnSucceeded(e -> {
            AuthService.AuthResult result = registrationTask.getValue();

            Platform.runLater(() -> {
                resetButtonState();

                if (result.isSuccess()) {
                    // Send welcome email asynchronously
                    sendWelcomeEmailAsync(emailField.getText().trim(), nameField.getText().trim());

                    showSuccessMessage(result.getMessage() + " A welcome email has been sent to your inbox.");

                    Task<Void> delayTask = new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            Thread.sleep(1500);
                            return null;
                        }
                    };

                    new Thread(delayTask).start();

                } else {
                    showGeneralError(result.getMessage());
                }
            });
        });

        registrationTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                resetButtonState();
                showGeneralError("Registration failed due to a system error. Please try again.");
            });
        });

        new Thread(registrationTask).start();
    }

    /**
     * Send welcome email asynchronously to avoid blocking UI
     */
    private void sendWelcomeEmailAsync(String email, String name) {
        Task<Boolean> welcomeEmailTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                trainapp.service.EmailService emailService = new trainapp.service.EmailService();
                return emailService.sendWelcomeEmail(email, name);
            }
        };
        new Thread(welcomeEmailTask).start();
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        SceneManager.switchScene("/fxml/Login.fxml");
        showGeneralError("Failed to load login page. Please try again.");

    }


    private void resetButtonState() {
        registerButton.setText("Create Account");
    }

    // ===================== VALIDATION METHODS =====================

    private boolean validateAllFields(boolean showErrors) {
        boolean nameValid = validateName(nameField.getText(), showErrors);
        boolean emailValid = validateEmail(emailField.getText(), showErrors);
        boolean phoneValid = validatePhone(phoneField.getText(), showErrors);
        boolean passwordValid = validatePassword(passwordField.getText(), showErrors);
        boolean confirmPasswordValid = validateConfirmPassword(confirmPasswordField.getText(), showErrors);

        return nameValid && emailValid && phoneValid && passwordValid && confirmPasswordValid;
    }

    private boolean validateName(String name, boolean showError) {
        if (name == null || name.trim().isEmpty()) {
            if (showError) showFieldError(nameError, "Name is required");
            return false;
        }

        if (name.trim().length() < 3) {
            if (showError) showFieldError(nameError, "Name must be at least 3 characters");
            return false;
        }

        if (name.length() > 15) {
            if (showError) showFieldError(nameError, "Name must be less than 15 characters");
            return false;
        }

        hideFieldError(nameError);
        return true;
    }

    private boolean validateEmail(String email, boolean showError) {
        if (email == null || email.trim().isEmpty()) {
            if (showError) showFieldError(emailError, "Email is required");
            return false;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            if (showError) showFieldError(emailError, "Please enter a valid email address");
            return false;
        }

        hideFieldError(emailError);
        return true;
    }

    private boolean validatePhone(String phone, boolean showError) {
        if (phone == null || phone.trim().isEmpty()) {
            if (showError) showFieldError(phoneError, "Phone number is required");
            return false;
        }

        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() != 10) {
            if (showError) showFieldError(phoneError, "Phone number must be 10-15 digits");
            return false;
        }

        hideFieldError(phoneError);
        return true;
    }

    private boolean validatePassword(String password, boolean showError) {
        if (password == null || password.isEmpty()) {
            if (showError) showFieldError(passwordError, "Password is required");
            return false;
        }

        if (password.length() < 6) {
            if (showError) showFieldError(passwordError, "Password must be at least 6 characters");
            return false;
        }

        hideFieldError(passwordError);
        return true;
    }

    private boolean validateConfirmPassword(String confirmPassword, boolean showError) {
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            if (showError) showFieldError(confirmPasswordError, "Please confirm your password");
            return false;
        }

        if (!confirmPassword.equals(passwordField.getText())) {
            if (showError) showFieldError(confirmPasswordError, "Passwords do not match");
            return false;
        }

        hideFieldError(confirmPasswordError);
        return true;
    }

    // ===================== UI HELPER METHODS =====================

    private void showFieldError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void hideFieldError(Label errorLabel) {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private void showGeneralError(String message) {
        if (errorMessage != null) {
            errorMessage.setText(message);
            errorMessage.setVisible(true);
            errorMessage.setManaged(true);
            errorMessage.getStyleClass().removeAll("success-message");
            errorMessage.getStyleClass().add("general-error");
        }
    }

    private void showSuccessMessage(String message) {
        if (errorMessage != null) {
            errorMessage.setText(message);
            errorMessage.setVisible(true);
            errorMessage.setManaged(true);
            errorMessage.getStyleClass().removeAll("general-error");
            errorMessage.getStyleClass().add("success-message");
        }
    }

    private void clearAllErrors() {
        hideFieldError(nameError);
        hideFieldError(emailError);
        hideFieldError(phoneError);
        hideFieldError(passwordError);
        hideFieldError(confirmPasswordError);

        if (errorMessage != null) {
            errorMessage.setVisible(false);
            errorMessage.setManaged(false);
        }
    }

    public void resetForm() {
        nameField.clear();
        emailField.clear();
        phoneField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        clearAllErrors();
        resetButtonState();
    }
}