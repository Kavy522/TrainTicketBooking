package trainapp.controller.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * TrainDetailsController manages the comprehensive train details view.
 *
 * <h2>Primary Responsibilities:</h2>
 * <ul>
 *   <li><b>Train Information Display</b> - Shows complete train details including number, name, type, and specifications</li>
 *   <li><b>Route Information</b> - Displays journey details with consistent timing, distance, and station information</li>
 *   <li><b>Seat Availability & Pricing</b> - Real-time seat availability and dynamic pricing for all classes</li>
 *   <li><b>Schedule Management</b> - Complete train schedule with halt information and timing details</li>
 *   <li><b>Booking Integration</b> - Seamless integration with booking system and user authentication</li>
 *   <li><b>Data Consistency</b> - Ensures consistent pricing and route information across the application</li>
 * </ul>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Dynamic fare calculation based on distance and route popularity</li>
 *   <li>Real-time seat availability checking</li>
 *   <li>Comprehensive train schedule with halt times</li>
 *   <li>Amenities display with visual tags</li>
 *   <li>Session-aware booking redirection</li>
 *   <li>Consistent data caching for performance</li>
 * </ul>
 *
 * <h2>Usage Flow:</h2>
 * <ol>
 *   <li>User navigates from train search results</li>
 *   <li>Controller loads train details with consistent data</li>
 *   <li>Displays comprehensive information including availability and pricing</li>
 *   <li>User can select seat class and proceed to booking</li>
 *   <li>Handles authentication and redirects appropriately</li>
 * </ol>
 */
public class TrainDetailsController {

    // =========================================================================
    // FXML UI COMPONENTS
    // =========================================================================

    // Train Basic Information Labels
    @FXML private Label trainNumberLabel;
    @FXML private Label trainNameLabel;
    @FXML private Label trainTypeLabel;
    @FXML private Label totalCoachesLabel;

    // Journey Timing and Route Labels
    @FXML private Label departureTimeLabel;
    @FXML private Label departureStationLabel;
    @FXML private Label departureDateLabel;
    @FXML private Label arrivalTimeLabel;
    @FXML private Label arrivalStationLabel;
    @FXML private Label arrivalDateLabel;
    @FXML private Label durationLabel;
    @FXML private Label distanceLabel;
    @FXML private Label haltsLabel;

    // Seat Availability and Pricing Labels
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
    @FXML private TableColumn<ScheduleItem, String> dayCol;

    // Amenities Display
    @FXML private FlowPane amenitiesPane;

    // =========================================================================
    // SERVICES AND DATA ACCESS OBJECTS
    // =========================================================================

    /** Service for train-related operations and calculations */
    private final TrainService trainService = new TrainService();

    /** Data access object for train information */
    private final TrainDAO trainDAO = new TrainDAO();

    /** Data access object for station information */
    private final StationDAO stationDAO = new StationDAO();

    /** Session management for user authentication state */
    private final SessionManager sessionManager = SessionManager.getInstance();

    /** Service for dynamic pricing and fare calculations */
    private final AdminDataStructureService adminService = new AdminDataStructureService();

    // =========================================================================
    // STATE MANAGEMENT
    // =========================================================================

    /** Current train being displayed */
    private Train currentTrain;

    /** Journey date selected by user */
    private LocalDate journeyDate;

    /** Source station for the journey */
    private String fromStation;

    /** Destination station for the journey */
    private String toStation;

    /** Consistent data container for pricing and route information */
    private ConsistentTrainData consistentData;

    // =========================================================================
    // INITIALIZATION AND SETUP
    // =========================================================================

    /**
     * Initializes the controller and sets up UI components.
     * Called automatically by JavaFX after FXML loading.
     *
     * <h3>Setup Operations:</h3>
     * <ul>
     *   <li>Configures schedule table columns</li>
     *   <li>Sets up data binding for table cells</li>
     *   <li>Prepares UI components for data display</li>
     * </ul>
     */
    @FXML
    public void initialize() {
        setupScheduleTable();
    }

