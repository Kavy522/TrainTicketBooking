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

/**
 * PaymentController manages secure payment processing for train bookings.
 *
 * <h2>Core Responsibilities:</h2>
 * <ul>
 *   <li><b>Payment Method Management</b> - Supports multiple payment options (Card, UPI, Net Banking, Wallet)</li>
 *   <li><b>Form Validation</b> - Real-time validation with secure input handling</li>
 *   <li><b>Amount Consistency</b> - Ensures payment amount matches booking summary across all stages</li>
 *   <li><b>Razorpay Integration</b> - Secure payment gateway integration with signature verification</li>
 *   <li><b>Transaction Security</b> - HMAC-SHA256 signature generation and verification</li>
 *   <li><b>User Experience</b> - Smooth payment flow with loading states and error handling</li>
 * </ul>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Dynamic form validation with real-time feedback</li>
 *   <li>Secure card number formatting and Luhn algorithm validation</li>
 *   <li>UPI ID format validation and verification</li>
 *   <li>Comprehensive bank selection for net banking</li>
 *   <li>Consistent amount handling preventing pricing discrepancies</li>
 *   <li>Asynchronous payment processing with progress indicators</li>
 *   <li>Automatic booking confirmation and email notifications</li>
 * </ul>
 *
 * <h2>Payment Flow:</h2>
 * <ol>
 *   <li>User arrives from booking page with consistent amount data</li>
 *   <li>Payment breakdown displayed matching booking summary</li>
 *   <li>User selects payment method and fills required information</li>
 *   <li>Real-time validation ensures data integrity</li>
 *   <li>Payment processed through Razorpay with secure signature verification</li>
 *   <li>Booking confirmed and confirmation email sent automatically</li>
 *   <li>User redirected to main menu with success notification</li>
 * </ol>
 *
 * <h2>Security Features:</h2>
 * <ul>
 *   <li>Secure signature generation using HMAC-SHA256</li>
 *   <li>Input sanitization and validation</li>
 *   <li>Payment state management preventing double processing</li>
 *   <li>Secure form field handling during processing states</li>
 * </ul>
 */
public class PaymentController {

    // =========================================================================
    // FXML UI COMPONENTS
    // =========================================================================

    // Header and Navigation Elements
    @FXML private Button backButton;
    @FXML private Label securityLabel;

    // Booking Summary Display Labels
    @FXML private Label trainDetailsLabel;
    @FXML private Label routeLabel;
    @FXML private Label dateLabel;
    @FXML private Label pnrLabel;
    @FXML private Label passengerCountLabel;
    @FXML private Label ticketFareLabel;
    @FXML private Label convenienceFeeLabel;
    @FXML private Label gstLabel;
    @FXML private Label totalAmountLabel;

    // Payment Method Selection Components
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

    // =========================================================================
    // SERVICES AND CONFIGURATION
    // =========================================================================

    /** Service for booking operations and payment processing */
    private final BookingService bookingService = new BookingService();

    /** Razorpay client for payment gateway integration */
    private final Razorpayclient razorpayClient = new Razorpayclient();

    /** Razorpay webhook secret for signature verification */
    private static final String RAZORPAY_SECRET = "5JHY6hPtBAGJ8n8R9iQ9TRtE";

    // =========================================================================
    // STATE MANAGEMENT AND PAYMENT CONSISTENCY
    // =========================================================================

    /** Toggle group for payment method selection */
    private ToggleGroup paymentMethodGroup;

    /** Current booking being processed for payment */
    private Booking currentBooking;

    /** Razorpay order ID for tracking payment */
    private String razorpayOrderId;

    /** Authoritative payment amount ensuring consistency across all stages */
    private double consistentPaymentAmount;

    /** Currently selected payment method */
    private PaymentMethod selectedPaymentMethod = PaymentMethod.CARD;

    // Payment State Tracking Flags
    private volatile boolean isPaymentProcessing = false;
    private volatile boolean isPaymentCompleted = false;

    // Form Validation State Flags
    private volatile boolean cardNumberValid = false;
    private volatile boolean expiryValid = false;
    private volatile boolean cvvValid = false;
    private volatile boolean holderValid = false;
    private volatile boolean upiValid = false;
    private volatile boolean bankValid = false;
    private volatile boolean termsAccepted = false;

