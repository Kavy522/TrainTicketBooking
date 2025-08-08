package trainapp.controller.ui;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import trainapp.util.SceneManager;

public class LoginController {

    @FXML
    TextField usernameField;
    @FXML
    PasswordField passwordField;
    @FXML
    Label usernameError;
    @FXML
    Label passwordError;
    @FXML
    Label errorMessage;

    @FXML
    public void initialize() {
        setupAutoErrorClear();
    }

    private void setupAutoErrorClear() {
        // Clear username error when user starts typing
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (usernameError.isVisible()) {
                clearUsernameError();
            }
            // Clear general error message when user starts typing
            if (errorMessage.isVisible()) {
                hideGeneralError();
            }
        });

        // Clear password error when user starts typing
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (passwordError.isVisible()) {
                clearPasswordError();
            }
            // Clear general error message when user starts typing
            if (errorMessage.isVisible()) {
                hideGeneralError();
            }
        });
    }

    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Clear previous errors
        clearAllErrors();

        // Validate fields individually
        boolean isUsernameValid = validateUsernameField(username);
        boolean isPasswordValid = validatePasswordField(password);

        if (isUsernameValid && isPasswordValid) {
            System.out.println("Login successful for: " + username);
            // TODO: Implement actual authentication with AuthService
            // TODO: Navigate to main menu using SceneManager
            clearForm();
        } else {
            // Show general error only if both fields are valid but authentication fails
            // This condition was incorrect in your original code
            if (isUsernameValid && isPasswordValid) {
                showGeneralError("Username or Password is incorrect!");
            }
        }
    }

    @FXML
    public void handleSignup(ActionEvent event) {
        // TODO: Navigate to registration page using SceneManager
        SceneManager.switchScene("/fxml/Register.fxml");
        System.out.println("Navigate to signup");
    }

    @FXML
    public void handleForgotPassword(ActionEvent event) {
        // TODO: Open forgot password dialog
        System.out.println("Open forgot password dialog");
    }

    private boolean validateUsernameField(String username) {
        if (username == null || username.trim().isEmpty()) {
            showUsernameError("Username is required");
            return false;
        }

        if (username.length() <= 3) {
            showUsernameError("Username must be more than 3 characters");
            return false;
        }

        if (username.length() >= 20) {
            showUsernameError("Username must be less than 20 characters");
            return false;
        }

        // Check for valid characters (alphanumeric and common symbols)
        if (!username.matches("^[a-zA-Z0-9._-]+$")) {
            showUsernameError("Username can only contain letters, numbers, dots, hyphens, and underscores");
            return false;
        }

        // Show success state
        usernameField.getStyleClass().remove("error");
        usernameField.getStyleClass().add("success");
        return true;
    }

    private boolean validatePasswordField(String password) {
        if (password == null || password.trim().isEmpty()) {
            showPasswordError("Password is required");
            return false;
        }

        if (password.length() < 6) {
            showPasswordError("Password must be at least 6 characters");
            return false;
        }

        if (password.length() > 50) {
            showPasswordError("Password must be less than 50 characters");
            return false;
        }

        // For login, we can be less strict than registration
        // Only check for basic requirements
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasNumber = password.matches(".*[0-9].*");

        if (!hasLetter) {
            showPasswordError("Password must contain at least one letter");
            return false;
        }

        if (!hasNumber) {
            showPasswordError("Password must contain at least one number");
            return false;
        }

        // Show success state
        passwordField.getStyleClass().remove("error");
        passwordField.getStyleClass().add("success");
        return true;
    }

    private void showUsernameError(String message) {
        usernameError.setText(message);
        usernameError.setVisible(true);
        usernameError.setManaged(true);
        usernameField.getStyleClass().remove("success");
        usernameField.getStyleClass().add("error");
    }

    private void showPasswordError(String message) {
        passwordError.setText(message);
        passwordError.setVisible(true);
        passwordError.setManaged(true);
        passwordField.getStyleClass().remove("success");
        passwordField.getStyleClass().add("error");
    }

    private void showGeneralError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisible(true);
        errorMessage.setManaged(true);

        // Auto-hide error after 4 seconds
        PauseTransition delay = new PauseTransition(Duration.millis(4000));
        delay.setOnFinished(e -> hideGeneralError());
        delay.play();
    }

    private void clearUsernameError() {
        usernameError.setText("");
        usernameError.setVisible(false);
        usernameError.setManaged(false);
        usernameField.getStyleClass().removeAll("error", "success");
    }

    private void clearPasswordError() {
        passwordError.setText("");
        passwordError.setVisible(false);
        passwordError.setManaged(false);
        passwordField.getStyleClass().removeAll("error", "success");
    }

    private void clearAllErrors() {
        clearUsernameError();
        clearPasswordError();
        hideGeneralError();
    }

    private void hideGeneralError() {
        errorMessage.setText("");
        errorMessage.setVisible(false);
        errorMessage.setManaged(false);
    }

    private void clearForm() {
        usernameField.clear();
        passwordField.clear();
        clearAllErrors();
    }
}