    /**
     * Configures the train schedule table with proper column bindings.
     * Sets up cell value factories for displaying schedule information.
     */
    private void setupScheduleTable() {
        stationNameCol.setCellValueFactory(new PropertyValueFactory<>("stationName"));
        arrivalTimeCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        departureTimeCol.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
        haltTimeCol.setCellValueFactory(new PropertyValueFactory<>("haltTime"));
        dayCol.setCellValueFactory(new PropertyValueFactory<>("day"));
    }

    // =========================================================================
    // DATA CONFIGURATION AND LOADING
    // =========================================================================

    /**
     * Main entry point for setting train details and journey information.
     *
     * <h3>Data Processing Flow:</h3>
     * <ol>
     *   <li>Stores journey parameters</li>
     *   <li>Loads or calculates consistent data</li>
     *   <li>Triggers UI update on JavaFX thread</li>
     * </ol>
     *
     * @param train The train object to display details for
     * @param journeyDate The date of travel
     * @param fromStation Source station name
     * @param toStation Destination station name
     */
    public void setTrainDetails(Train train, LocalDate journeyDate, String fromStation, String toStation) {
        this.currentTrain = train;
        this.journeyDate = journeyDate;
        this.fromStation = fromStation;
        this.toStation = toStation;

        loadConsistentData();
        Platform.runLater(this::loadTrainDetails);
    }

    /**
     * Loads consistent data from cache or calculates new values.
     * Ensures pricing and route information consistency across the application.
     *
     * <h3>Data Sources (Priority Order):</h3>
     * <ol>
     *   <li>Cached consistency data from previous calculations</li>
     *   <li>Fresh calculation using train service</li>
     *   <li>Fallback default values if calculation fails</li>
     * </ol>
     */
    private void loadConsistentData() {
        try {
            String route = fromStation + "-" + toStation;
            TrainBookingController.ConsistencyTracker.ConsistencyData cachedData =
                    TrainBookingController.ConsistencyTracker.getCalculation(currentTrain.getTrainNumber(), route);

            if (cachedData != null) {
                // Use cached consistent data
                String departureTime = trainService.getDepartureTime(currentTrain, fromStation);
                String arrivalTime = trainService.getArrivalTime(currentTrain, toStation);
                String duration = trainService.calculateDuration(currentTrain, fromStation, toStation);

                this.consistentData = new ConsistentTrainData(
                        cachedData.distance, departureTime, arrivalTime, duration, cachedData.prices);
            } else {
                // Calculate new consistent data
                calculateNewConsistentData();
            }
        } catch (Exception e) {
            calculateNewConsistentData();
        }
    }

    /**
     * Calculates fresh consistent data for the train route.
     * Performs dynamic pricing calculation and caches results for future use.
     */
    private void calculateNewConsistentData() {
        try {
            int distance = trainService.getDistanceBetween(currentTrain, fromStation, toStation);
            String departureTime = trainService.getDepartureTime(currentTrain, fromStation);
            String arrivalTime = trainService.getArrivalTime(currentTrain, toStation);
            String duration = trainService.calculateDuration(currentTrain, fromStation, toStation);
            Map<String, Double> pricing = calculateConsistentPricing(distance);

            this.consistentData = new ConsistentTrainData(distance, departureTime, arrivalTime, duration, pricing);

            // Cache the calculation for consistency
            TrainBookingController.ConsistencyTracker.recordCalculation(
                    currentTrain.getTrainNumber(), fromStation + "-" + toStation, distance, duration, pricing);
        } catch (Exception e) {
            setFallbackData();
        }
    }

