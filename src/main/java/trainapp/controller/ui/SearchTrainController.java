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
import java.util.stream.Collectors;

/**
 * SearchTrainController manages train search results display with advanced filtering and performance optimization.
 *
 * <h2>Core Responsibilities:</h2>
 * <ul>
 *   <li><b>Search Results Management</b> - Displays comprehensive train search results with detailed information</li>
 *   <li><b>Performance Optimization</b> - Implements caching, lazy loading, and batch processing for enhanced speed</li>
 *   <li><b>Advanced Filtering</b> - Supports multiple sorting options (time, price, duration, recommendation)</li>
 *   <li><b>Dynamic Loading</b> - Paginated results with load-more functionality for better user experience</li>
 *   <li><b>Consistent Pricing</b> - Ensures pricing consistency across search, details, and booking pages</li>
 *   <li><b>User Experience</b> - Smooth animations, responsive design, and intuitive navigation</li>
 * </ul>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Optimized async data loading with parallel processing</li>
 *   <li>Intelligent caching system for train data and station names</li>
 *   <li>Real-time seat availability checking with pricing information</li>
 *   <li>Multiple view modes (list and card views) with smooth transitions</li>
 *   <li>Advanced sorting algorithms for optimal result ordering</li>
 *   <li>Pagination system with configurable page sizes</li>
 *   <li>Responsive UI with loading states and error handling</li>
 * </ul>
 *
 * <h2>Performance Optimizations:</h2>
 * <ul>
 *   <li>Parallel stream processing for data calculations</li>
 *   <li>Efficient caching mechanisms reducing database calls</li>
 *   <li>Batch UI updates minimizing layout recalculations</li>
 *   <li>Optimized animation timing for smooth user experience</li>
 *   <li>Smart data loading preventing unnecessary API calls</li>
 * </ul>
 *
 * <h2>Search Flow:</h2>
 * <ol>
 *   <li>User searches for trains between stations on specific date</li>
 *   <li>Controller loads and processes train data asynchronously</li>
 *   <li>Results displayed with pagination and sorting options</li>
 *   <li>User can filter, sort, and navigate to detailed views</li>
 *   <li>Seamless integration with booking and authentication systems</li>
 * </ol>
 */
public class SearchTrainController {

    // =========================================================================
    // FXML UI COMPONENTS
    // =========================================================================

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

    @FXML private Button loadMoreButton;

    // =========================================================================
    // SERVICES AND DEPENDENCIES
    // =========================================================================

    /** Service for train-related operations */
    private final TrainService trainService = new TrainService();

    /** Data access object for station information */
    private final StationDAO stationDAO = new StationDAO();

    /** Session management for user authentication */
    private final SessionManager sessionManager = SessionManager.getInstance();

    /** Service for dynamic pricing calculations */
    private final AdminDataStructureService adminService = new AdminDataStructureService();

    // =========================================================================
    // STATE MANAGEMENT
    // =========================================================================

    /** Source station for the search query */
    private String fromStation;

    /** Destination station for the search query */
    private String toStation;

    /** Journey date for the search query */
    private LocalDate journeyDate;

    /** Complete list of trains from search results */
    private List<Train> allTrains = new ArrayList<>();

    /** Number of trains currently displayed */
    private int displayedTrains = 0;

    /** Number of trains to display per page load */
    private static final int TRAINS_PER_PAGE = 10;

    // Performance Caches
    /** Cache for consistent train data calculations */
    private Map<String, ConsistentTrainData> trainDataCache = new HashMap<>();

    /** Cache for station ID to name mappings */
    private Map<Integer, String> stationNameCache = new HashMap<>();

    /** Consistent distance calculation for the route */
    private int consistentDistance = -1;

    // =========================================================================
    // INITIALIZATION AND SETUP
    // =========================================================================

    /**
     * Initializes the search controller and sets up UI components.
     * Called automatically by JavaFX framework after FXML loading.
     */
    @FXML
    public void initialize() {
        setupSortCombo();
        setupViewToggles();
        initializeDefaultLabels();
    }

    /**
     * Sets up the sort dropdown with available options and change listener.
     */
    private void setupSortCombo() {
        if (sortCombo != null) {
            sortCombo.getItems().setAll(
                    "🏆 Recommended", "🕐 Departure Time", "🕐 Arrival Time",
                    "⏱️ Duration", "💰 Price"
            );
            sortCombo.setValue("🏆 Recommended");
            sortCombo.valueProperty().addListener((obs, oldV, newV) -> reloadWithSort(newV));
        }
    }

