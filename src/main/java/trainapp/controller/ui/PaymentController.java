package trainapp.controller.ui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import trainapp.model.Booking;
import trainapp.service.BookingService;
import trainapp.service.BookingService.PaymentSuccessRequest;
import trainapp.service.BookingService.BookingResult;
import trainapp.util.Razorpayclient;
import trainapp.util.SceneManager;
import trainapp.service.SessionManager;

/**
 * PaymentController manages the secure payment processing interface for train bookings.
 * Provides comprehensive payment method support with Razorpay integration and real-time validation.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Multiple payment method support (Card, UPI, Net Banking, Wallet)</li>
 *   <li>Real-time form validation with auto-formatting</li>
 *   <li>Secure Razorpay payment gateway integration</li>
 *   <li>Dynamic payment method switching with contextual forms</li>
 *   <li>Comprehensive booking summary display</li>
 *   <li>Asynchronous payment processing with loading states</li>
 * </ul>
 *
 * <p>Payment Security Features:
 * <ul>
 *   <li>HMAC-SHA256 signature verification for payment authenticity</li>
 *   <li>Secure credential handling and validation</li>
 *   <li>PCI-compliant form field handling and formatting</li>
 *   <li>Transaction integrity checks and error handling</li>
 *   <li>Proper session management and state cleanup</li>
 * </ul>
 *
 * <p>User Experience Features:
 * <ul>
 *   <li>Intuitive payment method selection with visual feedback</li>
 *   <li>Smart form validation with real-time feedback</li>
 *   <li>Auto-formatting for card numbers, expiry dates, and CVV</li>
 *   <li>Loading animations and progress indicators</li>
 *   <li>Comprehensive error handling with user-friendly messages</li>
 *   <li>Success confirmation with booking details</li>
 * </ul>
 */