    /**
     * Calculates dynamic pricing for all seat classes based on distance and route popularity.
     *
     * <h3>Pricing Algorithm:</h3>
     * <ul>
     *   <li>Base fare calculation using distance and class multipliers</li>
     *   <li>Popular route surcharge (5-20% depending on class)</li>
     *   <li>Dynamic adjustment based on demand and availability</li>
     * </ul>
     *
     * @param distance Journey distance in kilometers
     * @return Map containing pricing for all available classes
     */
    private Map<String, Double> calculateConsistentPricing(int distance) {
        Map<String, Double> pricing = new java.util.HashMap<>();

        // Calculate base fares using admin service
        double slPrice = adminService.calculateDynamicFare(TrainClass.SL, distance);
        double ac3Price = adminService.calculateDynamicFare(TrainClass._3A, distance);
        double ac2Price = adminService.calculateDynamicFare(TrainClass._2A, distance);
        double ac1Price = adminService.calculateDynamicFare(TrainClass._1A, distance);

        // Apply popular route surcharge
        if (isPopularRoute(fromStation, toStation)) {
            slPrice *= 1.2;    // 20% surcharge for SL
            ac3Price *= 1.15;  // 15% surcharge for 3A
            ac2Price *= 1.1;   // 10% surcharge for 2A
            ac1Price *= 1.05;  // 5% surcharge for 1A
        }

        pricing.put("SL", slPrice);
        pricing.put("3A", ac3Price);
        pricing.put("2A", ac2Price);
        pricing.put("1A", ac1Price);

        return pricing;
    }

    /**
     * Sets fallback data when calculation fails.
     * Provides reasonable default values to ensure UI remains functional.
     */
    private void setFallbackData() {
        Map<String, Double> fallbackPricing = new java.util.HashMap<>();
        fallbackPricing.put("SL", 295.0);
        fallbackPricing.put("3A", 840.0);
        fallbackPricing.put("2A", 1300.0);
        fallbackPricing.put("1A", 2050.0);

        this.consistentData = new ConsistentTrainData(350, "06:00", "12:30", "6h 30m", fallbackPricing);
    }

    // =========================================================================
    // UI DISPLAY METHODS
    // =========================================================================

    /**
     * Main method to load and display all train details.
     * Coordinates loading of different sections of train information.
     *
     * <h3>Loading Sequence:</h3>
     * <ol>
     *   <li>Basic train information (number, name, type)</li>
     *   <li>Route information with consistent timing</li>
     *   <li>Seat availability and pricing</li>
     *   <li>Complete train schedule</li>
     *   <li>Available amenities</li>
     * </ol>
     */
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

    /**
     * Loads and displays basic train information.
     * Updates UI labels with train number, name, type, and specifications.
     */
    private void loadTrainInfo() {
        trainNumberLabel.setText(currentTrain.getTrainNumber());
        trainNameLabel.setText(currentTrain.getName());
        trainTypeLabel.setText(getTrainType(currentTrain));
        totalCoachesLabel.setText(String.valueOf(currentTrain.getTotalCoaches()));
    }

    /**
     * Loads consistent route information including timing and distance.
     * Uses consistent data to ensure information matches booking page.
     */
    private void loadConsistentRouteInfo() {
        try {
            loadStationNames();
            loadConsistentJourneyTiming();
            loadConsistentDistanceAndHalts();
        } catch (Exception e) {
            setDefaultRouteInformation();
        }
    }

    /**
     * Loads and displays station names for departure and arrival.
     */
    private void loadStationNames() {
        String sourceStationName = getStationNameById(currentTrain.getSourceStationId());
        String destStationName = getStationNameById(currentTrain.getDestinationStationId());

        departureStationLabel.setText(sourceStationName);
        arrivalStationLabel.setText(destStationName);
    }

    /**
     * Loads consistent journey timing information.
     * Uses consistent data to ensure timing matches booking calculations.
     */
    private void loadConsistentJourneyTiming() {
        if (consistentData != null) {
            departureTimeLabel.setText(consistentData.departureTime);
            arrivalTimeLabel.setText(consistentData.arrivalTime);
            durationLabel.setText(consistentData.duration);
        } else {
            // Fallback times
            departureTimeLabel.setText("06:00");
            arrivalTimeLabel.setText("12:30");
            durationLabel.setText("6h 30m");
        }

        // Set journey dates
        departureDateLabel.setText(journeyDate.format(DateTimeFormatter.ofPattern("dd MMM")));
        arrivalDateLabel.setText(journeyDate.format(DateTimeFormatter.ofPattern("dd MMM")));
    }

