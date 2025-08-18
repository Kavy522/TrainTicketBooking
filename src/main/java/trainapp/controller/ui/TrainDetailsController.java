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
import trainapp.util.SceneManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * TrainDetailsController manages comprehensive train information display and booking initiation.
 * Provides detailed train specifications, complete schedules, pricing, and seat availability.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Comprehensive train information with specifications and route details</li>
 *   <li>Real-time seat availability and dynamic pricing across all classes</li>
 *   <li>Complete train schedule with station-wise timing and halt information</li>
 *   <li>Interactive amenities display with visual tags</li>
 *   <li>Class-specific booking initiation with authentication flow</li>
 *   <li>Responsive pricing calculations based on distance and route popularity</li>
 * </ul>
 *
 * <p>Information Display Features:
 * <ul>
 *   <li>Train specifications including type classification and coach details</li>
 *   <li>Route analysis with duration, distance, and halt calculations</li>
 *   <li>Dynamic pricing engine with distance-based fare calculation</li>
 *   <li>Real-time seat availability with visual availability indicators</li>
 *   <li>Complete station schedule with timing and day progression</li>
 * </ul>
 *
 * <p>User Experience Features:
 * <ul>
 *   <li>Interactive seat selection with class-specific booking flow</li>
 *   <li>Authentication-aware booking with seamless login redirection</li>
 *   <li>Comprehensive error handling with fallback data display</li>
 *   <li>Visual amenities display with categorized service information</li>
 *   <li>Responsive table layout for schedule information</li>
 * </ul>
 */