    /**
     * Sets up view toggle buttons for switching between list and card views.
     */
    private void setupViewToggles() {
        if (listViewToggle != null && cardViewToggle != null) {
            ToggleGroup viewToggle = new ToggleGroup();
            listViewToggle.setToggleGroup(viewToggle);
            cardViewToggle.setToggleGroup(viewToggle);
            listViewToggle.setSelected(true);
            viewToggle.selectedToggleProperty().addListener((obs, oldT, newT) -> rebuildCards());
        }
    }

    /**
     * Initializes default label text for UI components.
     */
    private void initializeDefaultLabels() {
        if (fromStationLabel != null) fromStationLabel.setText("Source Station");
        if (toStationLabel != null) toStationLabel.setText("Destination Station");
        if (dateLabel != null) dateLabel.setText(LocalDate.now().toString());
        if (resultsCountLabel != null) resultsCountLabel.setText("Searching...");
    }

    // =========================================================================
    // DATA LOADING AND PROCESSING
    // =========================================================================

    /**
     * Sets search parameters and initiates train data loading.
     *
     * @param fromStation Source station name
     * @param toStation Destination station name
     * @param journeyDate Date of travel
     */
    public void setSearchData(String fromStation, String toStation, LocalDate journeyDate) {
        this.fromStation = fromStation;
        this.toStation = toStation;
        this.journeyDate = journeyDate;
        updateHeaderLabels();
        loadTrainDataAsync();
    }

    /**
     * Updates header labels with search criteria information.
     */
    private void updateHeaderLabels() {
        if (fromStationLabel != null) fromStationLabel.setText(fromStation != null ? fromStation : "Source");
        if (toStationLabel != null) toStationLabel.setText(toStation != null ? toStation : "Destination");

        if (dateLabel != null && journeyDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
            dateLabel.setText(journeyDate.format(formatter));
        }
    }

    /**
     * Loads train data asynchronously with performance optimizations.
     * Uses parallel processing for enhanced speed and responsiveness.
     */
    private void loadTrainDataAsync() {
        showLoadingState(true);

        Task<List<Train>> loadTask = new Task<List<Train>>() {
            @Override
            protected List<Train> call() throws Exception {
                List<Train> trains = trainService.findTrainsBetweenStations(
                        fromStation != null ? fromStation : "Null",
                        toStation != null ? toStation : "Null"
                );

                // Optimized: Parallel processing for data calculation
                if (!trains.isEmpty()) {
                    trains.parallelStream().forEach(train -> calculateConsistentDataForTrain(train));
                    calculateConsistentDistance(trains);
                }

                return trains;
            }
        };

        loadTask.setOnSucceeded(e -> {
            allTrains = loadTask.getValue();
            displayedTrains = 0;
            showLoadingState(false);
            
            if (allTrains == null || allTrains.isEmpty()) {
                handleNoTrainsFound();
            } else {
                rebuildCards();
            }
        });

        loadTask.setOnFailed(e -> {
            showLoadingState(false);
            Throwable exception = loadTask.getException();
            if (exception != null) {
                exception.printStackTrace();
            }
            showError("Failed to load trains. Please try again.");
        });

        new Thread(loadTask).start();
    }

    /**
     * Calculates consistent distance for all trains in a single pass.
     * Updates cache with consistent distance values.
     *
     * @param trains List of trains to process
     */
    private void calculateConsistentDistance(List<Train> trains) {
        // Find max distance in one pass
        consistentDistance = trains.stream()
                .mapToInt(train -> trainService.getDistanceBetween(train, fromStation, toStation))
                .max()
                .orElse(-1);

        // Batch update cache
        trains.forEach(train -> {
            ConsistentTrainData data = trainDataCache.get(train.getTrainNumber());
            if (data != null) {
                trainDataCache.put(train.getTrainNumber(), new ConsistentTrainData(
                        consistentDistance, data.departureTime, data.arrivalTime, data.duration, data.pricing));
            }
        });
    }

    /**
     * Calculates consistent data for a single train including timing and pricing.
     *
     * @param train Train object to process
     */
    private void calculateConsistentDataForTrain(Train train) {
        try {
            int distance = trainService.getDistanceBetween(train, fromStation, toStation);
            String departureTime = trainService.getDepartureTime(train, fromStation);
            String arrivalTime = trainService.getArrivalTime(train, toStation);
            String duration = trainService.calculateDuration(train, fromStation, toStation);
            Map<String, Double> pricing = calculateConsistentPricingForTrain(train, distance);

            trainDataCache.put(train.getTrainNumber(),
                    new ConsistentTrainData(distance, departureTime, arrivalTime, duration, pricing));

        } catch (Exception e) {
            // Silent error handling - could use logger instead
        }
    }

