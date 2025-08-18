package trainapp.controller.ui;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import trainapp.dao.StationDAO;
import trainapp.model.Station;
import trainapp.model.Train;
import trainapp.service.SessionManager;
import trainapp.service.TrainService;
import trainapp.util.SceneManager;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * SearchTrainController manages train search results display and booking initiation.
 * Provides comprehensive train information with advanced filtering, sorting, and dynamic loading.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Dynamic train search results with pagination and lazy loading</li>
 *   <li>Advanced sorting options (departure, arrival, duration, price)</li>
 *   <li>Real-time seat availability display across all classes</li>
 *   <li>Contextual booking flow with authentication integration</li>
 *   <li>Animated card-based display with smooth transitions</li>
 *   <li>Comprehensive train details with amenities and route information</li>
 * </ul>
 *
 * <p>Search and Display Features:
 * <ul>
 *   <li>Asynchronous train data loading with progress indicators</li>
 *   <li>Multi-criteria sorting with user-friendly labels</li>
 *   <li>Paginated results display for optimal performance</li>
 *   <li>Station name resolution with error handling</li>
 *   <li>Duration and distance calculations between stations</li>
 * </ul>
 *
 * <p>User Experience Features:
 * <ul>
 *   <li>Smooth card animations with staggered entrance effects</li>
 *   <li>Contextual action buttons based on user authentication</li>
 *   <li>Responsive layout with flexible view options</li>
 *   <li>Error handling with user-friendly messages</li>
 *   <li>Session management for booking flow continuity</li>
 * </ul>
 */
