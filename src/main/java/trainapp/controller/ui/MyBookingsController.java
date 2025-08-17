package trainapp.controller.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import trainapp.model.*;
import trainapp.service.MyBookingsService;
import trainapp.service.MyBookingsService.*;
import trainapp.service.SessionManager;
import trainapp.util.SceneManager;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MyBookingsController {

    // Header
    @FXML private Button backButton;
    @FXML private Label userWelcomeLabel;
    @FXML private Button logoutButton;

    // Statistics Section
    @FXML private Label totalBookingsLabel;
    @FXML private Label confirmedBookingsLabel;
    @FXML private Label pendingBookingsLabel;
    @FXML private Label cancelledBookingsLabel;
    @FXML private Label totalSpentLabel;

    // Filter Section
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TextField searchField;
    @FXML private Button refreshButton;

    // Content Sections
    @FXML private VBox loadingSection;
    @FXML private VBox bookingsSection;
    @FXML private VBox emptyStateSection;
    @FXML private ScrollPane bookingsScrollPane;
    @FXML private VBox bookingsContainer;

    // Status and Messages
    @FXML private Label statusMessage;

    // Services
    private final MyBookingsService bookingsService = new MyBookingsService();
    private final SessionManager sessionManager = SessionManager.getInstance();

    // Data
    private List<DetailedBookingInfo> allBookings;
    private List<DetailedBookingInfo> filteredBookings;

    @FXML
    public void initialize() {
        setupUI();
        loadUserBookings();
    }

    private void setupUI() {
        // Set user welcome message
        if (sessionManager.isLoggedIn()) {
            User currentUser = sessionManager.getCurrentUser();
            userWelcomeLabel.setText("Welcome back, " + currentUser.getName() + "!");
        }

        // Setup filter combo
        statusFilterCombo.getItems().addAll("All Bookings", "Confirmed", "Pending", "Cancelled");
        statusFilterCombo.setValue("All Bookings");
        statusFilterCombo.setOnAction(e -> filterBookings());

        // Setup search field
        searchField.textProperty().addListener((obs, oldValue, newValue) -> filterBookings());

        // Hide all sections initially
        showLoadingSection(false);
        showBookingsSection(false);
        showEmptyStateSection(false);
    }

    /**
     * Load user bookings
     */
    private void loadUserBookings() {
        if (!sessionManager.isLoggedIn()) {
            showEmptyStateSection(true);
            showStatusMessage("Please login to view your bookings", "error");
            return;
        }

        showLoadingSection(true);
        showBookingsSection(false);
        showEmptyStateSection(false);
        refreshButton.setDisable(true);

        Task<MyBookingsResult> loadTask = new Task<MyBookingsResult>() {
            @Override
            protected MyBookingsResult call() throws Exception {
                return bookingsService.getAllBookings(sessionManager.getCurrentUser().getUserId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                MyBookingsResult result = loadTask.getValue();
                showLoadingSection(false);
                refreshButton.setDisable(false);

                if (result.isSuccess()) {
                    allBookings = result.getBookings();
                    filteredBookings = allBookings;
                    displayBookings();
                    loadBookingStatistics();
                    showBookingsSection(true);
                } else if (result.getType() == MyBookingsResultType.NO_BOOKINGS) {
                    showEmptyStateSection(true);
                    showStatusMessage(result.getMessage(), "info");
                } else {
                    showEmptyStateSection(true);
                    showStatusMessage(result.getMessage(), "error");
                }
            });
        });

        loadTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                showLoadingSection(false);
                refreshButton.setDisable(false);
                showEmptyStateSection(true);
                showStatusMessage("Failed to load bookings. Please try again.", "error");
            });
        });

        new Thread(loadTask).start();
    }

    /**
     * Load and display booking statistics
     */
    private void loadBookingStatistics() {
        if (!sessionManager.isLoggedIn()) return;

        BookingStatistics stats = bookingsService.getBookingStatistics(sessionManager.getCurrentUser().getUserId());

        totalBookingsLabel.setText(String.valueOf(stats.getTotalBookings()));
        confirmedBookingsLabel.setText(String.valueOf(stats.getConfirmedBookings()));
        pendingBookingsLabel.setText(String.valueOf(stats.getPendingBookings()));
        cancelledBookingsLabel.setText(String.valueOf(stats.getCancelledBookings()));
        totalSpentLabel.setText("₹" + String.format("%.0f", stats.getTotalSpent()));
    }

    /**
     * Display bookings in the UI
     */
    private void displayBookings() {
        bookingsContainer.getChildren().clear();

        if (filteredBookings == null || filteredBookings.isEmpty()) {
            Label noBookings = new Label("No bookings match your current filter");
            noBookings.getStyleClass().add("no-bookings-label");
            bookingsContainer.getChildren().add(noBookings);
            return;
        }

        for (DetailedBookingInfo bookingInfo : filteredBookings) {
            VBox bookingCard = createBookingCard(bookingInfo);
            bookingsContainer.getChildren().add(bookingCard);
        }

        showStatusMessage("Showing " + filteredBookings.size() + " booking(s)", "success");
    }

    /**
     * Create booking card UI
     */
    private VBox createBookingCard(DetailedBookingInfo bookingInfo) {
        VBox card = new VBox(15);
        card.getStyleClass().add("booking-card");

        // Header
        HBox header = createBookingHeader(bookingInfo);

        // Journey details
        VBox journeySection = createJourneySection(bookingInfo);

        // Passenger details
        VBox passengerSection = createPassengerSection(bookingInfo);

        // Action buttons
        HBox actionSection = createActionSection(bookingInfo);

        card.getChildren().addAll(header, journeySection, passengerSection, actionSection);
        return card;
    }

    /**
     * Create booking header with PNR and status
     */
    private HBox createBookingHeader(DetailedBookingInfo bookingInfo) {
        HBox header = new HBox(20);
        header.getStyleClass().add("booking-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // PNR and booking info
        VBox pnrSection = new VBox(5);
        Label pnrLabel = new Label("PNR: " + bookingInfo.getBooking().getPnr());
        pnrLabel.getStyleClass().add("pnr-label");

        Label bookingDate = new Label("Booked: " + bookingInfo.getBooking().getBookingTime()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
        bookingDate.getStyleClass().add("booking-date");

        pnrSection.getChildren().addAll(pnrLabel, bookingDate);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Status
        Label statusLabel = new Label(bookingsService.getFormattedStatus(bookingInfo.getBooking().getStatus()));
        statusLabel.getStyleClass().addAll("booking-status",
                bookingsService.getStatusColorClass(bookingInfo.getBooking().getStatus()));

        header.getChildren().addAll(pnrSection, spacer, statusLabel);
        return header;
    }

    /**
     * Create journey details section
     */
    private VBox createJourneySection(DetailedBookingInfo bookingInfo) {
        VBox section = new VBox(10);
        section.getStyleClass().add("journey-section");

        // Train details
        Label trainLabel = new Label(bookingInfo.getTrainDetails());
        trainLabel.getStyleClass().add("train-details");

        // Route and date
        HBox routeInfo = new HBox(30);
        routeInfo.setAlignment(Pos.CENTER_LEFT);

        Label routeLabel = new Label(bookingInfo.getRouteDetails());
        routeLabel.getStyleClass().add("route-details");

        Label dateLabel = new Label(bookingInfo.getFormattedJourneyDate());
        dateLabel.getStyleClass().add("journey-date");

        // Journey type indicator
        Label journeyTypeLabel;
        if (bookingInfo.isUpcomingJourney()) {
            journeyTypeLabel = new Label("🟢 Upcoming");
            journeyTypeLabel.getStyleClass().add("journey-upcoming");
        } else if (bookingInfo.isPastJourney()) {
            journeyTypeLabel = new Label("🔵 Completed");
            journeyTypeLabel.getStyleClass().add("journey-past");
        } else {
            journeyTypeLabel = new Label("🟡 Today");
            journeyTypeLabel.getStyleClass().add("journey-today");
        }

        routeInfo.getChildren().addAll(routeLabel, dateLabel, journeyTypeLabel);
        section.getChildren().addAll(trainLabel, routeInfo);

        return section;
    }

    /**
     * Create passenger details section
     */
    private VBox createPassengerSection(DetailedBookingInfo bookingInfo) {
        VBox section = new VBox(8);
        section.getStyleClass().add("passenger-section");

        // Passenger summary
        HBox passengerHeader = new HBox(15);
        passengerHeader.setAlignment(Pos.CENTER_LEFT);

        Label passengerCountLabel = new Label(bookingInfo.getPassengerSummary());
        passengerCountLabel.getStyleClass().add("passenger-count");

        Label fareLabel = new Label("Total Fare: ₹" + String.format("%.0f", bookingInfo.getBooking().getTotalFare()));
        fareLabel.getStyleClass().add("total-fare");

        passengerHeader.getChildren().addAll(passengerCountLabel, fareLabel);

        // Passenger list (simplified)
        if (bookingInfo.getPassengers() != null && !bookingInfo.getPassengers().isEmpty()) {
            StringBuilder passengerNames = new StringBuilder();
            for (int i = 0; i < bookingInfo.getPassengers().size(); i++) {
                if (i > 0) passengerNames.append(", ");
                passengerNames.append(bookingInfo.getPassengers().get(i).getName());
                if (i >= 2) { // Show max 3 names
                    int remaining = bookingInfo.getPassengers().size() - 3;
                    if (remaining > 0) {
                        passengerNames.append(" +").append(remaining).append(" more");
                    }
                    break;
                }
            }

            Label passengerNamesLabel = new Label(passengerNames.toString());
            passengerNamesLabel.getStyleClass().add("passenger-names");
            section.getChildren().addAll(passengerHeader, passengerNamesLabel);
        } else {
            section.getChildren().add(passengerHeader);
        }

        return section;
    }

    /**
     * Create action buttons section
     */
    private HBox createActionSection(DetailedBookingInfo bookingInfo) {
        HBox actions = new HBox(15);
        actions.getStyleClass().add("action-section");
        actions.setAlignment(Pos.CENTER_RIGHT);

        // View Details button
        Button viewDetailsBtn = new Button("View Details");
        viewDetailsBtn.getStyleClass().add("action-btn-secondary");
        viewDetailsBtn.setOnAction(e -> handleViewDetails(bookingInfo));

        // Download Ticket button (for confirmed bookings)
        Button downloadBtn = new Button("📄 Download");
        downloadBtn.getStyleClass().add("action-btn-primary");
        downloadBtn.setOnAction(e -> handleDownloadTicket(bookingInfo));

        // Cancel button (for upcoming bookings)
        Button cancelBtn = new Button("❌ Cancel");
        cancelBtn.getStyleClass().add("action-btn-danger");
        cancelBtn.setOnAction(e -> handleCancelBooking(bookingInfo));

        actions.getChildren().add(viewDetailsBtn);

        String status = bookingInfo.getBooking().getStatus().toLowerCase();
        if (status.equals("confirmed") || status.equals("conformed")) {
            actions.getChildren().add(downloadBtn);

            if (bookingInfo.isUpcomingJourney()) {
                actions.getChildren().add(cancelBtn);
            }
        }

        return actions;
    }

    /**
     * Filter bookings based on status and search text
     */
    @FXML
    public void filterBookings() {
        if (allBookings == null) return;

        String statusFilter = statusFilterCombo.getValue();
        String searchText = searchField.getText().toLowerCase().trim();

        filteredBookings = allBookings.stream()
                .filter(booking -> {
                    // Status filter
                    boolean statusMatch = true;
                    if (!"All Bookings".equals(statusFilter)) {
                        String bookingStatus = bookingsService.getFormattedStatus(booking.getBooking().getStatus());
                        statusMatch = bookingStatus.equalsIgnoreCase(statusFilter);
                    }

                    // Search filter
                    boolean searchMatch = true;
                    if (!searchText.isEmpty()) {
                        searchMatch = booking.getBooking().getPnr().toLowerCase().contains(searchText) ||
                                booking.getTrainDetails().toLowerCase().contains(searchText) ||
                                booking.getRouteDetails().toLowerCase().contains(searchText);
                    }

                    return statusMatch && searchMatch;
                })
                .collect(java.util.stream.Collectors.toList());

        displayBookings();
    }

    /**
     * Handle view booking details
     */
    private void handleViewDetails(DetailedBookingInfo bookingInfo) {
        // Navigate to PNR Status page with this booking's PNR
        PNRController pnrController = SceneManager.switchScene("/fxml/PNRStatus.fxml");
        pnrController.setPNRNumber(bookingInfo.getBooking().getPnr());
    }

    /**
     * Handle download ticket
     */
    private void handleDownloadTicket(DetailedBookingInfo bookingInfo) {
        // TODO: Implement ticket download
        showStatusMessage("Download feature will be available soon!", "info");
    }

    /**
     * Handle cancel booking
     */
    private void handleCancelBooking(DetailedBookingInfo bookingInfo) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Booking");
        confirm.setHeaderText("Cancel booking for PNR: " + bookingInfo.getBooking().getPnr());
        confirm.setContentText("Are you sure you want to cancel this booking? This action cannot be undone.");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            // TODO: Implement booking cancellation
            showStatusMessage("Cancellation feature will be available soon!", "info");
        }
    }

    /**
     * Handle refresh bookings
     */
    @FXML
    public void handleRefresh(ActionEvent event) {
        loadUserBookings();
    }

    /**
     * Handle back to main menu
     */
    @FXML
    public void handleBack(ActionEvent event) {
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    /**
     * Handle logout
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        sessionManager.logout();
        SceneManager.switchScene("/fxml/Login.fxml");
    }

    /**
     * Handle book new ticket
     */
    @FXML
    public void handleBookNewTicket(ActionEvent event) {
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    // UI Helper methods
    private void showLoadingSection(boolean show) {
        if (loadingSection != null) {
            loadingSection.setVisible(show);
            loadingSection.setManaged(show);
        }
    }

    private void showBookingsSection(boolean show) {
        if (bookingsSection != null) {
            bookingsSection.setVisible(show);
            bookingsSection.setManaged(show);
        }
    }

    private void showEmptyStateSection(boolean show) {
        if (emptyStateSection != null) {
            emptyStateSection.setVisible(show);
            emptyStateSection.setManaged(show);
        }
    }

    private void showStatusMessage(String message, String type) {
        if (statusMessage != null) {
            statusMessage.setText(message);
            statusMessage.getStyleClass().removeAll("success", "error", "info", "warning");
            statusMessage.getStyleClass().add(type);
            statusMessage.setVisible(true);
            statusMessage.setManaged(true);
        }
    }
}
