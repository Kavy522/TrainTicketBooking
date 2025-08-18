package trainapp.controller.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import trainapp.service.AuthService;
import trainapp.service.SessionManager;
import trainapp.util.SceneManager;

/**
 * RegisterController manages user registration with comprehensive validation and security.
 * Provides real-time field validation, secure account creation, and user-friendly feedback.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Real-time field validation with instant feedback</li>
 *   <li>Secure user registration with password confirmation</li>
 *   <li>Asynchronous account creation with loading states</li>
 *   <li>Comprehensive input validation and sanitization</li>
 *   <li>Welcome email integration for new users</li>
 *   <li>Error handling with detailed field-specific messaging</li>
 * </ul>
 *
 * <p>Validation Features:
 * <ul>
 *   <li>Name validation with length constraints (3-15 characters)</li>
 *   <li>Email format validation with regex pattern matching</li>
 *   <li>Phone number validation with 10-digit requirement</li>
 *   <li>Password strength validation with minimum requirements</li>
 *   <li>Password confirmation matching with real-time feedback</li>
 * </ul>
 *
 * <p>User Experience Features:
 * <ul>
 *   <li>Progressive validation with non-blocking error display</li>
 *   <li>Loading states with visual feedback during registration</li>
 *   <li>Success messaging with welcome email confirmation</li>
 *   <li>Form reset functionality for retry scenarios</li>
 *   <li>Seamless navigation to login interface</li>
 * </ul>
 */
