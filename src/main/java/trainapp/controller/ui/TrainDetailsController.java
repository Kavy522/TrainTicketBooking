package trainapp.controller.ui;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import trainapp.dao.StationDAO;
import trainapp.dao.TrainDAO;
import trainapp.model.Station;
import trainapp.model.Train;
import trainapp.model.TrainSchedule;
import trainapp.model.TrainClass;
import trainapp.service.SessionManager;
import trainapp.service.TrainService;
import trainapp.service.AdminDataStructureService;
import trainapp.util.SceneManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class TrainDetailsController {
    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------
    @FXML private Label trainNumberLabel;
    @FXML private Label trainNameLabel;
    @FXML private Label trainTypeLabel;
    @FXML private Label totalCoachesLabel;
    @FXML private Label departureTimeLabel;
    @FXML private Label departureStationLabel;
    @FXML private Label departureDateLabel;
    @FXML private Label arrivalTimeLabel;
    @FXML private Label arrivalStationLabel;
    @FXML private Label arrivalDateLabel;
    @FXML private Label durationLabel;
    @FXML private Label distanceLabel;
    @FXML private Label haltsLabel;
    @FXML private Label slAvailableLabel;
    @FXML private Label slPriceLabel;
    @FXML private Label threeAAvailableLabel;
    @FXML private Label threeAPriceLabel;
    @FXML private Label twoAAvailableLabel;
    @FXML private Label twoAPriceLabel;
    @FXML private Label firstAAvailableLabel;
    @FXML private Label firstAPriceLabel;
    @FXML private TableView<ScheduleItem> scheduleTable;
    @FXML private TableColumn<ScheduleItem, String> stationNameCol;
    @FXML private TableColumn<ScheduleItem, String> arrivalTimeCol;
    @FXML private TableColumn<ScheduleItem, String> departureTimeCol;
    @FXML private TableColumn<ScheduleItem, String> haltTimeCol;
    @FXML private TableColumn<ScheduleItem, String> dayCol;
    @FXML private FlowPane amenitiesPane;

    // -------------------------------------------------------------------------
    // Services and Data Access
    // -------------------------------------------------------------------------
    private TrainService trainService = new TrainService();
    private TrainDAO trainDAO = new TrainDAO();
    private StationDAO stationDAO = new StationDAO();
    private SessionManager sessionManager = SessionManager.getInstance();
    private AdminDataStructureService adminService = new AdminDataStructureService();

    // -------------------------------------------------------------------------
    // State Management
    // -------------------------------------------------------------------------
    private Train currentTrain;
    private LocalDate journeyDate;
    private String fromStation;
    private String toStation;
    private ConsistentTrainData consistentData;

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------
    @FXML
    public void initialize() {
        setupScheduleTable();
    }

    private void setupScheduleTable() {
        stationNameCol.setCellValueFactory(new PropertyValueFactory<>("stationName"));
        arrivalTimeCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        departureTimeCol.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
        haltTimeCol.setCellValueFactory(new PropertyValueFactory<>("haltTime"));
        dayCol.setCellValueFactory(new PropertyValueFactory<>("day"));
    }

    // -------------------------------------------------------------------------
    // Data Configuration and Loading with Consistency
    // -------------------------------------------------------------------------
    public void setTrainDetails(Train train, LocalDate journeyDate, String fromStation, String toStation) {
        this.currentTrain = train;
        this.journeyDate = journeyDate;
        this.fromStation = fromStation;
        this.toStation = toStation;
        loadConsistentData();
        Platform.runLater(this::loadTrainDetails);
    }

    private void loadConsistentData() {
        try {
            String route = fromStation + "-" + toStation;
            TrainBookingController.ConsistencyTracker.ConsistencyData cachedData =
                    TrainBookingController.ConsistencyTracker.getCalculation(currentTrain.getTrainNumber(), route);
            if (cachedData != null) {
                String departureTime = trainService.getDepartureTime(currentTrain, fromStation);
                String arrivalTime = trainService.getArrivalTime(currentTrain, toStation);
                String duration = trainService.calculateDuration(currentTrain, fromStation, toStation);
                this.consistentData = new ConsistentTrainData(
                        cachedData.distance, departureTime, arrivalTime, duration, cachedData.prices);
            } else {
                calculateNewConsistentData();
            }
        } catch (Exception e) {
            calculateNewConsistentData();
        }
    }

    private void calculateNewConsistentData() {
        try {
            int distance = trainService.getDistanceBetween(currentTrain, fromStation, toStation);
            String departureTime = trainService.getDepartureTime(currentTrain, fromStation);
            String arrivalTime = trainService.getArrivalTime(currentTrain, toStation);
            String duration = trainService.calculateDuration(currentTrain, fromStation, toStation);
            Map<String, Double> pricing = calculateConsistentPricing(distance);
            this.consistentData = new ConsistentTrainData(distance, departureTime, arrivalTime, duration, pricing);
            TrainBookingController.ConsistencyTracker.recordCalculation(
                    currentTrain.getTrainNumber(), fromStation + "-" + toStation, distance, duration, pricing);
        } catch (Exception e) {
            setFallbackData();
        }
    }

    private Map<String, Double> calculateConsistentPricing(int distance) {
        Map<String, Double> pricing = new java.util.HashMap<>();
        double slPrice = adminService.calculateDynamicFare(TrainClass.SL, distance);
        double ac3Price = adminService.calculateDynamicFare(TrainClass._3A, distance);
        double ac2Price = adminService.calculateDynamicFare(TrainClass._2A, distance);
        double ac1Price = adminService.calculateDynamicFare(TrainClass._1A, distance);
        if (isPopularRoute(fromStation, toStation)) {
            slPrice *= 1.2;
            ac3Price *= 1.15;
            ac2Price *= 1.1;
            ac1Price *= 1.05;
        }
        pricing.put("SL", slPrice);
        pricing.put("3A", ac3Price);
        pricing.put("2A", ac2Price);
        pricing.put("1A", ac1Price);
        return pricing;
    }

    private void setFallbackData() {
        Map<String, Double> fallbackPricing = new java.util.HashMap<>();
        fallbackPricing.put("SL", 295.0);
        fallbackPricing.put("3A", 840.0);
        fallbackPricing.put("2A", 1300.0);
        fallbackPricing.put("1A", 2050.0);
        this.consistentData = new ConsistentTrainData(350, "06:00", "12:30", "6h 30m", fallbackPricing);
    }

    private void loadTrainDetails() {
        try {
            loadTrainInfo();
            loadConsistentRouteInfo();
            loadConsistentSeatAvailability();
            loadCompleteSchedule();
            loadAmenities();
        } catch (Exception e) {
            showError("Failed to load train details: " + e.getMessage());
        }
    }
    private void loadTrainInfo() {
        trainNumberLabel.setText(currentTrain.getTrainNumber());
        trainNameLabel.setText(currentTrain.getName());
        trainTypeLabel.setText(getTrainType(currentTrain));
        totalCoachesLabel.setText(String.valueOf(currentTrain.getTotalCoaches()));
    }
    private void loadConsistentRouteInfo() {
        try {
            loadStationNames();
            loadConsistentJourneyTiming();
            loadConsistentDistanceAndHalts();
        } catch (Exception e) {
            setDefaultRouteInformation();
        }
    }
    private void loadStationNames() {
        String sourceStationName = getStationNameById(currentTrain.getSourceStationId());
        String destStationName = getStationNameById(currentTrain.getDestinationStationId());
        departureStationLabel.setText(sourceStationName);
        arrivalStationLabel.setText(destStationName);
    }
    private void loadConsistentJourneyTiming() {
        if (consistentData != null) {
            departureTimeLabel.setText(consistentData.departureTime);
            arrivalTimeLabel.setText(consistentData.arrivalTime);
            durationLabel.setText(consistentData.duration);
        } else {
            departureTimeLabel.setText("06:00");
            arrivalTimeLabel.setText("12:30");
            durationLabel.setText("6h 30m");
        }
        departureDateLabel.setText(journeyDate.format(DateTimeFormatter.ofPattern("dd MMM")));
        arrivalDateLabel.setText(journeyDate.format(DateTimeFormatter.ofPattern("dd MMM")));
    }
    private void loadConsistentDistanceAndHalts() {
        if (consistentData != null) {
            distanceLabel.setText(consistentData.distance + " km");
        } else {
            distanceLabel.setText("350 km");
        }
        int halts = trainService.getHaltsBetween(currentTrain, fromStation, toStation);
        haltsLabel.setText(halts + " halts");
    }
    private void setDefaultRouteInformation() {
        departureStationLabel.setText(fromStation);
        arrivalStationLabel.setText(toStation);
        departureTimeLabel.setText("06:00");
        arrivalTimeLabel.setText("12:30");
        durationLabel.setText("6h 30m");
        distanceLabel.setText("350 km");
        haltsLabel.setText("6 halts");
    }
    private void loadConsistentSeatAvailability() {
        try {
            Map<String, Integer> seatMap = trainService.getAvailableSeatsForDate(currentTrain, journeyDate);
            updateConsistentSeatAvailabilityDisplay(seatMap);
        } catch (Exception e) {
            setDefaultSeatAvailability();
        }
    }
    private void updateConsistentSeatAvailabilityDisplay(Map<String, Integer> seatMap) {
        if (consistentData != null && consistentData.pricing != null) {
            updateClassAvailability(slAvailableLabel, slPriceLabel, seatMap.getOrDefault("SL", 0), consistentData.pricing.get("SL"));
            updateClassAvailability(threeAAvailableLabel, threeAPriceLabel, seatMap.getOrDefault("3A", 0), consistentData.pricing.get("3A"));
            updateClassAvailability(twoAAvailableLabel, twoAPriceLabel, seatMap.getOrDefault("2A", 0), consistentData.pricing.get("2A"));
            updateClassAvailability(firstAAvailableLabel, firstAPriceLabel, seatMap.getOrDefault("1A", 0), consistentData.pricing.get("1A"));
        } else {
            setDefaultSeatAvailability();
        }
    }
    private void updateClassAvailability(Label availableLabel, Label priceLabel, int seats, Double price) {
        availableLabel.setText(String.valueOf(seats));
        availableLabel.getStyleClass().removeAll("available", "waitlist");
        availableLabel.getStyleClass().add(seats > 0 ? "available" : "waitlist");
        priceLabel.setText("₹" + String.format("%.0f", price != null ? price : 0.0));
    }
    private void setDefaultSeatAvailability() {
        slAvailableLabel.setText("45");
        slPriceLabel.setText("₹295");
        threeAAvailableLabel.setText("28");
        threeAPriceLabel.setText("₹840");
        twoAAvailableLabel.setText("15");
        twoAPriceLabel.setText("₹1300");
        firstAAvailableLabel.setText("8");
        firstAPriceLabel.setText("₹2050");
    }
    private void loadCompleteSchedule() {
        try {
            List<TrainSchedule> schedule = trainDAO.getTrainSchedule(currentTrain.getTrainId());
            ObservableList<ScheduleItem> scheduleItems = processTrainSchedule(schedule);
            scheduleTable.setItems(scheduleItems);
        } catch (Exception e) {
            loadSampleSchedule();
        }
    }
    private ObservableList<ScheduleItem> processTrainSchedule(List<TrainSchedule> schedule) {
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
                    String.valueOf(day)
            );
            scheduleItems.add(item);
            if (stop.getDepartureTime() != null && stop.getDepartureTime().getHour() < 6) {
                day++;
            }
        }
        return scheduleItems;
    }
    private void loadSampleSchedule() {
        ObservableList<ScheduleItem> sampleSchedule = FXCollections.observableArrayList(
                new ScheduleItem("New Delhi", "Start", "06:00", "0m", "1"),
                new ScheduleItem("Gwalior Jn", "08:45", "08:47", "2m", "1"),
                new ScheduleItem("Jhansi Jn", "10:03", "10:08", "5m", "1"),
                new ScheduleItem("Bhopal Jn", "13:05", "13:10", "5m", "1"),
                new ScheduleItem("Nagpur", "17:35", "17:45", "10m", "1"),
                new ScheduleItem("Mumbai Central", "22:30", "End", "0m", "1")
        );
        scheduleTable.setItems(sampleSchedule);
    }
    private void loadAmenities() {
        try {
            List<String> amenities = trainService.getTrainAmenities(currentTrain);
            displayAmenities(amenities);
        } catch (Exception e) { }
    }
    private void displayAmenities(List<String> amenities) {
        amenitiesPane.getChildren().clear();
        if (amenities == null || amenities.isEmpty()) {
            displayDefaultAmenities();
        } else {
            for (String amenity : amenities) {
                Label amenityLabel = createAmenityTag(amenity);
                amenitiesPane.getChildren().add(amenityLabel);
            }
        }
    }
    private void displayDefaultAmenities() {
        String[] defaultAmenities = {
                "🍽️ Catering", "🚿 Shower", "📶 WiFi", "❄️ AC",
                "🔌 Charging Point", "📺 Entertainment", "🧳 Luggage Space"
        };
        for (String amenity : defaultAmenities) {
            Label amenityLabel = createAmenityTag(amenity);
            amenitiesPane.getChildren().add(amenityLabel);
        }
    }
    private Label createAmenityTag(String amenity) {
        Label amenityLabel = new Label(amenity);
        amenityLabel.getStyleClass().add("amenity-chip");
        return amenityLabel;
    }
    @FXML
    public void handleSeatSelect(ActionEvent event) {
        Button sourceButton = (Button) event.getSource();
        String selectedClass = determineSelectedClass(sourceButton);
        redirectToBooking(selectedClass);
    }
    private String determineSelectedClass(Button sourceButton) {
        if (sourceButton.getParent().getId() != null) {
            switch (sourceButton.getParent().getId()) {
                case "slSeatCard":
                    return "Sleeper (SL)";
                case "threeASeatCard":
                    return "AC 3-Tier (3A)";
                case "twoASeatCard":
                    return "AC 2-Tier (2A)";
                case "firstASeatCard":
                    return "First AC (1A)";
            }
        }
        return "Unknown";
    }
    @FXML
    public void handleBookNow(ActionEvent event) {
        redirectToBooking("AC 3-Tier (3A)");
    }
    @FXML
    public void handleBack(ActionEvent event) {
        Stage stage = (Stage) trainNumberLabel.getScene().getWindow();
        stage.close();
    }
    private void redirectToBooking(String selectedClass) {
        if (!sessionManager.isLoggedIn()) {
            handleUnauthenticatedBooking();
        } else {
            openBookingPage(selectedClass);
        }
    }
    private void handleUnauthenticatedBooking() {
        sessionManager.setPendingBooking(currentTrain.getTrainId(), fromStation, toStation, journeyDate);
        redirectToLogin();
    }
    private void redirectToLogin() {
        LoginController loginController = SceneManager.switchScene("/fxml/Login.fxml");
        loginController.setLoginMessage("You need to login to book");
        loginController.setRedirectAfterLogin("/fxml/TrainBooking.fxml");
    }
    private void openBookingPage(String selectedClass) {
        TrainBookingController bookingController = SceneManager.switchScene("/fxml/TrainBooking.fxml");
        bookingController.setBookingData(
                currentTrain.getTrainId(),
                currentTrain.getTrainNumber(),
                currentTrain.getName(),
                fromStation,
                toStation,
                journeyDate
        );
    }
    private String getTrainType(Train train) {
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
    private boolean isPopularRoute(String from, String to) {
        if (from == null || to == null) return false;
        String route = from.toLowerCase() + "-" + to.toLowerCase();
        return (route.contains("delhi") && route.contains("mumbai")) ||
                (route.contains("bangalore") && route.contains("chennai")) ||
                (route.contains("kolkata") && route.contains("delhi"));
    }
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    public static class ScheduleItem {
        private String stationName;
        private String arrivalTime;
        private String departureTime;
        private String haltTime;
        private String day;
        public ScheduleItem(String stationName, String arrivalTime, String departureTime,
                            String haltTime, String day) {
            this.stationName = stationName;
            this.arrivalTime = arrivalTime;
            this.departureTime = departureTime;
            this.haltTime = haltTime;
            this.day = day;
        }
        public String getStationName() { return stationName; }
        public String getArrivalTime() { return arrivalTime; }
        public String getDepartureTime() { return departureTime; }
        public String getHaltTime() { return haltTime; }
        public String getDay() { return day; }
    }
    private static class ConsistentTrainData {
        public final int distance;
        public final String departureTime;
        public final String arrivalTime;
        public final String duration;
        public final Map<String, Double> pricing;
        public ConsistentTrainData(int distance, String departureTime, String arrivalTime,
                                   String duration, Map<String, Double> pricing) {
            this.distance = distance;
            this.departureTime = departureTime;
            this.arrivalTime = arrivalTime;
            this.duration = duration;
            this.pricing = pricing;
        }
    }
}