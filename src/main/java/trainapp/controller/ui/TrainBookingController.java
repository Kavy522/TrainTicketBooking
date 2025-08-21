package trainapp.controller.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import trainapp.dao.TrainDAO;
import trainapp.model.Train;
import trainapp.model.Booking;
import trainapp.model.TrainClass;
import trainapp.service.BookingService;
import trainapp.service.SessionManager;
import trainapp.service.TrainService;
import trainapp.service.AdminDataStructureService;
import trainapp.util.SceneManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * TrainBookingController manages the comprehensive train booking process with consistent data calculations.
 *
 * <h2>Core Responsibilities:</h2>
 * <ul>
 *   <li><b>Booking Workflow Management</b> - Handles complete train booking process from selection to payment</li>
 *   <li><b>Consistent Data Calculations</b> - Ensures pricing, timing, and distance consistency across all controllers</li>
 *   <li><b>Passenger Management</b> - Supports adding, editing, and removing passengers with validation</li>
 *   <li><b>Class Selection</b> - Dynamic class selection with real-time pricing and availability updates</li>
 *   <li><b>Payment Integration</b> - Seamless integration with payment gateway and booking confirmation</li>
 *   <li><b>Session Management</b> - Handles user authentication and session-aware operations</li>
 * </ul>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Real-time seat availability checking with class-wise pricing</li>
 *   <li>Dynamic fare calculation with surge pricing for popular routes</li>
 *   <li>Multi-passenger support with comprehensive validation</li>
 *   <li>Consistent amount calculation ensuring booking summary = payment amount</li>
 *   <li>Interactive UI with immediate feedback and error handling</li>
 *   <li>Cross-controller data consistency through caching mechanisms</li>
 * </ul>
 *
 * <h2>Data Consistency Strategy:</h2>
 * <ul>
 *   <li>Single source of truth for pricing calculations</li>
 *   <li>Cached consistent data shared across search, details, and payment controllers</li>
 *   <li>Exact amount matching from booking summary through payment to invoice</li>
 *   <li>Standardized distance and timing calculations</li>
 * </ul>
 *
 * <h2>Booking Flow:</h2>
 * <ol>
 *   <li>User selects train from search results</li>
 *   <li>Controller loads consistent train data with pricing</li>
 *   <li>User selects class and adds passengers</li>
 *   <li>Real-time calculation of total amount</li>
 *   <li>Booking creation with exact amount preservation</li>
 *   <li>Seamless redirect to payment with consistent data</li>
 * </ol>
 */
public class TrainBookingController {

    // =========================================================================
    // FXML UI COMPONENTS
    // =========================================================================

    // Header and Navigation Elements
    @FXML private Button backButton;
    @FXML private Label userLabel;
    @FXML private Label messageLabel;

    // Train Information Display Components
    @FXML private Label trainNumberLabel;
    @FXML private Label trainNameLabel;
    @FXML private Label fromStationLabel;
    @FXML private Label toStationLabel;
    @FXML private Label departureDateLabel;
    @FXML private Label arrivalDateLabel;
    @FXML private Label departureTimeLabel;
    @FXML private Label arrivalTimeLabel;
    @FXML private Label durationLabel;
    @FXML private Label distanceLabel;
    @FXML private Button viewDetailsBtn;

    // Class Selection Controls
    @FXML private RadioButton slRadio;
    @FXML private RadioButton acRadio;
    @FXML private RadioButton ac2Radio;
    @FXML private RadioButton ac1Radio;
    @FXML private Label slPriceLabel;
    @FXML private Label acPriceLabel;
    @FXML private Label ac2PriceLabel;
    @FXML private Label ac1PriceLabel;
    @FXML private Label slSeatsLabel;
    @FXML private Label acSeatsLabel;
    @FXML private Label ac2SeatsLabel;
    @FXML private Label ac1SeatsLabel;
    @FXML private VBox slClassCard;
    @FXML private VBox acClassCard;
    @FXML private VBox ac2ClassCard;
    @FXML private VBox ac1ClassCard;

    // Passenger Form Controls
    @FXML private TextField passengerNameField;
    @FXML private TextField passengerAgeField;
    @FXML private RadioButton maleRadio;
    @FXML private RadioButton femaleRadio;
    @FXML private RadioButton otherRadio;
    @FXML private Button addPassengerBtn;
    @FXML private Button clearFormBtn;

    // Passenger Management Components
    @FXML private VBox passengerListContainer;
    @FXML private Label passengerCountLabel;
    @FXML private Label totalAmountLabel;

    // Booking Summary Display
    @FXML private Label journeySummaryLabel;
    @FXML private Label dateSummaryLabel;
    @FXML private Label selectedClassLabel;
    @FXML private Label summaryDistanceLabel;
    @FXML private Label summaryPassengerCountLabel;
    @FXML private Label perTicketPriceLabel;
    @FXML private Label summaryFareLabel;
    @FXML private Label summaryTotalLabel;
    @FXML private Button proceedToPaymentBtn;

    // =========================================================================
    // SERVICES AND DEPENDENCIES
    // =========================================================================

    /** Session management for user authentication and state */
    private final SessionManager sessionManager = SessionManager.getInstance();

    /** Service for train-related operations */
    private final TrainService trainService = new TrainService();

    /** Data access object for train information */
    private final TrainDAO trainDAO = new TrainDAO();

