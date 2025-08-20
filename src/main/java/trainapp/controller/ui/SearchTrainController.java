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
 * Optimized SearchTrainController with enhanced performance
 */
public class SearchTrainController {

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

    // Services
    private final TrainService trainService = new TrainService();
    private final StationDAO stationDAO = new StationDAO();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final AdminDataStructureService adminService = new AdminDataStructureService();

    // State
    private String fromStation;
    private String toStation;
    private LocalDate journeyDate;
    private List<Train> allTrains = new ArrayList<>();
    private int displayedTrains = 0;
    private static final int TRAINS_PER_PAGE = 10;

    // Cache for performance
    private Map<String, ConsistentTrainData> trainDataCache = new HashMap<>();
    private Map<Integer, String> stationNameCache = new HashMap<>();
    private int consistentDistance = -1;

    @FXML
    public void initialize() {
        setupSortCombo();
        setupViewToggles();
        initializeDefaultLabels();
    }

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

    private void setupViewToggles() {
        if (listViewToggle != null && cardViewToggle != null) {
            ToggleGroup viewToggle = new ToggleGroup();
            listViewToggle.setToggleGroup(viewToggle);
            cardViewToggle.setToggleGroup(viewToggle);
            listViewToggle.setSelected(true);
            viewToggle.selectedToggleProperty().addListener((obs, oldT, newT) -> rebuildCards());
        }
    }

    private void initializeDefaultLabels() {
        if (fromStationLabel != null) fromStationLabel.setText("Source Station");
        if (toStationLabel != null) toStationLabel.setText("Destination Station");
        if (dateLabel != null) dateLabel.setText(LocalDate.now().toString());
        if (resultsCountLabel != null) resultsCountLabel.setText("Searching...");
    }

    public void setSearchData(String fromStation, String toStation, LocalDate journeyDate) {
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

    // OPTIMIZED: Faster async loading
    private void loadTrainDataAsync() {
        showLoadingState(true);

        Task<List<Train>> loadTask = new Task<List<Train>>() {
            @Override
            protected List<Train> call() throws Exception {
                // REMOVED: Thread.sleep(500) - no artificial delay

                List<Train> trains = trainService.findTrainsBetweenStations(
                        fromStation != null ? fromStation : "Null",
                        toStation != null ? toStation : "Null"
                );

                // OPTIMIZED: Parallel processing for data calculation
                trains.parallelStream().forEach(train -> calculateConsistentDataForTrain(train));
                calculateConsistentDistance(trains);

                return trains;
            }
        };

        loadTask.setOnSucceeded(e -> {
            allTrains = loadTask.getValue();
            displayedTrains = 0;
            showLoadingState(false);
            rebuildCards();
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

    // OPTIMIZED: Efficient distance calculation
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

    private void showLoadingState(boolean show) {
        if (loadingState != null) {
            loadingState.setVisible(show);
            loadingState.setManaged(show);
        }
    }

    // OPTIMIZED: Batch UI updates
    private void rebuildCards() {
        if (trainsContainer == null) return;

        clearDisplay();
        updateResultsCount();
        handleEmptyState();

        if (!allTrains.isEmpty()) {
            loadMoreTrains();
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
    }

    // OPTIMIZED: Efficient load more with batching
    private void loadMoreTrains() {
        int startIndex = displayedTrains;
        int endIndex = Math.min(displayedTrains + TRAINS_PER_PAGE, allTrains.size());

        List<Train> trainsToLoad = allTrains.subList(startIndex, endIndex);

        // OPTIMIZED: Batch UI operations
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

    // OPTIMIZED: Simplified card creation
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
        amenities.stream()
                .limit(3)
                .forEach(amenity -> {
                    Label tag = new Label(amenity);
                    tag.getStyleClass().add("tag-chip");
                    tags.getChildren().add(tag);
                });

        return tags;
    }

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

    private VBox createArrivalInfo(Train train, ConsistentTrainData data) {
        String destStationName = getStationNameById(train.getDestinationStationId());
        String arrTime = data != null ? data.arrivalTime : "18:30";
        return createStationInfo(arrTime, destStationName);
    }

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

    // OPTIMIZED: Faster animation
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

    private void reloadWithSort(String sortOption) {
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
        }
    }

    // Event Handlers
    private void handleViewDetails(Train train) {
        TrainDetailsController detailsController = SceneManager.switchScene("/fxml/TrainDetails.fxml");
        detailsController.setTrainDetails(train, journeyDate, fromStation, toStation);
    }

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

    @FXML
    public void handleLoadMore(ActionEvent event) {
        loadMoreTrains();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    @FXML
    public void handleModifySearch(ActionEvent event) {
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    // OPTIMIZED: Cached station name lookup
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
            alert.showAndWait();
        });
    }

    // Inner class for data storage
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