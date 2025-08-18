package trainapp.controller.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import trainapp.dao.StationDAO;
import trainapp.dao.TrainDAO;
import trainapp.model.Station;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TrainBookingController manages comprehensive train booking operations with passenger management.
 * Provides advanced pricing calculations, dynamic class selection, and integrated payment processing.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Dynamic pricing with surge pricing and distance-based calculations</li>
 *   <li>Multi-passenger booking with validation and management</li>
 *   <li>Real-time seat availability display across all classes</li>
 *   <li>Interactive class selection with visual feedback</li>
 *   <li>Comprehensive booking summary with detailed fare breakdown</li>
 *   <li>Integrated payment gateway with secure booking creation</li>
 * </ul>
 *
 * <p>Pricing and Calculation Features:
 * <ul>
 *   <li>Unified pricing engine with TrainClass enumeration support</li>
 *   <li>Surge pricing for popular routes with class-specific multipliers</li>
 *   <li>Dynamic fare calculation using AdminDataStructureService</li>
 *   <li>Real-time total calculation with convenience fees</li>
 *   <li>Distance-based pricing with station lookup integration</li>
 * </ul>
 *
 * <p>User Experience Features:
 * <ul>
 *   <li>Interactive class cards with click handlers and visual states</li>
 *   <li>Real-time form validation with contextual error messages</li>
 *   <li>Passenger management with add/remove functionality</li>
 *   <li>Comprehensive train details display with amenities</li>
 *   <li>Seamless navigation to payment processing</li>
 * </ul>
 */