    /** Service for booking operations and payment processing */
    private final BookingService bookingService = new BookingService();

    /** Service for dynamic fare calculations */
    private final AdminDataStructureService adminService = new AdminDataStructureService();

    // =========================================================================
    // STATE MANAGEMENT AND CONFIGURATION
    // =========================================================================

    // UI Control Groups
    private ToggleGroup classToggleGroup;
    private ToggleGroup genderToggleGroup;

    // Booking State Variables
    private final List<PassengerData> passengerList = new ArrayList<>();
    private int trainId;
    private String trainNumber;
    private String trainName;
    private String fromStation;
    private String toStation;
    private LocalDate journeyDate;
    private Train currentTrain;

    // Consistent Data Variables
    private int distanceKm = 0;
    private String departureTime = "";
    private String arrivalTime = "";
    private String duration = "";
    private final Map<String, Double> consistentPricing = new HashMap<>();

    // Configuration Constants
    private static final double CONVENIENCE_FEE = 20.0;
    private static final int MAX_PASSENGERS = 6;

    // =========================================================================
    // INITIALIZATION AND SETUP
    // =========================================================================

    /**
     * Initializes the booking controller and sets up UI components.
     * Called automatically by JavaFX framework after FXML loading.
     *
     * <h3>Initialization Steps:</h3>
     * <ol>
     *   <li>Sets up user interface with session information</li>
     *   <li>Configures toggle groups for class and gender selection</li>
     *   <li>Sets up event handlers for interactive elements</li>
     *   <li>Initializes form validation and listeners</li>
     *   <li>Updates initial totals and displays</li>
     * </ol>
     */
    @FXML
    public void initialize() {
        setupUserInterface();
        setupToggleGroups();
        setupClassCardClickHandlers();
        setupIndividualRadioListeners();
        Platform.runLater(this::updateTotals);
    }

    /**
     * Sets up user interface elements with session-specific information.
     * Displays welcome message for authenticated users.
     */
    private void setupUserInterface() {
        if (sessionManager.isLoggedIn()) {
            userLabel.setText("Welcome, " + sessionManager.getCurrentUser().getName() + "!");
        }
    }

    /**
     * Sets up toggle groups for class and gender selection.
     * Configures default selections and change listeners.
     */
    private void setupToggleGroups() {
        configureClassToggleGroup();
        configureGenderToggleGroup();
        setDefaultSelections();
        setupToggleGroupListeners();
    }

    /**
     * Configures the class selection toggle group.
     * Groups all class radio buttons for mutual exclusion.
     */
    private void configureClassToggleGroup() {
        classToggleGroup = new ToggleGroup();
        slRadio.setToggleGroup(classToggleGroup);
        acRadio.setToggleGroup(classToggleGroup);
        ac2Radio.setToggleGroup(classToggleGroup);
        ac1Radio.setToggleGroup(classToggleGroup);
    }

    /**
     * Configures the gender selection toggle group.
     * Groups all gender radio buttons for mutual exclusion.
     */
    private void configureGenderToggleGroup() {
        genderToggleGroup = new ToggleGroup();
        maleRadio.setToggleGroup(genderToggleGroup);
        femaleRadio.setToggleGroup(genderToggleGroup);
        otherRadio.setToggleGroup(genderToggleGroup);
    }

    /**
     * Sets default selections for toggle groups.
     * Defaults to AC 3-Tier class and Male gender.
     */
    private void setDefaultSelections() {
        acRadio.setSelected(true);
        maleRadio.setSelected(true);
    }