    /**
     * Loads consistent distance and halt information.
     */
    private void loadConsistentDistanceAndHalts() {
        if (consistentData != null) {
            distanceLabel.setText(consistentData.distance + " km");
        } else {
            distanceLabel.setText("350 km");
        }

        int halts = trainService.getHaltsBetween(currentTrain, fromStation, toStation);
        haltsLabel.setText(halts + " halts");
    }

    /**
     * Sets default route information when data loading fails.
     */
    private void setDefaultRouteInformation() {
        departureStationLabel.setText(fromStation);
        arrivalStationLabel.setText(toStation);
        departureTimeLabel.setText("06:00");
        arrivalTimeLabel.setText("12:30");
        durationLabel.setText("6h 30m");
        distanceLabel.setText("350 km");
        haltsLabel.setText("6 halts");
    }

    /**
     * Loads and displays seat availability with consistent pricing.
     * Updates availability status and prices for all seat classes.
     */
    private void loadConsistentSeatAvailability() {
        try {
            Map<String, Integer> seatMap = trainService.getAvailableSeatsForDate(currentTrain, journeyDate);
            updateConsistentSeatAvailabilityDisplay(seatMap);
        } catch (Exception e) {
            setDefaultSeatAvailability();
        }
    }

    /**
     * Updates seat availability display with consistent pricing.
     *
     * @param seatMap Map containing available seats for each class
     */
    private void updateConsistentSeatAvailabilityDisplay(Map<String, Integer> seatMap) {
        if (consistentData != null && consistentData.pricing != null) {
            updateClassAvailability(slAvailableLabel, slPriceLabel,
                    seatMap.getOrDefault("SL", 0), consistentData.pricing.get("SL"));
            updateClassAvailability(threeAAvailableLabel, threeAPriceLabel,
                    seatMap.getOrDefault("3A", 0), consistentData.pricing.get("3A"));
            updateClassAvailability(twoAAvailableLabel, twoAPriceLabel,
                    seatMap.getOrDefault("2A", 0), consistentData.pricing.get("2A"));
            updateClassAvailability(firstAAvailableLabel, firstAPriceLabel,
                    seatMap.getOrDefault("1A", 0), consistentData.pricing.get("1A"));
        } else {
            setDefaultSeatAvailability();
        }
    }

    /**
     * Updates availability and pricing display for a specific class.
     *
     * @param availableLabel Label showing seat count
     * @param priceLabel Label showing price
     * @param seats Number of available seats
     * @param price Price for the class
     */
    private void updateClassAvailability(Label availableLabel, Label priceLabel, int seats, Double price) {
        availableLabel.setText(String.valueOf(seats));

        // Update styling based on availability
        availableLabel.getStyleClass().removeAll("available", "waitlist");
        availableLabel.getStyleClass().add(seats > 0 ? "available" : "waitlist");

        priceLabel.setText("₹" + String.format("%.0f", price != null ? price : 0.0));
    }

    /**
     * Sets default seat availability when real data is unavailable.
     */
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

    /**
     * Loads and displays complete train schedule.
     * Shows all stops with arrival/departure times and halt durations.
     */
    private void loadCompleteSchedule() {
        try {
            List<TrainSchedule> schedule = trainDAO.getTrainSchedule(currentTrain.getTrainId());
            ObservableList<ScheduleItem> scheduleItems = processTrainSchedule(schedule);
            scheduleTable.setItems(scheduleItems);
        } catch (Exception e) {
            loadSampleSchedule();
        }
    }