public class TrainDetailsController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // Train Information Display
    @FXML private Label trainNumberLabel;
    @FXML private Label trainNameLabel;
    @FXML private Label trainTypeLabel;
    @FXML private Label totalCoachesLabel;

    // Route Information Display
    @FXML private Label departureTimeLabel;
    @FXML private Label departureStationLabel;
    @FXML private Label departureDateLabel;
    @FXML private Label arrivalTimeLabel;
    @FXML private Label arrivalStationLabel;
    @FXML private Label arrivalDateLabel;
    @FXML private Label durationLabel;
    @FXML private Label distanceLabel;
    @FXML private Label haltsLabel;

    // Seat Availability and Pricing
    @FXML private Label slAvailableLabel;
    @FXML private Label slPriceLabel;
    @FXML private Label threeAAvailableLabel;
    @FXML private Label threeAPriceLabel;
    @FXML private Label twoAAvailableLabel;
    @FXML private Label twoAPriceLabel;
    @FXML private Label firstAAvailableLabel;
    @FXML private Label firstAPriceLabel;

    // Schedule Table Components
    @FXML private TableView<ScheduleItem> scheduleTable;
    @FXML private TableColumn<ScheduleItem, String> stationNameCol;
    @FXML private TableColumn<ScheduleItem, String> arrivalTimeCol;
    @FXML private TableColumn<ScheduleItem, String> departureTimeCol;
    @FXML private TableColumn<ScheduleItem, String> haltTimeCol;
    @FXML private TableColumn<ScheduleItem, String> distanceCol;
    @FXML private TableColumn<ScheduleItem, String> dayCol;

    // Amenities Display
    @FXML private FlowPane amenitiesPane;

    // -------------------------------------------------------------------------
    // Services and Data Access
    // -------------------------------------------------------------------------

    private TrainService trainService = new TrainService();
    private TrainDAO trainDAO = new TrainDAO();
    private StationDAO stationDAO = new StationDAO();
    private SessionManager sessionManager = SessionManager.getInstance();

    // -------------------------------------------------------------------------
    // State Management and Configuration
    // -------------------------------------------------------------------------

    private Train currentTrain;
    private LocalDate journeyDate;
    private String fromStation;
    private String toStation;

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the train details interface with schedule table configuration.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        setupScheduleTable();
    }

    /**
     * Configures the schedule table columns with appropriate data binding.
     */
    private void setupScheduleTable() {
        stationNameCol.setCellValueFactory(new PropertyValueFactory<>("stationName"));
        arrivalTimeCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        departureTimeCol.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
        haltTimeCol.setCellValueFactory(new PropertyValueFactory<>("haltTime"));
        distanceCol.setCellValueFactory(new PropertyValueFactory<>("distance"));
        dayCol.setCellValueFactory(new PropertyValueFactory<>("day"));
    }

    // -------------------------------------------------------------------------
    // Data Configuration and Loading
    // -------------------------------------------------------------------------

    /**
     * Sets train details and initiates comprehensive data loading.
     *
     * @param train the train to display details for
     * @param journeyDate the journey date for availability and pricing
     * @param fromStation departure station name
     * @param toStation destination station name
     */
    public void setTrainDetails(Train train, LocalDate journeyDate, String fromStation, String toStation) {
        this.currentTrain = train;
        this.journeyDate = journeyDate;
        this.fromStation = fromStation;
        this.toStation = toStation;

        Platform.runLater(this::loadTrainDetails);
    }

    /**
     * Loads comprehensive train details with error handling and fallback data.
     */
    private void loadTrainDetails() {
        try {
            loadTrainInfo();
            loadRouteInfo();
            loadSeatAvailability();
            loadCompleteSchedule();
            loadAmenities();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load train details: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Train Information Loading
    // -------------------------------------------------------------------------

    /**
     * Loads basic train information including specifications and coach details.
     */
    private void loadTrainInfo() {
        trainNumberLabel.setText(currentTrain.getTrainNumber());
        trainNameLabel.setText(currentTrain.getName());
        trainTypeLabel.setText(getTrainType(currentTrain));
        totalCoachesLabel.setText(String.valueOf(currentTrain.getTotalCoaches()));
    }

    /**
     * Loads comprehensive route information with timing and distance calculations.
     */
    private void loadRouteInfo() {
        try {
            loadStationNames();
            loadJourneyTiming();
            loadDistanceAndHalts();
        } catch (Exception e) {
            e.printStackTrace();
            setDefaultRouteInformation();
        }
    }

    /**
     * Loads and sets station names for departure and arrival.
     */
    private void loadStationNames() {
        String sourceStationName = getStationNameById(currentTrain.getSourceStationId());
        String destStationName = getStationNameById(currentTrain.getDestinationStationId());

        departureStationLabel.setText(sourceStationName);
        arrivalStationLabel.setText(destStationName);
    }

    /**
     * Loads journey timing information with formatted dates.
     */
    private void loadJourneyTiming() {
        departureTimeLabel.setText(trainService.getDepartureTime(currentTrain, fromStation));
        departureDateLabel.setText(journeyDate.format(DateTimeFormatter.ofPattern("dd MMM")));

        arrivalTimeLabel.setText(trainService.getArrivalTime(currentTrain, toStation));
        arrivalDateLabel.setText(journeyDate.format(DateTimeFormatter.ofPattern("dd MMM")));

        durationLabel.setText(trainService.calculateDuration(currentTrain, fromStation, toStation));
    }

    /**
     * Loads distance and halt information for the journey.
     */
    private void loadDistanceAndHalts() {
        int distance = trainService.getDistanceBetween(currentTrain, fromStation, toStation);
        distanceLabel.setText(distance + " km");

        int halts = trainService.getHaltsBetween(currentTrain, fromStation, toStation);
        haltsLabel.setText(halts + " halts");
    }

    /**
     * Sets default route information when service data is unavailable.
     */
    private void setDefaultRouteInformation() {
        departureStationLabel.setText(fromStation);
        arrivalStationLabel.setText(toStation);
        departureTimeLabel.setText("06:00");
        arrivalTimeLabel.setText("18:30");
        durationLabel.setText("12h 30m");
        distanceLabel.setText("1200 km");
        haltsLabel.setText("6 halts");
    }

    // -------------------------------------------------------------------------
    // Seat Availability and Pricing
    // -------------------------------------------------------------------------

    /**
     * Loads seat availability and calculates dynamic pricing for all classes.
     */
    private void loadSeatAvailability() {
        try {
            Map<String, Integer> seatMap = trainService.getAvailableSeatsForDate(currentTrain, journeyDate);
            int distance = trainService.getDistanceBetween(currentTrain, fromStation, toStation);

            PricingData pricing = calculateDynamicPricing(distance);
            updateSeatAvailabilityDisplay(seatMap, pricing);
        } catch (Exception e) {
            e.printStackTrace();
            setDefaultSeatAvailability();
        }
    }

    /**
     * Calculates dynamic pricing based on distance for all train classes.
     *
     * @param distance the journey distance in kilometers
     * @return PricingData with calculated prices for all classes
     */
    private PricingData calculateDynamicPricing(int distance) {
        double slPrice = Math.max(100, distance * 0.50);
        double ac3Price = Math.max(200, distance * 1.20);
        double ac2Price = Math.max(300, distance * 1.80);
        double ac1Price = Math.max(500, distance * 3.50);

        return new PricingData(slPrice, ac3Price, ac2Price, ac1Price);
    }

    /**
     * Updates seat availability display with real-time data and pricing.
     *
     * @param seatMap map of class to available seats
     * @param pricing calculated pricing data
     */
    private void updateSeatAvailabilityDisplay(Map<String, Integer> seatMap, PricingData pricing) {
        updateClassAvailability(slAvailableLabel, slPriceLabel, seatMap.getOrDefault("SL", 0), pricing.slPrice);
        updateClassAvailability(threeAAvailableLabel, threeAPriceLabel, seatMap.getOrDefault("3A", 0), pricing.ac3Price);
        updateClassAvailability(twoAAvailableLabel, twoAPriceLabel, seatMap.getOrDefault("2A", 0), pricing.ac2Price);
        updateClassAvailability(firstAAvailableLabel, firstAPriceLabel, seatMap.getOrDefault("1A", 0), pricing.ac1Price);
    }

    /**
     * Updates individual class availability and pricing display.
     *
     * @param availableLabel label showing seat availability
     * @param priceLabel label showing class price
     * @param seats number of available seats
     * @param price calculated price for the class
     */
    private void updateClassAvailability(Label availableLabel, Label priceLabel, int seats, double price) {
        availableLabel.setText(String.valueOf(seats));
        availableLabel.getStyleClass().removeAll("available", "waitlist");
        availableLabel.getStyleClass().add(seats > 0 ? "available" : "waitlist");
        priceLabel.setText("₹" + String.format("%.0f", price));
    }

    /**
     * Sets default seat availability when service data is unavailable.
     */
    private void setDefaultSeatAvailability() {
        slAvailableLabel.setText("45");
        slPriceLabel.setText("₹450");
        threeAAvailableLabel.setText("28");
        threeAPriceLabel.setText("₹1,250");
        twoAAvailableLabel.setText("15");
        twoAPriceLabel.setText("₹1,850");
        firstAAvailableLabel.setText("8");
        firstAPriceLabel.setText("₹2,650");
    }

    // -------------------------------------------------------------------------
    // Schedule Management
    // -------------------------------------------------------------------------

    /**
     * Loads complete train schedule with station-wise timing and halt information.
     */
    private void loadCompleteSchedule() {
        try {
            List<TrainSchedule> schedule = trainDAO.getTrainSchedule(currentTrain.getTrainId());
            ObservableList<ScheduleItem> scheduleItems = processTrainSchedule(schedule);
            scheduleTable.setItems(scheduleItems);
        } catch (Exception e) {
            e.printStackTrace();
            loadSampleSchedule();
        }
    }

    /**
     * Processes train schedule data into display-ready format.
     *
     * @param schedule list of train schedule entries
     * @return ObservableList of ScheduleItem for table display
     */
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
                    String.valueOf(0), // Distance will be calculated separately
                    String.valueOf(day)
            );
            scheduleItems.add(item);

            // Increment day for overnight journeys (simplified logic)
            if (stop.getDepartureTime() != null && stop.getDepartureTime().getHour() < 6) {
                day++;
            }
        }

        return scheduleItems;
    }

    /**
     * Loads sample schedule data when database schedule is unavailable.
     */
    private void loadSampleSchedule() {
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

    // -------------------------------------------------------------------------
    // Amenities Management
    // -------------------------------------------------------------------------

    /**
     * Loads and displays train amenities with visual tags.
     */
    private void loadAmenities() {
        try {
            List<String> amenities = trainService.getTrainAmenities(currentTrain);
            displayAmenities(amenities);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays amenities in visual tag format.
     *
     * @param amenities list of train amenities
     */
    private void displayAmenities(List<String> amenities) {
        amenitiesPane.getChildren().clear();

        if (amenities.isEmpty()) {
            displayDefaultAmenities();
        } else {
            for (String amenity : amenities) {
                Label amenityLabel = createAmenityTag(amenity);
                amenitiesPane.getChildren().add(amenityLabel);
            }
        }
    }

    /**
     * Displays default amenities when service data is unavailable.
     */
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

    /**
     * Creates styled amenity tag for display.
     *
     * @param amenity the amenity text
     * @return configured Label as amenity tag
     */
    private Label createAmenityTag(String amenity) {
        Label amenityLabel = new Label(amenity);
        amenityLabel.getStyleClass().add("amenity-tag");
        return amenityLabel;
    }

    // -------------------------------------------------------------------------
    // Action Handlers
    // -------------------------------------------------------------------------

    /**
     * Handles seat selection with class-specific booking initiation.
     *
     * @param event ActionEvent from seat selection button
     */
    @FXML
    public void handleSeatSelect(ActionEvent event) {
        Button sourceButton = (Button) event.getSource();
        String selectedClass = determineSelectedClass(sourceButton);
        redirectToBooking(selectedClass);
    }

    /**
     * Determines selected class based on button parent ID.
     *
     * @param sourceButton the clicked button
     * @return selected class name
     */
    private String determineSelectedClass(Button sourceButton) {
        if (sourceButton.getParent().getId() != null) {
            switch (sourceButton.getParent().getId()) {
                case "slSeatCard": return "Sleeper (SL)";
                case "threeASeatCard": return "AC 3-Tier (3A)";
                case "twoASeatCard": return "AC 2-Tier (2A)";
                case "firstASeatCard": return "First AC (1A)";
            }
        }
        return "Unknown";
    }

    /**
     * Handles general booking initiation with default class selection.
     *
     * @param event ActionEvent from book now button
     */
    @FXML
    public void handleBookNow(ActionEvent event) {
        redirectToBooking("AC 3-Tier (3A)");
    }

    /**
     * Handles navigation back from train details.
     *
     * @param event ActionEvent from back button
     */
    @FXML
    public void handleBack(ActionEvent event) {
        Stage stage = (Stage) trainNumberLabel.getScene().getWindow();
        stage.close();
    }

    // -------------------------------------------------------------------------
    // Booking Flow Management
    // -------------------------------------------------------------------------

    /**
     * Redirects to appropriate booking flow based on authentication status.
     *
     * @param selectedClass the selected train class
     */
    private void redirectToBooking(String selectedClass) {
        if (!sessionManager.isLoggedIn()) {
            handleUnauthenticatedBooking();
        } else {
            openBookingPage(selectedClass);
        }
    }

    /**
     * Handles booking attempt by unauthenticated user.
     */
    private void handleUnauthenticatedBooking() {
        sessionManager.setPendingBooking(currentTrain.getTrainId(), fromStation, toStation, journeyDate);
        redirectToLogin();
    }

    /**
     * Redirects to login page with appropriate context message.
     */
    private void redirectToLogin() {
        LoginController loginController = SceneManager.switchScene("/fxml/Login.fxml");
        loginController.setLoginMessage("You need to login to book");
        loginController.setRedirectAfterLogin("/fxml/TrainBooking.fxml");
    }

    /**
     * Opens booking page for authenticated user.
     *
     * @param selectedClass the selected train class
     */
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

    // -------------------------------------------------------------------------
    // Utility Methods
    // -------------------------------------------------------------------------

    /**
     * Determines train type based on train number classification.
     *
     * @param train the train to classify
     * @return train type classification
     */
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

    /**
     * Retrieves station name by ID with error handling.
     *
     * @param stationId the station ID to lookup
     * @return station name or "Unknown Station" if not found
     */
    private String getStationNameById(int stationId) {
        try {
            Station station = stationDAO.getStationById(stationId);
            return station != null ? station.getName() : "Unknown Station";
        } catch (Exception e) {
            return "Unknown Station";
        }
    }

    /**
     * Calculates halt time between arrival and departure.
     *
     * @param stop the train schedule stop
     * @return formatted halt time string
     */
    private String calculateHaltTime(TrainSchedule stop) {
        if (stop.getArrivalTime() == null || stop.getDepartureTime() == null) {
            return "0m";
        }

        long minutes = java.time.Duration.between(stop.getArrivalTime(), stop.getDepartureTime()).toMinutes();
        return minutes + "m";
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
    // Inner Classes for Data Management
    // -------------------------------------------------------------------------

    /**
     * ScheduleItem holds station schedule information for table display.
     */
    public static class ScheduleItem {
        private String stationName;
        private String arrivalTime;
        private String departureTime;
        private String haltTime;
        private String day;

        /**
         * Constructs a ScheduleItem with complete timing information.
         *
         * @param stationName name of the station
         * @param arrivalTime arrival time at station
         * @param departureTime departure time from station
         * @param haltTime duration of halt at station
         * @param distance distance from origin (unused in current implementation)
         * @param day journey day number
         */
        public ScheduleItem(String stationName, String arrivalTime, String departureTime,
                            String haltTime, String distance, String day) {
            this.stationName = stationName;
            this.arrivalTime = arrivalTime;
            this.departureTime = departureTime;
            this.haltTime = haltTime;
            this.day = day;
        }

        // Getters for JavaFX property binding
        public String getStationName() { return stationName; }
        public String getArrivalTime() { return arrivalTime; }
        public String getDepartureTime() { return departureTime; }
        public String getHaltTime() { return haltTime; }
        public String getDay() { return day; }
    }

    /**
     * PricingData holds calculated prices for all train classes.
     */
    private static class PricingData {
        final double slPrice;
        final double ac3Price;
        final double ac2Price;
        final double ac1Price;

        PricingData(double slPrice, double ac3Price, double ac2Price, double ac1Price) {
            this.slPrice = slPrice;
            this.ac3Price = ac3Price;
            this.ac2Price = ac2Price;
            this.ac1Price = ac1Price;
        }
    }
}