    /**
     * Sets up change listeners for toggle groups.
     * Updates UI and calculations when selections change.
     */
    private void setupToggleGroupListeners() {
        classToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                updateClassSelection();
                updateTotals();
            }
        });
    }

    /**
     * Sets up individual radio button listeners for additional responsiveness.
     * Provides backup event handling for direct radio button interactions.
     */
    private void setupIndividualRadioListeners() {
        setupRadioButtonListener(slRadio, "SL Radio");
        setupRadioButtonListener(acRadio, "3A Radio");
        setupRadioButtonListener(ac2Radio, "2A Radio");
        setupRadioButtonListener(ac1Radio, "1A Radio");
    }

    /**
     * Sets up a listener for an individual radio button.
     *
     * @param radioButton The radio button to set up
     * @param debugName Name for debugging purposes
     */
    private void setupRadioButtonListener(RadioButton radioButton, String debugName) {
        if (radioButton != null) {
            radioButton.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    Platform.runLater(() -> {
                        updateClassSelection();
                        updateTotals();
                    });
                }
            });
        }
    }

    /**
     * Sets up click handlers for class selection cards.
     * Allows users to click on entire card to select class.
     */
    private void setupClassCardClickHandlers() {
        setupCardClickHandler(slClassCard, slRadio, "SL Card");
        setupCardClickHandler(acClassCard, acRadio, "3A Card");
        setupCardClickHandler(ac2ClassCard, ac2Radio, "2A Card");
        setupCardClickHandler(ac1ClassCard, ac1Radio, "1A Card");
    }

    /**
     * Sets up click handler for a specific class card.
     *
     * @param card The VBox card element
     * @param radio The associated radio button
     * @param debugName Name for debugging purposes
     */
    private void setupCardClickHandler(VBox card, RadioButton radio, String debugName) {
        if (card != null) {
            card.setOnMouseClicked(e -> radio.setSelected(true));
        }
    }

    // =========================================================================
    // DATA MANAGEMENT AND CONSISTENCY
    // =========================================================================

    /**
     * Sets booking data and initializes the booking interface.
     * Main entry point for booking functionality from other controllers.
     *
     * <h3>Data Processing:</h3>
     * <ol>
     *   <li>Stores booking parameters</li>
     *   <li>Loads consistent train data with calculations</li>
     *   <li>Initializes UI with train information</li>
     *   <li>Sets up pricing and availability data</li>
     * </ol>
     *
     * @param trainId Unique identifier for the train
     * @param trainNumber Train number for display
     * @param trainName Train name for display
     * @param fromStation Source station name
     * @param toStation Destination station name
     * @param journeyDate Date of travel
     */
    public void setBookingData(int trainId, String trainNumber, String trainName,
                               String fromStation, String toStation, LocalDate journeyDate) {
        this.trainId = trainId;
        this.trainNumber = trainNumber != null ? trainNumber : "";
        this.trainName = trainName != null ? trainName : "";
        this.fromStation = fromStation != null ? fromStation : "";
        this.toStation = toStation != null ? toStation : "";
        this.journeyDate = journeyDate;

        loadConsistentTrainData();
        initializeBookingInterface();
    }

    /**
     * Loads train data with consistent calculations across all controllers.
     * Ensures same distance, timing, and pricing data throughout the application.
     *
     * <h3>Consistency Features:</h3>
     * <ul>
     *   <li>Standardized distance calculation using time-based method</li>
     *   <li>Consistent timing data from train service</li>
     *   <li>Dynamic pricing with surge adjustments</li>
     *   <li>Cross-controller data caching</li>
     * </ul>
     */
    private void loadConsistentTrainData() {
        try {
            this.currentTrain = trainDAO.getTrainById(trainId);

            if (currentTrain != null) {
                // Calculate consistent distance using standardized method
                this.distanceKm = trainService.getDistanceBetween(currentTrain, this.fromStation, this.toStation);

                // Get consistent timing data
                this.departureTime = trainService.getDepartureTime(currentTrain, this.fromStation);
                this.arrivalTime = trainService.getArrivalTime(currentTrain, this.toStation);
                this.duration = trainService.calculateDuration(currentTrain, this.fromStation, this.toStation);

                // Calculate consistent pricing for all classes
                calculateConsistentPricing();

            } else {
                setFallbackData();
            }
        } catch (Exception e) {
            setFallbackData();
        }
    }

    /**
     * Calculates consistent pricing for all classes using standardized methodology.
     * Applies surge pricing for popular routes and stores results for cross-controller access.
     */
    private void calculateConsistentPricing() {
        // Use AdminDataStructureService for consistent pricing calculation
        double slPrice = adminService.calculateDynamicFare(TrainClass.SL, distanceKm);
        double ac3Price = adminService.calculateDynamicFare(TrainClass._3A, distanceKm);
        double ac2Price = adminService.calculateDynamicFare(TrainClass._2A, distanceKm);
        double ac1Price = adminService.calculateDynamicFare(TrainClass._1A, distanceKm);

        // Apply surge pricing if route is popular
        if (isPopularRoute(fromStation, toStation)) {
            slPrice *= 1.2;    // 20% surge
            ac3Price *= 1.15;  // 15% surge
            ac2Price *= 1.1;   // 10% surge
            ac1Price *= 1.05;  // 5% surge
        }

        // Store consistent pricing
        consistentPricing.put("SL", slPrice);
        consistentPricing.put("3A", ac3Price);
        consistentPricing.put("2A", ac2Price);
        consistentPricing.put("1A", ac1Price);

        // Store in consistency tracker for cross-controller access
        ConsistencyTracker.recordCalculation(trainNumber, fromStation + "-" + toStation,
                distanceKm, duration, consistentPricing);
    }

    /**
     * Sets fallback data when train data loading fails.
     * Provides reasonable defaults to ensure UI remains functional.
     */
    private void setFallbackData() {
        this.distanceKm = 350; // Fallback distance
        this.departureTime = "06:00";
        this.arrivalTime = "12:30";
        this.duration = "6h 30m";

        // Calculate fallback pricing
        calculateConsistentPricing();
    }

    /**
     * Sets pending booking data for post-authentication booking.
     * Used when user needs to login before completing booking.
     *
     * @param trainId Train identifier
     * @param fromStation Source station
     * @param toStation Destination station
     * @param journeyDate Travel date
     */
    public void setPendingBookingData(int trainId, String fromStation, String toStation, LocalDate journeyDate) {
        this.trainId = trainId;
        this.trainNumber = "12345";
        this.trainName = "Express Train";
        this.fromStation = fromStation != null ? fromStation : "";
        this.toStation = toStation != null ? toStation : "";
        this.journeyDate = journeyDate;

        loadConsistentTrainData();
        initializeBookingInterface();
    }

    /**
     * Initializes the booking interface with loaded train data.
     * Updates all UI components with consistent information.
     */
    private void initializeBookingInterface() {
        updateTrainDetails();
        updateClassPricing();
        updateAvailableSeats();
        updateTotals();
    }

    // =========================================================================
    // PRICING AND AMOUNT CALCULATION
    // =========================================================================

    /**
     * Calculates price for a specific train class using consistent pricing data.
     *
     * @param trainClass The train class to calculate price for
     * @return Calculated price for the specified class
     */
    private double calculatePriceForClass(TrainClass trainClass) {
        String classKey = switch (trainClass) {
            case SL -> "SL";
            case _3A -> "3A";
            case _2A -> "2A";
            case _1A -> "1A";
        };

        return consistentPricing.getOrDefault(classKey, 0.0);
    }

    /**
     * Gets the current ticket price based on selected class.
     *
     * @return Price per ticket for currently selected class
     */
    private double getCurrentTicketPrice() {
        if (classToggleGroup == null) {
            return 0;
        }

        RadioButton selected = (RadioButton) classToggleGroup.getSelectedToggle();
        if (selected == null) {
            return 0;
        }

        TrainClass trainClass = getTrainClassFromText(selected.getText());
        return calculatePriceForClass(trainClass);
    }

    /**
     * Converts class text to TrainClass enum.
     *
     * @param classText Text representation of the class
     * @return Corresponding TrainClass enum value
     */
    private TrainClass getTrainClassFromText(String classText) {
        if (classText == null) return TrainClass._3A;

        return switch (classText) {
            case "Sleeper (SL)" -> TrainClass.SL;
            case "AC 3-Tier (3A)", "AC 3 Tier (3A)" -> TrainClass._3A;
            case "AC 2-Tier (2A)", "AC 2 Tier (2A)" -> TrainClass._2A;
            case "AC First Class (1A)", "AC 1st Class (1A)", "AC First (1A)" -> TrainClass._1A;
            default -> TrainClass._3A;
        };
    }

    /**
     * Calculates the exact total amount for booking.
     * This is the single source of truth for amounts across booking, payment, and invoice.
     *
     * <h3>Critical Importance:</h3>
     * This method ensures that:
     * <ul>
     *   <li>Booking summary amount = Database saved amount</li>
     *   <li>Payment amount = Booking amount</li>
     *   <li>Invoice amount = Payment amount</li>
     *   <li>No discrepancies across the entire booking flow</li>
     * </ul>
     *
     * @return Total amount rounded to 2 decimal places for currency consistency
     */
    private double calculateTotalAmount() {
        double ticketPrice = getCurrentTicketPrice();
        double totalFare = passengerList.size() * ticketPrice;
        double totalAmount = totalFare + CONVENIENCE_FEE;

        // Critical: Round to 2 decimal places for currency consistency
        return Math.round(totalAmount * 100.0) / 100.0;
    }

    // =========================================================================
    // UI UPDATES AND DISPLAY MANAGEMENT
    // =========================================================================

    /**
     * Updates train details display with consistent data.
     * Ensures all timing and route information matches across controllers.
     */
    private void updateTrainDetails() {
        if (journeyDate == null) return;

        Platform.runLater(() -> {
            updateBasicTrainInformation();
            updateJourneyDates();
            updateConsistentTiming();
            updateDistanceInformation();
            updateSummaryLabels();
        });
    }

    /**
     * Updates basic train information labels.
     */
    private void updateBasicTrainInformation() {
        if (trainNumberLabel != null) trainNumberLabel.setText(trainNumber);
        if (trainNameLabel != null) trainNameLabel.setText(trainName);
        if (fromStationLabel != null) fromStationLabel.setText(fromStation);
        if (toStationLabel != null) toStationLabel.setText(toStation);
    }

    /**
     * Updates journey date labels with proper formatting.
     */
    private void updateJourneyDates() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");
        if (departureDateLabel != null) departureDateLabel.setText(journeyDate.format(formatter));
        if (arrivalDateLabel != null) arrivalDateLabel.setText(journeyDate.format(formatter));
    }

    /**
     * Updates timing information with consistent data across controllers.
     * Ensures departure, arrival, and duration match search and details pages.
     */
    private void updateConsistentTiming() {
        if (departureTimeLabel != null) departureTimeLabel.setText(departureTime);
        if (arrivalTimeLabel != null) arrivalTimeLabel.setText(arrivalTime);
        if (durationLabel != null) durationLabel.setText(duration);
    }

    /**
     * Updates distance information with consistent calculations.
     */
    private void updateDistanceInformation() {
        if (distanceLabel != null) distanceLabel.setText(distanceKm + " km");
        if (summaryDistanceLabel != null) summaryDistanceLabel.setText(distanceKm + " km");
    }

    /**
     * Updates booking summary labels with journey information.
     */
    private void updateSummaryLabels() {
        if (journeySummaryLabel != null) journeySummaryLabel.setText(fromStation + " → " + toStation);
        if (dateSummaryLabel != null) dateSummaryLabel.setText(journeyDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
    }

    /**
     * Updates class pricing display with consistent data across all controllers.
     * Ensures pricing matches search results and payment amounts.
     */
    private void updateClassPricing() {
        double slPrice = consistentPricing.get("SL");
        double ac3Price = consistentPricing.get("3A");
        double ac2Price = consistentPricing.get("2A");
        double ac1Price = consistentPricing.get("1A");

        Platform.runLater(() -> updatePriceLabels(slPrice, ac3Price, ac2Price, ac1Price));
    }

    /**
     * Updates price labels with calculated values.
     *
     * @param slPrice Sleeper class price
     * @param ac3Price AC 3-Tier price
     * @param ac2Price AC 2-Tier price
     * @param ac1Price AC First Class price
     */
    private void updatePriceLabels(double slPrice, double ac3Price, double ac2Price, double ac1Price) {
        if (slPriceLabel != null) {
            slPriceLabel.setText("₹" + String.format("%.0f", slPrice));
        }
        if (acPriceLabel != null) {
            acPriceLabel.setText("₹" + String.format("%.0f", ac3Price));
        }
        if (ac2PriceLabel != null) {
            ac2PriceLabel.setText("₹" + String.format("%.0f", ac2Price));
        }
        if (ac1PriceLabel != null) {
            ac1PriceLabel.setText("₹" + String.format("%.0f", ac1Price));
        }
    }

    /**
     * Updates available seats display for all classes.
     * Shows real-time seat availability with waitlist information.
     */
    private void updateAvailableSeats() {
        if (currentTrain != null && trainService != null && journeyDate != null) {
            try {
                Map<String, Integer> seatMap = trainService.getAvailableSeatsForDate(currentTrain, journeyDate);
                Platform.runLater(() -> updateSeatLabels(seatMap));
            } catch (Exception e) {
                // Silently handle errors - could integrate with logging system
            }
        }
    }

    /**
     * Updates seat availability labels with current data.
     *
     * @param seatMap Map of class names to available seat counts
     */
    private void updateSeatLabels(Map<String, Integer> seatMap) {
        updateSeatLabel(slSeatsLabel, seatMap.getOrDefault("SL", 0));
        updateSeatLabel(acSeatsLabel, seatMap.getOrDefault("3A", 0));
        updateSeatLabel(ac2SeatsLabel, seatMap.getOrDefault("2A", 0));
        updateSeatLabel(ac1SeatsLabel, seatMap.getOrDefault("1A", 0));
    }

    /**
     * Updates a single seat label with availability information.
     *
     * @param label The label to update
     * @param seats Number of available seats
     */
    private void updateSeatLabel(Label label, int seats) {
        if (label == null) return;

        if (seats > 0) {
            label.setText("Available: " + seats);
            label.setStyle("-fx-text-fill: #059669;");
        } else {
            label.setText("Waitlist");
            label.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    /**
     * Updates class selection visual state and UI elements.
     */
    private void updateClassSelection() {
        RadioButton selected = (RadioButton) classToggleGroup.getSelectedToggle();
        if (selected != null) {
            String className = selected.getText();
            if (selectedClassLabel != null) selectedClassLabel.setText(className);
            updateClassCardVisualStates(selected);
        }
    }

    /**
     * Updates visual states of class selection cards.
     *
     * @param selected Currently selected radio button
     */
    private void updateClassCardVisualStates(RadioButton selected) {
        resetClassCards();

        if (selected == slRadio && slClassCard != null) {
            slClassCard.getStyleClass().add("selected");
        } else if (selected == acRadio && acClassCard != null) {
            acClassCard.getStyleClass().add("selected");
        } else if (selected == ac2Radio && ac2ClassCard != null) {
            ac2ClassCard.getStyleClass().add("selected");
        } else if (selected == ac1Radio && ac1ClassCard != null) {
            ac1ClassCard.getStyleClass().add("selected");
        }
    }

    /**
     * Resets all class card visual states.
     */
    private void resetClassCards() {
        if (slClassCard != null) slClassCard.getStyleClass().removeAll("selected");
        if (acClassCard != null) acClassCard.getStyleClass().removeAll("selected");
        if (ac2ClassCard != null) ac2ClassCard.getStyleClass().removeAll("selected");
        if (ac1ClassCard != null) ac1ClassCard.getStyleClass().removeAll("selected");
    }

    /**
     * Updates total amounts and booking summary with current data.
     */
    private void updateTotals() {
        if (passengerList == null) return;

        BookingSummary summary = calculateBookingSummary();
        Platform.runLater(() -> updateTotalLabels(summary));
    }

    /**
     * Calculates booking summary with consistent calculations.
     *
     * @return BookingSummary object with calculated values
     */
    private BookingSummary calculateBookingSummary() {
        int passengerCount = passengerList.size();
        double ticketPrice = getCurrentTicketPrice();
        double totalFare = passengerCount * ticketPrice;
        double totalAmount = totalFare + CONVENIENCE_FEE;

        // Round for display consistency
        totalFare = Math.round(totalFare * 100.0) / 100.0;
        totalAmount = Math.round(totalAmount * 100.0) / 100.0;

        return new BookingSummary(passengerCount, ticketPrice, totalFare, totalAmount);
    }

    /**
     * Updates total labels with calculated summary data.
     *
     * @param summary Calculated booking summary
     */
    private void updateTotalLabels(BookingSummary summary) {
        if (passengerCountLabel != null) passengerCountLabel.setText(String.valueOf(summary.passengerCount));
        if (totalAmountLabel != null) totalAmountLabel.setText("₹" + String.format("%.2f", summary.totalAmount));
        if (summaryPassengerCountLabel != null) summaryPassengerCountLabel.setText(String.valueOf(summary.passengerCount));
        if (perTicketPriceLabel != null) perTicketPriceLabel.setText("₹" + String.format("%.2f", summary.ticketPrice));
        if (summaryFareLabel != null) summaryFareLabel.setText("₹" + String.format("%.2f", summary.totalFare));
        if (summaryTotalLabel != null) summaryTotalLabel.setText("₹" + String.format("%.2f", summary.totalAmount));

        if (proceedToPaymentBtn != null) {
            proceedToPaymentBtn.setDisable(summary.passengerCount == 0 || classToggleGroup.getSelectedToggle() == null);
        }
    }

    // =========================================================================
    // PASSENGER MANAGEMENT
    // =========================================================================

    /**
     * Handles adding a new passenger to the booking.
     * Validates input, checks limits, and updates UI accordingly.
     *
     * @param event Action event from add passenger button
     */
    @FXML
    public void handleAddPassenger(ActionEvent event) {
        PassengerInput input = collectPassengerInput();
        if (!validatePassengerInput(input)) return;
        if (passengerList.size() >= MAX_PASSENGERS) {
            showMessage("Maximum " + MAX_PASSENGERS + " passengers allowed per booking", "error");
            return;
        }

        PassengerData passenger = new PassengerData(input.name, input.age, input.gender);
        passengerList.add(passenger);
        addPassengerToList(passenger);
        clearForm();
        updateTotals();
        showMessage("Passenger added successfully!", "success");
    }

    /**
     * Collects passenger input from form fields.
     *
     * @return PassengerInput object with collected data
     */
    private PassengerInput collectPassengerInput() {
        String name = passengerNameField != null ? passengerNameField.getText().trim() : "";
        String ageText = passengerAgeField != null ? passengerAgeField.getText().trim() : "";
        String gender = getSelectedGender();
        return new PassengerInput(name, ageText, gender);
    }

    /**
     * Validates passenger input data.
     *
     * @param input PassengerInput to validate
     * @return true if input is valid, false otherwise
     */
    private boolean validatePassengerInput(PassengerInput input) {
        if (classToggleGroup == null || classToggleGroup.getSelectedToggle() == null) {
            showMessage("Please select a class first", "error");
            return false;
        }
        if (input.name.isEmpty()) {
            showMessage("Please enter passenger name", "error");
            return false;
        }
        if (input.ageText.isEmpty()) {
            showMessage("Please enter passenger age", "error");
            return false;
        }
        try {
            input.age = Integer.parseInt(input.ageText);
            if (input.age <= 0 || input.age > 120) {
                showMessage("Please enter a valid age (1-120)", "error");
                return false;
            }
        } catch (NumberFormatException e) {
            showMessage("Please enter a valid age", "error");
            return false;
        }
        if (input.gender == null || input.gender.isEmpty()) {
            showMessage("Please select gender", "error");
            return false;
        }
        return true;
    }

    /**
     * Adds passenger to the visual list in UI.
     *
     * @param passenger PassengerData to add to list
     */
    private void addPassengerToList(PassengerData passenger) {
        if (passengerListContainer == null) return;
        HBox passengerItem = createPassengerItem(passenger);
        passengerListContainer.getChildren().add(passengerItem);
    }

    /**
     * Creates visual representation of a passenger in the list.
     *
     * @param passenger PassengerData to create item for
     * @return HBox containing passenger information and remove button
     */
    private HBox createPassengerItem(PassengerData passenger) {
        HBox passengerItem = new HBox(15);
        passengerItem.getStyleClass().add("passenger-item");

        Label nameLabel = new Label(passenger.getName());
        nameLabel.getStyleClass().add("passenger-name");

        Label ageLabel = new Label("Age: " + passenger.getAge());
        ageLabel.getStyleClass().add("passenger-details");

        Label genderLabel = new Label(passenger.getGender());
        genderLabel.getStyleClass().add("passenger-details");

        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("remove-passenger-btn");
        removeBtn.setOnAction(e -> removePassenger(passenger, passengerItem));

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        passengerItem.getChildren().addAll(nameLabel, ageLabel, genderLabel, spacer, removeBtn);
        return passengerItem;
    }

    /**
     * Removes a passenger from the booking.
     *
     * @param passenger PassengerData to remove
     * @param passengerItem UI element to remove
     */
    private void removePassenger(PassengerData passenger, HBox passengerItem) {
        passengerList.remove(passenger);
        passengerListContainer.getChildren().remove(passengerItem);
        updateTotals();
        showMessage("Passenger removed", "success");
    }

    /**
     * Gets the currently selected gender from radio buttons.
     *
     * @return Selected gender string or null if none selected
     */
    private String getSelectedGender() {
        if (genderToggleGroup == null) return null;
        RadioButton selectedRadio = (RadioButton) genderToggleGroup.getSelectedToggle();
        return selectedRadio != null ? selectedRadio.getText() : null;
    }

    /**
     * Clears the passenger input form.
     */
    private void clearForm() {
        if (passengerNameField != null) passengerNameField.clear();
        if (passengerAgeField != null) passengerAgeField.clear();
        if (maleRadio != null) maleRadio.setSelected(true);
    }

    // =========================================================================
    // EVENT HANDLERS
    // =========================================================================

    /**
     * Handles view details button click to show train information.
     *
     * @param event Action event from view details button
     */
    @FXML
    public void handleViewDetails(ActionEvent event) {
        try {
            Alert detailsDialog = new Alert(Alert.AlertType.INFORMATION);
            detailsDialog.setTitle("Train Details");
            detailsDialog.setHeaderText(trainNumber + " - " + trainName);
            detailsDialog.setContentText(buildTrainDetailsText());
            detailsDialog.getDialogPane().setPrefWidth(600);
            detailsDialog.showAndWait();
        } catch (Exception e) {
            showMessage("Failed to load train details: " + e.getMessage(), "error");
        }
    }

    /**
     * Builds comprehensive train details text for display.
     *
     * @return Formatted string with train details
     */
    private String buildTrainDetailsText() {
        StringBuilder details = new StringBuilder();
        details.append("Journey Details:\n");
        details.append("From: ").append(fromStation).append(" at ").append(departureTime).append("\n");
        details.append("To: ").append(toStation).append(" at ").append(arrivalTime).append("\n");
        details.append("Duration: ").append(duration).append("\n");
        details.append("Distance: ").append(distanceKm).append(" km\n\n");

        details.append("Class & Pricing:\n");
        details.append("Sleeper (SL): ₹").append(String.format("%.0f", consistentPricing.get("SL"))).append("\n");
        details.append("AC 3 Tier (3A): ₹").append(String.format("%.0f", consistentPricing.get("3A"))).append("\n");
        details.append("AC 2 Tier (2A): ₹").append(String.format("%.0f", consistentPricing.get("2A"))).append("\n");
        details.append("AC First (1A): ₹").append(String.format("%.0f", consistentPricing.get("1A"))).append("\n");

        return details.toString();
    }

    /**
     * Handles proceed to payment button click.
     * Validates requirements and initiates booking and payment process.
     *
     * @param event Action event from proceed to payment button
     */
    @FXML
    public void handleProceedToPayment(ActionEvent event) {
        if (!validateBookingRequirements()) return;
        proceedWithBookingAndPayment();
    }

    /**
     * Validates booking requirements before proceeding to payment.
     *
     * @return true if all requirements are met, false otherwise
     */
    private boolean validateBookingRequirements() {
        if (passengerList.isEmpty()) {
            showMessage("Please add at least one passenger", "error");
            return false;
        }
        if (classToggleGroup == null || classToggleGroup.getSelectedToggle() == null) {
            showMessage("Please select a class", "error");
            return false;
        }
        return true;
    }

    /**
     * Proceeds with booking creation and payment processing.
     * Creates booking request with consistent amount and redirects to payment.
     */
    private void proceedWithBookingAndPayment() {
        try {
            setPaymentProcessingState(true);
            BookingService.BookingRequest bookingRequest = createBookingRequest();
            BookingService.BookingResult result = bookingService.createBookingWithPayment(bookingRequest);

            if (result.isSuccess()) {
                redirectToPaymentPage(result.getBooking(), result.getRazorpayOrderId());
            } else {
                showMessage("Booking failed: " + result.getMessage(), "error");
            }
        } catch (Exception e) {
            showMessage("Error creating booking: " + e.getMessage(), "error");
        } finally {
            setPaymentProcessingState(false);
        }
    }

    /**
     * Creates booking request with consistent amount calculation.
     * This amount becomes the single source of truth for the entire booking flow.
     *
     * @return BookingRequest with exact total amount for database storage
     */
    private BookingService.BookingRequest createBookingRequest() {
        RadioButton selectedClass = (RadioButton) classToggleGroup.getSelectedToggle();
        String seatClass = getClassCode(selectedClass.getText());

        // Use calculateTotalAmount method for consistency - this is the booking summary total
        double exactTotalAmount = calculateTotalAmount();

        BookingService.BookingRequest bookingRequest = new BookingService.BookingRequest();
        bookingRequest.setUserId(sessionManager.getCurrentUser().getUserId());
        bookingRequest.setTrainId(trainId);
        bookingRequest.setFromStation(fromStation);
        bookingRequest.setToStation(toStation);
        bookingRequest.setJourneyDate(journeyDate);
        bookingRequest.setSeatClass(seatClass);
        bookingRequest.setPassengerCount(passengerList.size());
        bookingRequest.setTotalAmount(exactTotalAmount); // This exact amount will be saved to DB

        List<BookingService.PassengerInfo> passengers = createPassengerInfoList();
        bookingRequest.setPassengers(passengers);

        return bookingRequest;
    }

    /**
     * Creates passenger information list for booking service.
     *
     * @return List of PassengerInfo objects for booking request
     */
    private List<BookingService.PassengerInfo> createPassengerInfoList() {
        List<BookingService.PassengerInfo> passengers = new ArrayList<>();
        for (PassengerData passengerData : passengerList) {
            BookingService.PassengerInfo passengerInfo = new BookingService.PassengerInfo();
            passengerInfo.setName(passengerData.getName());
            passengerInfo.setAge(passengerData.getAge());
            passengerInfo.setGender(passengerData.getGender());
            passengers.add(passengerInfo);
        }
        return passengers;
    }

    /**
     * Redirects to payment page with booking data and consistent amount.
     * The booking amount from database matches the booking summary amount shown to user.
     *
     * @param booking Created booking object
     * @param razorpayOrderId Razorpay order ID for payment processing
     */
    private void redirectToPaymentPage(Booking booking, String razorpayOrderId) {
        // The booking.getTotalFare() is what was saved to DB - it's the booking summary amount
        double bookingAmount = booking.getTotalFare();

        PaymentController paymentController = SceneManager.switchScene("/fxml/Payment.fxml");
        paymentController.setBookingData(booking, razorpayOrderId, bookingAmount);
    }

    /**
     * Sets payment processing state for UI feedback.
     *
     * @param processing true if processing, false otherwise
     */
    private void setPaymentProcessingState(boolean processing) {
        if (proceedToPaymentBtn != null) {
            proceedToPaymentBtn.setText(processing ? "Processing..." : "Proceed to Payment");
            proceedToPaymentBtn.setDisable(processing);
        }
    }

    /**
     * Converts class text to class code for database storage.
     *
     * @param classText Display text of the class
     * @return Class code for database
     */
    private String getClassCode(String classText) {
        if (classText == null) return "3A";

        return switch (classText) {
            case "Sleeper (SL)" -> "SL";
            case "AC 3-Tier (3A)" -> "3A";
            case "AC 2-Tier (2A)" -> "2A";
            case "AC First Class (1A)" -> "1A";
            default -> "3A";
        };
    }

    // =========================================================================
    // NAVIGATION HANDLERS
    // =========================================================================

    /**
     * Handles back navigation to train search.
     *
     * @param event Action event from back button
     */
    @FXML
    public void handleBack(ActionEvent event) {
        SceneManager.switchScene("/fxml/TrainSearch.fxml");
    }

    /**
     * Handles form clearing action.
     *
     * @param event Action event from clear form button
     */
    @FXML
    public void handleClearForm(ActionEvent event) {
        clearForm();
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    /**
     * Determines if a route is popular for surge pricing.
     *
     * @param from Source station
     * @param to Destination station
     * @return true if route is popular, false otherwise
     */
    private boolean isPopularRoute(String from, String to) {
        if (from == null || to == null) return false;
        String route = from.toLowerCase() + "-" + to.toLowerCase();
        return (route.contains("delhi") && route.contains("mumbai")) ||
                (route.contains("bangalore") && route.contains("chennai")) ||
                (route.contains("kolkata") && route.contains("delhi"));
    }

    /**
     * Shows message to user with appropriate styling.
     *
     * @param message Message text to display
     * @param type Message type ("success" or "error")
     */
    private void showMessage(String message, String type) {
        if (messageLabel == null) return;

        messageLabel.setText(message);
        messageLabel.setVisible(true);
        applyMessageStyling(type);

        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    if (messageLabel != null) messageLabel.setVisible(false);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Applies styling to message label based on type.
     *
     * @param type Message type for styling
     */
    private void applyMessageStyling(String type) {
        switch (type) {
            case "success":
                messageLabel.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: #059669; " +
                        "-fx-padding: 8px 12px; -fx-background-radius: 6px; " +
                        "-fx-border-color: #a7f3d0; -fx-border-width: 1px; " +
                        "-fx-border-radius: 6px; -fx-font-weight: bold;");
                break;
            case "error":
                messageLabel.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; " +
                        "-fx-padding: 8px 12px; -fx-background-radius: 6px; " +
                        "-fx-border-color: #fecaca; -fx-border-width: 1px; " +
                        "-fx-border-radius: 6px; -fx-font-weight: bold;");
                break;
        }
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    /**
     * Data class for passenger information storage.
     */
    public static class PassengerData {
        private final String name;
        private final int age;
        private final String gender;

        /**
         * Creates a new passenger data object.
         *
         * @param name Passenger name
         * @param age Passenger age
         * @param gender Passenger gender
         */
        public PassengerData(String name, int age, String gender) {
            this.name = name;
            this.age = age;
            this.gender = gender;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
        public String getGender() { return gender; }
    }

    /**
     * Data class for passenger input collection.
     */
    private static class PassengerInput {
        String name;
        String ageText;
        String gender;
        int age;

        PassengerInput(String name, String ageText, String gender) {
            this.name = name;
            this.ageText = ageText;
            this.gender = gender;
        }
    }

    /**
     * Data class for booking summary calculations.
     */
    private static class BookingSummary {
        final int passengerCount;
        final double ticketPrice;
        final double totalFare;
        final double totalAmount;

        BookingSummary(int passengerCount, double ticketPrice, double totalFare, double totalAmount) {
            this.passengerCount = passengerCount;
            this.ticketPrice = ticketPrice;
            this.totalFare = totalFare;
            this.totalAmount = totalAmount;
        }
    }

    /**
     * Consistency tracker for cross-controller data verification.
     * Ensures pricing and calculation consistency across search, booking, and payment controllers.
     */
    public static class ConsistencyTracker {
        private static final Map<String, ConsistencyData> dataStore = new HashMap<>();

        /**
         * Records calculation data for cross-controller consistency.
         *
         * @param trainNumber Train number identifier
         * @param route Route string (from-to)
         * @param distance Journey distance in kilometers
         * @param duration Journey duration string
         * @param prices Map of class prices
         */
        public static void recordCalculation(String trainNumber, String route,
                                             int distance, String duration,
                                             Map<String, Double> prices) {
            String key = trainNumber + "-" + route;
            ConsistencyData data = new ConsistencyData(distance, duration, new HashMap<>(prices));
            dataStore.put(key, data);
        }

        /**
         * Retrieves previously calculated data for consistency.
         *
         * @param trainNumber Train number identifier
         * @param route Route string (from-to)
         * @return ConsistencyData object or null if not found
         */
        public static ConsistencyData getCalculation(String trainNumber, String route) {
            return dataStore.get(trainNumber + "-" + route);
        }

        /**
         * Data container for consistent calculations.
         */
        public static class ConsistencyData {
            public final int distance;
            public final String duration;
            public final Map<String, Double> prices;

            public ConsistencyData(int distance, String duration, Map<String, Double> prices) {
                this.distance = distance;
                this.duration = duration;
                this.prices = prices;
            }
        }
    }
}