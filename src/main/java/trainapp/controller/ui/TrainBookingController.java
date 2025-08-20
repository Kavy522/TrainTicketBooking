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
 * TrainBookingController with consistent data calculations across all controllers.
 * Ensures same km, time, and pricing based on schedule timing.
 * FIXED: Guarantees booking summary amount = saved amount = payment amount = invoice amount
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
    private String departureTime = "";
    private String arrivalTime = "";
    private String duration = "";
    private final double CONVENIENCE_FEE = 20.0;

    // Consistent pricing data to share across controllers
    private Map<String, Double> consistentPricing = new HashMap<>();

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    @FXML
    public void initialize() {
        setupUserInterface();
        setupToggleGroups();
        setupClassCardClickHandlers();
        setupIndividualRadioListeners();
        Platform.runLater(this::updateTotals);
    }

    private void setupUserInterface() {
        if (sessionManager.isLoggedIn()) {
            userLabel.setText("Welcome, " + sessionManager.getCurrentUser().getName() + "!");
        }
    }

    private void setupToggleGroups() {
        configureClassToggleGroup();
        configureGenderToggleGroup();
        setDefaultSelections();
        setupToggleGroupListeners();
    }

    private void configureClassToggleGroup() {
        classToggleGroup = new ToggleGroup();
        slRadio.setToggleGroup(classToggleGroup);
        acRadio.setToggleGroup(classToggleGroup);
        ac2Radio.setToggleGroup(classToggleGroup);
        ac1Radio.setToggleGroup(classToggleGroup);
    }

    private void configureGenderToggleGroup() {
        genderToggleGroup = new ToggleGroup();
        maleRadio.setToggleGroup(genderToggleGroup);
        femaleRadio.setToggleGroup(genderToggleGroup);
        otherRadio.setToggleGroup(genderToggleGroup);
    }

    private void setDefaultSelections() {
        acRadio.setSelected(true);
        maleRadio.setSelected(true);
    }

    private void setupToggleGroupListeners() {
        classToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                System.out.println("Toggle group changed to: " + ((RadioButton) newToggle).getText());
                updateClassSelection();
                updateTotals();
            }
        });
    }

    private void setupIndividualRadioListeners() {
        setupRadioButtonListener(slRadio, "SL Radio");
        setupRadioButtonListener(acRadio, "3A Radio");
        setupRadioButtonListener(ac2Radio, "2A Radio");
        setupRadioButtonListener(ac1Radio, "1A Radio");
    }

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

    private void setupClassCardClickHandlers() {
        setupCardClickHandler(slClassCard, slRadio, "SL Card");
        setupCardClickHandler(acClassCard, acRadio, "3A Card");
        setupCardClickHandler(ac2ClassCard, ac2Radio, "2A Card");
        setupCardClickHandler(ac1ClassCard, ac1Radio, "1A Card");
    }

    private void setupCardClickHandler(VBox card, RadioButton radio, String debugName) {
        if (card != null) {
            card.setOnMouseClicked(e -> {
                System.out.println(debugName + " clicked");
                radio.setSelected(true);
            });
        }
    }

    // -------------------------------------------------------------------------
    // Booking Data Configuration with Consistent Calculations
    // -------------------------------------------------------------------------

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
     * This method ensures same km, time, and pricing data.
     */
    private void loadConsistentTrainData() {
        try {
            this.currentTrain = trainDAO.getTrainById(trainId);

            if (currentTrain != null) {
                // Calculate consistent distance using time-based method
                this.distanceKm = trainService.getDistanceBetween(currentTrain, this.fromStation, this.toStation);

                // Get consistent timing data
                this.departureTime = trainService.getDepartureTime(currentTrain, this.fromStation);
                this.arrivalTime = trainService.getArrivalTime(currentTrain, this.toStation);
                this.duration = trainService.calculateDuration(currentTrain, this.fromStation, this.toStation);

                // Calculate consistent pricing for all classes
                calculateConsistentPricing();

                System.out.println("=== TrainBooking - Consistent Data Loaded ===");
                System.out.println("Distance: " + distanceKm + " km");
                System.out.println("Departure: " + departureTime + ", Arrival: " + arrivalTime);
                System.out.println("Duration: " + duration);
                System.out.println("Prices: " + consistentPricing);

            } else {
                System.err.println("Train not found with ID: " + trainId);
                setFallbackData();
            }
        } catch (Exception e) {
            System.err.println("Error loading consistent train data: " + e.getMessage());
            e.printStackTrace();
            setFallbackData();
        }
    }

    /**
     * Calculates consistent pricing for all classes using same methodology.
     */
    private void calculateConsistentPricing() {
        // Use AdminDataStructureService for consistent pricing calculation
        double slPrice = adminService.calculateDynamicFare(TrainClass.SL, distanceKm);
        double ac3Price = adminService.calculateDynamicFare(TrainClass._3A, distanceKm);
        double ac2Price = adminService.calculateDynamicFare(TrainClass._2A, distanceKm);
        double ac1Price = adminService.calculateDynamicFare(TrainClass._1A, distanceKm);

        // Apply surge pricing if route is popular
        if (isPopularRoute(fromStation, toStation)) {
            slPrice *= 1.2;
            ac3Price *= 1.15;
            ac2Price *= 1.1;
            ac1Price *= 1.05;
            System.out.println("Applied surge pricing for popular route");
        }

        // Store consistent pricing
        consistentPricing.put("SL", slPrice);
        consistentPricing.put("3A", ac3Price);
        consistentPricing.put("2A", ac2Price);
        consistentPricing.put("1A", ac1Price);

        // Store in AdminService for cross-controller access
        ConsistencyTracker.recordCalculation(trainNumber, fromStation + "-" + toStation,
                distanceKm, duration, consistentPricing);
    }

    private void setFallbackData() {
        this.distanceKm = 350; // Fallback for cldy-nd route
        this.departureTime = "06:00";
        this.arrivalTime = "12:30";
        this.duration = "6h 30m";

        // Calculate fallback pricing
        calculateConsistentPricing();
    }

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

    private void initializeBookingInterface() {
        updateTrainDetails();
        updateClassPricing();
        updateAvailableSeats();
        updateTotals();
    }

    // -------------------------------------------------------------------------
    // Consistent Pricing Engine
    // -------------------------------------------------------------------------

    private double calculatePriceForClass(TrainClass trainClass) {
        String classKey = switch (trainClass) {
            case SL -> "SL";
            case _3A -> "3A";
            case _2A -> "2A";
            case _1A -> "1A";
        };

        double price = consistentPricing.getOrDefault(classKey, 0.0);
        System.out.println("TrainBooking - Retrieved consistent price: ₹" + price + " for " + trainClass);
        return price;
    }

    private double getCurrentTicketPrice() {
        if (classToggleGroup == null) {
            System.err.println("Cannot get current ticket price - missing dependencies");
            return 0;
        }

        RadioButton selected = (RadioButton) classToggleGroup.getSelectedToggle();
        if (selected == null) {
            System.err.println("No class selected");
            return 0;
        }

        TrainClass trainClass = getTrainClassFromText(selected.getText());
        return calculatePriceForClass(trainClass);
    }

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

    // -------------------------------------------------------------------------
    // FIXED: Consistent Amount Calculation - THE MOST IMPORTANT METHOD
    // -------------------------------------------------------------------------

    /**
     * FIXED: This method calculates the exact amount that will be saved to DB
     * and shown in payment/invoice. This is the single source of truth for amounts.
     */
    private double calculateTotalAmount() {
        double ticketPrice = getCurrentTicketPrice();
        double totalFare = passengerList.size() * ticketPrice;
        double totalAmount = totalFare + CONVENIENCE_FEE;

        // CRITICAL: Round to 2 decimal places for currency consistency
        totalAmount = Math.round(totalAmount * 100.0) / 100.0;

        System.out.println("=== CALCULATED BOOKING SUMMARY TOTAL ===");
        System.out.println("Ticket Price: ₹" + ticketPrice);
        System.out.println("Passengers: " + passengerList.size());
        System.out.println("Total Fare: ₹" + totalFare);
        System.out.println("Convenience Fee: ₹" + CONVENIENCE_FEE);
        System.out.println("FINAL TOTAL: ₹" + totalAmount);
        System.out.println("This amount will be saved to DB and used everywhere!");

        return totalAmount;
    }

    // -------------------------------------------------------------------------
    // Interface Updates with Consistent Data
    // -------------------------------------------------------------------------

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

    private void updateBasicTrainInformation() {
        if (trainNumberLabel != null) trainNumberLabel.setText(trainNumber);
        if (trainNameLabel != null) trainNameLabel.setText(trainName);
        if (fromStationLabel != null) fromStationLabel.setText(fromStation);
        if (toStationLabel != null) toStationLabel.setText(toStation);
    }

    private void updateJourneyDates() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");
        if (departureDateLabel != null) departureDateLabel.setText(journeyDate.format(formatter));
        if (arrivalDateLabel != null) arrivalDateLabel.setText(journeyDate.format(formatter));
    }

    /**
     * Updates timing information with consistent data across controllers.
     */
    private void updateConsistentTiming() {
        if (departureTimeLabel != null) departureTimeLabel.setText(departureTime);
        if (arrivalTimeLabel != null) arrivalTimeLabel.setText(arrivalTime);
        if (durationLabel != null) durationLabel.setText(duration);

        System.out.println("TrainBooking - Updated timing: Dep=" + departureTime +
                ", Arr=" + arrivalTime + ", Duration=" + duration);
    }

    private void updateDistanceInformation() {
        if (distanceLabel != null) distanceLabel.setText(distanceKm + " km");
        if (summaryDistanceLabel != null) summaryDistanceLabel.setText(distanceKm + " km");

        System.out.println("TrainBooking - Updated distance: " + distanceKm + " km");
    }

    private void updateSummaryLabels() {
        if (journeySummaryLabel != null) journeySummaryLabel.setText(fromStation + " → " + toStation);
        if (dateSummaryLabel != null) dateSummaryLabel.setText(journeyDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
    }

    /**
     * Updates class pricing with consistent data across all controllers.
     */
    private void updateClassPricing() {
        System.out.println("=== TrainBooking - Updating Class Pricing with Consistent Data ===");

        double slPrice = consistentPricing.get("SL");
        double ac3Price = consistentPricing.get("3A");
        double ac2Price = consistentPricing.get("2A");
        double ac1Price = consistentPricing.get("1A");

        System.out.println("Using consistent prices - SL: ₹" + slPrice + ", 3A: ₹" + ac3Price +
                ", 2A: ₹" + ac2Price + ", 1A: ₹" + ac1Price);

        Platform.runLater(() -> updatePriceLabels(slPrice, ac3Price, ac2Price, ac1Price));

        System.out.println("=== TrainBooking - Class Pricing Update Complete ===");
    }

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

    private void updateSeatLabels(Map<String, Integer> seatMap) {
        updateSeatLabel(slSeatsLabel, seatMap.getOrDefault("SL", 0));
        updateSeatLabel(acSeatsLabel, seatMap.getOrDefault("3A", 0));
        updateSeatLabel(ac2SeatsLabel, seatMap.getOrDefault("2A", 0));
        updateSeatLabel(ac1SeatsLabel, seatMap.getOrDefault("1A", 0));
    }

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

    private void updateClassSelection() {
        RadioButton selected = (RadioButton) classToggleGroup.getSelectedToggle();
        if (selected != null) {
            String className = selected.getText();
            if (selectedClassLabel != null) selectedClassLabel.setText(className);
            updateClassCardVisualStates(selected);
        }
    }

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

    private void resetClassCards() {
        if (slClassCard != null) slClassCard.getStyleClass().removeAll("selected");
        if (acClassCard != null) acClassCard.getStyleClass().removeAll("selected");
        if (ac2ClassCard != null) ac2ClassCard.getStyleClass().removeAll("selected");
        if (ac1ClassCard != null) ac1ClassCard.getStyleClass().removeAll("selected");
    }

    private void updateTotals() {
        if (passengerList == null) passengerList = new ArrayList<>();

        BookingSummary summary = calculateBookingSummary();
        Platform.runLater(() -> updateTotalLabels(summary));
    }

    // FIXED: Use consistent calculation for booking summary
    private BookingSummary calculateBookingSummary() {
        int passengerCount = passengerList.size();
        double ticketPrice = getCurrentTicketPrice();
        double totalFare = passengerCount * ticketPrice;
        double totalAmount = totalFare + CONVENIENCE_FEE;

        // Round for display consistency
        totalFare = Math.round(totalFare * 100.0) / 100.0;
        totalAmount = Math.round(totalAmount * 100.0) / 100.0;

        System.out.println("=== Booking Summary Calculated ===");
        System.out.println("Passengers: " + passengerCount + ", Ticket Price: ₹" + ticketPrice +
                ", Total Fare: ₹" + totalFare + ", Total Amount: ₹" + totalAmount);

        return new BookingSummary(passengerCount, ticketPrice, totalFare, totalAmount);
    }

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

    // -------------------------------------------------------------------------
    // Passenger Management
    // -------------------------------------------------------------------------

    @FXML
    public void handleAddPassenger(ActionEvent event) {
        PassengerInput input = collectPassengerInput();
        if (!validatePassengerInput(input)) return;
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

    private PassengerInput collectPassengerInput() {
        String name = passengerNameField != null ? passengerNameField.getText().trim() : "";
        String ageText = passengerAgeField != null ? passengerAgeField.getText().trim() : "";
        String gender = getSelectedGender();
        return new PassengerInput(name, ageText, gender);
    }

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

    private void addPassengerToList(PassengerData passenger) {
        if (passengerListContainer == null) return;
        HBox passengerItem = createPassengerItem(passenger);
        passengerListContainer.getChildren().add(passengerItem);
    }

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

    private void removePassenger(PassengerData passenger, HBox passengerItem) {
        passengerList.remove(passenger);
        passengerListContainer.getChildren().remove(passengerItem);
        updateTotals();
        showMessage("Passenger removed", "success");
    }

    private String getSelectedGender() {
        if (genderToggleGroup == null) return null;
        RadioButton selectedRadio = (RadioButton) genderToggleGroup.getSelectedToggle();
        return selectedRadio != null ? selectedRadio.getText() : null;
    }

    private void clearForm() {
        if (passengerNameField != null) passengerNameField.clear();
        if (passengerAgeField != null) passengerAgeField.clear();
        if (maleRadio != null) maleRadio.setSelected(true);
    }

    // -------------------------------------------------------------------------
    // Action Handlers
    // -------------------------------------------------------------------------

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
            e.printStackTrace();
            showMessage("Failed to load train details: " + e.getMessage(), "error");
        }
    }

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

    @FXML
    public void handleProceedToPayment(ActionEvent event) {
        if (!validateBookingRequirements()) return;
        proceedWithBookingAndPayment();
    }

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
            e.printStackTrace();
            showMessage("Error creating booking: " + e.getMessage(), "error");
        } finally {
            setPaymentProcessingState(false);
        }
    }

    // FIXED: Create booking request with consistent amount
    private BookingService.BookingRequest createBookingRequest() {
        RadioButton selectedClass = (RadioButton) classToggleGroup.getSelectedToggle();
        String seatClass = getClassCode(selectedClass.getText());

        // Use the calculateTotalAmount method for consistency - this is the booking summary total!
        double exactTotalAmount = calculateTotalAmount();

        BookingService.BookingRequest bookingRequest = new BookingService.BookingRequest();
        bookingRequest.setUserId(sessionManager.getCurrentUser().getUserId());
        bookingRequest.setTrainId(trainId);
        bookingRequest.setFromStation(fromStation);
        bookingRequest.setToStation(toStation);
        bookingRequest.setJourneyDate(journeyDate);
        bookingRequest.setSeatClass(seatClass);
        bookingRequest.setPassengerCount(passengerList.size());
        bookingRequest.setTotalAmount(exactTotalAmount); // This exact amount will be saved to DB!

        List<BookingService.PassengerInfo> passengers = createPassengerInfoList();
        bookingRequest.setPassengers(passengers);

        System.out.println("=== BOOKING REQUEST CREATED ===");
        System.out.println("Total amount being sent to service: ₹" + exactTotalAmount);
        System.out.println("This will be saved to database and used in payment/invoice");

        return bookingRequest;
    }

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

    // FIXED: Redirect with booking's saved amount (which is the booking summary amount)
    private void redirectToPaymentPage(Booking booking, String razorpayOrderId) {
        // The booking.getTotalFare() is what was saved to DB - it's the booking summary amount!
        double bookingAmount = booking.getTotalFare();

        System.out.println("=== REDIRECTING TO PAYMENT ===");
        System.out.println("Booking amount from DB: ₹" + bookingAmount);
        System.out.println("This matches the booking summary amount shown to user");

        PaymentController paymentController = SceneManager.switchScene("/fxml/Payment.fxml");
        paymentController.setBookingData(booking, razorpayOrderId, bookingAmount);

        System.out.println("Redirected to payment with consistent amount: ₹" + bookingAmount);
    }

    private void setPaymentProcessingState(boolean processing) {
        if (proceedToPaymentBtn != null) {
            proceedToPaymentBtn.setText(processing ? "Processing..." : "Proceed to Payment");
            proceedToPaymentBtn.setDisable(processing);
        }
    }

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

    // -------------------------------------------------------------------------
    // Navigation Handlers
    // -------------------------------------------------------------------------

    @FXML
    public void handleBack(ActionEvent event) {
        SceneManager.switchScene("/fxml/TrainSearch.fxml");
    }

    @FXML
    public void handleClearForm(ActionEvent event) {
        clearForm();
    }

    // -------------------------------------------------------------------------
    // Utility Methods
    // -------------------------------------------------------------------------

    private boolean isPopularRoute(String from, String to) {
        if (from == null || to == null) return false;
        String route = from.toLowerCase() + "-" + to.toLowerCase();
        return (route.contains("delhi") && route.contains("mumbai")) ||
                (route.contains("bangalore") && route.contains("chennai")) ||
                (route.contains("kolkata") && route.contains("delhi"));
    }

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
     */
    public static class ConsistencyTracker {
        private static final Map<String, ConsistencyData> dataStore = new HashMap<>();

        public static void recordCalculation(String trainNumber, String route,
                                             int distance, String duration,
                                             Map<String, Double> prices) {
            String key = trainNumber + "-" + route;
            ConsistencyData data = new ConsistencyData(distance, duration, new HashMap<>(prices));
            dataStore.put(key, data);

            System.out.println("ConsistencyTracker - Recorded: " + key +
                    " | Distance: " + distance + "km | Duration: " + duration +
                    " | Prices: " + prices);
        }

        public static ConsistencyData getCalculation(String trainNumber, String route) {
            return dataStore.get(trainNumber + "-" + route);
        }

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