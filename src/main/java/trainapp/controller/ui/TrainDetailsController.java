package trainapp.controller.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import trainapp.dao.StationDAO;
import trainapp.dao.TrainDAO;
import trainapp.model.Station;
import trainapp.model.Train;
import trainapp.model.TrainSchedule;
import trainapp.service.SessionManager;
import trainapp.service.TrainService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class TrainDetailsController {

    // Train Info Labels
    @FXML private Label trainNumberLabel;
    @FXML private Label trainNameLabel;
    @FXML private Label trainTypeLabel;
    @FXML private Label totalCoachesLabel;

    // Route Info Labels
    @FXML private Label departureTimeLabel;
    @FXML private Label departureStationLabel;
    @FXML private Label departureDateLabel;
    @FXML private Label arrivalTimeLabel;
    @FXML private Label arrivalStationLabel;
    @FXML private Label arrivalDateLabel;
    @FXML private Label durationLabel;
    @FXML private Label distanceLabel;
    @FXML private Label haltsLabel;

    // Seat Availability Labels
    @FXML private Label slAvailableLabel;
    @FXML private Label slPriceLabel;
    @FXML private Label threeAAvailableLabel;
    @FXML private Label threeAPriceLabel;
    @FXML private Label twoAAvailableLabel;
    @FXML private Label twoAPriceLabel;
    @FXML private Label firstAAvailableLabel;
    @FXML private Label firstAPriceLabel;

    // Schedule Table
    @FXML private TableView<ScheduleItem> scheduleTable;
    @FXML private TableColumn<ScheduleItem, String> stationNameCol;
    @FXML private TableColumn<ScheduleItem, String> arrivalTimeCol;
    @FXML private TableColumn<ScheduleItem, String> departureTimeCol;
    @FXML private TableColumn<ScheduleItem, String> haltTimeCol;
    @FXML private TableColumn<ScheduleItem, String> distanceCol;
    @FXML private TableColumn<ScheduleItem, String> dayCol;

    // Amenities
    @FXML private FlowPane amenitiesPane;

    // Services and Data
    private TrainService trainService = new TrainService();
    private TrainDAO trainDAO = new TrainDAO();
    private StationDAO stationDAO = new StationDAO();
    private SessionManager sessionManager = SessionManager.getInstance();

    private Train currentTrain;
    private LocalDate journeyDate;
    private String fromStation;
    private String toStation;

    @FXML
    public void initialize() {
        setupScheduleTable();
    }

    private void setupScheduleTable() {
        stationNameCol.setCellValueFactory(new PropertyValueFactory<>("stationName"));
        arrivalTimeCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        departureTimeCol.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
        haltTimeCol.setCellValueFactory(new PropertyValueFactory<>("haltTime"));
        distanceCol.setCellValueFactory(new PropertyValueFactory<>("distance"));
        dayCol.setCellValueFactory(new PropertyValueFactory<>("day"));
    }

    public void setTrainDetails(Train train, LocalDate journeyDate, String fromStation, String toStation) {
        this.currentTrain = train;
        this.journeyDate = journeyDate;
        this.fromStation = fromStation;
        this.toStation = toStation;

        Platform.runLater(this::loadTrainDetails);
    }

    private void loadTrainDetails() {
        try {
            // Load train information
            loadTrainInfo();

            // Load route information
            loadRouteInfo();

            // Load seat availability and pricing
            loadSeatAvailability();

            // Load complete schedule
            loadCompleteSchedule();

            // Load amenities
            loadAmenities();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load train details: " + e.getMessage());
        }
    }

    private void loadTrainInfo() {
        trainNumberLabel.setText(currentTrain.getTrainNumber());
        trainNameLabel.setText(currentTrain.getName());
        trainTypeLabel.setText(getTrainType(currentTrain));
        totalCoachesLabel.setText(String.valueOf(currentTrain.getTotalCoaches()));
    }

    private void loadRouteInfo() {
        try {
            // Get station names
            String sourceStationName = getStationNameById(currentTrain.getSourceStationId());
            String destStationName = getStationNameById(currentTrain.getDestinationStationId());

            // Set departure info
            departureStationLabel.setText(sourceStationName);
            departureTimeLabel.setText(trainService.getDepartureTime(currentTrain, fromStation));
            departureDateLabel.setText(journeyDate.format(DateTimeFormatter.ofPattern("dd MMM")));

            // Set arrival info
            arrivalStationLabel.setText(destStationName);
            arrivalTimeLabel.setText(trainService.getArrivalTime(currentTrain, toStation));
            arrivalDateLabel.setText(journeyDate.format(DateTimeFormatter.ofPattern("dd MMM")));

            // Set duration and distance
            durationLabel.setText(trainService.calculateDuration(currentTrain, fromStation, toStation));
            int distance = trainService.getDistanceBetween(currentTrain, fromStation, toStation);
            distanceLabel.setText(distance + " km");

            int halts = trainService.getHaltsBetween(currentTrain, fromStation, toStation);
            haltsLabel.setText(halts + " halts");

        } catch (Exception e) {
            e.printStackTrace();
            // Set default values
            departureStationLabel.setText(fromStation);
            arrivalStationLabel.setText(toStation);
            departureTimeLabel.setText("06:00");
            arrivalTimeLabel.setText("18:30");
            durationLabel.setText("12h 30m");
            distanceLabel.setText("1200 km");
            haltsLabel.setText("6 halts");
        }
    }

    private void loadSeatAvailability() {
        try {
            Map<String, Integer> seatMap = trainService.getAvailableSeatsForDate(currentTrain, journeyDate);
            int distance = trainService.getDistanceBetween(currentTrain, fromStation, toStation);

            // Calculate dynamic pricing
            double slPrice = Math.max(100, distance * 0.50);
            double ac3Price = Math.max(200, distance * 1.20);
            double ac2Price = Math.max(300, distance * 1.80);
            double ac1Price = Math.max(500, distance * 3.50);

            // Update SL
            int slSeats = seatMap.getOrDefault("SL", 0);
            slAvailableLabel.setText(String.valueOf(slSeats));
            slAvailableLabel.getStyleClass().removeAll("available", "waitlist");
            slAvailableLabel.getStyleClass().add(slSeats > 0 ? "available" : "waitlist");
            slPriceLabel.setText("₹" + String.format("%.0f", slPrice));

            // Update 3A
            int ac3Seats = seatMap.getOrDefault("3A", 0);
            threeAAvailableLabel.setText(String.valueOf(ac3Seats));
            threeAAvailableLabel.getStyleClass().removeAll("available", "waitlist");
            threeAAvailableLabel.getStyleClass().add(ac3Seats > 0 ? "available" : "waitlist");
            threeAPriceLabel.setText("₹" + String.format("%.0f", ac3Price));

            // Update 2A
            int ac2Seats = seatMap.getOrDefault("2A", 0);
            twoAAvailableLabel.setText(String.valueOf(ac2Seats));
            twoAAvailableLabel.getStyleClass().removeAll("available", "waitlist");
            twoAAvailableLabel.getStyleClass().add(ac2Seats > 0 ? "available" : "waitlist");
            twoAPriceLabel.setText("₹" + String.format("%.0f", ac2Price));

            // Update 1A
            int ac1Seats = seatMap.getOrDefault("1A", 0);
            firstAAvailableLabel.setText(String.valueOf(ac1Seats));
            firstAAvailableLabel.getStyleClass().removeAll("available", "waitlist");
            firstAAvailableLabel.getStyleClass().add(ac1Seats > 0 ? "available" : "waitlist");
            firstAPriceLabel.setText("₹" + String.format("%.0f", ac1Price));

        } catch (Exception e) {
            e.printStackTrace();
            // Set default values
            slAvailableLabel.setText("45");
            slPriceLabel.setText("₹450");
            threeAAvailableLabel.setText("28");
            threeAPriceLabel.setText("₹1,250");
            twoAAvailableLabel.setText("15");
            twoAPriceLabel.setText("₹1,850");
            firstAAvailableLabel.setText("8");
            firstAPriceLabel.setText("₹2,650");
        }
    }

    private void loadCompleteSchedule() {
        try {
            List<TrainSchedule> schedule = trainDAO.getTrainSchedule(currentTrain.getTrainId());
            ObservableList<ScheduleItem> scheduleItems = FXCollections.observableArrayList();

            int day = 1;
            for (TrainSchedule stop : schedule) {
                Station station = stationDAO.getStationById(stop.getStationId());
                String stationName = station != null ? station.getName() : "Unknown";

                ScheduleItem item = new ScheduleItem(
                        stationName,
                        stop.getArrivalTime() != null ? stop.getArrivalTime().toString() : "Start",
                        stop.getDepartureTime() != null ? stop.getDepartureTime().toString() : "End",
                        calculateHaltTime(stop),
                        String.valueOf(0),
                        String.valueOf(day)
                );
                scheduleItems.add(item);

                // Increment day for overnight journeys (simplified logic)
                if (stop.getDepartureTime() != null && stop.getDepartureTime().getHour() < 6) {
                    day++;
                }
            }

            scheduleTable.setItems(scheduleItems);

        } catch (Exception e) {
            e.printStackTrace();
            // Add sample data
            ObservableList<ScheduleItem> sampleSchedule = FXCollections.observableArrayList(
                    new ScheduleItem("New Delhi", "Start", "06:00", "0m", "0", "1"),
                    new ScheduleItem("Gwalior Jn", "08:45", "08:47", "2m", "319", "1"),
                    new ScheduleItem("Jhansi Jn", "10:03", "10:08", "5m", "415", "1"),
                    new ScheduleItem("Bhopal Jn", "13:05", "13:10", "5m", "707", "1"),
                    new ScheduleItem("Nagpur", "17:35", "17:45", "10m", "1061", "1"),
                    new ScheduleItem("Mumbai Central", "22:30", "End", "0m", "1384", "1")
            );
            scheduleTable.setItems(sampleSchedule);
        }
    }

    private void loadAmenities() {
        try {
            List<String> amenities = trainService.getTrainAmenities(currentTrain);
            amenitiesPane.getChildren().clear();

            for (String amenity : amenities) {
                Label amenityLabel = new Label(amenity);
                amenityLabel.getStyleClass().add("amenity-tag");
                amenitiesPane.getChildren().add(amenityLabel);
            }

            // Add some default amenities if none found
            if (amenities.isEmpty()) {
                String[] defaultAmenities = {
                        "🍽️ Catering", "🚿 Shower", "📶 WiFi", "❄️ AC",
                        "🔌 Charging Point", "📺 Entertainment", "🧳 Luggage Space"
                };
                for (String amenity : defaultAmenities) {
                    Label amenityLabel = new Label(amenity);
                    amenityLabel.getStyleClass().add("amenity-tag");
                    amenitiesPane.getChildren().add(amenityLabel);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSeatSelect(ActionEvent event) {
        // This will be handled by the individual seat cards
        Button sourceButton = (Button) event.getSource();

        // Determine which class was selected based on the button's parent
        String selectedClass = "Unknown";
        if (sourceButton.getParent().getId() != null) {
            switch (sourceButton.getParent().getId()) {
                case "slSeatCard":
                    selectedClass = "Sleeper (SL)";
                    break;
                case "threeASeatCard":
                    selectedClass = "AC 3-Tier (3A)";
                    break;
                case "twoASeatCard":
                    selectedClass = "AC 2-Tier (2A)";
                    break;
                case "firstASeatCard":
                    selectedClass = "First AC (1A)";
                    break;
            }
        }

        // Redirect to booking page with selected class
        redirectToBooking(selectedClass);
    }

    @FXML
    public void handleBookNow(ActionEvent event) {
        // Redirect to booking with default class (3A)
        redirectToBooking("AC 3-Tier (3A)");
    }

    @FXML
    public void handleBack(ActionEvent event) {
        // Close the details window
        Stage stage = (Stage) trainNumberLabel.getScene().getWindow();
        stage.close();
    }

    private void redirectToBooking(String selectedClass) {
        if (!sessionManager.isLoggedIn()) {
            // Store booking data and redirect to login
            sessionManager.setPendingBooking(currentTrain.getTrainId(), fromStation, toStation, journeyDate);
            redirectToLogin();
        } else {
            // Go directly to booking
            openBookingPage(selectedClass);
        }
    }

    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();

            trainapp.controller.ui.LoginController loginController = loader.getController();
            loginController.setLoginMessage("You need to login to book");
            loginController.setRedirectAfterLogin("/fxml/TrainBooking.fxml");

            Stage currentStage = (Stage) trainNumberLabel.getScene().getWindow();
            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(root));
            loginStage.setTitle("Login Required");
            loginStage.show();

            currentStage.close();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open login page");
        }
    }

    private void openBookingPage(String selectedClass) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TrainBooking.fxml"));
            Parent root = loader.load();

            trainapp.controller.ui.TrainBookingController bookingController = loader.getController();
            bookingController.setBookingData(
                    currentTrain.getTrainId(),
                    currentTrain.getTrainNumber(),
                    currentTrain.getName(),
                    fromStation,
                    toStation,
                    journeyDate
            );

            Stage currentStage = (Stage) trainNumberLabel.getScene().getWindow();
            Stage bookingStage = new Stage();
            bookingStage.setScene(new Scene(root));
            bookingStage.setTitle("Book Train Ticket");
            bookingStage.show();

            currentStage.close();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open booking page");
        }
    }

    // Helper methods
    private String getTrainType(Train train) {
        // Determine train type based on train number or name
        String number = train.getTrainNumber();
        if (number.startsWith("1") || number.startsWith("2")) {
            return "Superfast";
        } else if (number.startsWith("5") || number.startsWith("6")) {
            return "Passenger";
        } else {
            return "Express";
        }
    }

    private String getStationNameById(int stationId) {
        try {
            Station station = stationDAO.getStationById(stationId);
            return station != null ? station.getName() : "Unknown Station";
        } catch (Exception e) {
            return "Unknown Station";
        }
    }

    private String calculateHaltTime(TrainSchedule stop) {
        if (stop.getArrivalTime() == null || stop.getDepartureTime() == null) {
            return "0m";
        }

        long minutes = java.time.Duration.between(stop.getArrivalTime(), stop.getDepartureTime()).toMinutes();
        return minutes + "m";
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner class for schedule table data
    public static class ScheduleItem {
        private String stationName;
        private String arrivalTime;
        private String departureTime;
        private String haltTime;
        private String day;

        public ScheduleItem(String stationName, String arrivalTime, String departureTime,
                            String haltTime, String distance, String day) {
            this.stationName = stationName;
            this.arrivalTime = arrivalTime;
            this.departureTime = departureTime;
            this.haltTime = haltTime;
            this.day = day;
        }

        // Getters
        public String getStationName() { return stationName; }
        public String getArrivalTime() { return arrivalTime; }
        public String getDepartureTime() { return departureTime; }
        public String getHaltTime() { return haltTime; }
        public String getDay() { return day; }
    }
}