    /**
     * Calculates consistent pricing for all seat classes with surge pricing.
     *
     * @param train Train object for pricing calculation
     * @param distance Journey distance in kilometers
     * @return Map of class names to calculated prices
     */
    private Map<String, Double> calculateConsistentPricingForTrain(Train train, int distance) {
        Map<String, Double> pricing = new HashMap<>();

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

    // =========================================================================
    // UI DISPLAY AND MANAGEMENT
    // =========================================================================

    /**
     * Controls loading state visibility for user feedback.
     *
     * @param show true to show loading state, false to hide
     */
    private void showLoadingState(boolean show) {
        if (loadingState != null) {
            loadingState.setVisible(show);
            loadingState.setManaged(show);
        }
    }

    /**
     * Rebuilds train cards with current data and settings.
     * Optimized for batch processing and efficient UI updates.
     */
    private void rebuildCards() {
        if (trainsContainer == null) return;

        clearDisplay();
        updateResultsCount();
        handleEmptyState();

        if (!allTrains.isEmpty()) {
            loadMoreTrains();
        }
    }

    /**
     * Clears current display and resets pagination counter.
     */
    private void clearDisplay() {
        trainsContainer.getChildren().clear();
        displayedTrains = 0;
    }

    /**
     * Updates results count label with current search results.
     */
    private void updateResultsCount() {
        if (resultsCountLabel != null) {
            resultsCountLabel.setText("Found " + allTrains.size() + " trains");
        }
    }

    /**
     * Handles empty state display when no trains are found.
     */
    private void handleEmptyState() {
        boolean isEmpty = allTrains.isEmpty();
        if (emptyState != null) {
            emptyState.setVisible(isEmpty);
            emptyState.setManaged(isEmpty);
        }
    }

    /**
     * Loads next batch of trains with pagination support and batch processing.
     */
    private void loadMoreTrains() {
        int startIndex = displayedTrains;
        int endIndex = Math.min(displayedTrains + TRAINS_PER_PAGE, allTrains.size());

        List<Train> trainsToLoad = allTrains.subList(startIndex, endIndex);

        // Optimized: Batch UI operations
        Platform.runLater(() -> {
            List<VBox> cards = trainsToLoad.stream()
                    .map(this::createEnhancedTrainCard)
                    .collect(Collectors.toList());

            trainsContainer.getChildren().addAll(cards);

            // Animate cards in batch
            for (int i = 0; i < cards.size(); i++) {
                animateCardEntrance(cards.get(i), i);
            }

            displayedTrains = endIndex;
            updateLoadMoreButton();
        });
    }

    /**
     * Updates load more button state and text based on remaining trains.
     */
    private void updateLoadMoreButton() {
        boolean hasMore = displayedTrains < allTrains.size();

        if (loadMoreSection != null) {
            loadMoreSection.setVisible(hasMore);
            loadMoreSection.setManaged(hasMore);
        }

        if (loadMoreButton != null && hasMore) {
            int remaining = allTrains.size() - displayedTrains;
            int toLoad = Math.min(remaining, TRAINS_PER_PAGE);
            loadMoreButton.setText("Load " + toLoad + " More Trains (" + remaining + " remaining)");
        }
    }

    // =========================================================================
    // TRAIN CARD CREATION
    // =========================================================================

    /**
     * Creates comprehensive train card with all relevant information.
     *
     * @param train Train object to create card for
     * @return Complete VBox train card
     */
    private VBox createEnhancedTrainCard(Train train) {
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

    /**
     * Creates train header section with information and seat availability.
     *
     * @param train Train object for header creation
     * @return HBox containing train header elements
     */
    private HBox createTrainHeader(Train train) {
        HBox header = new HBox(12);
        header.getStyleClass().add("train-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox info = createTrainInfoSection(train);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox seats = createSeatsSection(train);

        header.getChildren().addAll(info, spacer, seats);
        return header;
    }

    /**
     * Creates train information section with number, name, and amenities.
     *
     * @param train Train object for information display
     * @return VBox containing train information
     */
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

    /**
     * Creates amenities tags display for train features.
     *
     * @param train Train object to get amenities for
     * @return HBox containing amenity tags
     */
    private HBox createAmenitiesTags(Train train) {
        HBox tags = new HBox(6);
        tags.setAlignment(Pos.CENTER_LEFT);

        List<String> amenities = trainService.getTrainAmenities(train);
        amenities.stream()
                .limit(3)
                .forEach(amenity -> {
                    Label tag = new Label(amenity);
                    tag.getStyleClass().add("tag-chip");
                    tags.getChildren().add(tag);
                });

        return tags;
    }

    /**
     * Creates route section showing departure, journey, and arrival information.
     *
     * @param train Train object for route display
     * @return HBox containing route information
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

    /**
     * Creates departure information display.
     *
     * @param train Train object
     * @param data Consistent train data from cache
     * @return VBox with departure information
     */
    private VBox createDepartureInfo(Train train, ConsistentTrainData data) {
        String sourceStationName = getStationNameById(train.getSourceStationId());
        String depTime = data != null ? data.departureTime : "06:00";
        return createStationInfo(depTime, sourceStationName);
    }

    /**
     * Creates journey duration and details information.
     *
     * @param train Train object
     * @param data Consistent train data from cache
     * @return VBox with journey duration information
     */
    private VBox createJourneyDurationInfo(Train train, ConsistentTrainData data) {
        if (data != null) {
            int halts = trainService.getHaltsBetween(train, fromStation, toStation);
            return createDurationInfo(data.duration, halts + " halts • " + data.distance + " km");
        } else {
            String duration = trainService.calculateDuration(train, fromStation, toStation);
            int halts = trainService.getHaltsBetween(train, fromStation, toStation);
            int distance = consistentDistance > 0 ? consistentDistance :
                    trainService.getDistanceBetween(train, fromStation, toStation);
            return createDurationInfo(duration, halts + " halts • " + distance + " km");
        }
    }

    /**
     * Creates arrival information display.
     *
     * @param train Train object
     * @param data Consistent train data from cache
     * @return VBox with arrival information
     */
    private VBox createArrivalInfo(Train train, ConsistentTrainData data) {
        String destStationName = getStationNameById(train.getDestinationStationId());
        String arrTime = data != null ? data.arrivalTime : "18:30";
        return createStationInfo(arrTime, destStationName);
    }

    /**
     * Creates action section with view details and book now buttons.
     *
     * @param train Train object for action handling
     * @return HBox containing action buttons
     */
    private HBox createActionSection(Train train) {
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setStyle("-fx-padding: 12 0 0 0;");

        Button viewDetails = new Button("View Details");
        viewDetails.getStyleClass().addAll("ghost-btn-small");
        viewDetails.setOnAction(e -> handleViewDetails(train));

        Button bookNow = new Button("Book Now");
        bookNow.getStyleClass().addAll("empty-action-primary");
        bookNow.setOnAction(e -> handleBookTrain(train));

        actions.getChildren().addAll(viewDetails, bookNow);
        return actions;
    }

    /**
     * Creates seats section showing availability and pricing for all classes.
     *
     * @param train Train object for seat information
     * @return HBox containing seat class information
     */
    private HBox createSeatsSection(Train train) {
        HBox seats = new HBox(12);
        seats.getStyleClass().add("seats-section");
        seats.setAlignment(Pos.CENTER_RIGHT);

        Map<String, Integer> seatMap = trainService.getAvailableSeatsForDate(train, journeyDate);
        ConsistentTrainData data = trainDataCache.get(train.getTrainNumber());

        if (data != null && data.pricing != null) {
            seats.getChildren().addAll(
                    createSeatBoxWithPrice("SL", seatMap.getOrDefault("SL", 0), data.pricing.get("SL")),
                    createSeatBoxWithPrice("3A", seatMap.getOrDefault("3A", 0), data.pricing.get("3A")),
                    createSeatBoxWithPrice("2A", seatMap.getOrDefault("2A", 0), data.pricing.get("2A")),
                    createSeatBoxWithPrice("1A", seatMap.getOrDefault("1A", 0), data.pricing.get("1A"))
            );
        } else {
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
     * Creates seat class box with pricing information.
     *
     * @param className Seat class name
     * @param count Available seat count
     * @param price Price for the class
     * @return VBox containing seat class information
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

    /**
     * Creates seat class box without pricing information.
     *
     * @param className Seat class name
     * @param count Available seat count
     * @return VBox containing seat class information
     */
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

    /**
     * Creates station information display with time and name.
     *
     * @param time Station time (departure/arrival)
     * @param stationName Name of the station
     * @return VBox containing station information
     */
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

    /**
     * Creates journey duration information display.
     *
     * @param duration Journey duration string
     * @param details Additional details (halts, distance)
     * @return VBox containing duration information
     */
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

    // =========================================================================
    // ANIMATION AND VISUAL EFFECTS
    // =========================================================================

    /**
     * Animates card entrance with optimized timing for smooth user experience.
     *
     * @param card VBox card to animate
     * @param index Card index for staggered timing
     */
    private void animateCardEntrance(VBox card, int index) {
        card.setOpacity(0);
        card.setTranslateY(10); // Reduced animation distance

        FadeTransition fade = new FadeTransition(Duration.millis(200), card); // Faster animation
        fade.setToValue(1.0);
        fade.setDelay(Duration.millis(index * 25)); // Reduced delay

        TranslateTransition slide = new TranslateTransition(Duration.millis(200), card);
        slide.setToY(0);
        slide.setDelay(Duration.millis(index * 25));

        fade.play();
        slide.play();
    }

    // =========================================================================
    // SORTING AND FILTERING
    // =========================================================================

    /**
     * Reloads and re-displays results with new sorting option.
     *
     * @param sortOption Selected sorting option from dropdown
     */
    private void reloadWithSort(String sortOption) {
        if (allTrains.isEmpty()) return;
        applySortingStrategy(sortOption);
        rebuildCards();
    }

    /**
     * Applies selected sorting strategy to train list.
     *
     * @param sortOption Selected sorting option
     */
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
        }
    }

    // =========================================================================
    // EVENT HANDLERS
    // =========================================================================

    /**
     * Handles view details button click for train information.
     *
     * @param train Train object to view details for
     */
    private void handleViewDetails(Train train) {
        TrainDetailsController detailsController = SceneManager.switchScene("/fxml/TrainDetails.fxml");
        detailsController.setTrainDetails(train, journeyDate, fromStation, toStation);
    }

    /**
     * Handles book now button click with authentication check.
     *
     * @param train Train object to book
     */
    private void handleBookTrain(Train train) {
        if (!sessionManager.isLoggedIn()) {
            sessionManager.setPendingBooking(train.getTrainId(), fromStation, toStation, journeyDate);
            LoginController loginController = SceneManager.switchScene("/fxml/Login.fxml");
            loginController.setLoginMessage("You need to login to book");
            loginController.setRedirectAfterLogin("/fxml/TrainBooking.fxml");
        } else {
            TrainBookingController bookingController = SceneManager.switchScene("/fxml/TrainBooking.fxml");
            bookingController.setBookingData(
                    train.getTrainId(), train.getTrainNumber(), train.getName(),
                    fromStation, toStation, journeyDate
            );
        }
    }

    /**
     * Handles load more button click to show additional results.
     *
     * @param event Action event from load more button
     */
    @FXML
    public void handleLoadMore(ActionEvent event) {
        loadMoreTrains();
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

    /**
     * Handles modify search navigation to main menu.
     *
     * @param event Action event from modify search button
     */
    @FXML
    public void handleModifySearch(ActionEvent event) {
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    /**
     * Handles the case when no trains are found between selected stations.
     * Shows an informative alert and redirects user back to MainMenu.
     */
    private void handleNoTrainsFound() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Trains Available");
            alert.setHeaderText("No trains found for this route");
            alert.setContentText("Sorry, no trains are available between " + fromStation + " and " + toStation +
                    " on " + journeyDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) +
                    ".\n\nPlease try a different route or date.");

            // Set alert styling
            alert.getDialogPane().getStyleClass().add("alert-dialog");

            // Show alert and wait for user response
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // Redirect to MainMenu
                    SceneManager.switchScene("/fxml/MainMenu.fxml");
                }
            });
        });
    }

    // =========================================================================
    // UTILITY AND HELPER METHODS
    // =========================================================================

    /**
     * Retrieves station name by ID with efficient caching.
     *
     * @param stationId Station ID to look up
     * @return Station name or "Unknown Station" if not found
     */
    private String getStationNameById(int stationId) {
        return stationNameCache.computeIfAbsent(stationId, id -> {
            try {
                Station station = stationDAO.getStationById(id);
                return station != null ? station.getName() : "Unknown Station";
            } catch (Exception e) {
                return "Unknown Station";
            }
        });
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
     * Displays error messages to the user with proper threading.
     *
     * @param message Error message to display
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    /**
     * Data container for consistent train information across the application.
     * Stores computed values to avoid recalculation and ensure consistency.
     */
    private static class ConsistentTrainData {
        /** Journey distance in kilometers */
        public final int distance;

        /** Departure time as formatted string */
        public final String departureTime;

        /** Arrival time as formatted string */
        public final String arrivalTime;

        /** Journey duration as formatted string */
        public final String duration;

        /** Pricing information for all seat classes */
        public final Map<String, Double> pricing;

        /**
         * Creates consistent train data container.
         *
         * @param distance Journey distance in kilometers
         * @param departureTime Departure time string
         * @param arrivalTime Arrival time string
         * @param duration Journey duration string
         * @param pricing Map of class names to prices
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