public class RegisterController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // Form Input Fields
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

    // Field-Specific Error Labels
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

    // Action Controls
    @FXML
    private Button registerButton;
    @FXML
    private Hyperlink loginLink;

    // -------------------------------------------------------------------------
    // Services and Dependencies
    // -------------------------------------------------------------------------

    private final AuthService authService = new AuthService();

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the registration interface with field validation setup.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        setupFieldValidation();
    }

    /**
     * Sets up real-time field validation listeners for all input fields.
     * Provides immediate feedback as users type without blocking interaction.
     */
    private void setupFieldValidation() {
        setupNameValidation();
        setupEmailValidation();
        setupPhoneValidation();
        setupPasswordValidation();
        setupConfirmPasswordValidation();
    }

    /**
     * Sets up name field validation with real-time feedback.
     */
    private void setupNameValidation() {
        nameField.textProperty().addListener((observable, oldValue, newValue) ->
                validateName(newValue, false));
    }

    /**
     * Sets up email field validation with format checking.
     */
    private void setupEmailValidation() {
        emailField.textProperty().addListener((observable, oldValue, newValue) ->
                validateEmail(newValue, false));
    }

    /**
     * Sets up phone field validation with digit requirements.
     */
    private void setupPhoneValidation() {
        phoneField.textProperty().addListener((observable, oldValue, newValue) ->
                validatePhone(newValue, false));
    }

    /**
     * Sets up password field validation with strength requirements.
     */
    private void setupPasswordValidation() {
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            validatePassword(newValue, false);
            if (!confirmPasswordField.getText().isEmpty()) {
                validateConfirmPassword(confirmPasswordField.getText(), false);
            }
        });
    }

    /**
     * Sets up password confirmation validation with matching verification.
     */
    private void setupConfirmPasswordValidation() {
        confirmPasswordField.textProperty().addListener((observable, oldValue, newValue) ->
                validateConfirmPassword(newValue, false));
    }

    // -------------------------------------------------------------------------
    // Registration Processing
    // -------------------------------------------------------------------------

    /**
     * Handles user registration with comprehensive validation and async processing.
     * Validates all fields, creates account, and provides appropriate feedback.
     *
     * @param event ActionEvent from register button
     */
    @FXML
    public void handleRegister(ActionEvent event) {
        clearAllErrors();

        if (!validateAllFields(true)) {
            return;
        }

        initiateRegistrationProcess();
    }

    /**
     * Initiates the registration process with loading state and async handling.
     */
    private void initiateRegistrationProcess() {
        setRegistrationLoadingState(true);

        Task<AuthService.AuthResult> registrationTask = createRegistrationTask();
        configureRegistrationTaskHandlers(registrationTask);
        new Thread(registrationTask).start();
    }

    /**
     * Creates the registration task for async processing.
     *
     * @return configured Task for user registration
     */
    private Task<AuthService.AuthResult> createRegistrationTask() {
        return new Task<AuthService.AuthResult>() {
            @Override
            protected AuthService.AuthResult call() throws Exception {
                Thread.sleep(1000); // Simulate processing time for UX

                return authService.registerUser(
                        nameField.getText().trim(),
                        emailField.getText().trim(),
                        phoneField.getText().trim(),
                        passwordField.getText(),
                        confirmPasswordField.getText()
                );
            }
        };
    }

    /**
     * Configures success and failure handlers for registration task.
     *
     * @param registrationTask the task to configure handlers for
     */
    private void configureRegistrationTaskHandlers(Task<AuthService.AuthResult> registrationTask) {
        registrationTask.setOnSucceeded(e -> Platform.runLater(() ->
                handleRegistrationSuccess(registrationTask.getValue())));

        registrationTask.setOnFailed(e -> Platform.runLater(() ->
                handleRegistrationFailure()));
    }

    /**
     * Handles successful registration with welcome email and navigation.
     *
     * @param result the registration result
     */
    private void handleRegistrationSuccess(AuthService.AuthResult result) {
        setRegistrationLoadingState(false);

        if (result.isSuccess()) {
            sendWelcomeEmailAsync(emailField.getText().trim(), nameField.getText().trim());
            showSuccessMessage(result.getMessage() + " A welcome email has been sent to your inbox.");
            scheduleNavigationToLogin();
        } else {
            showGeneralError(result.getMessage());
        }
    }

    /**
     * Handles registration failure with appropriate error messaging.
     */
    private void handleRegistrationFailure() {
        setRegistrationLoadingState(false);
        showGeneralError("Registration failed due to a system error. Please try again.");
    }

    /**
     * Sets the registration button loading state with visual feedback.
     *
     * @param loading true to show loading state
     */
    private void setRegistrationLoadingState(boolean loading) {
        registerButton.setText(loading ? "Creating Account..." : "Create Account");
        registerButton.setDisable(loading);
    }

    /**
     * Schedules automatic navigation to login page after successful registration.
     */
    private void scheduleNavigationToLogin() {
        Task<Void> delayTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(1500);
                Platform.runLater(() -> SceneManager.switchScene("/fxml/Login.fxml"));
                return null;
            }
        };
        new Thread(delayTask).start();
    }

    /**
     * Sends welcome email asynchronously to avoid blocking UI.
     *
     * @param email user's email address
     * @param name  user's full name
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

    // -------------------------------------------------------------------------
    // Navigation Handlers
    // -------------------------------------------------------------------------

    /**
     * Handles navigation to login page.
     *
     * @param event ActionEvent from login link
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        try {
            SceneManager.switchScene("/fxml/Login.fxml");
        } catch (Exception e) {
            showGeneralError("Failed to load login page. Please try again.");
        }
    }

    // -------------------------------------------------------------------------
    // Field Validation Methods
    // -------------------------------------------------------------------------

    /**
     * Validates all form fields with optional error display.
     *
     * @param showErrors true to display validation errors
     * @return true if all fields are valid
     */
    private boolean validateAllFields(boolean showErrors) {
        boolean nameValid = validateName(nameField.getText(), showErrors);
        boolean emailValid = validateEmail(emailField.getText(), showErrors);
        boolean phoneValid = validatePhone(phoneField.getText(), showErrors);
        boolean passwordValid = validatePassword(passwordField.getText(), showErrors);
        boolean confirmPasswordValid = validateConfirmPassword(confirmPasswordField.getText(), showErrors);

        return nameValid && emailValid && phoneValid && passwordValid && confirmPasswordValid;
    }

    /**
     * Validates name field with length and format requirements.
     *
     * @param name      the name to validate
     * @param showError true to display error if validation fails
     * @return true if name is valid
     */
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

    /**
     * Validates email field with regex pattern matching.
     *
     * @param email     the email to validate
     * @param showError true to display error if validation fails
     * @return true if email is valid
     */
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

    /**
     * Validates phone field with digit count requirements.
     *
     * @param phone     the phone number to validate
     * @param showError true to display error if validation fails
     * @return true if phone is valid
     */
    private boolean validatePhone(String phone, boolean showError) {
        if (phone == null || phone.trim().isEmpty()) {
            if (showError) showFieldError(phoneError, "Phone number is required");
            return false;
        }

        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() != 10) {
            if (showError) showFieldError(phoneError, "Phone number must be 10 digits");
            return false;
        }

        hideFieldError(phoneError);
        return true;
    }

    /**
     * Validates password field with minimum length requirements.
     *
     * @param password  the password to validate
     * @param showError true to display error if validation fails
     * @return true if password is valid
     */
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

    /**
     * Validates password confirmation with matching verification.
     *
     * @param confirmPassword the confirmation password to validate
     * @param showError       true to display error if validation fails
     * @return true if confirmation password matches
     */
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

    // -------------------------------------------------------------------------
    // UI State Management and Messaging
    // -------------------------------------------------------------------------

    /**
     * Displays field-specific error message with appropriate styling.
     *
     * @param errorLabel the error label to update
     * @param message    the error message to display
     */
    private void showFieldError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    /**
     * Hides field-specific error message.
     *
     * @param errorLabel the error label to hide
     */
    private void hideFieldError(Label errorLabel) {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
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
            errorMessage.getStyleClass().removeAll("success-message");
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
            errorMessage.getStyleClass().removeAll("general-error");
            errorMessage.getStyleClass().add("success-message");
        }
    }

    /**
     * Clears all error messages and resets error state.
     */
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

    // -------------------------------------------------------------------------
    // Utility Methods
    // -------------------------------------------------------------------------

    /**
     * Resets the registration form to initial state.
     * Clears all fields, errors, and resets button state.
     */
    public void resetForm() {
        nameField.clear();
        emailField.clear();
        phoneField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        clearAllErrors();
        setRegistrationLoadingState(false);
    }
}
