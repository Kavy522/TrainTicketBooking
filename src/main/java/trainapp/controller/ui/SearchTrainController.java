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

    private final TrainService trainService = new TrainService();
    private final StationDAO stationDAO = new StationDAO();
    private final SessionManager sessionManager = SessionManager.getInstance();

    private String fromStation;
    private String toStation;
    private LocalDate journeyDate;
    private List<Train> allTrains = new ArrayList<>();
    private int displayedTrains = 0;
    private static final int TRAINS_PER_PAGE = 10;

    @FXML
    public void initialize() {
        System.out.println("SearchTrainController: Initializing controller");
        setupSortCombo();
        setupViewToggles();

        // Set default values for testing
        if (fromStationLabel != null) fromStationLabel.setText("Source Station");
        if (toStationLabel != null) toStationLabel.setText("Destination Station");
        if (dateLabel != null) dateLabel.setText(LocalDate.now().toString());
        if (resultsCountLabel != null) resultsCountLabel.setText("Searching...");
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

    private void loadTrainDataAsync() {
        System.out.println("SearchTrainController: Starting async train data load");
        showLoadingState(true);

        Task<List<Train>> loadTask = new Task<List<Train>>() {
            @Override
            protected List<Train> call() throws Exception {
                System.out.println("SearchTrainController: Background task - loading trains from: " +
                        fromStation + " to: " + toStation);

                // Simulate loading time
                Thread.sleep(500);

                List<Train> trains = trainService.findTrainsBetweenStations(
                        fromStation != null ? fromStation : "Null",
                        toStation != null ? toStation : "Null"
                );

                System.out.println("SearchTrainController: Background task - found " + trains.size() + " trains");
                return trains;
            }
        };

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

        new Thread(loadTask).start();
    }

    private void showLoadingState(boolean show) {
        if (loadingState != null) {
            loadingState.setVisible(show);
            loadingState.setManaged(show);
        }
    }

    private void rebuildCards() {
        System.out.println("SearchTrainController: Rebuilding cards with " + allTrains.size() + " trains");

        if (trainsContainer == null) {
            System.err.println("SearchTrainController: trainsContainer is null!");
            return;
        }

        trainsContainer.getChildren().clear();
        displayedTrains = 0;

        if (resultsCountLabel != null) {
            resultsCountLabel.setText("Found " + allTrains.size() + " trains");
        }

        boolean isEmpty = allTrains.isEmpty();
        if (emptyState != null) {
            emptyState.setVisible(isEmpty);
            emptyState.setManaged(isEmpty);
        }

        if (isEmpty) {
            System.out.println("SearchTrainController: No trains to display");
            return;
        }

        loadMoreTrains(allTrains);
        updateLoadMoreButton(allTrains);
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

    private VBox createEnhancedTrainCard(Train train) {
        System.out.println("SearchTrainController: Creating card for train " + train.getTrainNumber());

        VBox card = new VBox();
        card.getStyleClass().add("train-card");
        card.setSpacing(12);

        // Header with train info and tags
        HBox header = createTrainHeader(train);

        // Route information with timing
        HBox route = createRouteSection(train);

        // Book button
        HBox actions = createActionSection(train);

        card.getChildren().addAll(header, route, actions);
        return card;
    }

    private HBox createTrainHeader(Train train) {
        HBox header = new HBox(12);
        header.getStyleClass().add("train-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // Train info
        VBox info = new VBox(4);
        Label number = new Label(train.getTrainNumber() != null ? train.getTrainNumber() : "N/A");
        number.getStyleClass().add("train-number");

        Label name = new Label(train.getName() != null ? train.getName() : "Unknown Train");
        name.getStyleClass().add("train-name");

        // Tags
        HBox tags = new HBox(6);
        tags.setAlignment(Pos.CENTER_LEFT);

        List<String> amenities = trainService.getTrainAmenities(train);
        for (String amenity : amenities) {
            Label tag = createTag(amenity, "tag-chip");
            tags.getChildren().add(tag);
            if (tags.getChildren().size() >= 3) break; // Limit to 3 tags
        }

        info.getChildren().addAll(number, name, tags);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Seats section
        HBox seats = createSeatsSection(train);

        header.getChildren().addAll(info, spacer, seats);
        return header;
    }

    private HBox createRouteSection(Train train) {
        HBox route = new HBox(40);
        route.getStyleClass().add("route-box");
        route.setAlignment(Pos.CENTER_LEFT);

        // Departure
        String sourceStationName = getStationNameById(train.getSourceStationId());
        String depTime = trainService.getDepartureTime(train, sourceStationName);
        VBox depInfo = createStationInfo(depTime, sourceStationName);

        // Duration and distance
        String duration = trainService.calculateDuration(train, fromStation, toStation);
        int halts = trainService.getHaltsBetween(train, fromStation, toStation);
        int distance = trainService.getDistanceBetween(train, fromStation, toStation);
        VBox durationInfo = createDurationInfo(duration, halts + " halts • " + distance + " km");

        // Arrival
        String destStationName = getStationNameById(train.getDestinationStationId());
        String arrTime = trainService.getArrivalTime(train, destStationName);
        VBox arrInfo = createStationInfo(arrTime, destStationName);

        route.getChildren().addAll(depInfo, durationInfo, arrInfo);
        return route;
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

    /**
     * Handle View Details - Open popup with train details
     */
    /**
     * Handle View Details - Open your TrainDetails.fxml window
     */
    private void handleViewDetails(Train train) {
        try {
            System.out.println("SearchTrainController: Opening train details for " + train.getTrainNumber());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TrainDetails.fxml"));
            Parent root = loader.load();

            TrainDetailsController detailsController = loader.getController();
            detailsController.setTrainDetails(train, journeyDate, fromStation, toStation);

            // Create new stage for train details
            Stage detailsStage = new Stage();
            detailsStage.setTitle("Train Details - " + train.getTrainNumber() + " " + train.getName());
            detailsStage.setScene(new Scene(root));
            detailsStage.initModality(Modality.APPLICATION_MODAL);
            detailsStage.setResizable(true);
            detailsStage.centerOnScreen();

            // Set minimum size
            detailsStage.setMinWidth(1000);
            detailsStage.setMinHeight(700);

            detailsStage.show();

            System.out.println("SearchTrainController: Train details window opened successfully");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open train details: " + e.getMessage());
        }
    }

    /**
     * Handle Book Train - Check login and redirect appropriately
     */
    private void handleBookTrain(Train train) {
        System.out.println("SearchTrainController: Booking train " + train.getTrainNumber());

        if (!sessionManager.isLoggedIn()) {
            // User not logged in - redirect to login with message
            try {
                // Store booking data for after login
                sessionManager.setPendingBooking(train.getTrainId(), fromStation, toStation, journeyDate);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
                Parent root = loader.load();

                trainapp.controller.ui.LoginController loginController = loader.getController();
                loginController.setLoginMessage("You need to login to book");
                loginController.setRedirectAfterLogin("/fxml/TrainBooking.fxml");

                Stage stage = (Stage) trainsContainer.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.centerOnScreen();

                System.out.println("SearchTrainController: Redirected to login");

            } catch (IOException e) {
                e.printStackTrace();
                showError("Failed to load login page: " + e.getMessage());
            }
        } else {
            // User is logged in - go directly to booking page
            redirectToBookingPage(train);
        }
    }

    /**
     * Redirect to booking page with train data
     */
    private void redirectToBookingPage(Train train) {
        try {
            System.out.println("SearchTrainController: Redirecting to booking page");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TrainBooking.fxml"));
            Parent root = loader.load();

            trainapp.controller.ui.TrainBookingController bookingController = loader.getController();
            bookingController.setBookingData(
                    train.getTrainId(),
                    train.getTrainNumber(),
                    train.getName(),
                    fromStation,
                    toStation,
                    journeyDate
            );

            Stage stage = (Stage) trainsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();

            System.out.println("SearchTrainController: Successfully loaded booking page");

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load booking page: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("An error occurred while booking: " + e.getMessage());
        }
    }

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

    private void reloadWithSort(String sortOption) {
        System.out.println("SearchTrainController: Sorting by " + sortOption);

        if (allTrains.isEmpty()) return;

        // Sort the trains based on the selected option
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

        rebuildCards();
    }

    private String getStationNameById(int stationId) {
        try {
            Station station = stationDAO.getStationById(stationId);
            return station != null ? station.getName() : "Unknown Station";
        } catch (Exception e) {
            System.err.println("Error getting station name for ID " + stationId + ": " + e.getMessage());
            return "Unknown Station";
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Style the alert
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

    @FXML
    public void handleLoadMore(ActionEvent event) {
        System.out.println("SearchTrainController: Loading more trains");
        loadMoreTrains(allTrains);
    }

    @FXML
    public void handleBack(ActionEvent event) {
        try {
            System.out.println("SearchTrainController: Going back to main menu");
            SceneManager.switchScene("/fxml/MainMenu.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to go back to main menu: " + e.getMessage());
        }
    }

    @FXML
    public void handleModifySearch(ActionEvent event) {
        try {
            System.out.println("SearchTrainController: Modifying search");
            SceneManager.switchScene("/fxml/MainMenu.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to modify search: " + e.getMessage());
        }
    }
}