    /**
     * Processes train schedule data into display format.
     *
     * @param schedule List of train schedule entries
     * @return Observable list of schedule items for table display
     */
    private ObservableList<ScheduleItem> processTrainSchedule(List<TrainSchedule> schedule) {
        ObservableList<ScheduleItem> scheduleItems = FXCollections.observableArrayList();
        int day = 1;
        LocalTime previousTime = null;

        for (TrainSchedule stop : schedule) {
            Station station = stationDAO.getStationById(stop.getStationId());
            String stationName = station != null ? station.getName() : "Unknown";

            // Get the relevant time for day calculation (departure time, or arrival if no departure)
            LocalTime currentTime = stop.getDepartureTime() != null ?
                    stop.getDepartureTime() : stop.getArrivalTime();

            // Check for day transition: if current time is significantly earlier than previous time,
            // it likely indicates we've moved to the next day
            if (previousTime != null && currentTime != null) {
                // If current time is before 6 AM and previous time was after 18:00 (6 PM),
                // or if there's a significant backward time jump, increment day
                if ((currentTime.getHour() < 6 && previousTime.getHour() >= 18) ||
                        (currentTime.isBefore(previousTime) &&
                                java.time.Duration.between(currentTime, previousTime).toHours() > 12)) {
                    day++;
                }
            }

            ScheduleItem item = new ScheduleItem(
                    stationName,
                    stop.getArrivalTime() != null ? stop.getArrivalTime().toString() : "Start",
                    stop.getDepartureTime() != null ? stop.getDepartureTime().toString() : "End",
                    calculateHaltTime(stop),
                    String.valueOf(day)
            );
            scheduleItems.add(item);

            // Update previous time for next iteration
            if (currentTime != null) {
                previousTime = currentTime;
            }
        }

        return scheduleItems;
    }

    /**
     * Loads sample schedule when real data is unavailable.
     */
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

    /**
     * Loads and displays train amenities.
     * Shows available facilities and services on the train.
     */
    private void loadAmenities() {
        try {
            List<String> amenities = trainService.getTrainAmenities(currentTrain);
            displayAmenities(amenities);
        } catch (Exception e) {
            // Silently fall back to default amenities
        }
    }

    /**
     * Displays amenities as visual tags.
     *
     * @param amenities List of available amenities
     */
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

    /**
     * Displays default amenities when specific data is unavailable.
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
     * Creates a styled amenity tag label.
     *
     * @param amenity Amenity text to display
     * @return Styled label for the amenity
     */
    private Label createAmenityTag(String amenity) {
        Label amenityLabel = new Label(amenity);
        amenityLabel.getStyleClass().add("amenity-chip");
        return amenityLabel;
    }

    // =========================================================================
    // EVENT HANDLERS
    // =========================================================================

    /**
     * Handles seat selection from availability cards.
     * Determines selected class and redirects to booking.
     *
     * @param event Action event from seat selection button
     */
    @FXML
    public void handleSeatSelect(ActionEvent event) {
        Button sourceButton = (Button) event.getSource();
        String selectedClass = determineSelectedClass(sourceButton);
        redirectToBooking(selectedClass);
    }

    /**
     * Handles the main "Book Now" button click.
     * Defaults to AC 3-Tier class for booking.
     *
     * @param event Action event from book now button
     */
    @FXML
    public void handleBookNow(ActionEvent event) {
        redirectToBooking("AC 3-Tier (3A)");
    }