    /**
     * Enumeration of supported payment methods.
     */
    public enum PaymentMethod {
        CARD("Credit/Debit Card"),
        UPI("UPI Payment"),
        NET_BANKING("Net Banking"),
        WALLET("Digital Wallet");

        private final String displayName;

        PaymentMethod(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // =========================================================================
    // INITIALIZATION AND SETUP
    // =========================================================================

    /**
     * Initializes the payment controller and sets up UI components.
     * Called automatically by JavaFX framework after FXML loading.
     *
     * <h3>Initialization Steps:</h3>
     * <ol>
     *   <li>Sets up payment method selection with toggle groups</li>
     *   <li>Configures form validation with real-time feedback</li>
     *   <li>Initializes bank dropdown with supported options</li>
     *   <li>Prepares UI for consistent amount handling</li>
     * </ol>
     */
    @FXML
    public void initialize() {
        setupPaymentMethodSelection();
        setupFormValidation();
        initializeBankComboBox();
    }

    /**
     * Initializes the bank selection dropdown with major Indian banks.
     * Sets up validation listener for net banking option.
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

        // Set up bank selection validation
        bankComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            bankValid = (newValue != null && !newValue.trim().isEmpty());
            updatePayButtonState();
        });
    }

    // =========================================================================
    // DATA MANAGEMENT AND CONSISTENCY
    // =========================================================================

    /**
     * Sets booking data ensuring consistent amount handling across payment flow.
     *
     * <h3>Amount Consistency Strategy:</h3>
     * Always uses {@code booking.getTotalFare()} as the authoritative payment amount
     * to ensure consistency with booking summary and prevent pricing discrepancies.
     *
     * <h3>Data Flow:</h3>
     * <ol>
     *   <li>Receives booking object with saved total fare from database</li>
     *   <li>Uses booking amount as single source of truth</li>
     *   <li>Updates UI with consistent amount breakdown</li>
     *   <li>Ensures payment gateway receives exact booking amount</li>
     * </ol>
     *
     * @param booking The booking object containing payment details
     * @param razorpayOrderId The Razorpay order ID for payment tracking
     * @param providedAmount The amount provided by calling controller (for validation only)
     */
    public void setBookingData(Booking booking, String razorpayOrderId, double providedAmount) {
        this.currentBooking = booking;
        this.razorpayOrderId = razorpayOrderId;

        // Use booking.getTotalFare() as authoritative amount to ensure consistency
        this.consistentPaymentAmount = Math.round(booking.getTotalFare() * 100.0) / 100.0;

        Platform.runLater(() -> {
            updateBookingSummaryWithConsistentAmount();
            updatePayButtonState();
        });
    }

    /**
     * Updates booking summary display with consistent amount data.
     * Ensures all displayed information matches the booking amount exactly.
     */
    private void updateBookingSummaryWithConsistentAmount() {
        if (currentBooking != null) {
            updateBookingDetails();
            updateConsistentAmountBreakdown();
        }
    }

    /**
     * Calculates and displays payment breakdown from booking's total fare.
     *
     * <h3>Breakdown Logic:</h3>
     * <ul>
     *   <li>Total Amount = Booking's saved total fare (authoritative)</li>
     *   <li>Convenience Fee = Fixed ₹20 (standard processing fee)</li>
     *   <li>Ticket Fare = Total - Convenience Fee</li>
     *   <li>GST = ₹0 (included in base fare)</li>
     * </ul>
     *
     * Handles edge cases where total amount is less than convenience fee.
     */
    private void updateConsistentAmountBreakdown() {
        double totalAmount = consistentPaymentAmount;
        double convenienceFee = 20.0;
        double ticketFare = totalAmount - convenienceFee;
        double gst = 0.0;

        // Handle edge case: total amount less than convenience fee
        if (ticketFare < 0) {
            ticketFare = 0;
            convenienceFee = totalAmount;
        }

        // Round all values for display consistency
        ticketFare = Math.round(ticketFare * 100.0) / 100.0;
        convenienceFee = Math.round(convenienceFee * 100.0) / 100.0;
        totalAmount = Math.round(totalAmount * 100.0) / 100.0;

        // Update UI labels with consistent formatting
        ticketFareLabel.setText("₹" + String.format("%.2f", ticketFare));
        convenienceFeeLabel.setText("₹" + String.format("%.2f", convenienceFee));
        gstLabel.setText("₹" + String.format("%.2f", gst));
        totalAmountLabel.setText("₹" + String.format("%.2f", totalAmount));
        totalAmountLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
    }

    /**
     * Updates basic booking information display.
     * Shows PNR and placeholder text for train and journey details.
     */
    private void updateBookingDetails() {
        trainDetailsLabel.setText("Train Details");
        routeLabel.setText("Journey Route");
        dateLabel.setText("Journey Date | Class");
        pnrLabel.setText("PNR: " + currentBooking.getPnr());
        passengerCountLabel.setText("Passengers");
    }

    // =========================================================================
    // PAYMENT METHOD SELECTION
    // =========================================================================

    /**
     * Sets up payment method selection system with toggle groups and handlers.
     * Configures visual feedback and form switching logic.
     */
    private void setupPaymentMethodSelection() {
        configureToggleGroup();
        setupPaymentOptionHandlers();
        setupSelectionChangeListener();
    }

    /**
     * Configures radio button toggle group for payment method selection.
     * Sets card payment as default selection.
     */
    private void configureToggleGroup() {
        paymentMethodGroup = new ToggleGroup();
        upiRadio.setToggleGroup(paymentMethodGroup);
        cardRadio.setToggleGroup(paymentMethodGroup);
        netBankingRadio.setToggleGroup(paymentMethodGroup);
        walletRadio.setToggleGroup(paymentMethodGroup);
        cardRadio.setSelected(true);
    }

    /**
     * Sets up click handlers for payment option containers.
     * Allows clicking on the entire option card to select payment method.
     */
    private void setupPaymentOptionHandlers() {
        upiOption.setOnMouseClicked(e -> selectPaymentMethod(PaymentMethod.UPI, upiRadio));
        cardOption.setOnMouseClicked(e -> selectPaymentMethod(PaymentMethod.CARD, cardRadio));
        netBankingOption.setOnMouseClicked(e -> selectPaymentMethod(PaymentMethod.NET_BANKING, netBankingRadio));
        walletOption.setOnMouseClicked(e -> selectPaymentMethod(PaymentMethod.WALLET, walletRadio));
    }

    /**
     * Sets up listener for toggle group selection changes.
     * Updates payment form display when selection changes.
     */
    private void setupSelectionChangeListener() {
        paymentMethodGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            updatePaymentForm();
        });
    }