public class SearchTrainController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // Search Header Information
    @FXML private Label fromStationLabel;
    @FXML private Label toStationLabel;
    @FXML private Label dateLabel;
    @FXML private Label resultsCountLabel;

    // Control and Filter Elements
    @FXML private ComboBox<String> sortCombo;
    @FXML private ToggleButton listViewToggle;
    @FXML private ToggleButton cardViewToggle;

    // Content Display Areas
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

    // -------------------------------------------------------------------------
    // Search State and Configuration
    // -------------------------------------------------------------------------

    private String fromStation;
    private String toStation;
    private LocalDate journeyDate;
    private List<Train> allTrains = new ArrayList<>();
    private int displayedTrains = 0;
    private static final int TRAINS_PER_PAGE = 10;

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the search results interface with default configurations.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        System.out.println("SearchTrainController: Initializing controller");
        setupSortCombo();
        setupViewToggles();
        initializeDefaultLabels();
    }

    /**
     * Sets up the sorting combo box with available sort options.
     */
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

    /**
     * Sets up view toggle buttons for list and card display modes.
     */
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

    /**
     * Initializes default label values for testing and fallback scenarios.
     */
    private void initializeDefaultLabels() {
        if (fromStationLabel != null) fromStationLabel.setText("Source Station");
        if (toStationLabel != null) toStationLabel.setText("Destination Station");
        if (dateLabel != null) dateLabel.setText(LocalDate.now().toString());
        if (resultsCountLabel != null) resultsCountLabel.setText("Searching...");
    }

    // -------------------------------------------------------------------------
    // Search Data Configuration
    // -------------------------------------------------------------------------

    /**
     * Sets search parameters and initiates train data loading.
     *
     * @param fromStation departure station name
     * @param toStation destination station name
     * @param journeyDate date of travel
     */
    public void setSearchData(String fromStation, String toStation, LocalDate journeyDate) {
        System.out.println("SearchTrainController: Setting search data - From: " + fromStation +
                ", To: " + toStation + ", Date: " + journeyDate);

        this.fromStation = fromStation;
        this.toStation = toStation;
        this.journeyDate = journeyDate;

        updateHeaderLabels();
        loadTrainDataAsync();
    }

    /**
     * Updates header labels with current search parameters.
     */
    private void updateHeaderLabels() {
        if (fromStationLabel != null) fromStationLabel.setText(fromStation != null ? fromStation : "Source");
        if (toStationLabel != null) toStationLabel.setText(toStation != null ? toStation : "Destination");

        if (dateLabel != null && journeyDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
            dateLabel.setText(journeyDate.format(formatter));
        }
    }

    // -------------------------------------------------------------------------
    // Asynchronous Data Loading
    // -------------------------------------------------------------------------

    /**
     * Loads train data asynchronously with proper loading state management.
     */
    private void loadTrainDataAsync() {
        System.out.println("SearchTrainController: Starting async train data load");
        showLoadingState(true);

        Task<List<Train>> loadTask = createTrainLoadingTask();
        configureTaskHandlers(loadTask);
        new Thread(loadTask).start();
    }

    /**
     * Creates the background task for loading train data.
     *
     * @return configured Task for train data loading
     */
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

                System.out.println("SearchTrainController: Background task - found " + trains.size() + " trains");
                return trains;
            }
        };
    }

    /**
     * Configures success and failure handlers for the train loading task.
     *
     * @param loadTask the task to configure handlers for
     */
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

    /**
     * Shows or hides the loading state display.
     *
     * @param show true to show loading state
     */
    private void showLoadingState(boolean show) {
        if (loadingState != null) {
            loadingState.setVisible(show);
            loadingState.setManaged(show);
        }
    }

    // -------------------------------------------------------------------------
    // Results Display and Pagination
    // -------------------------------------------------------------------------

    /**
     * Rebuilds the complete train cards display with current data and settings.
     */
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

    /**
     * Clears the current display and resets pagination state.
     */
    private void clearDisplay() {
        trainsContainer.getChildren().clear();
        displayedTrains = 0;
    }

    /**
     * Updates the results count display label.
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

        if (isEmpty) {
            System.out.println("SearchTrainController: No trains to display");
        }
    }

    /**
     * Loads additional trains using pagination with animation effects.
     *
     * @param trains the complete list of trains
     */
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

    /**
     * Updates the load more button visibility based on available trains.
     *
     * @param trains the complete list of trains
     */
    private void updateLoadMoreButton(List<Train> trains) {
        boolean hasMore = displayedTrains < trains.size();
        if (loadMoreSection != null) {
            loadMoreSection.setVisible(hasMore);
            loadMoreSection.setManaged(hasMore);
        }
    }

    // -------------------------------------------------------------------------
    // Train Card Creation and Display
    // -------------------------------------------------------------------------

    /**
     * Creates an enhanced train card with comprehensive information display.
     *
     * @param train the train to create a card for
     * @return configured VBox containing the train card
     */
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

    /**
     * Creates the train header section with basic information and amenities.
     *
     * @param train the train to create header for
     * @return configured HBox containing train header
     */
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

    /**
     * Creates the train information section with number, name, and amenities.
     *
     * @param train the train to create info for
     * @return configured VBox containing train information
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
     * Creates amenities tags display for the train.
     *
     * @param train the train to create amenities for
     * @return configured HBox containing amenity tags
     */
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

    /**
     * Creates a spacer region for header layout.
     *
     * @return configured Region as spacer
     */
    private Region createHeaderSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /**
     * Creates the route section with departure, duration, and arrival information.
     *
     * @param train the train to create route section for
     * @return configured HBox containing route information
     */
    private HBox createRouteSection(Train train) {
        HBox route = new HBox(40);
        route.getStyleClass().add("route-box");
        route.setAlignment(Pos.CENTER_LEFT);

        VBox depInfo = createDepartureInfo(train);
        VBox durationInfo = createJourneyDurationInfo(train);
        VBox arrInfo = createArrivalInfo(train);

        route.getChildren().addAll(depInfo, durationInfo, arrInfo);
        return route;
    }

    /**
     * Creates departure information display.
     *
     * @param train the train to create departure info for
     * @return configured VBox containing departure information
     */
    private VBox createDepartureInfo(Train train) {
        String sourceStationName = getStationNameById(train.getSourceStationId());
        String depTime = trainService.getDepartureTime(train, sourceStationName);
        return createStationInfo(depTime, sourceStationName);
    }

    /**
     * Creates journey duration and details information.
     *
     * @param train the train to create duration info for
     * @return configured VBox containing duration information
     */
    private VBox createJourneyDurationInfo(Train train) {
        String duration = trainService.calculateDuration(train, fromStation, toStation);
        int halts = trainService.getHaltsBetween(train, fromStation, toStation);
        int distance = trainService.getDistanceBetween(train, fromStation, toStation);
        return createDurationInfo(duration, halts + " halts • " + distance + " km");
    }

    /**
     * Creates arrival information display.
     *
     * @param train the train to create arrival info for
     * @return configured VBox containing arrival information
     */
    private VBox createArrivalInfo(Train train) {
        String destStationName = getStationNameById(train.getDestinationStationId());
        String arrTime = trainService.getArrivalTime(train, destStationName);
        return createStationInfo(arrTime, destStationName);
    }

    /**
     * Creates the action section with view details and book buttons.
     *
     * @param train the train to create actions for
     * @return configured HBox containing action buttons
     */
    private HBox createActionSection(Train train) {
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setStyle("-fx-padding: 12 0 0 0;");

        Button viewDetails = createViewDetailsButton(train);
        Button bookNow = createBookNowButton(train);

        actions.getChildren().addAll(viewDetails, bookNow);
        return actions;
    }

    /**
     * Creates the view details button.
     *
     * @param train the train for the button
     * @return configured Button for viewing details
     */
    private Button createViewDetailsButton(Train train) {
        Button viewDetails = new Button("View Details");
        viewDetails.getStyleClass().addAll("ghost-btn-small");
        viewDetails.setOnAction(e -> handleViewDetails(train));
        return viewDetails;
    }

    /**
     * Creates the book now button.
     *
     * @param train the train for the button
     * @return configured Button for booking
     */
    private Button createBookNowButton(Train train) {
        Button bookNow = new Button("Book Now");
        bookNow.getStyleClass().addAll("empty-action-primary");
        bookNow.setOnAction(e -> handleBookTrain(train));
        return bookNow;
    }

    // -------------------------------------------------------------------------
    // UI Component Creation Utilities
    // -------------------------------------------------------------------------

    /**
     * Creates seat availability section for different classes.
     *
     * @param train the train to create seats section for
     * @return configured HBox containing seat information
     */
    private HBox createSeatsSection(Train train) {
        HBox seats = new HBox(12);
        seats.getStyleClass().add("seats-section");
        seats.setAlignment(Pos.CENTER_RIGHT);

        Map<String, Integer> seatMap = trainService.getAvailableSeatsForDate(train, journeyDate);

        seats.getChildren().addAll(
                createSeatBox("SL", seatMap.getOrDefault("SL", 0)),
                createSeatBox("3A", seatMap.getOrDefault("3A", 0)),
                createSeatBox("2A", seatMap.getOrDefault("2A", 0)),
                createSeatBox("1A", seatMap.getOrDefault("1A", 0))
        );

        return seats;
    }

    /**
     * Creates individual seat class availability box.
     *
     * @param className the class name (SL, 3A, 2A, 1A)
     * @param count the available seat count
     * @return configured VBox containing seat class information
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
     * @param time the departure/arrival time
     * @param stationName the station name
     * @return configured VBox containing station information
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
     * Creates duration information display with journey details.
     *
     * @param duration the journey duration
     * @param details additional journey details (halts, distance)
     * @return configured VBox containing duration information
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

    /**
     * Creates a styled tag label.
     *
     * @param text the tag text
     * @param styleClass the CSS style class
     * @return configured Label as tag
     */
    private Label createTag(String text, String styleClass) {
        Label tag = new Label(text);
        tag.getStyleClass().add(styleClass);
        return tag;
    }

    // -------------------------------------------------------------------------
    // Animation and Visual Effects
    // -------------------------------------------------------------------------

    /**
     * Animates card entrance with fade and slide effects.
     *
     * @param card the card to animate
     * @param index the card index for staggered animation
     */
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

    /**
     * Reloads and sorts trains based on selected sort option.
     *
     * @param sortOption the selected sort option
     */
    private void reloadWithSort(String sortOption) {
        System.out.println("SearchTrainController: Sorting by " + sortOption);

        if (allTrains.isEmpty()) return;

        applySortingStrategy(sortOption);
        rebuildCards();
    }

    /**
     * Applies the appropriate sorting strategy based on user selection.
     *
     * @param sortOption the selected sort option
     */
    private void applySortingStrategy(String sortOption) {
        switch (sortOption) {
            case "🕐 Departure Time":
                allTrains.sort((t1, t2) -> {
                    String time1 = trainService.getDepartureTime(t1, fromStation);
                    String time2 = trainService.getDepartureTime(t2, fromStation);
                    return time1.compareTo(time2);
                });
                break;
            case "🕐 Arrival Time":
                allTrains.sort((t1, t2) -> {
                    String time1 = trainService.getArrivalTime(t1, toStation);
                    String time2 = trainService.getArrivalTime(t2, toStation);
                    return time1.compareTo(time2);
                });
                break;
            case "⏱️ Duration":
                allTrains.sort((t1, t2) -> {
                    String dur1 = trainService.calculateDuration(t1, fromStation, toStation);
                    String dur2 = trainService.calculateDuration(t2, fromStation, toStation);
                    return dur1.compareTo(dur2);
                });
                break;
            case "💰 Price":
                allTrains.sort((t1, t2) -> Integer.compare(t1.getTotalCoaches(), t2.getTotalCoaches()));
                break;
            default: // Recommended
                // Keep original order or implement recommendation logic
                break;
        }
    }

    // -------------------------------------------------------------------------
    // Action Handlers for User Interactions
    // -------------------------------------------------------------------------

    /**
     * Handles viewing detailed train information.
     * Opens train details window with comprehensive information.
     *
     * @param train the train to view details for
     */
    private void handleViewDetails(Train train) {
        System.out.println("SearchTrainController: Opening train details for " + train.getTrainNumber());
        TrainDetailsController detailsController = SceneManager.switchScene("/fxml/TrainDetails.fxml");
        detailsController.setTrainDetails(train, journeyDate, fromStation, toStation);
        System.out.println("SearchTrainController: Train details window opened successfully");
    }

    /**
     * Handles train booking with authentication flow management.
     * Redirects to login if not authenticated, otherwise proceeds to booking.
     *
     * @param train the train to book
     */
    private void handleBookTrain(Train train) {
        System.out.println("SearchTrainController: Booking train " + train.getTrainNumber());

        if (!sessionManager.isLoggedIn()) {
            handleUnauthenticatedBooking(train);
        } else {
            redirectToBookingPage(train);
        }
    }

    /**
     * Handles booking attempt by unauthenticated user.
     * Sets up pending booking and redirects to login.
     *
     * @param train the train to book
     */
    private void handleUnauthenticatedBooking(Train train) {
        sessionManager.setPendingBooking(train.getTrainId(), fromStation, toStation, journeyDate);
        LoginController loginController = SceneManager.switchScene("/fxml/Login.fxml");
        loginController.setLoginMessage("You need to login to book");
        loginController.setRedirectAfterLogin("/fxml/TrainBooking.fxml");
        System.out.println("SearchTrainController: Redirected to login");
    }

    /**
     * Redirects authenticated user to booking page with train data.
     *
     * @param train the train to book
     */
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

    /**
     * Handles load more button click to display additional trains.
     *
     * @param event ActionEvent from load more button
     */
    @FXML
    public void handleLoadMore(ActionEvent event) {
        System.out.println("SearchTrainController: Loading more trains");
        loadMoreTrains(allTrains);
    }

    /**
     * Handles back navigation to main menu.
     *
     * @param event ActionEvent from back button
     */
    @FXML
    public void handleBack(ActionEvent event) {
        System.out.println("SearchTrainController: Going back to main menu");
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    /**
     * Handles search modification request.
     * Returns user to main menu for new search.
     *
     * @param event ActionEvent from modify search button
     */
    @FXML
    public void handleModifySearch(ActionEvent event) {
        System.out.println("SearchTrainController: Modifying search");
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    // -------------------------------------------------------------------------
    // Utility Methods
    // -------------------------------------------------------------------------

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
            System.err.println("Error getting station name for ID " + stationId + ": " + e.getMessage());
            return "Unknown Station";
        }
    }

    /**
     * Displays error message in styled alert dialog.
     *
     * @param message the error message to display
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Apply custom styling if available
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
}