    /**
     * Handles back navigation to main menu.
     *
     * @param event Action event from back button
     */
    @FXML
    public void handleBack(ActionEvent event) {
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    // =========================================================================
    // NAVIGATION METHODS
    // =========================================================================

    /**
     * Main booking redirection method with authentication check.
     * Handles both authenticated and unauthenticated booking attempts.
     *
     * @param selectedClass The seat class selected for booking
     */
    private void redirectToBooking(String selectedClass) {
        if (!sessionManager.isLoggedIn()) {
            handleUnauthenticatedBooking();
        } else {
            openBookingPage(selectedClass);
        }
    }

    /**
     * Handles booking attempt by unauthenticated users.
     * Stores pending booking information and redirects to login.
     */
    private void handleUnauthenticatedBooking() {
        // Store booking details for post-login redirect
        sessionManager.setPendingBooking(currentTrain.getTrainId(), fromStation, toStation, journeyDate);
        redirectToLogin();
    }

    /**
     * Redirects unauthenticated users to login page.
     * Sets up post-login redirect to booking page.
     */
    private void redirectToLogin() {
        LoginController loginController = SceneManager.switchScene("/fxml/Login.fxml");
        loginController.setLoginMessage("You need to login to book");
        loginController.setRedirectAfterLogin("/fxml/TrainBooking.fxml");
    }

    /**
     * Opens the booking page for authenticated users.
     * Passes all necessary booking parameters.
     *
     * @param selectedClass The seat class selected for booking
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

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    /**
     * Determines selected seat class from button source.
     * Maps UI component IDs to class names.
     *
     * @param sourceButton The button that triggered the selection
     * @return String representation of the selected class
     */
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

    /**
     * Determines train type based on train number pattern.
     * Uses Indian Railways numbering convention.
     *
     * @param train The train object to classify
     * @return String representation of train type
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
     * @param stationId The station ID to look up
     * @return Station name or "Unknown Station" if not found
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
     * Calculates halt time for a schedule stop.
     *
     * @param stop The train schedule stop
     * @return Formatted halt time string
     */
    private String calculateHaltTime(TrainSchedule stop) {
        if (stop.getArrivalTime() == null || stop.getDepartureTime() == null) {
            return "0m";
        }

        long minutes = java.time.Duration.between(stop.getArrivalTime(), stop.getDepartureTime()).toMinutes();
        return minutes + "m";
    }

    /**
     * Determines if a route is popular for pricing adjustments.
     *
     * @param from Source station name
     * @param to Destination station name
     * @return true if the route is considered popular
     */
    private boolean isPopularRoute(String from, String to) {
        if (from == null || to == null) return false;

        String route = from.toLowerCase() + "-" + to.toLowerCase();
        return (route.contains("delhi") && route.contains("mumbai")) ||
                (route.contains("bangalore") && route.contains("chennai")) ||
                (route.contains("kolkata") && route.contains("delhi"));
    }

    /**
     * Displays error messages to the user.
     *
     * @param message The error message to display
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
     * Data transfer object for schedule table items.
     * Represents a single row in the train schedule table.
     */
    public static class ScheduleItem {
        private final String stationName;
        private final String arrivalTime;
        private final String departureTime;
        private final String haltTime;
        private final String day;

        /**
         * Creates a new schedule item.
         *
         * @param stationName Name of the station
         * @param arrivalTime Arrival time at station
         * @param departureTime Departure time from station
         * @param haltTime Duration of halt at station
         * @param day Day number of journey
         */
        public ScheduleItem(String stationName, String arrivalTime, String departureTime,
                            String haltTime, String day) {
            this.stationName = stationName;
            this.arrivalTime = arrivalTime;
            this.departureTime = departureTime;
            this.haltTime = haltTime;
            this.day = day;
        }

        public String getDay() {
            return day;
        }

        public String getStationName() {
            return stationName;
        }

        public String getArrivalTime() {
            return arrivalTime;
        }

        public String getDepartureTime() {
            return departureTime;
        }

        public String getHaltTime() {
            return haltTime;
        }
    }

    /**
     * Container class for consistent train data across the application.
     * Ensures pricing and route information consistency.
     */
    private static class ConsistentTrainData {
        public final int distance;
        public final String departureTime;
        public final String arrivalTime;
        public final String duration;
        public final Map<String, Double> pricing;

        /**
         * Creates consistent train data container.
         *
         * @param distance Journey distance in kilometers
         * @param departureTime Departure time string
         * @param arrivalTime Arrival time string
         * @param duration Journey duration string
         * @param pricing Map of class-wise pricing
         */
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