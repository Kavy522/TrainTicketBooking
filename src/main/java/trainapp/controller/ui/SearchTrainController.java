package trainapp.controller.ui;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import trainapp.dao.StationDAO;
import trainapp.model.Station;
import trainapp.model.Train;
import trainapp.model.TrainClass;
import trainapp.service.SessionManager;
import trainapp.service.TrainService;
import trainapp.service.AdminDataStructureService;
import trainapp.util.SceneManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * SearchTrainController with consistent data calculations to match other controllers.
 * Ensures same km, time, and pricing across all train displays.
 */
public class SearchTrainController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    @FXML private Label fromStationLabel;
    @FXML private Label toStationLabel;
    @FXML private Label dateLabel;
    @FXML private Label resultsCountLabel;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ToggleButton listViewToggle;
    @FXML private ToggleButton cardViewToggle;
    @FXML private VBox trainsContainer;
    @FXML private VBox emptyState;
    @FXML private VBox loadingState;
    @FXML private HBox loadMoreSection;

    // -------------------------------------------------------------------------
    // Services and Data Management
    // -------------------------------------------------------------------------

    private final TrainService trainService = new TrainService();
    private final StationDAO stationDAO = new StationDAO();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final AdminDataStructureService adminService = new AdminDataStructureService();

    // -------------------------------------------------------------------------
    // Search State and Configuration
    // -------------------------------------------------------------------------

    private String fromStation;
    private String toStation;
    private LocalDate journeyDate;
    private List<Train> allTrains = new ArrayList<>();
    private int displayedTrains = 0;
    private static final int TRAINS_PER_PAGE = 10;

    // Store consistent data for all trains
    private Map<String, ConsistentTrainData> trainDataCache = new HashMap<>();

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    @FXML
    public void initialize() {
        System.out.println("SearchTrainController: Initializing controller");
        setupSortCombo();
        setupViewToggles();
        initializeDefaultLabels();
    }

    private void setupSortCombo() {
        if (sortCombo != null) {
            sortCombo.getItems().setAll(
                    "🏆 Recommended",
                    "🕐 Departure Time",
                    "🕐 Arrival Time",
                    "⏱️ Duration",
                    "💰 Price"
            );
            sortCombo.setValue("🏆 Recommended");
            sortCombo.valueProperty().addListener((obs, oldV, newV) -> reloadWithSort(newV));
        }
    }

    private void setupViewToggles() {
        if (listViewToggle != null && cardViewToggle != null) {
            ToggleGroup viewToggle = new ToggleGroup();
            listViewToggle.setToggleGroup(viewToggle);
            cardViewToggle.setToggleGroup(viewToggle);
            listViewToggle.setSelected(true);

            viewToggle.selectedToggleProperty().addListener((obs, oldT, newT) -> {
                rebuildCards();
            });
        }
    }

    private void initializeDefaultLabels() {
        if (fromStationLabel != null) fromStationLabel.setText("Source Station");
        if (toStationLabel != null) toStationLabel.setText("Destination Station");
        if (dateLabel != null) dateLabel.setText(LocalDate.now().toString());
        if (resultsCountLabel != null) resultsCountLabel.setText("Searching...");
    }

    // -------------------------------------------------------------------------
    // Search Data Configuration
    // -------------------------------------------------------------------------

    public void setSearchData(String fromStation, String toStation, LocalDate journeyDate) {
        System.out.println("SearchTrainController: Setting search data - From: " + fromStation +
                ", To: " + toStation + ", Date: " + journeyDate);

        this.fromStation = fromStation;
        this.toStation = toStation;
        this.journeyDate = journeyDate;

        updateHeaderLabels();
        loadTrainDataAsync();
    }

    private void updateHeaderLabels() {
        if (fromStationLabel != null) fromStationLabel.setText(fromStation != null ? fromStation : "Source");
        if (toStationLabel != null) toStationLabel.setText(toStation != null ? toStation : "Destination");

        if (dateLabel != null && journeyDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
            dateLabel.setText(journeyDate.format(formatter));
        }
    }

    // -------------------------------------------------------------------------
    // Asynchronous Data Loading with Consistent Calculations
    // -------------------------------------------------------------------------

    private void loadTrainDataAsync() {
        System.out.println("SearchTrainController: Starting async train data load");
        showLoadingState(true);

        Task<List<Train>> loadTask = createTrainLoadingTask();
        configureTaskHandlers(loadTask);
        new Thread(loadTask).start();
    }

    private Task<List<Train>> createTrainLoadingTask() {
        return new Task<List<Train>>() {
            @Override
            protected List<Train> call() throws Exception {
                System.out.println("SearchTrainController: Background task - loading trains from: " +
                        fromStation + " to: " + toStation);

                Thread.sleep(500); // Simulate loading time for UX

                List<Train> trains = trainService.findTrainsBetweenStations(
                        fromStation != null ? fromStation : "Null",
                        toStation != null ? toStation : "Null"
                );

                // Calculate consistent data for each train
                for (Train train : trains) {
                    calculateConsistentDataForTrain(train);
                }

                System.out.println("SearchTrainController: Background task - found " + trains.size() + " trains with consistent data");
                return trains;
            }
        };
    }

    /**
     * Calculates consistent data (distance, timing, pricing) for each train.
     */
    private void calculateConsistentDataForTrain(Train train) {
        try {
            // Calculate consistent distance using time-based method
            int distance = trainService.getDistanceBetween(train, fromStation, toStation);

            // Get consistent timing data
            String departureTime = trainService.getDepartureTime(train, fromStation);
            String arrivalTime = trainService.getArrivalTime(train, toStation);
            String duration = trainService.calculateDuration(train, fromStation, toStation);

            // Calculate consistent pricing for all classes
            Map<String, Double> pricing = calculateConsistentPricingForTrain(train, distance);

            // Store consistent data
            String trainKey = train.getTrainNumber();
            trainDataCache.put(trainKey, new ConsistentTrainData(distance, departureTime, arrivalTime, duration, pricing));

            // Also record in global consistency tracker
            TrainBookingController.ConsistencyTracker.recordCalculation(
                    train.getTrainNumber(), fromStation + "-" + toStation, distance, duration, pricing);

            System.out.println("SearchTrain - Cached consistent data for " + train.getTrainNumber() +
                    ": " + distance + "km, " + duration + ", Prices: " + pricing);

        } catch (Exception e) {
            System.err.println("Error calculating consistent data for train " + train.getTrainNumber() + ": " + e.getMessage());
        }
    }

    /**
     * Calculates consistent pricing for a train using same methodology as TrainBookingController.
     */
    private Map<String, Double> calculateConsistentPricingForTrain(Train train, int distance) {
        Map<String, Double> pricing = new HashMap<>();

        // Use AdminDataStructureService for consistent pricing calculation
        double slPrice = adminService.calculateDynamicFare(TrainClass.SL, distance);
        double ac3Price = adminService.calculateDynamicFare(TrainClass._3A, distance);
        double ac2Price = adminService.calculateDynamicFare(TrainClass._2A, distance);
        double ac1Price = adminService.calculateDynamicFare(TrainClass._1A, distance);

        // Apply surge pricing if route is popular (same logic as TrainBookingController)
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

    private void configureTaskHandlers(Task<List<Train>> loadTask) {
        loadTask.setOnSucceeded(e -> {
            System.out.println("SearchTrainController: Task succeeded");
            allTrains = loadTask.getValue();
            displayedTrains = 0;
            showLoadingState(false);
            rebuildCards();
        });

        loadTask.setOnFailed(e -> {
            System.err.println("SearchTrainController: Task failed");
            showLoadingState(false);
            Throwable exception = loadTask.getException();
            if (exception != null) {
                exception.printStackTrace();
            }
            showError("Failed to load trains. Please try again.\n" +
                    (exception != null ? exception.getMessage() : "Unknown error"));
        });
    }

    private void showLoadingState(boolean show) {
        if (loadingState != null) {
            loadingState.setVisible(show);
            loadingState.setManaged(show);
        }
    }

    // -------------------------------------------------------------------------
    // Results Display and Pagination
    // -------------------------------------------------------------------------

    private void rebuildCards() {
        System.out.println("SearchTrainController: Rebuilding cards with " + allTrains.size() + " trains");

        if (trainsContainer == null) {
            System.err.println("SearchTrainController: trainsContainer is null!");
            return;
        }

        clearDisplay();
        updateResultsCount();
        handleEmptyState();

        if (!allTrains.isEmpty()) {
            loadMoreTrains(allTrains);
            updateLoadMoreButton(allTrains);
        }
    }

    private void clearDisplay() {
        trainsContainer.getChildren().clear();
        displayedTrains = 0;
    }

    private void updateResultsCount() {
        if (resultsCountLabel != null) {
            resultsCountLabel.setText("Found " + allTrains.size() + " trains");
        }
    }

    private void handleEmptyState() {
        boolean isEmpty = allTrains.isEmpty();
        if (emptyState != null) {
            emptyState.setVisible(isEmpty);
            emptyState.setManaged(isEmpty);
        }

        if (isEmpty) {
            System.out.println("SearchTrainController: No trains to display");
        }
    }

    private void loadMoreTrains(List<Train> trains) {
        int endIndex = Math.min(displayedTrains + TRAINS_PER_PAGE, trains.size());

        System.out.println("SearchTrainController: Loading trains from index " + displayedTrains +
                " to " + endIndex);

        for (int i = displayedTrains; i < endIndex; i++) {
            VBox card = createEnhancedTrainCard(trains.get(i));
            trainsContainer.getChildren().add(card);
            animateCardEntrance(card, i - displayedTrains);
        }

        displayedTrains = endIndex;
        updateLoadMoreButton(trains);

        System.out.println("SearchTrainController: Now displaying " + displayedTrains + " trains");
    }

    private void updateLoadMoreButton(List<Train> trains) {
        boolean hasMore = displayedTrains < trains.size();
        if (loadMoreSection != null) {
            loadMoreSection.setVisible(hasMore);
            loadMoreSection.setManaged(hasMore);
        }
    }

    // -------------------------------------------------------------------------
    // Train Card Creation with Consistent Data
    // -------------------------------------------------------------------------

    private VBox createEnhancedTrainCard(Train train) {
        System.out.println("SearchTrainController: Creating card for train " + train.getTrainNumber());

        VBox card = new VBox();
        card.getStyleClass().add("train-card");
        card.setSpacing(12);

        card.getChildren().addAll(
                createTrainHeader(train),
                createRouteSection(train),
                createActionSection(train)
        );

        return card;
    }

    private HBox createTrainHeader(Train train) {
        HBox header = new HBox(12);
        header.getStyleClass().add("train-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox info = createTrainInfoSection(train);
        Region spacer = createHeaderSpacer();
        HBox seats = createSeatsSection(train);

        header.getChildren().addAll(info, spacer, seats);
        return header;
    }

    private VBox createTrainInfoSection(Train train) {
        VBox info = new VBox(4);

        Label number = new Label(train.getTrainNumber() != null ? train.getTrainNumber() : "N/A");
        number.getStyleClass().add("train-number");

        Label name = new Label(train.getName() != null ? train.getName() : "Unknown Train");
        name.getStyleClass().add("train-name");

        HBox tags = createAmenitiesTags(train);

        info.getChildren().addAll(number, name, tags);
        return info;
    }

    private HBox createAmenitiesTags(Train train) {
        HBox tags = new HBox(6);
        tags.setAlignment(Pos.CENTER_LEFT);

        List<String> amenities = trainService.getTrainAmenities(train);
        for (String amenity : amenities) {
            Label tag = createTag(amenity, "tag-chip");
            tags.getChildren().add(tag);
            if (tags.getChildren().size() >= 3) break; // Limit to 3 tags
        }

        return tags;
    }

    private Region createHeaderSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /**
     * Creates route section with consistent timing data.
     */
    private HBox createRouteSection(Train train) {
        HBox route = new HBox(40);
        route.getStyleClass().add("route-box");
        route.setAlignment(Pos.CENTER_LEFT);

        ConsistentTrainData data = trainDataCache.get(train.getTrainNumber());

        VBox depInfo = createDepartureInfo(train, data);
        VBox durationInfo = createJourneyDurationInfo(train, data);
        VBox arrInfo = createArrivalInfo(train, data);

        route.getChildren().addAll(depInfo, durationInfo, arrInfo);
        return route;
    }

    private VBox createDepartureInfo(Train train, ConsistentTrainData data) {
        String sourceStationName = getStationNameById(train.getSourceStationId());
        String depTime = data != null ? data.departureTime : "06:00";
        return createStationInfo(depTime, sourceStationName);
    }

    /**
     * Creates journey duration info with consistent calculated data.
     */
    private VBox createJourneyDurationInfo(Train train, ConsistentTrainData data) {
        if (data != null) {
            // Use consistent cached data
            int halts = trainService.getHaltsBetween(train, fromStation, toStation);
            return createDurationInfo(data.duration, halts + " halts • " + data.distance + " km");
        } else {
            // Fallback to service calculation
            String duration = trainService.calculateDuration(train, fromStation, toStation);
            int halts = trainService.getHaltsBetween(train, fromStation, toStation);
            int distance = trainService.getDistanceBetween(train, fromStation, toStation);
            return createDurationInfo(duration, halts + " halts • " + distance + " km");
        }
    }

    private VBox createArrivalInfo(Train train, ConsistentTrainData data) {
        String destStationName = getStationNameById(train.getDestinationStationId());
        String arrTime = data != null ? data.arrivalTime : "18:30";
        return createStationInfo(arrTime, destStationName);
    }

    private HBox createActionSection(Train train) {
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setStyle("-fx-padding: 12 0 0 0;");

        Button viewDetails = createViewDetailsButton(train);
        Button bookNow = createBookNowButton(train);

        actions.getChildren().addAll(viewDetails, bookNow);
        return actions;
    }

    private Button createViewDetailsButton(Train train) {
        Button viewDetails = new Button("View Details");
        viewDetails.getStyleClass().addAll("ghost-btn-small");
        viewDetails.setOnAction(e -> handleViewDetails(train));
        return viewDetails;
    }

    private Button createBookNowButton(Train train) {
        Button bookNow = new Button("Book Now");
        bookNow.getStyleClass().addAll("empty-action-primary");
        bookNow.setOnAction(e -> handleBookTrain(train));
        return bookNow;
    }

    // -------------------------------------------------------------------------
    // UI Component Creation with Consistent Pricing
    // -------------------------------------------------------------------------

    /**
     * Creates seat availability section with consistent pricing data.
     */
    private HBox createSeatsSection(Train train) {
        HBox seats = new HBox(12);
        seats.getStyleClass().add("seats-section");
        seats.setAlignment(Pos.CENTER_RIGHT);

        Map<String, Integer> seatMap = trainService.getAvailableSeatsForDate(train, journeyDate);
        ConsistentTrainData data = trainDataCache.get(train.getTrainNumber());

        if (data != null && data.pricing != null) {
            // Use consistent cached pricing
            seats.getChildren().addAll(
                    createSeatBoxWithPrice("SL", seatMap.getOrDefault("SL", 0), data.pricing.get("SL")),
                    createSeatBoxWithPrice("3A", seatMap.getOrDefault("3A", 0), data.pricing.get("3A")),
                    createSeatBoxWithPrice("2A", seatMap.getOrDefault("2A", 0), data.pricing.get("2A")),
                    createSeatBoxWithPrice("1A", seatMap.getOrDefault("1A", 0), data.pricing.get("1A"))
            );

            System.out.println("SearchTrain - Using consistent pricing for " + train.getTrainNumber() + ": " + data.pricing);
        } else {
            // Fallback to basic seat boxes
            seats.getChildren().addAll(
                    createSeatBox("SL", seatMap.getOrDefault("SL", 0)),
                    createSeatBox("3A", seatMap.getOrDefault("3A", 0)),
                    createSeatBox("2A", seatMap.getOrDefault("2A", 0)),
                    createSeatBox("1A", seatMap.getOrDefault("1A", 0))
            );
        }

        return seats;
    }

    /**
     * Creates individual seat class box with consistent pricing display.
     */
    private VBox createSeatBoxWithPrice(String className, int count, Double price) {
        VBox box = new VBox(4);
        box.getStyleClass().add("seat-class");
        box.setAlignment(Pos.CENTER);

        Label name = new Label(className);
        name.getStyleClass().add("class-name");

        Label countLabel = new Label(count > 0 ? String.valueOf(count) : "WL");
        countLabel.getStyleClass().addAll("seat-count", count > 0 ? "available" : "waitlist");

        Label priceLabel = new Label("₹" + String.format("%.0f", price != null ? price : 0.0));
        priceLabel.getStyleClass().add("price-info");

        if (count == 0) {
            Label waitlistLabel = new Label("Waitlist");
            waitlistLabel.getStyleClass().add("waitlist-info");
            box.getChildren().addAll(name, countLabel, waitlistLabel, priceLabel);
        } else {
            box.getChildren().addAll(name, countLabel, priceLabel);
        }

        return box;
    }

    private VBox createSeatBox(String className, int count) {
        VBox box = new VBox(4);
        box.getStyleClass().add("seat-class");
        box.setAlignment(Pos.CENTER);

        Label name = new Label(className);
        name.getStyleClass().add("class-name");

        Label countLabel = new Label(count > 0 ? String.valueOf(count) : "WL");
        countLabel.getStyleClass().addAll("seat-count", count > 0 ? "available" : "waitlist");

        if (count == 0) {
            Label waitlistLabel = new Label("Waitlist");
            waitlistLabel.getStyleClass().add("waitlist-info");
            box.getChildren().addAll(name, countLabel, waitlistLabel);
        } else {
            box.getChildren().addAll(name, countLabel);
        }

        return box;
    }

    private VBox createStationInfo(String time, String stationName) {
        VBox info = new VBox(2);
        info.setAlignment(Pos.CENTER);

        Label timeLabel = new Label(time != null ? time : "--:--");
        timeLabel.getStyleClass().add("station-time");

        Label stationLabel = new Label(stationName != null ? stationName : "Unknown");
        stationLabel.getStyleClass().add("station-name-small");

        info.getChildren().addAll(timeLabel, stationLabel);
        return info;
    }

    private VBox createDurationInfo(String duration, String details) {
        VBox info = new VBox(2);
        info.setAlignment(Pos.CENTER);

        Label durationLabel = new Label(duration != null ? duration : "--h --m");
        durationLabel.getStyleClass().add("duration");

        Label detailsLabel = new Label(details != null ? details : "-- halts");
        detailsLabel.getStyleClass().add("stops");

        info.getChildren().addAll(durationLabel, detailsLabel);
        return info;
    }

    private Label createTag(String text, String styleClass) {
        Label tag = new Label(text);
        tag.getStyleClass().add(styleClass);
        return tag;
    }

    // -------------------------------------------------------------------------
    // Animation and Visual Effects
    // -------------------------------------------------------------------------

    private void animateCardEntrance(VBox card, int index) {
        card.setOpacity(0);
        card.setTranslateY(20);

        FadeTransition fade = new FadeTransition(Duration.millis(300), card);
        fade.setToValue(1.0);
        fade.setDelay(Duration.millis(index * 50));

        TranslateTransition slide = new TranslateTransition(Duration.millis(300), card);
        slide.setToY(0);
        slide.setDelay(Duration.millis(index * 50));

        fade.play();
        slide.play();
    }

    // -------------------------------------------------------------------------
    // Sorting and Filtering Operations
    // -------------------------------------------------------------------------

    private void reloadWithSort(String sortOption) {
        System.out.println("SearchTrainController: Sorting by " + sortOption);

        if (allTrains.isEmpty()) return;

        applySortingStrategy(sortOption);
        rebuildCards();
    }

    private void applySortingStrategy(String sortOption) {
        switch (sortOption) {
            case "🕐 Departure Time":
                allTrains.sort((t1, t2) -> {
                    ConsistentTrainData data1 = trainDataCache.get(t1.getTrainNumber());
                    ConsistentTrainData data2 = trainDataCache.get(t2.getTrainNumber());
                    String time1 = data1 != null ? data1.departureTime : "06:00";
                    String time2 = data2 != null ? data2.departureTime : "06:00";
                    return time1.compareTo(time2);
                });
                break;
            case "🕐 Arrival Time":
                allTrains.sort((t1, t2) -> {
                    ConsistentTrainData data1 = trainDataCache.get(t1.getTrainNumber());
                    ConsistentTrainData data2 = trainDataCache.get(t2.getTrainNumber());
                    String time1 = data1 != null ? data1.arrivalTime : "18:30";
                    String time2 = data2 != null ? data2.arrivalTime : "18:30";
                    return time1.compareTo(time2);
                });
                break;
            case "⏱️ Duration":
                allTrains.sort((t1, t2) -> {
                    ConsistentTrainData data1 = trainDataCache.get(t1.getTrainNumber());
                    ConsistentTrainData data2 = trainDataCache.get(t2.getTrainNumber());
                    String dur1 = data1 != null ? data1.duration : "12h 30m";
                    String dur2 = data2 != null ? data2.duration : "12h 30m";
                    return dur1.compareTo(dur2);
                });
                break;
            case "💰 Price":
                allTrains.sort((t1, t2) -> {
                    ConsistentTrainData data1 = trainDataCache.get(t1.getTrainNumber());
                    ConsistentTrainData data2 = trainDataCache.get(t2.getTrainNumber());
                    double price1 = data1 != null && data1.pricing != null ? data1.pricing.get("3A") : 500;
                    double price2 = data2 != null && data2.pricing != null ? data2.pricing.get("3A") : 500;
                    return Double.compare(price1, price2);
                });
                break;
            default: // Recommended
                // Keep original order or implement recommendation logic
                break;
        }
    }

    // -------------------------------------------------------------------------
    // Action Handlers for User Interactions
    // -------------------------------------------------------------------------

    private void handleViewDetails(Train train) {
        System.out.println("SearchTrainController: Opening train details for " + train.getTrainNumber());
        TrainDetailsController detailsController = SceneManager.switchScene("/fxml/TrainDetails.fxml");
        detailsController.setTrainDetails(train, journeyDate, fromStation, toStation);
        System.out.println("SearchTrainController: Train details window opened successfully");
    }

    private void handleBookTrain(Train train) {
        System.out.println("SearchTrainController: Booking train " + train.getTrainNumber());

        if (!sessionManager.isLoggedIn()) {
            handleUnauthenticatedBooking(train);
        } else {
            redirectToBookingPage(train);
        }
    }

    private void handleUnauthenticatedBooking(Train train) {
        sessionManager.setPendingBooking(train.getTrainId(), fromStation, toStation, journeyDate);
        LoginController loginController = SceneManager.switchScene("/fxml/Login.fxml");
        loginController.setLoginMessage("You need to login to book");
        loginController.setRedirectAfterLogin("/fxml/TrainBooking.fxml");
        System.out.println("SearchTrainController: Redirected to login");
    }

    private void redirectToBookingPage(Train train) {
        System.out.println("SearchTrainController: Redirecting to booking page");
        TrainBookingController bookingController = SceneManager.switchScene("/fxml/TrainBooking.fxml");
        bookingController.setBookingData(
                train.getTrainId(),
                train.getTrainNumber(),
                train.getName(),
                fromStation,
                toStation,
                journeyDate
        );
        System.out.println("SearchTrainController: Successfully loaded booking page");
    }

    // -------------------------------------------------------------------------
    // FXML Action Handlers
    // -------------------------------------------------------------------------

    @FXML
    public void handleLoadMore(ActionEvent event) {
        System.out.println("SearchTrainController: Loading more trains");
        loadMoreTrains(allTrains);
    }

    @FXML
    public void handleBack(ActionEvent event) {
        System.out.println("SearchTrainController: Going back to main menu");
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    @FXML
    public void handleModifySearch(ActionEvent event) {
        System.out.println("SearchTrainController: Modifying search");
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    // -------------------------------------------------------------------------
    // Utility Methods
    // -------------------------------------------------------------------------

    private String getStationNameById(int stationId) {
        try {
            Station station = stationDAO.getStationById(stationId);
            return station != null ? station.getName() : "Unknown Station";
        } catch (Exception e) {
            System.err.println("Error getting station name for ID " + stationId + ": " + e.getMessage());
            return "Unknown Station";
        }
    }

    private boolean isPopularRoute(String from, String to) {
        if (from == null || to == null) return false;
        String route = from.toLowerCase() + "-" + to.toLowerCase();
        return (route.contains("delhi") && route.contains("mumbai")) ||
                (route.contains("bangalore") && route.contains("chennai")) ||
                (route.contains("kolkata") && route.contains("delhi"));
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);

            try {
                alert.getDialogPane().getStylesheets().add(
                        getClass().getResource("/css/MainMenu.css").toExternalForm()
                );
            } catch (Exception e) {
                // Ignore styling errors
            }

            alert.showAndWait();
        });
    }

    // -------------------------------------------------------------------------
    // Inner Classes for Consistent Data Management
    // -------------------------------------------------------------------------

    /**
     * Stores consistent calculated data for each train.
     */
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