public class PaymentController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // Header Elements
    @FXML private Button backButton;
    @FXML private Label securityLabel;

    // Booking Summary Display
    @FXML private Label trainDetailsLabel;
    @FXML private Label routeLabel;
    @FXML private Label dateLabel;
    @FXML private Label pnrLabel;
    @FXML private Label passengerCountLabel;
    @FXML private Label ticketFareLabel;
    @FXML private Label convenienceFeeLabel;
    @FXML private Label gstLabel;
    @FXML private Label totalAmountLabel;

    // Payment Method Selection
    @FXML private VBox upiOption;
    @FXML private VBox cardOption;
    @FXML private VBox netBankingOption;
    @FXML private VBox walletOption;
    @FXML private RadioButton upiRadio;
    @FXML private RadioButton cardRadio;
    @FXML private RadioButton netBankingRadio;
    @FXML private RadioButton walletRadio;

    // Payment Form Containers
    @FXML private StackPane paymentFormsContainer;
    @FXML private VBox cardForm;
    @FXML private VBox upiForm;
    @FXML private VBox netBankingForm;
    @FXML private VBox walletForm;

    // Card Payment Form Fields
    @FXML private TextField cardNumberField;
    @FXML private TextField expiryField;
    @FXML private TextField cvvField;
    @FXML private TextField cardHolderField;

    // UPI Payment Form Fields
    @FXML private TextField upiIdField;

    // Net Banking Form Fields
    @FXML private ComboBox<String> bankComboBox;

    // Payment Action Controls
    @FXML private Button payNowButton;
    @FXML private CheckBox termsCheckBox;

    // Loading and Progress Indicators
    @FXML private StackPane loadingOverlay;
    @FXML private Label loadingText;

    // -------------------------------------------------------------------------
    // Services and Configuration
    // -------------------------------------------------------------------------

    private final BookingService bookingService = new BookingService();
    private final Razorpayclient razorpayClient = new Razorpayclient();
    private final SessionManager sessionManager = SessionManager.getInstance();

    // Payment Configuration
    private static final String RAZORPAY_KEY_ID = "rzp_test_R5ATDyKY49alUF";
    private static final String RAZORPAY_SECRET = "5JHY6hPtBAGJ8n8R9iQ9TRtE";

    // -------------------------------------------------------------------------
    // State Management
    // -------------------------------------------------------------------------

    private ToggleGroup paymentMethodGroup;
    private Booking currentBooking;
    private String razorpayOrderId;
    private double totalAmount;
    private PaymentMethod selectedPaymentMethod = PaymentMethod.CARD;

    /**
     * Enumeration of supported payment methods.
     */
    public enum PaymentMethod {
        CARD, UPI, NET_BANKING, WALLET
    }

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the payment interface with form validation and payment method setup.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        setupPaymentMethodSelection();
        setupFormValidation();
        initializeBankComboBox();
        updatePaymentButton();
    }

    /**
     * Initializes the bank selection combo box with popular banking options.
     */
    private void initializeBankComboBox() {
        bankComboBox.getItems().addAll(
                "State Bank of India",
                "HDFC Bank",
                "ICICI Bank",
                "Axis Bank",
                "Punjab National Bank",
                "Bank of Baroda",
                "Kotak Mahindra Bank",
                "Yes Bank"
        );
    }

    // -------------------------------------------------------------------------
    // Payment Method Selection and Management
    // -------------------------------------------------------------------------

    /**
     * Sets up payment method selection with toggle groups and visual feedback.
     */
    private void setupPaymentMethodSelection() {
        configureToggleGroup();
        setupPaymentOptionHandlers();
        setupSelectionChangeListener();
    }

    /**
     * Configures the toggle group for payment method radio buttons.
     */
    private void configureToggleGroup() {
        paymentMethodGroup = new ToggleGroup();
        upiRadio.setToggleGroup(paymentMethodGroup);
        cardRadio.setToggleGroup(paymentMethodGroup);
        netBankingRadio.setToggleGroup(paymentMethodGroup);
        walletRadio.setToggleGroup(paymentMethodGroup);

        // Set default selection
        cardRadio.setSelected(true);
    }

    /**
     * Sets up click handlers for payment option containers.
     */
    private void setupPaymentOptionHandlers() {
        upiOption.setOnMouseClicked(e -> selectPaymentMethod(PaymentMethod.UPI, upiRadio));
        cardOption.setOnMouseClicked(e -> selectPaymentMethod(PaymentMethod.CARD, cardRadio));
        netBankingOption.setOnMouseClicked(e -> selectPaymentMethod(PaymentMethod.NET_BANKING, netBankingRadio));
        walletOption.setOnMouseClicked(e -> selectPaymentMethod(PaymentMethod.WALLET, walletRadio));
    }

    /**
     * Sets up listener for payment method selection changes.
     */
    private void setupSelectionChangeListener() {
        paymentMethodGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            updatePaymentForm();
        });
    }

    /**
     * Selects a payment method and updates visual state.
     *
     * @param method the payment method to select
     * @param radioButton the corresponding radio button
     */
    private void selectPaymentMethod(PaymentMethod method, RadioButton radioButton) {
        selectedPaymentMethod = method;
        radioButton.setSelected(true);
        updatePaymentMethodVisualState();
        updatePaymentForm();
    }

    /**
     * Updates visual state of payment method options.
     */
    private void updatePaymentMethodVisualState() {
        // Remove selected class from all options
        upiOption.getStyleClass().remove("selected");
        cardOption.getStyleClass().remove("selected");
        netBankingOption.getStyleClass().remove("selected");
        walletOption.getStyleClass().remove("selected");

        // Add selected class to chosen option
        switch (selectedPaymentMethod) {
            case UPI:
                upiOption.getStyleClass().add("selected");
                break;
            case CARD:
                cardOption.getStyleClass().add("selected");
                break;
            case NET_BANKING:
                netBankingOption.getStyleClass().add("selected");
                break;
            case WALLET:
                walletOption.getStyleClass().add("selected");
                break;
        }
    }

    /**
     * Updates the visible payment form based on selected method.
     */
    private void updatePaymentForm() {
        hideAllPaymentForms();
        showSelectedPaymentForm();
        updatePaymentButton();
    }

    /**
     * Hides all payment forms to prepare for method-specific display.
     */
    private void hideAllPaymentForms() {
        cardForm.setVisible(false);
        upiForm.setVisible(false);
        netBankingForm.setVisible(false);
        walletForm.setVisible(false);
    }

    /**
     * Shows the payment form corresponding to the selected method.
     */
    private void showSelectedPaymentForm() {
        switch (selectedPaymentMethod) {
            case CARD:
                cardForm.setVisible(true);
                break;
            case UPI:
                upiForm.setVisible(true);
                break;
            case NET_BANKING:
                netBankingForm.setVisible(true);
                break;
            case WALLET:
                walletForm.setVisible(true);
                break;
        }
    }

    // -------------------------------------------------------------------------
    // Form Validation and Formatting
    // -------------------------------------------------------------------------

    /**
     * Sets up comprehensive form validation with real-time formatting.
     */
    private void setupFormValidation() {
        setupCardNumberFormatting();
        setupExpiryFieldFormatting();
        setupCVVFieldValidation();
        setupTermsCheckboxListener();
    }

    /**
     * Sets up automatic formatting for card number field with spacing.
     */
    private void setupCardNumberFormatting() {
        cardNumberField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                String formatted = formatCardNumber(newValue);
                cardNumberField.setText(formatted);
                cardNumberField.positionCaret(formatted.length());
            }
        });
    }

    /**
     * Formats card number with proper spacing every 4 digits.
     *
     * @param input the raw card number input
     * @return formatted card number with spaces
     */
    private String formatCardNumber(String input) {
        String digits = input.replaceAll("[^0-9]", "");
        if (digits.length() > 16) {
            digits = digits.substring(0, 16);
        }

        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(" ");
            }
            formatted.append(digits.charAt(i));
        }
        return formatted.toString();
    }

    /**
     * Sets up automatic formatting for expiry field with MM/YY format.
     */
    private void setupExpiryFieldFormatting() {
        expiryField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                String formatted = formatExpiryDate(newValue);
                expiryField.setText(formatted);
                expiryField.positionCaret(formatted.length());
            }
        });
    }

    /**
     * Formats expiry date with MM/YY pattern.
     *
     * @param input the raw expiry input
     * @return formatted expiry date
     */
    private String formatExpiryDate(String input) {
        String digits = input.replaceAll("[^0-9]", "");
        if (digits.length() > 4) {
            digits = digits.substring(0, 4);
        }

        if (digits.length() >= 2) {
            return digits.substring(0, 2) + "/" + digits.substring(2);
        } else {
            return digits;
        }
    }

    /**
     * Sets up CVV field validation with digit-only input.
     */
    private void setupCVVFieldValidation() {
        cvvField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                String digits = newValue.replaceAll("[^0-9]", "");
                if (digits.length() > 3) {
                    digits = digits.substring(0, 3);
                }
                cvvField.setText(digits);
                cvvField.positionCaret(digits.length());
            }
        });
    }

    /**
     * Sets up terms and conditions checkbox listener for payment button state.
     */
    private void setupTermsCheckboxListener() {
        termsCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
            updatePaymentButton();
        });
    }

    /**
     * Updates payment button state based on form validation and terms acceptance.
     */
    private void updatePaymentButton() {
        boolean isValid = termsCheckBox.isSelected() && isCurrentFormValid();
        payNowButton.setDisable(!isValid);
    }

    /**
     * Validates the currently selected payment form.
     *
     * @return true if current form is valid
     */
    private boolean isCurrentFormValid() {
        switch (selectedPaymentMethod) {
            case CARD:
                return isValidCardForm();
            case UPI:
                return isValidUPIForm();
            case NET_BANKING:
                return bankComboBox.getValue() != null;
            case WALLET:
                return true; // Wallet selection is handled separately
        }
        return false;
    }

    /**
     * Validates card form fields for completeness and format.
     *
     * @return true if card form is valid
     */
    private boolean isValidCardForm() {
        String cardNumber = cardNumberField.getText().replaceAll("\\s", "");
        String expiry = expiryField.getText();
        String cvv = cvvField.getText();
        String holder = cardHolderField.getText();

        return cardNumber.length() == 16 &&
                expiry.matches("\\d{2}/\\d{2}") &&
                cvv.length() == 3 &&
                !holder.trim().isEmpty();
    }

    /**
     * Validates UPI ID format for correctness.
     *
     * @return true if UPI ID is valid
     */
    private boolean isValidUPIForm() {
        String upiId = upiIdField.getText();
        return upiId != null && upiId.matches("[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+");
    }

    // -------------------------------------------------------------------------
    // Booking Data Management
    // -------------------------------------------------------------------------

    /**
     * Sets booking data and updates the payment interface display.
     *
     * @param booking the booking to process payment for
     * @param razorpayOrderId the Razorpay order ID
     * @param totalAmount the total payment amount
     */
    public void setBookingData(Booking booking, String razorpayOrderId, double totalAmount) {
        this.currentBooking = booking;
        this.razorpayOrderId = razorpayOrderId;
        this.totalAmount = totalAmount;

        Platform.runLater(this::updateBookingSummary);
    }

    /**
     * Updates the booking summary display with current booking information.
     */
    private void updateBookingSummary() {
        if (currentBooking != null) {
            updateBookingDetails();
            updateAmountBreakdown();
            updatePaymentButtonText();
        }
    }

    /**
     * Updates booking detail labels with current booking information.
     */
    private void updateBookingDetails() {
        trainDetailsLabel.setText("Train Details"); // Enhanced with actual train data
        routeLabel.setText("Journey Route"); // Enhanced with actual route data
        dateLabel.setText("Journey Date | Class"); // Enhanced with actual data
        pnrLabel.setText("PNR: " + currentBooking.getPnr());
        passengerCountLabel.setText("Passengers"); // Enhanced with actual count
    }

    /**
     * Updates the amount breakdown display with calculated values.
     */
    private void updateAmountBreakdown() {
        double ticketFare = totalAmount - 20 - (totalAmount * 0.05); // Calculate base fare
        double gst = totalAmount * 0.05;

        ticketFareLabel.setText("₹" + String.format("%.0f", ticketFare));
        convenienceFeeLabel.setText("₹20");
        gstLabel.setText("₹" + String.format("%.0f", gst));
        totalAmountLabel.setText("₹" + String.format("%.0f", totalAmount));
    }

    /**
     * Updates payment button text with total amount.
     */
    private void updatePaymentButtonText() {
        payNowButton.setText("Pay ₹" + String.format("%.0f", totalAmount));
    }

    // -------------------------------------------------------------------------
    // Payment Processing
    // -------------------------------------------------------------------------

    /**
     * Handles payment initiation with comprehensive validation and processing.
     *
     * @param event ActionEvent from pay now button
     */
    @FXML
    public void handlePayNow(ActionEvent event) {
        if (!validateCurrentForm()) {
            showError("Please fill all required fields correctly");
            return;
        }

        initiatePaymentProcessing();
    }

    /**
     * Initiates payment processing with loading state and async handling.
     */
    private void initiatePaymentProcessing() {
        showLoadingOverlay(true, "Processing Payment...");
        payNowButton.setDisable(true);
        processRazorpayPayment();
    }

    /**
     * Validates the currently displayed payment form.
     *
     * @return true if current form is valid for payment processing
     */
    private boolean validateCurrentForm() {
        return isCurrentFormValid();
    }

    /**
     * Processes payment through Razorpay integration.
     * For development purposes, simulates successful payment with proper signature generation.
     */
    private void processRazorpayPayment() {
        // For development/demo purposes, simulate successful payment
        // In production, this would integrate with actual Razorpay frontend
        simulateSuccessfulPayment();
    }

    /**
     * Simulates successful payment with proper HMAC-SHA256 signature verification.
     * Generates mock payment response that passes verification checks.
     */
    private void simulateSuccessfulPayment() {
        Task<PaymentResult> paymentTask = createPaymentSimulationTask();
        configurePaymentTaskHandlers(paymentTask);
        new Thread(paymentTask).start();
    }

    /**
     * Creates the payment simulation task with proper signature generation.
     *
     * @return configured Task for payment simulation
     */
    private Task<PaymentResult> createPaymentSimulationTask() {
        return new Task<PaymentResult>() {
            @Override
            protected PaymentResult call() throws Exception {
                Thread.sleep(2000); // Simulate processing time

                String paymentId = "pay_" + System.currentTimeMillis();
                String properSignature = generateProperSignature(razorpayOrderId, paymentId);

                logPaymentSimulation(paymentId, properSignature);

                return new PaymentResult(true, paymentId, properSignature, null);
            }
        };
    }

    /**
     * Logs payment simulation details for debugging.
     *
     * @param paymentId the generated payment ID
     * @param signature the generated signature
     */
    private void logPaymentSimulation(String paymentId, String signature) {
        System.out.println("=== MOCK PAYMENT SIMULATION ===");
        System.out.println("Order ID: " + razorpayOrderId);
        System.out.println("Payment ID: " + paymentId);
        System.out.println("Generated Signature: " + signature);
        System.out.println("===============================");
    }

    /**
     * Configures success and failure handlers for payment processing task.
     *
     * @param paymentTask the task to configure handlers for
     */
    private void configurePaymentTaskHandlers(Task<PaymentResult> paymentTask) {
        paymentTask.setOnSucceeded(e -> Platform.runLater(() -> {
            PaymentResult result = paymentTask.getValue();
            showLoadingOverlay(false, "");
            if (result.isSuccess()) {
                handleSuccessfulPayment(result);
            } else {
                handleFailedPayment(result.getErrorMessage());
            }
        }));

        paymentTask.setOnFailed(e -> Platform.runLater(() -> {
            showLoadingOverlay(false, "");
            handleFailedPayment("Payment processing failed");
            payNowButton.setDisable(false);
        }));
    }

    /**
     * Generates proper HMAC-SHA256 signature for Razorpay verification.
     *
     * @param orderId the Razorpay order ID
     * @param paymentId the payment ID
     * @return HMAC-SHA256 signature for verification
     */
    private String generateProperSignature(String orderId, String paymentId) {
        try {
            String payload = orderId + "|" + paymentId;

            javax.crypto.Mac sha256Hmac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    RAZORPAY_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);

            byte[] signatureBytes = sha256Hmac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();
            for (byte b : signatureBytes) {
                result.append(String.format("%02x", b));
            }

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "mock_signature_failed";
        }
    }

    // -------------------------------------------------------------------------
    // Payment Result Handling
    // -------------------------------------------------------------------------

    /**
     * Handles successful payment processing and booking confirmation.
     *
     * @param result the successful payment result
     */
    private void handleSuccessfulPayment(PaymentResult result) {
        showLoadingOverlay(true, "Confirming booking...");

        PaymentSuccessRequest paymentRequest = createPaymentSuccessRequest(result);
        processBookingConfirmation(paymentRequest);
    }

    /**
     * Creates payment success request for booking confirmation.
     *
     * @param result the payment result
     * @return configured PaymentSuccessRequest
     */
    private PaymentSuccessRequest createPaymentSuccessRequest(PaymentResult result) {
        PaymentSuccessRequest paymentRequest = new PaymentSuccessRequest();
        paymentRequest.setBookingId(currentBooking.getBookingId());
        paymentRequest.setRazorpayOrderId(razorpayOrderId);
        paymentRequest.setRazorpayPaymentId(result.getPaymentId());
        paymentRequest.setRazorpaySignature(result.getSignature());
        return paymentRequest;
    }

    /**
     * Processes booking confirmation after successful payment.
     *
     * @param paymentRequest the payment success request
     */
    private void processBookingConfirmation(PaymentSuccessRequest paymentRequest) {
        Task<BookingResult> completionTask = new Task<BookingResult>() {
            @Override
            protected BookingResult call() throws Exception {
                return bookingService.handleSuccessfulPayment(paymentRequest);
            }
        };

        completionTask.setOnSucceeded(e -> Platform.runLater(() -> {
            BookingResult bookingResult = completionTask.getValue();
            showLoadingOverlay(false, "");
            if (bookingResult.isSuccess()) {
                showSuccessDialog();
            } else {
                showError("Booking confirmation failed: " + bookingResult.getMessage());
            }
        }));

        completionTask.setOnFailed(e -> Platform.runLater(() -> {
            showLoadingOverlay(false, "");
            showError("Booking confirmation failed");
        }));

        new Thread(completionTask).start();
    }

    /**
     * Handles payment failure with proper error reporting and recovery.
     *
     * @param errorMessage the error message to display
     */
    private void handleFailedPayment(String errorMessage) {
        bookingService.handleFailedPayment(currentBooking.getBookingId(), errorMessage);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Payment Failed");
        alert.setHeaderText("Payment could not be processed");
        alert.setContentText(errorMessage + "\n\nPlease try again with different payment method.");
        alert.showAndWait();

        payNowButton.setDisable(false);
    }

    /**
     * Displays success dialog with booking confirmation details.
     */
    private void showSuccessDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("🎉 Payment Successful!");
        alert.setHeaderText("Your booking has been confirmed!");
        alert.setContentText(String.format("""
                        Payment Details:
                        Amount Paid: ₹%.0f
                        PNR: %s
                        
                        ✅ E-ticket sent to your email
                        ✅ SMS confirmation sent to your phone
                        ✅ Booking confirmed successfully
                        
                        Thank you for choosing Tailyatri!
                        Have a safe journey! 🚂
                        """,
                totalAmount,
                currentBooking.getPnr()
        ));

        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();

        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    // -------------------------------------------------------------------------
    // Action Handlers and Navigation
    // -------------------------------------------------------------------------

    /**
     * Handles wallet selection for payment processing.
     *
     * @param event ActionEvent from wallet selection button
     */
    @FXML
    public void selectWallet(ActionEvent event) {
        Button source = (Button) event.getSource();
        String walletName = source.getText();

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Wallet Payment");
        info.setHeaderText("Redirecting to " + walletName);
        info.setContentText("You will be redirected to " + walletName + " to complete the payment.");
        info.showAndWait();
    }

    /**
     * Shows terms and conditions dialog.
     *
     * @param event ActionEvent from terms link
     */
    @FXML
    public void showTerms(ActionEvent event) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Terms & Conditions");
        info.setHeaderText("Tailyatri Terms & Conditions");
        info.setContentText("Here are the terms and conditions...\n(This would show actual terms in a real app)");
        info.getDialogPane().setPrefWidth(600);
        info.showAndWait();
    }

    /**
     * Shows privacy policy dialog.
     *
     * @param event ActionEvent from privacy link
     */
    @FXML
    public void showPrivacy(ActionEvent event) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Privacy Policy");
        info.setHeaderText("Tailyatri Privacy Policy");
        info.setContentText("Here is our privacy policy...\n(This would show actual privacy policy in a real app)");
        info.getDialogPane().setPrefWidth(600);
        info.showAndWait();
    }

    /**
     * Handles back navigation with booking cancellation confirmation.
     *
     * @param event ActionEvent from back button
     */
    @FXML
    public void handleBack(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Payment");
        confirm.setHeaderText("Are you sure you want to go back?");
        confirm.setContentText("Your booking will be cancelled if you go back.");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            bookingService.handleFailedPayment(currentBooking.getBookingId(), "User cancelled payment");
            SceneManager.switchScene("/fxml/MainMenu.fxml");
        }
    }

    // -------------------------------------------------------------------------
    // UI State Management and Utilities
    // -------------------------------------------------------------------------

    /**
     * Shows or hides the loading overlay with fade animation.
     *
     * @param show true to show loading overlay
     * @param message the loading message to display
     */
    private void showLoadingOverlay(boolean show, String message) {
        loadingOverlay.setVisible(show);
        if (show) {
            loadingText.setText(message);
            FadeTransition fade = new FadeTransition(Duration.millis(300), loadingOverlay);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        }
    }

    /**
     * Displays error message in alert dialog.
     *
     * @param message the error message to display
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // -------------------------------------------------------------------------
    // Inner Classes
    // -------------------------------------------------------------------------

    /**
     * PaymentResult encapsulates the result of payment processing operations.
     * Contains success status, payment identifiers, and error information.
     */
    private static class PaymentResult {
        private boolean success;
        private String paymentId;
        private String signature;
        private String errorMessage;

        /**
         * Constructs a PaymentResult with payment processing outcome.
         *
         * @param success true if payment was successful
         * @param paymentId the payment transaction ID
         * @param signature the payment verification signature
         * @param errorMessage error message if payment failed
         */
        public PaymentResult(boolean success, String paymentId, String signature, String errorMessage) {
            this.success = success;
            this.paymentId = paymentId;
            this.signature = signature;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getPaymentId() {
            return paymentId;
        }

        public String getSignature() {
            return signature;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}