public class TrainBookingController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // Header Elements
    @FXML private Button backButton;
    @FXML private Label userLabel;
    @FXML private Label messageLabel;

    // Train Information Display
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

    // Passenger Management
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

    // -------------------------------------------------------------------------
    // Toggle Groups and UI Controls
    // -------------------------------------------------------------------------

    private ToggleGroup classToggleGroup;
    private ToggleGroup genderToggleGroup;

    // -------------------------------------------------------------------------
    // Services and Data Access
    // -------------------------------------------------------------------------

    private SessionManager sessionManager = SessionManager.getInstance();
    private TrainService trainService = new TrainService();
    private TrainDAO trainDAO = new TrainDAO();
    private StationDAO stationDAO = new StationDAO();
    private BookingService bookingService = new BookingService();
    private AdminDataStructureService adminService = new AdminDataStructureService();

    // -------------------------------------------------------------------------
    // Booking State and Configuration
    // -------------------------------------------------------------------------

    private List<PassengerData> passengerList = new ArrayList<>();
    private int trainId;
    private String trainNumber;
    private String trainName;
    private String fromStation;
    private String toStation;
    private LocalDate journeyDate;
    private Train currentTrain;
    private int distanceKm = 0;
    private final double CONVENIENCE_FEE = 20.0;

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the train booking interface with user authentication and UI setup.
     * Called automatically by JavaFX after FXML loading.
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
     * Sets up the user interface with welcome message for authenticated users.
     */
    private void setupUserInterface() {
        if (sessionManager.isLoggedIn()) {
            userLabel.setText("Welcome, " + sessionManager.getCurrentUser().getName() + "!");
        }
    }

    /**
     * Sets up toggle groups for class selection and gender selection.
     */
    private void setupToggleGroups() {
        configureClassToggleGroup();
        configureGenderToggleGroup();
        setDefaultSelections();
        setupToggleGroupListeners();
    }

    /**
     * Configures the class selection toggle group.
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
     */
    private void configureGenderToggleGroup() {
        genderToggleGroup = new ToggleGroup();
        maleRadio.setToggleGroup(genderToggleGroup);
        femaleRadio.setToggleGroup(genderToggleGroup);
        otherRadio.setToggleGroup(genderToggleGroup);
    }

    /**
     * Sets default selections for toggle groups.
     */
    private void setDefaultSelections() {
        acRadio.setSelected(true);
        maleRadio.setSelected(true);
    }

    /**
     * Sets up toggle group change listeners.
     */
    private void setupToggleGroupListeners() {
        classToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                System.out.println("Toggle group changed to: " + ((RadioButton) newToggle).getText());
                updateClassSelection();
                updateTotals();
            }
        });
    }

    /**
     * Sets up individual radio button listeners for enhanced responsiveness.
     */
    private void setupIndividualRadioListeners() {
        setupRadioButtonListener(slRadio, "SL Radio");
        setupRadioButtonListener(acRadio, "3A Radio");
        setupRadioButtonListener(ac2Radio, "2A Radio");
        setupRadioButtonListener(ac1Radio, "1A Radio");
    }

    /**
     * Sets up listener for individual radio button.
     *
     * @param radioButton the radio button to configure
     * @param debugName debug name for logging
     */
    private void setupRadioButtonListener(RadioButton radioButton, String debugName) {
        if (radioButton != null) {
            radioButton.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    System.out.println(debugName + " listener triggered");
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
     */
    private void setupClassCardClickHandlers() {
        setupCardClickHandler(slClassCard, slRadio, "SL Card");
        setupCardClickHandler(acClassCard, acRadio, "3A Card");
        setupCardClickHandler(ac2ClassCard, ac2Radio, "2A Card");
        setupCardClickHandler(ac1ClassCard, ac1Radio, "1A Card");
    }

    /**
     * Sets up click handler for individual class card.
     *
     * @param card the VBox card to configure
     * @param radio the corresponding radio button
     * @param debugName debug name for logging
     */
    private void setupCardClickHandler(VBox card, RadioButton radio, String debugName) {
        if (card != null) {
            card.setOnMouseClicked(e -> {
                System.out.println(debugName + " clicked");
                radio.setSelected(true);
            });
        }
    }

    // -------------------------------------------------------------------------
    // Booking Data Configuration
    // -------------------------------------------------------------------------

    /**
     * Sets comprehensive booking data and initializes the interface.
     *
     * @param trainId unique train identifier
     * @param trainNumber train number for display
     * @param trainName full train name
     * @param fromStation departure station name
     * @param toStation destination station name
     * @param journeyDate travel date
     */
    public void setBookingData(int trainId, String trainNumber, String trainName,
                               String fromStation, String toStation, LocalDate journeyDate) {
        this.trainId = trainId;
        this.trainNumber = trainNumber != null ? trainNumber : "";
        this.trainName = trainName != null ? trainName : "";
        this.fromStation = fromStation != null ? fromStation : "";
        this.toStation = toStation != null ? toStation : "";
        this.journeyDate = journeyDate;

        loadTrainDataAndCalculateDistance();
        initializeBookingInterface();
    }

    /**
     * Sets pending booking data for users redirected after authentication.
     *
     * @param trainId unique train identifier
     * @param fromStation departure station name
     * @param toStation destination station name
     * @param journeyDate travel date
     */
    public void setPendingBookingData(int trainId, String fromStation, String toStation, LocalDate journeyDate) {
        this.trainId = trainId;
        this.trainNumber = "12345";
        this.trainName = "Express Train";
        this.fromStation = fromStation != null ? fromStation : "";
        this.toStation = toStation != null ? toStation : "";
        this.journeyDate = journeyDate;
        this.distanceKm = calculateDistance(this.fromStation, this.toStation);

        initializeBookingInterface();
    }

    /**
     * Loads train data and calculates distance for pricing.
     */
    private void loadTrainDataAndCalculateDistance() {
        try {
            this.currentTrain = trainDAO.getTrainById(trainId);
            if (currentTrain != null) {
                this.distanceKm = trainService.getDistanceBetween(currentTrain, this.fromStation, this.toStation);
            } else {
                this.distanceKm = calculateDistance(this.fromStation, this.toStation);
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.distanceKm = 1200; // Fallback distance
        }

        System.out.println("Booking data set - Distance: " + distanceKm + " km");
    }

    /**
     * Initializes the booking interface with loaded data.
     */
    private void initializeBookingInterface() {
        updateTrainDetails();
        updateClassPricing();
        updateAvailableSeats();
        updateTotals();
    }

    // -------------------------------------------------------------------------
    // Pricing Engine and Calculations
    // -------------------------------------------------------------------------

    /**
     * Calculates price for any train class using unified pricing logic with surge pricing.
     * Provides comprehensive pricing with debugging and error handling.
     *
     * @param trainClass the train class to calculate price for
     * @return calculated price including surge pricing
     */
    private double calculatePriceForClass(TrainClass trainClass) {
        if (adminService == null) {
            System.err.println("AdminService is null!");
            return 0;
        }

        System.out.println("Calculating price for class: " + trainClass + ", distance: " + distanceKm);

        try {
            double basePrice = adminService.calculateDynamicFare(trainClass, distanceKm);
            System.out.println("Base price from admin service: " + basePrice + " for class " + trainClass);

            // Apply surge pricing for popular routes
            if (isPopularRoute(fromStation, toStation)) {
                double surgeMultiplier = getSurgeMultiplierForClass(trainClass);
                basePrice *= surgeMultiplier;
                System.out.println("Applied surge multiplier " + surgeMultiplier + ", final price: " + basePrice);
            }

            return basePrice;
        } catch (Exception e) {
            System.err.println("Error calculating price for class " + trainClass + ": " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Gets surge multiplier based on train class for popular routes.
     *
     * @param trainClass the train class
     * @return surge multiplier for the class
     */
    private double getSurgeMultiplierForClass(TrainClass trainClass) {
        switch (trainClass) {
            case SL: return 1.2;
            case _3A: return 1.15;
            case _2A: return 1.1;
            case _1A: return 1.05;
            default: return 1.0;
        }
    }

    /**
     * Gets current ticket price based on selected class.
     *
     * @return current ticket price or 0 if no class selected
     */
    private double getCurrentTicketPrice() {
        if (classToggleGroup == null || adminService == null) {
            System.err.println("Cannot get current ticket price - missing dependencies");
            return 0;
        }

        RadioButton selected = (RadioButton) classToggleGroup.getSelectedToggle();
        if (selected == null) {
            System.err.println("No class selected");
            return 0;
        }

        String classText = selected.getText();
        TrainClass trainClass = getTrainClassFromText(classText);
        double price = calculatePriceForClass(trainClass);

        System.out.println("Current ticket price for " + classText + " (" + trainClass + "): ₹" + price);
        return price;
    }

    /**
     * Maps radio button text to TrainClass enumeration.
     * Handles various text formats with fallback to 3A class.
     *
     * @param classText the text from radio button
     * @return corresponding TrainClass enum
     */
    private TrainClass getTrainClassFromText(String classText) {
        if (classText == null) {
            System.err.println("Class text is null, defaulting to 3A");
            return TrainClass._3A;
        }

        TrainClass result;
        switch (classText) {
            case "Sleeper (SL)":
                result = TrainClass.SL;
                break;
            case "AC 3-Tier (3A)":
            case "AC 3 Tier (3A)":
                result = TrainClass._3A;
                break;
            case "AC 2-Tier (2A)":
            case "AC 2 Tier (2A)":
                result = TrainClass._2A;
                break;
            case "AC First Class (1A)":
            case "AC 1st Class (1A)":
            case "AC First (1A)":
                result = TrainClass._1A;
                break;
            default:
                System.err.println("Unknown class text: '" + classText + "', defaulting to 3A");
                result = TrainClass._3A;
                break;
        }

        System.out.println("Mapped '" + classText + "' to TrainClass." + result);
        return result;
    }

    // -------------------------------------------------------------------------
    // Interface Updates and Display Management
    // -------------------------------------------------------------------------

    /**
     * Updates train details display with journey information.
     */
    private void updateTrainDetails() {
        if (journeyDate == null) return;

        Platform.runLater(() -> {
            updateBasicTrainInformation();
            updateJourneyDates();
            updateJourneyTiming();
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
     * Updates journey date labels with formatted dates.
     */
    private void updateJourneyDates() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");
        if (departureDateLabel != null) departureDateLabel.setText(journeyDate.format(formatter));
        if (arrivalDateLabel != null) arrivalDateLabel.setText(journeyDate.format(formatter));
    }

    /**
     * Updates journey timing information with train service data or defaults.
     */
    private void updateJourneyTiming() {
        if (currentTrain != null && trainService != null) {
            if (departureTimeLabel != null) departureTimeLabel.setText(trainService.getDepartureTime(currentTrain, fromStation));
            if (arrivalTimeLabel != null) arrivalTimeLabel.setText(trainService.getArrivalTime(currentTrain, toStation));
            if (durationLabel != null) durationLabel.setText(trainService.calculateDuration(currentTrain, fromStation, toStation));
        } else {
            setDefaultTimingInformation();
        }
    }

    /**
     * Sets default timing information when train service data is unavailable.
     */
    private void setDefaultTimingInformation() {
        if (departureTimeLabel != null) departureTimeLabel.setText("06:00");
        if (arrivalTimeLabel != null) arrivalTimeLabel.setText("18:30");
        if (durationLabel != null) durationLabel.setText("12h 30m");
    }

    /**
     * Updates distance information display.
     */
    private void updateDistanceInformation() {
        if (distanceLabel != null) distanceLabel.setText(distanceKm + " km");
        if (summaryDistanceLabel != null) summaryDistanceLabel.setText(distanceKm + " km");
    }

    /**
     * Updates booking summary labels.
     */
    private void updateSummaryLabels() {
        if (journeySummaryLabel != null) journeySummaryLabel.setText(fromStation + " → " + toStation);
        if (dateSummaryLabel != null) dateSummaryLabel.setText(journeyDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
    }

    /**
     * Updates class pricing display with unified calculation and detailed logging.
     */
    private void updateClassPricing() {
        if (adminService == null) {
            System.err.println("Cannot update class pricing - adminService is null");
            return;
        }

        System.out.println("=== Updating Class Pricing ===");

        double slPrice = calculatePriceForClass(TrainClass.SL);
        double ac3Price = calculatePriceForClass(TrainClass._3A);
        double ac2Price = calculatePriceForClass(TrainClass._2A);
        double ac1Price = calculatePriceForClass(TrainClass._1A);

        System.out.println("Calculated prices - SL: " + slPrice + ", 3A: " + ac3Price +
                ", 2A: " + ac2Price + ", 1A: " + ac1Price);

        Platform.runLater(() -> updatePriceLabels(slPrice, ac3Price, ac2Price, ac1Price));

        System.out.println("=== Class Pricing Update Complete ===");
    }

    /**
     * Updates price labels with calculated values.
     *
     * @param slPrice Sleeper class price
     * @param ac3Price AC 3-Tier price
     * @param ac2Price AC 2-Tier price
     * @param ac1Price AC First class price
     */
    private void updatePriceLabels(double slPrice, double ac3Price, double ac2Price, double ac1Price) {
        if (slPriceLabel != null) {
            slPriceLabel.setText("₹" + String.format("%.0f", slPrice));
            System.out.println("Updated SL label to: ₹" + String.format("%.0f", slPrice));
        }
        if (acPriceLabel != null) {
            acPriceLabel.setText("₹" + String.format("%.0f", ac3Price));
            System.out.println("Updated 3A label to: ₹" + String.format("%.0f", ac3Price));
        }
        if (ac2PriceLabel != null) {
            ac2PriceLabel.setText("₹" + String.format("%.0f", ac2Price));
            System.out.println("Updated 2A label to: ₹" + String.format("%.0f", ac2Price));
        }
        if (ac1PriceLabel != null) {
            ac1PriceLabel.setText("₹" + String.format("%.0f", ac1Price));
            System.out.println("Updated 1A label to: ₹" + String.format("%.0f", ac1Price));
        }
    }

    /**
     * Updates available seats display for all classes.
     */
    private void updateAvailableSeats() {
        if (currentTrain != null && trainService != null && journeyDate != null) {
            try {
                Map<String, Integer> seatMap = trainService.getAvailableSeatsForDate(currentTrain, journeyDate);
                Platform.runLater(() -> updateSeatLabels(seatMap));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Updates seat labels with availability information.
     *
     * @param seatMap map of class to available seats
     */
    private void updateSeatLabels(Map<String, Integer> seatMap) {
        updateSeatLabel(slSeatsLabel, seatMap.getOrDefault("SL", 0));
        updateSeatLabel(acSeatsLabel, seatMap.getOrDefault("3A", 0));
        updateSeatLabel(ac2SeatsLabel, seatMap.getOrDefault("2A", 0));
        updateSeatLabel(ac1SeatsLabel, seatMap.getOrDefault("1A", 0));
    }

    /**
     * Updates individual seat label with availability status and styling.
     *
     * @param label the label to update
     * @param seats number of available seats
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
     * Updates class selection visual state and labels.
     */
    private void updateClassSelection() {
        RadioButton selected = (RadioButton) classToggleGroup.getSelectedToggle();
        if (selected != null) {
            String className = selected.getText();
            System.out.println("Class selection updated to: " + className);
            if (selectedClassLabel != null) selectedClassLabel.setText(className);

            updateClassCardVisualStates(selected);
        }
    }

    /**
     * Updates visual states of class selection cards.
     *
     * @param selected the selected radio button
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
     * Updates total amounts and booking summary with unified calculation.
     */
    private void updateTotals() {
        if (passengerList == null) passengerList = new ArrayList<>();

        BookingSummary summary = calculateBookingSummary();
        Platform.runLater(() -> updateTotalLabels(summary));

        System.out.println("=== Totals Update Complete ===");
    }

    /**
     * Calculates comprehensive booking summary.
     *
     * @return BookingSummary with all calculated values
     */
    private BookingSummary calculateBookingSummary() {
        int passengerCount = passengerList.size();
        double ticketPrice = getCurrentTicketPrice();
        double totalFare = passengerCount * ticketPrice;
        double totalAmount = totalFare + CONVENIENCE_FEE;

        System.out.println("=== Updating Totals ===");
        System.out.println("Passengers: " + passengerCount + ", Ticket Price: ₹" + ticketPrice +
                ", Total Fare: ₹" + totalFare + ", Total Amount: ₹" + totalAmount);

        return new BookingSummary(passengerCount, ticketPrice, totalFare, totalAmount);
    }

    /**
     * Updates total amount labels with calculated summary.
     *
     * @param summary the calculated booking summary
     */
    private void updateTotalLabels(BookingSummary summary) {
        if (passengerCountLabel != null) passengerCountLabel.setText(String.valueOf(summary.passengerCount));
        if (totalAmountLabel != null) totalAmountLabel.setText("₹" + String.format("%.0f", summary.totalAmount));
        if (summaryPassengerCountLabel != null) summaryPassengerCountLabel.setText(String.valueOf(summary.passengerCount));
        if (perTicketPriceLabel != null) perTicketPriceLabel.setText("₹" + String.format("%.0f", summary.ticketPrice));
        if (summaryFareLabel != null) summaryFareLabel.setText("₹" + String.format("%.0f", summary.totalFare));
        if (summaryTotalLabel != null) summaryTotalLabel.setText("₹" + String.format("%.0f", summary.totalAmount));

        if (proceedToPaymentBtn != null) {
            proceedToPaymentBtn.setDisable(summary.passengerCount == 0 || classToggleGroup.getSelectedToggle() == null);
        }
    }

    // -------------------------------------------------------------------------
    // Passenger Management
    // -------------------------------------------------------------------------

    /**
     * Handles adding passenger with comprehensive validation.
     *
     * @param event ActionEvent from add passenger button
     */
    @FXML
    public void handleAddPassenger(ActionEvent event) {
        PassengerInput input = collectPassengerInput();

        if (!validatePassengerInput(input)) {
            return;
        }

        if (passengerList.size() >= 6) {
            showMessage("Maximum 6 passengers allowed per booking", "error");
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
     * @return PassengerInput with collected data
     */
    private PassengerInput collectPassengerInput() {
        String name = passengerNameField != null ? passengerNameField.getText().trim() : "";
        String ageText = passengerAgeField != null ? passengerAgeField.getText().trim() : "";
        String gender = getSelectedGender();

        return new PassengerInput(name, ageText, gender);
    }

    /**
     * Validates passenger input with detailed error checking.
     *
     * @param input the passenger input to validate
     * @return true if input is valid
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
     * Adds passenger to the visual list with remove functionality.
     *
     * @param passenger the passenger to add to display
     */
    private void addPassengerToList(PassengerData passenger) {
        if (passengerListContainer == null) return;

        HBox passengerItem = createPassengerItem(passenger);
        passengerListContainer.getChildren().add(passengerItem);
    }

    /**
     * Creates passenger item UI component with remove functionality.
     *
     * @param passenger the passenger data
     * @return configured HBox containing passenger information
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
     * Removes passenger from list and updates totals.
     *
     * @param passenger the passenger to remove
     * @param passengerItem the UI item to remove
     */
    private void removePassenger(PassengerData passenger, HBox passengerItem) {
        passengerList.remove(passenger);
        passengerListContainer.getChildren().remove(passengerItem);
        updateTotals();
        showMessage("Passenger removed", "success");
    }

    /**
     * Gets selected gender from toggle group.
     *
     * @return selected gender or null if none selected
     */
    private String getSelectedGender() {
        if (genderToggleGroup == null) return null;
        RadioButton selectedRadio = (RadioButton) genderToggleGroup.getSelectedToggle();
        return selectedRadio != null ? selectedRadio.getText() : null;
    }

    /**
     * Clears passenger form fields and resets to defaults.
     */
    private void clearForm() {
        if (passengerNameField != null) passengerNameField.clear();
        if (passengerAgeField != null) passengerAgeField.clear();
        if (maleRadio != null) maleRadio.setSelected(true);
    }

    // -------------------------------------------------------------------------
    // Action Handlers
    // -------------------------------------------------------------------------

    /**
     * Handles viewing detailed train information with comprehensive display.
     *
     * @param event ActionEvent from view details button
     */
    @FXML
    public void handleViewDetails(ActionEvent event) {
        try {
            Alert detailsDialog = new Alert(Alert.AlertType.INFORMATION);
            detailsDialog.setTitle("Train Details");
            detailsDialog.setHeaderText(trainNumber + " - " + trainName);

            String details = buildTrainDetailsText();
            detailsDialog.setContentText(details);
            detailsDialog.getDialogPane().setPrefWidth(600);
            detailsDialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Failed to load train details: " + e.getMessage(), "error");
        }
    }

    /**
     * Builds comprehensive train details text for display.
     *
     * @return formatted train details string
     */
    private String buildTrainDetailsText() {
        StringBuilder details = new StringBuilder();

        // Journey information
        details.append("Journey Details:\n");
        details.append("From: ").append(fromStation).append(" at ").append(departureTimeLabel != null ? departureTimeLabel.getText() : "06:00").append("\n");
        details.append("To: ").append(toStation).append(" at ").append(arrivalTimeLabel != null ? arrivalTimeLabel.getText() : "18:30").append("\n");
        details.append("Duration: ").append(durationLabel != null ? durationLabel.getText() : "12h 30m").append("\n");
        details.append("Distance: ").append(distanceKm).append(" km\n\n");

        // Class and pricing information
        details.append("Class & Pricing:\n");
        details.append("Sleeper (SL): ").append(slPriceLabel != null ? slPriceLabel.getText() : "₹0").append(" - ").append(slSeatsLabel != null ? slSeatsLabel.getText() : "N/A").append("\n");
        details.append("AC 3 Tier (3A): ").append(acPriceLabel != null ? acPriceLabel.getText() : "₹0").append(" - ").append(acSeatsLabel != null ? acSeatsLabel.getText() : "N/A").append("\n");
        details.append("AC 2 Tier (2A): ").append(ac2PriceLabel != null ? ac2PriceLabel.getText() : "₹0").append(" - ").append(ac2SeatsLabel != null ? ac2SeatsLabel.getText() : "N/A").append("\n");
        details.append("AC First (1A): ").append(ac1PriceLabel != null ? ac1PriceLabel.getText() : "₹0").append(" - ").append(ac1SeatsLabel != null ? ac1SeatsLabel.getText() : "N/A").append("\n\n");

        // Amenities if available
        if (currentTrain != null && trainService != null) {
            List<String> amenities = trainService.getTrainAmenities(currentTrain);
            if (amenities != null && !amenities.isEmpty()) {
                details.append("Amenities:\n");
                for (String amenity : amenities) {
                    details.append("• ").append(amenity).append("\n");
                }
            }
        }

        return details.toString();
    }

    /**
     * Handles proceeding to payment with validation and booking creation.
     *
     * @param event ActionEvent from proceed to payment button
     */
    @FXML
    public void handleProceedToPayment(ActionEvent event) {
        if (!validateBookingRequirements()) {
            return;
        }

        proceedWithBookingAndPayment();
    }

    /**
     * Validates booking requirements before proceeding to payment.
     *
     * @return true if all requirements are met
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
     * Proceeds with booking creation and payment initialization.
     */
    private void proceedWithBookingAndPayment() {
        try {
            setPaymentProcessingState(true);

            BookingService.BookingRequest bookingRequest = createBookingRequest();
            BookingService.BookingResult result = bookingService.createBookingWithPayment(bookingRequest);

            if (result.isSuccess()) {
                double totalAmount = calculateTotalAmount();
                redirectToPaymentPage(result.getBooking(), result.getRazorpayOrderId(), totalAmount);
            } else {
                showMessage("Booking failed: " + result.getMessage(), "error");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Error creating booking: " + e.getMessage(), "error");
        } finally {
            setPaymentProcessingState(false);
        }
    }

    /**
     * Creates comprehensive booking request from current form data.
     *
     * @return configured BookingRequest
     */
    private BookingService.BookingRequest createBookingRequest() {
        RadioButton selectedClass = (RadioButton) classToggleGroup.getSelectedToggle();
        String seatClass = getClassCode(selectedClass.getText());

        double ticketPrice = getCurrentTicketPrice();
        double totalFare = passengerList.size() * ticketPrice;
        double totalAmount = totalFare + CONVENIENCE_FEE;

        BookingService.BookingRequest bookingRequest = new BookingService.BookingRequest();
        bookingRequest.setUserId(sessionManager.getCurrentUser().getUserId());
        bookingRequest.setTrainId(trainId);
        bookingRequest.setFromStation(fromStation);
        bookingRequest.setToStation(toStation);
        bookingRequest.setJourneyDate(journeyDate);
        bookingRequest.setSeatClass(seatClass);
        bookingRequest.setPassengerCount(passengerList.size());
        bookingRequest.setTotalAmount(totalAmount);

        List<BookingService.PassengerInfo> passengers = createPassengerInfoList();
        bookingRequest.setPassengers(passengers);

        return bookingRequest;
    }

    /**
     * Creates passenger info list for booking request.
     *
     * @return list of PassengerInfo objects
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
     * Calculates total amount including convenience fees.
     *
     * @return total amount for payment
     */
    private double calculateTotalAmount() {
        double ticketPrice = getCurrentTicketPrice();
        double totalFare = passengerList.size() * ticketPrice;
        return totalFare + CONVENIENCE_FEE;
    }

    /**
     * Redirects to payment page with booking and payment data.
     *
     * @param booking the created booking
     * @param razorpayOrderId the Razorpay order ID
     * @param totalAmount the total payment amount
     */
    private void redirectToPaymentPage(Booking booking, String razorpayOrderId, double totalAmount) {
        PaymentController paymentController = SceneManager.switchScene("/fxml/Payment.fxml");
        paymentController.setBookingData(booking, razorpayOrderId, totalAmount);
        System.out.println("TrainBookingController: Redirected to payment page");
    }

    /**
     * Sets payment processing state with button feedback.
     *
     * @param processing true to show processing state
     */
    private void setPaymentProcessingState(boolean processing) {
        if (proceedToPaymentBtn != null) {
            proceedToPaymentBtn.setText(processing ? "Processing..." : "Proceed to Payment");
            proceedToPaymentBtn.setDisable(processing);
        }
    }

    /**
     * Maps class text to class code for booking system.
     *
     * @param classText the class text from radio button
     * @return class code for booking
     */
    private String getClassCode(String classText) {
        if (classText == null) return "3A";

        switch (classText) {
            case "Sleeper (SL)": return "SL";
            case "AC 3-Tier (3A)": return "3A";
            case "AC 2-Tier (2A)": return "2A";
            case "AC First Class (1A)": return "1A";
            default: return "3A";
        }
    }

    // -------------------------------------------------------------------------
    // Navigation Handlers
    // -------------------------------------------------------------------------

    /**
     * Handles navigation back to train search results.
     *
     * @param event ActionEvent from back button
     */
    @FXML
    public void handleBack(ActionEvent event) {
        SceneManager.switchScene("/fxml/TrainSearch.fxml");
    }

    /**
     * Handles clearing passenger form.
     *
     * @param event ActionEvent from clear form button
     */
    @FXML
    public void handleClearForm(ActionEvent event) {
        clearForm();
    }

    // -------------------------------------------------------------------------
    // Utility Methods
    // -------------------------------------------------------------------------

    /**
     * Calculates distance between stations with fallback logic.
     *
     * @param from departure station name
     * @param to destination station name
     * @return calculated distance in kilometers
     */
    private int calculateDistance(String from, String to) {
        if (from == null || to == null || stationDAO == null) return 1200;

        try {
            Station fromSt = stationDAO.getStationByName(from);
            Station toSt = stationDAO.getStationByName(to);
            if (fromSt != null && toSt != null) {
                return Math.abs(fromSt.getStationId() - toSt.getStationId()) * 50;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1200; // Fallback distance
    }

    /**
     * Determines if route is popular for surge pricing application.
     *
     * @param from departure station
     * @param to destination station
     * @return true if route is popular
     */
    private boolean isPopularRoute(String from, String to) {
        if (from == null || to == null) return false;
        String route = from.toLowerCase() + "-" + to.toLowerCase();
        return (route.contains("delhi") && route.contains("mumbai")) ||
                (route.contains("bangalore") && route.contains("chennai")) ||
                (route.contains("kolkata") && route.contains("delhi"));
    }

    /**
     * Displays status message with styling and auto-hide functionality.
     *
     * @param message the message to display
     * @param type the message type for styling
     */
    private void showMessage(String message, String type) {
        if (messageLabel == null) return;

        messageLabel.setText(message);
        messageLabel.setVisible(true);
        applyMessageStyling(type);

        // Auto-hide after 3 seconds
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
     * Applies styling to message label based on message type.
     *
     * @param type the message type
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

    // -------------------------------------------------------------------------
    // Inner Classes for Data Management
    // -------------------------------------------------------------------------

    /**
     * PassengerData holds passenger information for booking.
     */
    public static class PassengerData {
        private String name;
        private int age;
        private String gender;

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
     * PassengerInput holds form input data for validation.
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
     * BookingSummary holds calculated booking totals.
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
}