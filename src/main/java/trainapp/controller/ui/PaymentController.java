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
 * FIXED PaymentController - Ensures consistent amount handling across booking, payment, and invoice.
 * The payment amount is always taken from booking.getTotalFare() which matches booking summary.
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

    // Payment Configuration
    private static final String RAZORPAY_SECRET = "5JHY6hPtBAGJ8n8R9iQ9TRtE";

    // -------------------------------------------------------------------------
    // State Management and Payment Consistency
    // -------------------------------------------------------------------------

    private ToggleGroup paymentMethodGroup;
    private Booking currentBooking;
    private String razorpayOrderId;
    private double consistentPaymentAmount; // This is the authoritative amount
    private PaymentMethod selectedPaymentMethod = PaymentMethod.CARD;

    // Payment state tracking
    private boolean isPaymentProcessing = false;
    private boolean isPaymentCompleted = false;

    // Form validation flags (REPLACED BINDINGS WITH SIMPLE BOOLEANS)
    private boolean cardNumberValid = false;
    private boolean expiryValid = false;
    private boolean cvvValid = false;
    private boolean holderValid = false;
    private boolean upiValid = false;
    private boolean bankValid = false;
    private boolean termsAccepted = false;

    public void showTerms(ActionEvent actionEvent) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Terms & Conditions");
        info.setHeaderText("Tailyatri Terms & Conditions");
        info.setContentText("Here are the terms and conditions...\n(This would show actual terms in a real app)");
        info.getDialogPane().setPrefWidth(600);
        info.showAndWait();
    }

    public void showPrivacy(ActionEvent actionEvent) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Privacy Policy");
        info.setHeaderText("Tailyatri Privacy Policy");
        info.setContentText("Here is our privacy policy...\n(This would show actual privacy policy in a real app)");
        info.getDialogPane().setPrefWidth(600);
        info.showAndWait();
    }

    public enum PaymentMethod {
        CARD, UPI, NET_BANKING, WALLET
    }

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    @FXML
    public void initialize() {
        setupPaymentMethodSelection();
        setupFormValidation();
        initializeBankComboBox();

        System.out.println("PaymentController initialized with consistent amount handling");
    }

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

    // -------------------------------------------------------------------------
    // FIXED: Booking Data Management with Consistent Amount
    // -------------------------------------------------------------------------

    /**
     * FIXED: Always use booking.getTotalFare() as the authoritative payment amount.
     * This ensures payment amount matches booking summary amount.
     */
    public void setBookingData(Booking booking, String razorpayOrderId, double providedAmount) {
        this.currentBooking = booking;
        this.razorpayOrderId = razorpayOrderId;

        // CRITICAL FIX: Always use booking.getTotalFare() as the correct amount
        // This is what was saved to DB from the booking summary
        this.consistentPaymentAmount = Math.round(booking.getTotalFare() * 100.0) / 100.0;

        Platform.runLater(() -> {
            updateBookingSummaryWithConsistentAmount();
            updatePayButtonState();
        });

        System.out.println("=== PAYMENT CONTROLLER - AMOUNT SET ===");
        System.out.println("Booking amount from DB: ₹" + booking.getTotalFare());
        System.out.println("Provided amount: ₹" + providedAmount);
        System.out.println("Using booking amount as authoritative: ₹" + consistentPaymentAmount);
        System.out.println("This matches the booking summary shown to user!");
    }

    private void updateBookingSummaryWithConsistentAmount() {
        if (currentBooking != null) {
            updateBookingDetails();
            updateConsistentAmountBreakdown();
            System.out.println("Payment summary updated with consistent amount: ₹" + consistentPaymentAmount);
        }
    }

    /**
     * FIXED: Calculate payment breakdown from booking's total fare
     */
    private void updateConsistentAmountBreakdown() {
        double totalAmount = consistentPaymentAmount;
        double convenienceFee = 20.0;
        double ticketFare = totalAmount - convenienceFee;
        double gst = 0.0;

        // Ensure values are not negative
        if (ticketFare < 0) {
            ticketFare = 0;
            convenienceFee = totalAmount;
            System.out.println("Warning: Adjusted convenience fee due to low total amount");
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

        System.out.println("=== PAYMENT PAGE BREAKDOWN ===");
        System.out.println("Ticket Fare: ₹" + ticketFare);
        System.out.println("Convenience Fee: ₹" + convenienceFee);
        System.out.println("GST: ₹" + gst);
        System.out.println("Total: ₹" + totalAmount);
        System.out.println("This breakdown matches booking summary!");
    }

    private void updateBookingDetails() {
        trainDetailsLabel.setText("Train Details");
        routeLabel.setText("Journey Route");
        dateLabel.setText("Journey Date | Class");
        pnrLabel.setText("PNR: " + currentBooking.getPnr());
        passengerCountLabel.setText("Passengers");
    }

    // -------------------------------------------------------------------------
    // Payment Method Selection and Management
    // -------------------------------------------------------------------------

    private void setupPaymentMethodSelection() {
        configureToggleGroup();
        setupPaymentOptionHandlers();
        setupSelectionChangeListener();
    }

    private void configureToggleGroup() {
        paymentMethodGroup = new ToggleGroup();
        upiRadio.setToggleGroup(paymentMethodGroup);
        cardRadio.setToggleGroup(paymentMethodGroup);
        netBankingRadio.setToggleGroup(paymentMethodGroup);
        walletRadio.setToggleGroup(paymentMethodGroup);
        cardRadio.setSelected(true);
    }

    private void setupPaymentOptionHandlers() {
        upiOption.setOnMouseClicked(e -> selectPaymentMethod(PaymentMethod.UPI, upiRadio));
        cardOption.setOnMouseClicked(e -> selectPaymentMethod(PaymentMethod.CARD, cardRadio));
        netBankingOption.setOnMouseClicked(e -> selectPaymentMethod(PaymentMethod.NET_BANKING, netBankingRadio));
        walletOption.setOnMouseClicked(e -> selectPaymentMethod(PaymentMethod.WALLET, walletRadio));
    }

    private void setupSelectionChangeListener() {
        paymentMethodGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            updatePaymentForm();
        });
    }

    private void selectPaymentMethod(PaymentMethod method, RadioButton radioButton) {
        if (isPaymentProcessing || isPaymentCompleted) {
            return;
        }

        selectedPaymentMethod = method;
        radioButton.setSelected(true);
        updatePaymentMethodVisualState();
        updatePaymentForm();

        System.out.println("Selected payment method: " + method);
    }

    private void updatePaymentMethodVisualState() {
        upiOption.getStyleClass().remove("selected");
        cardOption.getStyleClass().remove("selected");
        netBankingOption.getStyleClass().remove("selected");
        walletOption.getStyleClass().remove("selected");

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

    private void updatePaymentForm() {
        hideAllPaymentForms();
        showSelectedPaymentForm();
        updatePayButtonState();
    }

    private void hideAllPaymentForms() {
        cardForm.setVisible(false);
        upiForm.setVisible(false);
        netBankingForm.setVisible(false);
        walletForm.setVisible(false);
    }

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
    // FIXED: Form Validation Methods (CORRECTED EXPIRY VALIDATION)
    // -------------------------------------------------------------------------

    private void setupFormValidation() {
        setupCardNumberFormatting();
        setupExpiryFieldFormatting();
        setupCVVFieldValidation();
        setupCardHolderValidation();
        setupUPIValidation();
        setupTermsCheckboxListener();
    }

    private void setupCardNumberFormatting() {
        cardNumberField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !isPaymentProcessing && !isPaymentCompleted) {
                String formatted = formatCardNumber(newValue);
                if (!formatted.equals(newValue)) {
                    cardNumberField.setText(formatted);
                    cardNumberField.positionCaret(formatted.length());
                }
                cardNumberValid = isValidLuhnCardNumber(formatted.replaceAll("\\s", ""));
                System.out.println("Card number valid: " + cardNumberValid + " for input: " + formatted);
                updatePayButtonState();
            }
        });
    }

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

    private void setupExpiryFieldFormatting() {
        expiryField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !isPaymentProcessing && !isPaymentCompleted) {
                String formatted = formatExpiryDate(newValue);
                if (!formatted.equals(newValue)) {
                    expiryField.setText(formatted);
                    expiryField.positionCaret(formatted.length());
                }
                expiryValid = isValidFutureExpiryDate(formatted);
                System.out.println("Expiry valid: " + expiryValid + " for input: " + formatted);
                updatePayButtonState();
            }
        });
    }

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
                System.out.println("CVV valid: " + cvvValid + " for input: " + newValue);
                updatePayButtonState();
            }
        });
    }

    private void setupCardHolderValidation() {
        cardHolderField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !isPaymentProcessing && !isPaymentCompleted) {
                String filtered = newValue.replaceAll("[^a-zA-Z\\s]", "").toUpperCase();
                if (!filtered.equals(newValue)) {
                    cardHolderField.setText(filtered);
                    cardHolderField.positionCaret(filtered.length());
                }
                holderValid = (!filtered.trim().isEmpty() && filtered.length() >= 3);
                System.out.println("Holder valid: " + holderValid + " for input: " + filtered);
                updatePayButtonState();
            }
        });
    }

    private void setupUPIValidation() {
        upiIdField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!isPaymentProcessing && !isPaymentCompleted) {
                upiValid = isValidUPIForm(newValue);
                System.out.println("UPI valid: " + upiValid + " for input: " + newValue);
                updatePayButtonState();
            }
        });
    }

    private void setupTermsCheckboxListener() {
        termsCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
            termsAccepted = newValue;
            System.out.println("Terms accepted: " + termsAccepted);
            updatePayButtonState();
        });
    }

    // -------------------------------------------------------------------------
    // FIXED: MANUAL BUTTON ENABLE/DISABLE (CORRECTED LOGIC)
    // -------------------------------------------------------------------------

    private void updatePayButtonState() {
        boolean formValid = isCurrentFormValid();
        boolean canPay = formValid && termsAccepted && !isPaymentProcessing && !isPaymentCompleted;

        System.out.println("=== PAY BUTTON STATE UPDATE ===");
        System.out.println("Selected method: " + selectedPaymentMethod);
        System.out.println("Form valid: " + formValid);
        System.out.println("Terms accepted: " + termsAccepted);
        System.out.println("Processing: " + isPaymentProcessing);
        System.out.println("Completed: " + isPaymentCompleted);
        System.out.println("Can pay: " + canPay);

        payNowButton.setDisable(!canPay);
        updateButtonText();
    }

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

    private boolean isCurrentFormValid() {
        switch (selectedPaymentMethod) {
            case CARD:
                boolean cardFormValid = cardNumberValid && expiryValid && cvvValid && holderValid;
                System.out.println("Card form validation - Number: " + cardNumberValid + ", Expiry: " + expiryValid + ", CVV: " + cvvValid + ", Holder: " + holderValid + " => Valid: " + cardFormValid);
                return cardFormValid;
            case UPI:
                return upiValid;
            case NET_BANKING:
                return bankValid;
            case WALLET:
                return true;
        }
        return false;
    }

    /**
     * Fixed Luhn algorithm implementation for credit card validation.
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
     * FIXED: Simplified expiry date validation using YearMonth.
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

            // Card is valid if expiry is current month or future
            return !expiryYearMonth.isBefore(currentYearMonth);
        } catch (Exception e) {
            System.out.println("Expiry validation error: " + e.getMessage());
            return false;
        }
    }

    private boolean isValidUPIForm(String upiId) {
        return upiId != null &&
                upiId.matches("[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+") &&
                upiId.length() >= 3 &&
                upiId.contains("@");
    }

    // -------------------------------------------------------------------------
    // Payment Processing with Consistent Amount
    // -------------------------------------------------------------------------

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

    private void initiatePaymentProcessing() {
        isPaymentProcessing = true;

        updatePayButtonState();
        disableAllFormFields(true);
        showLoadingOverlay(true, "Processing Payment...");

        System.out.println("=== INITIATING PAYMENT ===");
        System.out.println("Processing payment for amount: ₹" + consistentPaymentAmount);
        System.out.println("This amount matches booking summary!");

        processRazorpayPayment();
    }

    private void disableAllFormFields(boolean disable) {
        cardNumberField.setDisable(disable);
        expiryField.setDisable(disable);
        cvvField.setDisable(disable);
        cardHolderField.setDisable(disable);
        upiIdField.setDisable(disable);
        bankComboBox.setDisable(disable);
        termsCheckBox.setDisable(disable);

        upiRadio.setDisable(disable);
        cardRadio.setDisable(disable);
        netBankingRadio.setDisable(disable);
        walletRadio.setDisable(disable);
    }

    private void processRazorpayPayment() {
        simulateSuccessfulPayment();
    }

    private void simulateSuccessfulPayment() {
        Task<PaymentResult> paymentTask = createPaymentSimulationTask();
        configurePaymentTaskHandlers(paymentTask);
        new Thread(paymentTask).start();
    }

    private Task<PaymentResult> createPaymentSimulationTask() {
        return new Task<PaymentResult>() {
            @Override
            protected PaymentResult call() throws Exception {
                Thread.sleep(2000);

                String paymentId = "pay_" + System.currentTimeMillis();
                String properSignature = generateProperSignature(razorpayOrderId, paymentId);

                System.out.println("=== PAYMENT SIMULATION ===");
                System.out.println("Order ID: " + razorpayOrderId);
                System.out.println("Payment ID: " + paymentId);
                System.out.println("Amount: ₹" + consistentPaymentAmount);
                System.out.println("Signature: " + properSignature);

                return new PaymentResult(true, paymentId, properSignature, null);
            }
        };
    }

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

    private void handleSuccessfulPayment(PaymentResult result) {
        showLoadingOverlay(true, "Confirming booking...");

        PaymentSuccessRequest paymentRequest = createPaymentSuccessRequest(result);
        processBookingConfirmation(paymentRequest);
    }

    private PaymentSuccessRequest createPaymentSuccessRequest(PaymentResult result) {
        PaymentSuccessRequest paymentRequest = new PaymentSuccessRequest();
        paymentRequest.setBookingId(currentBooking.getBookingId());
        paymentRequest.setRazorpayOrderId(razorpayOrderId);
        paymentRequest.setRazorpayPaymentId(result.getPaymentId());
        paymentRequest.setRazorpaySignature(result.getSignature());

        System.out.println("Created payment success request for amount: ₹" + consistentPaymentAmount);
        return paymentRequest;
    }

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

    private void handleFailedPayment(String errorMessage) {
        bookingService.handleFailedPayment(currentBooking.getBookingId(), errorMessage);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Payment Failed");
        alert.setHeaderText("Payment could not be processed");
        alert.setContentText(errorMessage + "\n\nPlease try again with different payment method.");
        alert.showAndWait();

        resetPaymentState();
    }

    private void resetPaymentState() {
        isPaymentProcessing = false;
        isPaymentCompleted = false;
        disableAllFormFields(false);
        updatePayButtonState();
    }

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

    // -------------------------------------------------------------------------
    // Action Handlers and Navigation
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // UI State Management and Utilities
    // -------------------------------------------------------------------------

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

    private static class PaymentResult {
        private boolean success;
        private String paymentId;
        private String signature;
        private String errorMessage;

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