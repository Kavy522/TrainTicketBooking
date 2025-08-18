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
    private AdminDataStructureService adminService = new AdminDataStructureService();

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

    // ENHANCED UNIFIED PRICE CALCULATION METHOD
    /**
     * Calculate price for any train class using unified logic with enhanced debugging
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

            // Apply surge pricing consistently
            if (isPopularRoute(fromStation, toStation)) {
                double surgeMultiplier = 1.0;
                switch (trainClass) {
                    case SL: surgeMultiplier = 1.2; break;
                    case _3A: surgeMultiplier = 1.15; break;
                    case _2A: surgeMultiplier = 1.1; break;
                    case _1A: surgeMultiplier = 1.05; break;
                    default: surgeMultiplier = 1.0; break;
                }
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

    @FXML
    public void initialize() {
        if (sessionManager.isLoggedIn()) {
            userLabel.setText("Welcome, " + sessionManager.getCurrentUser().getName() + "!");
        }

        setupToggleGroups();
        setupClassCardClickHandlers();
        setupIndividualRadioListeners();
        Platform.runLater(this::updateTotals);
    }

    private void setupToggleGroups() {
        classToggleGroup = new ToggleGroup();
        slRadio.setToggleGroup(classToggleGroup);
        acRadio.setToggleGroup(classToggleGroup);
        ac2Radio.setToggleGroup(classToggleGroup);
        ac1Radio.setToggleGroup(classToggleGroup);

        genderToggleGroup = new ToggleGroup();
        maleRadio.setToggleGroup(genderToggleGroup);
        femaleRadio.setToggleGroup(genderToggleGroup);
        otherRadio.setToggleGroup(genderToggleGroup);

        acRadio.setSelected(true);
        maleRadio.setSelected(true);

        classToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                System.out.println("Toggle group changed to: " + ((RadioButton) newToggle).getText());
                updateClassSelection();
                updateTotals();
            }
        });
    }

    private void setupIndividualRadioListeners() {
        if (slRadio != null) {
            slRadio.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    System.out.println("SL Radio listener triggered");
                    Platform.runLater(() -> {
                        updateClassSelection();
                        updateTotals();
                    });
                }
            });
        }

        if (acRadio != null) {
            acRadio.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    System.out.println("3A Radio listener triggered");
                    Platform.runLater(() -> {
                        updateClassSelection();
                        updateTotals();
                    });
                }
            });
        }

        if (ac2Radio != null) {
            ac2Radio.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    System.out.println("2A Radio listener triggered");
                    Platform.runLater(() -> {
                        updateClassSelection();
                        updateTotals();
                    });
                }
            });
        }

        if (ac1Radio != null) {
            ac1Radio.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    System.out.println("1A Radio listener triggered");
                    Platform.runLater(() -> {
                        updateClassSelection();
                        updateTotals();
                    });
                }
            });
        }
    }

    private void setupClassCardClickHandlers() {
        if (slClassCard != null) {
            slClassCard.setOnMouseClicked(e -> {
                System.out.println("SL Card clicked");
                slRadio.setSelected(true);
            });
        }
        if (acClassCard != null) {
            acClassCard.setOnMouseClicked(e -> {
                System.out.println("3A Card clicked");
                acRadio.setSelected(true);
            });
        }
        if (ac2ClassCard != null) {
            ac2ClassCard.setOnMouseClicked(e -> {
                System.out.println("2A Card clicked");
                ac2Radio.setSelected(true);
            });
        }
        if (ac1ClassCard != null) {
            ac1ClassCard.setOnMouseClicked(e -> {
                System.out.println("1A Card clicked");
                ac1Radio.setSelected(true);
            });
        }
    }

    public void setBookingData(int trainId, String trainNumber, String trainName,
                               String fromStation, String toStation, LocalDate journeyDate) {
        this.trainId = trainId;
        this.trainNumber = trainNumber != null ? trainNumber : "";
        this.trainName = trainName != null ? trainName : "";
        this.fromStation = fromStation != null ? fromStation : "";
        this.toStation = toStation != null ? toStation : "";
        this.journeyDate = journeyDate;

        try {
            this.currentTrain = trainDAO.getTrainById(trainId);
            if (currentTrain != null) {
                this.distanceKm = trainService.getDistanceBetween(currentTrain, this.fromStation, this.toStation);
            } else {
                this.distanceKm = calculateDistance(this.fromStation, this.toStation);
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.distanceKm = 1200;
        }

        System.out.println("Booking data set - Distance: " + distanceKm + " km");
        updateTrainDetails();
        updateClassPricing();
        updateAvailableSeats();
        updateTotals();
    }

    public void setPendingBookingData(int trainId, String fromStation, String toStation, LocalDate journeyDate) {
        this.trainId = trainId;
        this.trainNumber = "12345";
        this.trainName = "Express Train";
        this.fromStation = fromStation != null ? fromStation : "";
        this.toStation = toStation != null ? toStation : "";
        this.journeyDate = journeyDate;
        this.distanceKm = calculateDistance(this.fromStation, this.toStation);

        updateTrainDetails();
        updateClassPricing();
        updateAvailableSeats();
        updateTotals();
    }

    private void updateTrainDetails() {
        if (journeyDate == null) return;

        Platform.runLater(() -> {
            if (trainNumberLabel != null) trainNumberLabel.setText(trainNumber);
            if (trainNameLabel != null) trainNameLabel.setText(trainName);
            if (fromStationLabel != null) fromStationLabel.setText(fromStation);
            if (toStationLabel != null) toStationLabel.setText(toStation);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");
            if (departureDateLabel != null) departureDateLabel.setText(journeyDate.format(formatter));
            if (arrivalDateLabel != null) arrivalDateLabel.setText(journeyDate.format(formatter));

            if (currentTrain != null && trainService != null) {
                if (departureTimeLabel != null) departureTimeLabel.setText(trainService.getDepartureTime(currentTrain, fromStation));
                if (arrivalTimeLabel != null) arrivalTimeLabel.setText(trainService.getArrivalTime(currentTrain, toStation));
                if (durationLabel != null) durationLabel.setText(trainService.calculateDuration(currentTrain, fromStation, toStation));
            } else {
                if (departureTimeLabel != null) departureTimeLabel.setText("06:00");
                if (arrivalTimeLabel != null) arrivalTimeLabel.setText("18:30");
                if (durationLabel != null) durationLabel.setText("12h 30m");
            }

            if (distanceLabel != null) distanceLabel.setText(distanceKm + " km");
            if (journeySummaryLabel != null) journeySummaryLabel.setText(fromStation + " → " + toStation);
            if (dateSummaryLabel != null) dateSummaryLabel.setText(journeyDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            if (summaryDistanceLabel != null) summaryDistanceLabel.setText(distanceKm + " km");
        });
    }

    /**
     * ENHANCED: Use unified calculation with detailed logging for all classes
     */
    private void updateClassPricing() {
        if (adminService == null) {
            System.err.println("Cannot update class pricing - adminService is null");
            return;
        }

        System.out.println("=== Updating Class Pricing ===");

        // Calculate prices for all classes
        double slPrice = calculatePriceForClass(TrainClass.SL);
        double ac3Price = calculatePriceForClass(TrainClass._3A);
        double ac2Price = calculatePriceForClass(TrainClass._2A);
        double ac1Price = calculatePriceForClass(TrainClass._1A);

        System.out.println("Calculated prices - SL: " + slPrice + ", 3A: " + ac3Price +
                ", 2A: " + ac2Price + ", 1A: " + ac1Price);

        Platform.runLater(() -> {
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
        });

        System.out.println("=== Class Pricing Update Complete ===");
    }

    private void updateAvailableSeats() {
        if (currentTrain != null && trainService != null && journeyDate != null) {
            try {
                Map<String, Integer> seatMap = trainService.getAvailableSeatsForDate(currentTrain, journeyDate);
                Platform.runLater(() -> {
                    updateSeatLabel(slSeatsLabel, seatMap.getOrDefault("SL", 0));
                    updateSeatLabel(acSeatsLabel, seatMap.getOrDefault("3A", 0));
                    updateSeatLabel(ac2SeatsLabel, seatMap.getOrDefault("2A", 0));
                    updateSeatLabel(ac1SeatsLabel, seatMap.getOrDefault("1A", 0));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
            System.out.println("Class selection updated to: " + className);
            if (selectedClassLabel != null) selectedClassLabel.setText(className);

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
    }

    private void resetClassCards() {
        if (slClassCard != null) slClassCard.getStyleClass().removeAll("selected");
        if (acClassCard != null) acClassCard.getStyleClass().removeAll("selected");
        if (ac2ClassCard != null) ac2ClassCard.getStyleClass().removeAll("selected");
        if (ac1ClassCard != null) ac1ClassCard.getStyleClass().removeAll("selected");
    }

    /**
     * ENHANCED: Use unified calculation with detailed logging for summary
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
     * FIXED: Updated to match actual radio button text from FXML
     */
    private TrainClass getTrainClassFromText(String classText) {
        if (classText == null) {
            System.err.println("Class text is null, defaulting to 3A");
            return TrainClass._3A;
        }

        TrainClass result;
        switch (classText) {
            case "Sleeper (SL)": result = TrainClass.SL; break;
            case "AC 3-Tier (3A)": result = TrainClass._3A; break;
            case "AC 3 Tier (3A)": result = TrainClass._3A; break; // Alternative format
            case "AC 2-Tier (2A)": result = TrainClass._2A; break;
            case "AC 2 Tier (2A)": result = TrainClass._2A; break; // FIXED: Added space version
            case "AC First Class (1A)": result = TrainClass._1A; break;
            case "AC 1st Class (1A)": result = TrainClass._1A; break; // Alternative format
            case "AC First (1A)": result = TrainClass._1A; break; // Alternative format
            default:
                System.err.println("Unknown class text: '" + classText + "', defaulting to 3A");
                result = TrainClass._3A;
                break;
        }

        System.out.println("Mapped '" + classText + "' to TrainClass." + result);
        return result;
    }

    private void updateTotals() {
        if (passengerList == null) passengerList = new ArrayList<>();

        int passengerCount = passengerList.size();
        double ticketPrice = getCurrentTicketPrice();
        double totalFare = passengerCount * ticketPrice;
        double totalAmount = totalFare + CONVENIENCE_FEE;

        System.out.println("=== Updating Totals ===");
        System.out.println("Passengers: " + passengerCount + ", Ticket Price: ₹" + ticketPrice +
                ", Total Fare: ₹" + totalFare + ", Total Amount: ₹" + totalAmount);

        Platform.runLater(() -> {
            if (passengerCountLabel != null) passengerCountLabel.setText(String.valueOf(passengerCount));
            if (totalAmountLabel != null) totalAmountLabel.setText("₹" + String.format("%.0f", totalAmount));
            if (summaryPassengerCountLabel != null) summaryPassengerCountLabel.setText(String.valueOf(passengerCount));
            if (perTicketPriceLabel != null) perTicketPriceLabel.setText("₹" + String.format("%.0f", ticketPrice));
            if (summaryFareLabel != null) summaryFareLabel.setText("₹" + String.format("%.0f", totalFare));
            if (summaryTotalLabel != null) summaryTotalLabel.setText("₹" + String.format("%.0f", totalAmount));

            if (proceedToPaymentBtn != null) {
                proceedToPaymentBtn.setDisable(passengerCount == 0 || classToggleGroup.getSelectedToggle() == null);
            }
        });

        System.out.println("=== Totals Update Complete ===");
    }

    @FXML
    public void handleAddPassenger(ActionEvent event) {
        String name = passengerNameField != null ? passengerNameField.getText().trim() : "";
        String ageText = passengerAgeField != null ? passengerAgeField.getText().trim() : "";
        String gender = getSelectedGender();

        if (classToggleGroup == null || classToggleGroup.getSelectedToggle() == null) {
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

        PassengerData passenger = new PassengerData(name, age, gender);
        passengerList.add(passenger);

        addPassengerToList(passenger);
        clearForm();
        updateTotals();
        showMessage("Passenger added successfully!", "success");
    }

    private void addPassengerToList(PassengerData passenger) {
        if (passengerListContainer == null) return;

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
        return 1200;
    }

    private boolean isPopularRoute(String from, String to) {
        if (from == null || to == null) return false;
        String route = from.toLowerCase() + "-" + to.toLowerCase();
        return (route.contains("delhi") && route.contains("mumbai")) ||
                (route.contains("bangalore") && route.contains("chennai")) ||
                (route.contains("kolkata") && route.contains("delhi"));
    }

    @FXML
    public void handleViewDetails(ActionEvent event) {
        try {
            Alert detailsDialog = new Alert(Alert.AlertType.INFORMATION);
            detailsDialog.setTitle("Train Details");
            detailsDialog.setHeaderText(trainNumber + " - " + trainName);
            StringBuilder details = new StringBuilder();
            details.append("Journey Details:\n");
            details.append("From: ").append(fromStation).append(" at ").append(departureTimeLabel != null ? departureTimeLabel.getText() : "06:00").append("\n");
            details.append("To: ").append(toStation).append(" at ").append(arrivalTimeLabel != null ? arrivalTimeLabel.getText() : "18:30").append("\n");
            details.append("Duration: ").append(durationLabel != null ? durationLabel.getText() : "12h 30m").append("\n");
            details.append("Distance: ").append(distanceKm).append(" km\n\n");
            details.append("Class & Pricing:\n");
            details.append("Sleeper (SL): ").append(slPriceLabel != null ? slPriceLabel.getText() : "₹0").append(" - ").append(slSeatsLabel != null ? slSeatsLabel.getText() : "N/A").append("\n");
            details.append("AC 3 Tier (3A): ").append(acPriceLabel != null ? acPriceLabel.getText() : "₹0").append(" - ").append(acSeatsLabel != null ? acSeatsLabel.getText() : "N/A").append("\n");
            details.append("AC 2 Tier (2A): ").append(ac2PriceLabel != null ? ac2PriceLabel.getText() : "₹0").append(" - ").append(ac2SeatsLabel != null ? ac2SeatsLabel.getText() : "N/A").append("\n");
            details.append("AC First (1A): ").append(ac1PriceLabel != null ? ac1PriceLabel.getText() : "₹0").append(" - ").append(ac1SeatsLabel != null ? ac1SeatsLabel.getText() : "N/A").append("\n\n");

            if (currentTrain != null && trainService != null) {
                List<String> amenities = trainService.getTrainAmenities(currentTrain);
                if (amenities != null && !amenities.isEmpty()) {
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
    public void handleProceedToPayment(ActionEvent event) {
        if (passengerList.isEmpty()) {
            showMessage("Please add at least one passenger", "error");
            return;
        }

        if (classToggleGroup == null || classToggleGroup.getSelectedToggle() == null) {
            showMessage("Please select a class", "error");
            return;
        }

        proceedWithBookingAndPayment();
    }

    private void proceedWithBookingAndPayment() {
        try {
            if (proceedToPaymentBtn != null) {
                proceedToPaymentBtn.setText("Processing...");
                proceedToPaymentBtn.setDisable(true);
            }

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

            List<BookingService.PassengerInfo> passengers = new ArrayList<>();
            for (PassengerData passengerData : passengerList) {
                BookingService.PassengerInfo passengerInfo = new BookingService.PassengerInfo();
                passengerInfo.setName(passengerData.getName());
                passengerInfo.setAge(passengerData.getAge());
                passengerInfo.setGender(passengerData.getGender());
                passengers.add(passengerInfo);
            }
            bookingRequest.setPassengers(passengers);

            BookingService.BookingResult result = bookingService.createBookingWithPayment(bookingRequest);

            if (result.isSuccess()) {
                redirectToPaymentPage(result.getBooking(), result.getRazorpayOrderId(), totalAmount);
            } else {
                showMessage("Booking failed: " + result.getMessage(), "error");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Error creating booking: " + e.getMessage(), "error");
        } finally {
            resetPaymentButton();
        }
    }

    private void redirectToPaymentPage(Booking booking, String razorpayOrderId, double totalAmount) {
        PaymentController paymentController = SceneManager.switchScene("/fxml/Payment.fxml");
        paymentController.setBookingData(booking, razorpayOrderId, totalAmount);
        System.out.println("TrainBookingController: Redirected to payment page");
    }

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

    private void resetPaymentButton() {
        if (proceedToPaymentBtn != null) {
            proceedToPaymentBtn.setText("Proceed to Payment");
            proceedToPaymentBtn.setDisable(false);
        }
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
        if (messageLabel == null) return;

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
                Platform.runLater(() -> {
                    if (messageLabel != null) messageLabel.setVisible(false);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

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