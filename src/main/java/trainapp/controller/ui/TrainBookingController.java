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
import trainapp.service.BookingService;
import trainapp.service.SessionManager;
import trainapp.service.TrainService;
import trainapp.util.SceneManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrainBookingController {

    // Header elements
    @FXML private Button backButton;
    @FXML private Label userLabel;
    @FXML private Label messageLabel;

    // Train details
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

    // Class selection
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

    // Passenger form
    @FXML private TextField passengerNameField;
    @FXML private TextField passengerAgeField;
    @FXML private RadioButton maleRadio;
    @FXML private RadioButton femaleRadio;
    @FXML private RadioButton otherRadio;
    @FXML private Button addPassengerBtn;
    @FXML private Button clearFormBtn;

    // Passenger list
    @FXML private VBox passengerListContainer;
    @FXML private Label passengerCountLabel;
    @FXML private Label totalAmountLabel;

    // Booking summary
    @FXML private Label journeySummaryLabel;
    @FXML private Label dateSummaryLabel;
    @FXML private Label selectedClassLabel;
    @FXML private Label summaryDistanceLabel;
    @FXML private Label summaryPassengerCountLabel;
    @FXML private Label perTicketPriceLabel;
    @FXML private Label summaryFareLabel;
    @FXML private Label summaryTotalLabel;
    @FXML private Button proceedToPaymentBtn;

    // Toggle groups
    private ToggleGroup classToggleGroup;
    private ToggleGroup genderToggleGroup;

    // Services and DAOs
    private SessionManager sessionManager = SessionManager.getInstance();
    private TrainService trainService = new TrainService();
    private TrainDAO trainDAO = new TrainDAO();
    private StationDAO stationDAO = new StationDAO();
    private BookingService bookingService = new BookingService();

    // Data
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

    // Pricing structure per km
    private final double SL_RATE_PER_KM = 0.50;
    private final double AC3_RATE_PER_KM = 1.20;
    private final double AC2_RATE_PER_KM = 1.80;
    private final double AC1_RATE_PER_KM = 3.50;

    @FXML
    public void initialize() {
        // Set user info
        if (sessionManager.isLoggedIn()) {
            userLabel.setText("Welcome, " + sessionManager.getCurrentUser().getName() + "!");
        }

        // Setup toggle groups
        setupToggleGroups();
        updateTotals();
    }

    private void setupToggleGroups() {
        // Class selection toggle group
        classToggleGroup = new ToggleGroup();
        slRadio.setToggleGroup(classToggleGroup);
        acRadio.setToggleGroup(classToggleGroup);
        ac2Radio.setToggleGroup(classToggleGroup);
        ac1Radio.setToggleGroup(classToggleGroup);

        // Gender toggle group
        genderToggleGroup = new ToggleGroup();
        maleRadio.setToggleGroup(genderToggleGroup);
        femaleRadio.setToggleGroup(genderToggleGroup);
        otherRadio.setToggleGroup(genderToggleGroup);

        // Set defaults
        acRadio.setSelected(true); // Default to 3A
        maleRadio.setSelected(true); // Default to Male

        // Add listeners for class selection
        classToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            updateClassSelection();
            updateTotals();
        });
    }

    public void setBookingData(int trainId, String trainNumber, String trainName,
                               String fromStation, String toStation, LocalDate journeyDate) {
        this.trainId = trainId;
        this.trainNumber = trainNumber;
        this.trainName = trainName;
        this.fromStation = fromStation;
        this.toStation = toStation;
        this.journeyDate = journeyDate;

        // Fetch train details
        try {
            this.currentTrain = trainDAO.getTrainById(trainId);
            this.distanceKm = trainService.getDistanceBetween(currentTrain, fromStation, toStation);
        } catch (Exception e) {
            e.printStackTrace();
            this.distanceKm = 1200; // Default fallback
        }

        updateTrainDetails();
        updateClassPricing();
        updateAvailableSeats();
    }

    public void setPendingBookingData(int trainId, String fromStation, String toStation, LocalDate journeyDate) {
        this.trainId = trainId;
        this.trainNumber = "12345"; // Would fetch from database
        this.trainName = "Express Train"; // Would fetch from database
        this.fromStation = fromStation;
        this.toStation = toStation;
        this.journeyDate = journeyDate;
        this.distanceKm = calculateDistance(fromStation, toStation);

        updateTrainDetails();
        updateClassPricing();
        updateAvailableSeats();
    }

    private void updateTrainDetails() {
        Platform.runLater(() -> {
            trainNumberLabel.setText(trainNumber);
            trainNameLabel.setText(trainName);
            fromStationLabel.setText(fromStation);
            toStationLabel.setText(toStation);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");
            departureDateLabel.setText(journeyDate.format(formatter));
            arrivalDateLabel.setText(journeyDate.format(formatter));

            // Set times (would come from train service)
            if (currentTrain != null) {
                departureTimeLabel.setText(trainService.getDepartureTime(currentTrain, fromStation));
                arrivalTimeLabel.setText(trainService.getArrivalTime(currentTrain, toStation));
                durationLabel.setText(trainService.calculateDuration(currentTrain, fromStation, toStation));
            } else {
                departureTimeLabel.setText("06:00");
                arrivalTimeLabel.setText("18:30");
                durationLabel.setText("12h 30m");
            }

            distanceLabel.setText(distanceKm + " km");
            journeySummaryLabel.setText(fromStation + " → " + toStation);
            dateSummaryLabel.setText(journeyDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            summaryDistanceLabel.setText(distanceKm + " km");
        });
    }

    private void updateClassPricing() {
        // Calculate prices based on distance
        double slPrice = Math.max(100, distanceKm * SL_RATE_PER_KM);
        double acPrice = Math.max(200, distanceKm * AC3_RATE_PER_KM);
        double ac2Price = Math.max(300, distanceKm * AC2_RATE_PER_KM);
        double ac1Price = Math.max(500, distanceKm * AC1_RATE_PER_KM);

        // Apply surge pricing for popular routes (optional)
        if (isPopularRoute(fromStation, toStation)) {
            slPrice *= 1.2;
            acPrice *= 1.15;
            ac2Price *= 1.1;
            ac1Price *= 1.05;
        }

        double finalSlPrice = slPrice;
        double finalAcPrice = acPrice;
        double finalAc2Price = ac2Price;
        double finalAc1Price = ac1Price;

        Platform.runLater(() -> {
            slPriceLabel.setText("₹" + String.format("%.0f", finalSlPrice));
            acPriceLabel.setText("₹" + String.format("%.0f", finalAcPrice));
            ac2PriceLabel.setText("₹" + String.format("%.0f", finalAc2Price));
            ac1PriceLabel.setText("₹" + String.format("%.0f", finalAc1Price));
        });
    }

    private void updateAvailableSeats() {
        if (currentTrain != null) {
            Map<String, Integer> seatMap = trainService.getAvailableSeatsForDate(currentTrain, journeyDate);
            Platform.runLater(() -> {
                updateSeatLabel(slSeatsLabel, seatMap.getOrDefault("SL", 0));
                updateSeatLabel(acSeatsLabel, seatMap.getOrDefault("3A", 0));
                updateSeatLabel(ac2SeatsLabel, seatMap.getOrDefault("2A", 0));
                updateSeatLabel(ac1SeatsLabel, seatMap.getOrDefault("1A", 0));
            });
        }
    }

    private void updateSeatLabel(Label label, int seats) {
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
            selectedClassLabel.setText(className);

            // Update visual selection
            resetClassCards();
            if (selected == slRadio) {
                slClassCard.getStyleClass().add("selected");
            } else if (selected == acRadio) {
                acClassCard.getStyleClass().add("selected");
            } else if (selected == ac2Radio) {
                ac2ClassCard.getStyleClass().add("selected");
            } else if (selected == ac1Radio) {
                ac1ClassCard.getStyleClass().add("selected");
            }
        }
    }

    private void resetClassCards() {
        slClassCard.getStyleClass().removeAll("selected");
        acClassCard.getStyleClass().removeAll("selected");
        ac2ClassCard.getStyleClass().removeAll("selected");
        ac1ClassCard.getStyleClass().removeAll("selected");
    }

    private double getCurrentTicketPrice() {
        RadioButton selected = (RadioButton) classToggleGroup.getSelectedToggle();
        if (selected == null) return 0;

        if (selected == slRadio) {
            return Math.max(100, distanceKm * SL_RATE_PER_KM);
        } else if (selected == acRadio) {
            return Math.max(200, distanceKm * AC3_RATE_PER_KM);
        } else if (selected == ac2Radio) {
            return Math.max(300, distanceKm * AC2_RATE_PER_KM);
        } else if (selected == ac1Radio) {
            return Math.max(500, distanceKm * AC1_RATE_PER_KM);
        }

        return 0;
    }

    @FXML
    public void handleViewDetails(ActionEvent event) {
        try {
            Alert detailsDialog = new Alert(Alert.AlertType.INFORMATION);
            detailsDialog.setTitle("Train Details");
            detailsDialog.setHeaderText(trainNumber + " - " + trainName);

            StringBuilder details = new StringBuilder();
            details.append("Journey Details:\n");
            details.append("From: ").append(fromStation).append(" at ").append(departureTimeLabel.getText()).append("\n");
            details.append("To: ").append(toStation).append(" at ").append(arrivalTimeLabel.getText()).append("\n");
            details.append("Duration: ").append(durationLabel.getText()).append("\n");
            details.append("Distance: ").append(distanceKm).append(" km\n\n");

            details.append("Class & Pricing:\n");
            details.append("Sleeper (SL): ").append(slPriceLabel.getText()).append(" - ").append(slSeatsLabel.getText()).append("\n");
            details.append("AC 3 Tier (3A): ").append(acPriceLabel.getText()).append(" - ").append(acSeatsLabel.getText()).append("\n");
            details.append("AC 2 Tier (2A): ").append(ac2PriceLabel.getText()).append(" - ").append(ac2SeatsLabel.getText()).append("\n");
            details.append("AC First (1A): ").append(ac1PriceLabel.getText()).append(" - ").append(ac1SeatsLabel.getText()).append("\n\n");

            if (currentTrain != null) {
                List<String> amenities = trainService.getTrainAmenities(currentTrain);
                if (!amenities.isEmpty()) {
                    details.append("Amenities:\n");
                    for (String amenity : amenities) {
                        details.append("• ").append(amenity).append("\n");
                    }
                }
            }

            detailsDialog.setContentText(details.toString());
            detailsDialog.getDialogPane().setPrefWidth(600);
            detailsDialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Failed to load train details: " + e.getMessage(), "error");
        }
    }

    @FXML
    public void handleAddPassenger(ActionEvent event) {
        String name = passengerNameField.getText().trim();
        String ageText = passengerAgeField.getText().trim();
        String gender = getSelectedGender();

        // Validation
        if (classToggleGroup.getSelectedToggle() == null) {
            showMessage("Please select a class first", "error");
            return;
        }

        if (name.isEmpty()) {
            showMessage("Please enter passenger name", "error");
            return;
        }

        if (ageText.isEmpty()) {
            showMessage("Please enter passenger age", "error");
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageText);
            if (age <= 0 || age > 120) {
                showMessage("Please enter a valid age (1-120)", "error");
                return;
            }
        } catch (NumberFormatException e) {
            showMessage("Please enter a valid age", "error");
            return;
        }

        if (gender == null || gender.isEmpty()) {
            showMessage("Please select gender", "error");
            return;
        }

        if (passengerList.size() >= 6) {
            showMessage("Maximum 6 passengers allowed per booking", "error");
            return;
        }

        // Add passenger
        PassengerData passenger = new PassengerData(name, age, gender);
        passengerList.add(passenger);

        // Update UI
        addPassengerToList(passenger);
        clearForm();
        updateTotals();
        showMessage("Passenger added successfully!", "success");
    }

    private void addPassengerToList(PassengerData passenger) {
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
        removeBtn.setOnAction(e -> {
            passengerList.remove(passenger);
            passengerListContainer.getChildren().remove(passengerItem);
            updateTotals();
            showMessage("Passenger removed", "success");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        passengerItem.getChildren().addAll(nameLabel, ageLabel, genderLabel, spacer, removeBtn);
        passengerListContainer.getChildren().add(passengerItem);
    }

    private void updateTotals() {
        int passengerCount = passengerList.size();
        double ticketPrice = getCurrentTicketPrice();
        double totalFare = passengerCount * ticketPrice;
        double totalAmount = totalFare + CONVENIENCE_FEE;

        Platform.runLater(() -> {
            passengerCountLabel.setText(String.valueOf(passengerCount));
            totalAmountLabel.setText("₹" + String.format("%.0f", totalAmount));
            summaryPassengerCountLabel.setText(String.valueOf(passengerCount));
            perTicketPriceLabel.setText("₹" + String.format("%.0f", ticketPrice));
            summaryFareLabel.setText("₹" + String.format("%.0f", totalFare));
            summaryTotalLabel.setText("₹" + String.format("%.0f", totalAmount));

            // Enable/disable payment button
            proceedToPaymentBtn.setDisable(passengerCount == 0 || classToggleGroup.getSelectedToggle() == null);
        });
    }

    // Helper methods
    private String getSelectedGender() {
        RadioButton selectedRadio = (RadioButton) genderToggleGroup.getSelectedToggle();
        return selectedRadio != null ? selectedRadio.getText() : null;
    }

    private void clearForm() {
        passengerNameField.clear();
        passengerAgeField.clear();
        maleRadio.setSelected(true);
    }

    private int calculateDistance(String from, String to) {
        // Simplified distance calculation - in real app, use actual station distances
        try {
            Station fromSt = stationDAO.getStationByName(from);
            Station toSt = stationDAO.getStationByName(to);
            if (fromSt != null && toSt != null) {
                // Calculate based on station coordinates or predefined distances
                return Math.abs(fromSt.getStationId() - toSt.getStationId()) * 50; // Simplified
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 1200; // Default
    }

    private boolean isPopularRoute(String from, String to) {
        // Define popular routes that have surge pricing
        String route = from.toLowerCase() + "-" + to.toLowerCase();
        return route.contains("delhi") && route.contains("mumbai") ||
                route.contains("bangalore") && route.contains("chennai") ||
                route.contains("kolkata") && route.contains("delhi");
    }

    @FXML
    public void handleProceedToPayment(ActionEvent event) {
        if (passengerList.isEmpty()) {
            showMessage("Please add at least one passenger", "error");
            return;
        }

        if (classToggleGroup.getSelectedToggle() == null) {
            showMessage("Please select a class", "error");
            return;
        }

        // Start booking process with payment integration
        proceedWithBookingAndPayment();
    }

    /**
     * Integrated booking and payment process
     */
    private void proceedWithBookingAndPayment() {
        try {
            // Show loading state
            proceedToPaymentBtn.setText("Processing...");
            proceedToPaymentBtn.setDisable(true);

            // Get selected class
            RadioButton selectedClass = (RadioButton) classToggleGroup.getSelectedToggle();
            String seatClass = getClassCode(selectedClass.getText());

            // Calculate total amount
            double ticketPrice = getCurrentTicketPrice();
            double totalFare = passengerList.size() * ticketPrice;
            double totalAmount = totalFare + CONVENIENCE_FEE;

            // Create booking request
            BookingService.BookingRequest bookingRequest = new BookingService.BookingRequest();
            bookingRequest.setUserId(sessionManager.getCurrentUser().getUserId());
            bookingRequest.setTrainId(trainId);
            bookingRequest.setFromStation(fromStation);
            bookingRequest.setToStation(toStation);
            bookingRequest.setJourneyDate(journeyDate);
            bookingRequest.setSeatClass(seatClass);
            bookingRequest.setPassengerCount(passengerList.size());
            bookingRequest.setTotalAmount(totalAmount);

            // Convert passenger data
            List<BookingService.PassengerInfo> passengers = new ArrayList<>();
            for (PassengerData passengerData : passengerList) {
                BookingService.PassengerInfo passengerInfo = new BookingService.PassengerInfo();
                passengerInfo.setName(passengerData.getName());
                passengerInfo.setAge(passengerData.getAge());
                passengerInfo.setGender(passengerData.getGender());
                passengers.add(passengerInfo);
            }
            bookingRequest.setPassengers(passengers);

            // Create booking and get Razorpay order
            BookingService.BookingResult result = bookingService.createBookingWithPayment(bookingRequest);

            if (result.isSuccess()) {
                // Redirect to payment page with booking and Razorpay order details
                redirectToPaymentPage(result.getBooking(), result.getRazorpayOrderId(), totalAmount);
            } else {
                showMessage("Booking failed: " + result.getMessage(), "error");
                resetPaymentButton();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Error creating booking: " + e.getMessage(), "error");
            resetPaymentButton();
        }
    }

    /**
     * Redirect to payment page with booking details
     */
    private void redirectToPaymentPage(Booking booking, String razorpayOrderId, double totalAmount) {
        PaymentController paymentController = SceneManager.switchScene("/fxml/Payment.fxml");
        paymentController.setBookingData(booking, razorpayOrderId, totalAmount);
        System.out.println("TrainBookingController: Redirected to payment page");
    }

    /**
     * Convert class display text to database code
     */
    private String getClassCode(String classText) {
        switch (classText) {
            case "Sleeper (SL)":
                return "SL";
            case "AC 3-Tier (3A)":
                return "3A";
            case "AC 2-Tier (2A)":
                return "2A";
            case "AC First Class (1A)":
                return "1A";
            default:
                return "3A"; // Default to 3A
        }
    }

    /**
     * Reset payment button state
     */
    private void resetPaymentButton() {
        proceedToPaymentBtn.setText("Proceed to Payment");
        proceedToPaymentBtn.setDisable(false);
    }

    private String generatePNR() {
        return "PNR" + System.currentTimeMillis() % 10000000L;
    }

    @FXML
    public void handleBack(ActionEvent event) {
            SceneManager.switchScene("/fxml/TrainSearch.fxml");
    }

    @FXML
    public void handleClearForm(ActionEvent event) {
        clearForm();
    }

    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);

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

        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> messageLabel.setVisible(false));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // Inner class for passenger data
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
}