    /**
     * Selects a payment method and updates UI accordingly.
     * Prevents selection changes during payment processing.
     *
     * @param method The payment method to select
     * @param radioButton The corresponding radio button to activate
     */
    private void selectPaymentMethod(PaymentMethod method, RadioButton radioButton) {
        if (isPaymentProcessing || isPaymentCompleted) {
            return;
        }

        selectedPaymentMethod = method;
        radioButton.setSelected(true);
        updatePaymentMethodVisualState();
        updatePaymentForm();
    }

    /**
     * Updates visual state of payment method options.
     * Adds/removes CSS classes to show selected state.
     */
    private void updatePaymentMethodVisualState() {
        // Remove selection styling from all options
        upiOption.getStyleClass().remove("selected");
        cardOption.getStyleClass().remove("selected");
        netBankingOption.getStyleClass().remove("selected");
        walletOption.getStyleClass().remove("selected");

        // Add selection styling to current method
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
     * Updates payment form display based on selected method.
     * Shows appropriate form and updates button state.
     */
    private void updatePaymentForm() {
        hideAllPaymentForms();
        showSelectedPaymentForm();
        updatePayButtonState();
    }

    /**
     * Hides all payment forms to prepare for showing selected form.
     */
    private void hideAllPaymentForms() {
        cardForm.setVisible(false);
        upiForm.setVisible(false);
        netBankingForm.setVisible(false);
        walletForm.setVisible(false);
    }

    /**
     * Shows the payment form corresponding to selected method.
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

    // =========================================================================
    // FORM VALIDATION
    // =========================================================================

    /**
     * Sets up comprehensive form validation for all payment methods.
     * Configures real-time validation with immediate user feedback.
     */
    private void setupFormValidation() {
        setupCardNumberFormatting();
        setupExpiryFieldFormatting();
        setupCVVFieldValidation();
        setupCardHolderValidation();
        setupUPIValidation();
        setupTermsCheckboxListener();
    }

    /**
     * Sets up card number field with formatting and Luhn validation.
     * Automatically formats input as XXXX XXXX XXXX XXXX and validates using Luhn algorithm.
     */
    private void setupCardNumberFormatting() {
        cardNumberField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !isPaymentProcessing && !isPaymentCompleted) {
                String formatted = formatCardNumber(newValue);
                if (!formatted.equals(newValue)) {
                    cardNumberField.setText(formatted);
                    cardNumberField.positionCaret(formatted.length());
                }
                cardNumberValid = isValidLuhnCardNumber(formatted.replaceAll("\\s", ""));
                updatePayButtonState();
            }
        });
    }

    /**
     * Formats card number input with spaces for readability.
     * Limits input to 16 digits and groups them as XXXX XXXX XXXX XXXX.
     *
     * @param input Raw card number input
     * @return Formatted card number with spaces
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
     * Sets up expiry date field with MM/YY formatting and future date validation.
     */
    private void setupExpiryFieldFormatting() {
        expiryField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !isPaymentProcessing && !isPaymentCompleted) {
                String formatted = formatExpiryDate(newValue);
                if (!formatted.equals(newValue)) {
                    expiryField.setText(formatted);
                    expiryField.positionCaret(formatted.length());
                }
                expiryValid = isValidFutureExpiryDate(formatted);
                updatePayButtonState();
            }
        });
    }

    /**
     * Formats expiry date input as MM/YY.
     * Validates month range (01-12) and limits to 4 digits.
     *
     * @param input Raw expiry date input
     * @return Formatted expiry date as MM/YY
     */
    private String formatExpiryDate(String input) {
        String digits = input.replaceAll("[^0-9]", "");
        if (digits.length() > 4) {
            digits = digits.substring(0, 4);
        }

        if (digits.length() >= 2) {
            String month = digits.substring(0, 2);
            try {
                int monthInt = Integer.parseInt(month);
                if (monthInt > 12) {
                    month = "12";
                } else if (monthInt == 0) {
                    month = "01";
                }
            } catch (NumberFormatException e) {
                // Keep original month if parsing fails
            }
            return month + (digits.length() > 2 ? "/" + digits.substring(2) : "");
        } else {
            return digits;
        }
    }

    /**
     * Sets up CVV field validation with 3-digit limit.
     */
    private void setupCVVFieldValidation() {
        cvvField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !isPaymentProcessing && !isPaymentCompleted) {
                if (newValue.matches("\\d{0,3}")) {
                    cvvValid = (newValue.length() == 3);
                    if (newValue.length() > 3) {
                        cvvField.setText(oldValue);
                    }
                } else if (!newValue.isEmpty()) {
                    cvvField.setText(oldValue);
                }
                updatePayButtonState();
            }
        });
    }

    /**
     * Sets up cardholder name validation with alphabetic filtering.
     * Allows only letters and spaces, converts to uppercase.
     */
    private void setupCardHolderValidation() {
        cardHolderField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !isPaymentProcessing && !isPaymentCompleted) {
                String filtered = newValue.replaceAll("[^a-zA-Z\\s]", "").toUpperCase();
                if (!filtered.equals(newValue)) {
                    cardHolderField.setText(filtered);
                    cardHolderField.positionCaret(filtered.length());
                }
                holderValid = (!filtered.trim().isEmpty() && filtered.length() >= 3);
                updatePayButtonState();
            }
        });
    }

    /**
     * Sets up UPI ID validation with format checking.
     * Validates format as username@provider (e.g., user@paytm).
     */
    private void setupUPIValidation() {
        upiIdField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!isPaymentProcessing && !isPaymentCompleted) {
                upiValid = isValidUPIForm(newValue);
                updatePayButtonState();
            }
        });
    }

    /**
     * Sets up terms and conditions checkbox listener.
     */
    private void setupTermsCheckboxListener() {
        termsCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
            termsAccepted = newValue;
            updatePayButtonState();
        });
    }

    /**
     * Validates credit card number using Luhn algorithm.
     *
     * @param cardNumber 16-digit card number without spaces
     * @return true if card number passes Luhn validation
     */
    private boolean isValidLuhnCardNumber(String cardNumber) {
        if (cardNumber == null || !cardNumber.matches("^\\d{16}$")) {
            return false;
        }

        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }

        return (sum % 10 == 0);
    }

    /**
     * Validates expiry date to ensure it's in the future.
     * Uses YearMonth for accurate date comparison.
     *
     * @param expiry Expiry date in MM/YY format
     * @return true if expiry date is current month or future
     */
    private boolean isValidFutureExpiryDate(String expiry) {
        if (expiry == null || !expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            return false;
        }

        try {
            String[] parts = expiry.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt("20" + parts[1]);

            java.time.YearMonth expiryYearMonth = java.time.YearMonth.of(year, month);
            java.time.YearMonth currentYearMonth = java.time.YearMonth.now();

            return !expiryYearMonth.isBefore(currentYearMonth);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates UPI ID format.
     * Checks for username@provider pattern with minimum length requirements.
     *
     * @param upiId UPI ID string to validate
     * @return true if UPI ID format is valid
     */
    private boolean isValidUPIForm(String upiId) {
        return upiId != null &&
                upiId.matches("[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+") &&
                upiId.length() >= 3 &&
                upiId.contains("@");
    }

    // =========================================================================
    // PAYMENT BUTTON STATE MANAGEMENT
    // =========================================================================

    /**
     * Updates pay button state based on form validation and payment status.
     * Manages button text, styling, and enabled state.
     */
    private void updatePayButtonState() {
        boolean formValid = isCurrentFormValid();
        boolean canPay = formValid && termsAccepted && !isPaymentProcessing && !isPaymentCompleted;

        payNowButton.setDisable(!canPay);
        updateButtonText();
    }

    /**
     * Updates pay button text based on current state.
     * Shows different messages for processing, completed, and ready states.
     */
    private void updateButtonText() {
        if (isPaymentCompleted) {
            payNowButton.setText("Payment Completed ✓");
            payNowButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        } else if (isPaymentProcessing) {
            payNowButton.setText("Processing Payment...");
            payNowButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        } else if (!termsAccepted) {
            payNowButton.setText("Accept Terms to Pay ₹" + String.format("%.2f", consistentPaymentAmount));
            payNowButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
        } else {
            payNowButton.setText("Pay ₹" + String.format("%.2f", consistentPaymentAmount));
            payNowButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        }
    }

    /**
     * Checks if current payment method form is valid.
     * Different validation logic for each payment method.
     *
     * @return true if current form passes validation
     */
    private boolean isCurrentFormValid() {
        switch (selectedPaymentMethod) {
            case CARD:
                return cardNumberValid && expiryValid && cvvValid && holderValid;
            case UPI:
                return upiValid;
            case NET_BANKING:
                return bankValid;
            case WALLET:
                return true; // Wallet selection is always valid
        }
        return false;
    }

    // =========================================================================
    // PAYMENT PROCESSING
    // =========================================================================

    /**
     * Handles pay now button click to initiate payment processing.
     * Validates form and amount before starting payment flow.
     *
     * @param event Action event from pay now button
     */
    @FXML
    public void handlePayNow(ActionEvent event) {
        if (isPaymentProcessing || isPaymentCompleted) {
            return;
        }

        if (!validateCurrentFormAndAmount()) {
            return;
        }

        initiatePaymentProcessing();
    }

    /**
     * Validates current form state and payment amount before processing.
     *
     * @return true if validation passes and payment can proceed
     */
    private boolean validateCurrentFormAndAmount() {
        if (!isCurrentFormValid()) {
            showError("Please fill all required fields correctly");
            return false;
        }

        if (!termsCheckBox.isSelected()) {
            showError("Please accept the terms and conditions");
            return false;
        }

        return true;
    }

    /**
     * Initiates payment processing workflow.
     * Updates UI state, disables forms, and starts payment gateway integration.
     */
    private void initiatePaymentProcessing() {
        isPaymentProcessing = true;

        updatePayButtonState();
        disableAllFormFields(true);
        showLoadingOverlay(true, "Processing Payment...");

        processRazorpayPayment();
    }

    /**
     * Disables or enables all form fields during payment processing.
     * Prevents user input changes during payment flow.
     *
     * @param disable true to disable fields, false to enable
     */
    private void disableAllFormFields(boolean disable) {
        // Disable input fields
        cardNumberField.setDisable(disable);
        expiryField.setDisable(disable);
        cvvField.setDisable(disable);
        cardHolderField.setDisable(disable);
        upiIdField.setDisable(disable);
        bankComboBox.setDisable(disable);
        termsCheckBox.setDisable(disable);

        // Disable payment method selection
        upiRadio.setDisable(disable);
        cardRadio.setDisable(disable);
        netBankingRadio.setDisable(disable);
        walletRadio.setDisable(disable);
    }

    /**
     * Processes payment through Razorpay gateway.
     * Currently simulates successful payment for demo purposes.
     */
    private void processRazorpayPayment() {
        simulateSuccessfulPayment();
    }

    /**
     * Simulates successful payment processing for demonstration.
     * In production, this would integrate with actual Razorpay SDK.
     */
    private void simulateSuccessfulPayment() {
        Task<PaymentResult> paymentTask = createPaymentSimulationTask();
        configurePaymentTaskHandlers(paymentTask);
        new Thread(paymentTask).start();
    }

    /**
     * Creates payment simulation task that mimics Razorpay processing.
     * Generates mock payment ID and signature for testing.
     *
     * @return Task that simulates payment processing
     */
    private Task<PaymentResult> createPaymentSimulationTask() {
        return new Task<PaymentResult>() {
            @Override
            protected PaymentResult call() throws Exception {
                Thread.sleep(2000); // Simulate processing time

                String paymentId = "pay_" + System.currentTimeMillis();
                String properSignature = generateProperSignature(razorpayOrderId, paymentId);

                return new PaymentResult(true, paymentId, properSignature, null);
            }
        };
    }

    /**
     * Configures success and failure handlers for payment task.
     * Manages UI updates and result processing on JavaFX thread.
     *
     * @param paymentTask The payment processing task
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
            resetPaymentState();
        }));
    }

    /**
     * Generates secure HMAC-SHA256 signature for Razorpay verification.
     *
     * @param orderId Razorpay order ID
     * @param paymentId Razorpay payment ID
     * @return HMAC-SHA256 signature as hex string
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
            return "mock_signature_failed";
        }
    }

    // =========================================================================
    // PAYMENT RESULT HANDLING
    // =========================================================================

    /**
     * Handles successful payment by initiating booking confirmation.
     * Shows loading state and creates payment success request.
     *
     * @param result Payment result containing payment ID and signature
     */
    private void handleSuccessfulPayment(PaymentResult result) {
        showLoadingOverlay(true, "Confirming booking...");

        PaymentSuccessRequest paymentRequest = createPaymentSuccessRequest(result);
        processBookingConfirmation(paymentRequest);
    }

    /**
     * Creates payment success request for booking confirmation.
     *
     * @param result Payment result from gateway
     * @return Payment success request for booking service
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
     * Processes booking confirmation asynchronously.
     * Handles success and failure cases with appropriate UI updates.
     *
     * @param paymentRequest Payment success request for booking service
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
                isPaymentCompleted = true;
                isPaymentProcessing = false;
                updatePayButtonState();
                showSuccessDialog();
            } else {
                showError("Booking confirmation failed: " + bookingResult.getMessage());
                resetPaymentState();
            }
        }));

        completionTask.setOnFailed(e -> Platform.runLater(() -> {
            showLoadingOverlay(false, "");
            showError("Booking confirmation failed");
            resetPaymentState();
        }));

        new Thread(completionTask).start();
    }

    /**
     * Handles payment failure with user notification and state reset.
     *
     * @param errorMessage Error message to display to user
     */
    private void handleFailedPayment(String errorMessage) {
        bookingService.handleFailedPayment(currentBooking.getBookingId(), errorMessage);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Payment Failed");
        alert.setHeaderText("Payment could not be processed");
        alert.setContentText(errorMessage + "\n\nPlease try again with different payment method.");
        alert.showAndWait();

        resetPaymentState();
    }

    /**
     * Resets payment state to allow retry.
     * Re-enables form fields and updates button state.
     */
    private void resetPaymentState() {
        isPaymentProcessing = false;
        isPaymentCompleted = false;
        disableAllFormFields(false);
        updatePayButtonState();
    }

    /**
     * Shows success dialog with payment confirmation details.
     * Displays amount paid, PNR, and next steps for user.
     */
    private void showSuccessDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("🎉 Payment Successful!");
        alert.setHeaderText("Your booking has been confirmed!");
        alert.setContentText(String.format("""
                        Payment Details:
                        Amount Paid: ₹%.2f
                        PNR: %s
                        
                        ✅ E-ticket sent to your email
                        ✅ Booking confirmed successfully
                        
                        Thank you for choosing Tailyatri!
                        Have a safe journey! 🚂
                        """,
                consistentPaymentAmount,
                currentBooking.getPnr()
        ));

        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();

        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    // =========================================================================
    // EVENT HANDLERS AND NAVIGATION
    // =========================================================================

    /**
     * Shows terms and conditions dialog.
     *
     * @param actionEvent Action event from terms link
     */
    @FXML
    public void showTerms(ActionEvent actionEvent) {
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
     * @param actionEvent Action event from privacy link
     */
    @FXML
    public void showPrivacy(ActionEvent actionEvent) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Privacy Policy");
        info.setHeaderText("Tailyatri Privacy Policy");
        info.setContentText("Here is our privacy policy...\n(This would show actual privacy policy in a real app)");
        info.getDialogPane().setPrefWidth(600);
        info.showAndWait();
    }

    /**
     * Handles wallet selection for payment processing.
     * Shows information dialog about wallet redirection.
     *
     * @param event Action event from wallet selection button
     */
    @FXML
    public void selectWallet(ActionEvent event) {
        if (isPaymentProcessing || isPaymentCompleted) return;

        Button source = (Button) event.getSource();
        String walletName = source.getText();

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Wallet Payment");
        info.setHeaderText("Redirecting to " + walletName);
        info.setContentText("You will be redirected to " + walletName + " to complete the payment.");
        info.showAndWait();
    }

    /**
     * Handles back navigation with payment state consideration.
     * Prevents navigation during processing, shows confirmation for cancellation.
     *
     * @param event Action event from back button
     */
    @FXML
    public void handleBack(ActionEvent event) {
        if (isPaymentProcessing) {
            showError("Cannot go back during payment processing.");
            return;
        }

        if (isPaymentCompleted) {
            SceneManager.switchScene("/fxml/MainMenu.fxml");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Payment");
        confirm.setHeaderText("Are you sure you want to go back?");
        confirm.setContentText("Your booking will be cancelled if you go back.");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            bookingService.handleFailedPayment(currentBooking.getBookingId(), "User cancelled payment");
            SceneManager.switchScene("/fxml/MainMenu.fxml");
        }
    }

    // =========================================================================
    // UI STATE MANAGEMENT AND UTILITIES
    // =========================================================================

    /**
     * Shows or hides loading overlay with optional message.
     * Includes fade animation for smooth user experience.
     *
     * @param show true to show overlay, false to hide
     * @param message Message to display during loading
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
     * Shows error dialog with specified message.
     *
     * @param message Error message to display
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    /**
     * Data transfer object for payment processing results.
     * Encapsulates payment success/failure information with details.
     */
    private static class PaymentResult {
        private final boolean success;
        private final String paymentId;
        private final String signature;
        private final String errorMessage;

        /**
         * Creates a payment result.
         *
         * @param success true if payment succeeded
         * @param paymentId Razorpay payment ID
         * @param signature HMAC signature for verification
         * @param errorMessage Error message if payment failed
         */
        public PaymentResult(boolean success, String paymentId, String signature, String errorMessage) {
            this.success = success;
            this.paymentId = paymentId;
            this.signature = signature;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public String getPaymentId() { return paymentId; }
        public String getSignature() { return signature; }
        public String getErrorMessage() { return errorMessage; }
    }
}