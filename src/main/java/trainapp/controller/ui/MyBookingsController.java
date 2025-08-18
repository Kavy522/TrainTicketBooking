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
import trainapp.dao.BookingDAO;
import trainapp.model.*;
import trainapp.service.MyBookingsService;
import trainapp.service.MyBookingsService.*;
import trainapp.service.SessionManager;
import trainapp.util.SceneManager;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * MyBookingsController manages the user's booking history and related operations.
 * Provides comprehensive booking management with filtering, detailed views, and actions.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Complete booking history with detailed information display</li>
 *   <li>Advanced filtering by status and search functionality</li>
 *   <li>Real-time booking statistics and analytics</li>
 *   <li>Contextual actions based on booking status and journey timing</li>
 *   <li>Responsive UI with loading states and empty state handling</li>
 *   <li>Integrated navigation to related services (PNR status, downloads)</li>
 * </ul>
 *
 * <p>Booking Management Features:
 * <ul>
 *   <li>Status-based filtering (All, Confirmed, Pending, Cancelled)</li>
 *   <li>Text search across PNR, train details, and routes</li>
 *   <li>Journey timeline categorization (Upcoming, Today, Completed)</li>
 *   <li>Passenger information display with smart truncation</li>
 *   <li>Contextual action buttons based on booking state</li>
 * </ul>
 *
 * <p>User Experience Features:
 * <ul>
 *   <li>Asynchronous data loading with progress indicators</li>
 *   <li>Dynamic UI cards with comprehensive booking information</li>
 *   <li>Auto-refreshing statistics and real-time status updates</li>
 *   <li>Intuitive navigation and action flow</li>
 *   <li>Responsive design with proper state management</li>
 * </ul>
 */
public class MyBookingsController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // Header Section
    @FXML private Button backButton;
    @FXML private Label userWelcomeLabel;
    @FXML private Button logoutButton;

    // Statistics Section
    @FXML private Label totalBookingsLabel;
    @FXML private Label confirmedBookingsLabel;
    @FXML private Label pendingBookingsLabel;
    @FXML private Label cancelledBookingsLabel;
    @FXML private Label totalSpentLabel;

    // Filter and Search Section
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TextField searchField;
    @FXML private Button refreshButton;

    // Content Display Sections
    @FXML private VBox loadingSection;
    @FXML private VBox bookingsSection;
    @FXML private VBox emptyStateSection;
    @FXML private ScrollPane bookingsScrollPane;
    @FXML private VBox bookingsContainer;

    // Status and Messaging
    @FXML private Label statusMessage;

    // -------------------------------------------------------------------------
    // Services and Data Management
    // -------------------------------------------------------------------------

    private final MyBookingsService bookingsService = new MyBookingsService();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final BookingDAO bookingDAO = new BookingDAO();

    private List<DetailedBookingInfo> allBookings;
    private List<DetailedBookingInfo> filteredBookings;

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the booking management interface with user data and UI setup.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        setupUI();
        loadUserBookings();
    }

    /**
     * Sets up the user interface components, filters, and event handlers.
     * Configures initial state and user-specific welcome message.
     */
    private void setupUI() {
        setupUserWelcomeMessage();
        setupFilterControls();
        setupSearchFunctionality();
        initializeUIState();
    }

    /**
     * Sets up the user welcome message based on current session.
     */
    private void setupUserWelcomeMessage() {
        if (sessionManager.isLoggedIn()) {
            User currentUser = sessionManager.getCurrentUser();
            userWelcomeLabel.setText("Welcome back, " + currentUser.getName() + "!");
        }
    }

    /**
     * Configures the status filter combo box with available options.
     */
    private void setupFilterControls() {
        statusFilterCombo.getItems().addAll("All Bookings", "Confirmed", "Pending", "Cancelled");
        statusFilterCombo.setValue("All Bookings");
        statusFilterCombo.setOnAction(e -> filterBookings());
    }

    /**
     * Sets up real-time search functionality for booking filtering.
     */
    private void setupSearchFunctionality() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> filterBookings());
    }

    /**
     * Initializes the UI state by hiding all content sections.
     */
    private void initializeUIState() {
        showLoadingSection(false);
        showBookingsSection(false);
        showEmptyStateSection(false);
    }

    // -------------------------------------------------------------------------
    // Data Loading and Management
    // -------------------------------------------------------------------------

    /**
     * Loads user bookings asynchronously with proper error handling and state management.
     * Validates user authentication and manages UI state transitions.
     */
    private void loadUserBookings() {
        if (!validateUserAuthentication()) {
            return;
        }

        initiateAsyncBookingLoad();
    }

    /**
     * Validates user authentication for booking access.
     *
     * @return true if user is authenticated
     */
    private boolean validateUserAuthentication() {
        if (!sessionManager.isLoggedIn()) {
            showEmptyStateSection(true);
            showStatusMessage("Please login to view your bookings", "error");
            return false;
        }
        return true;
    }

    /**
     * Initiates asynchronous booking data loading with proper UI state management.
     */
    private void initiateAsyncBookingLoad() {
        showLoadingSection(true);
        showBookingsSection(false);
        showEmptyStateSection(false);
        refreshButton.setDisable(true);

        Task<MyBookingsResult> loadTask = createBookingLoadTask();
        configureTaskHandlers(loadTask);
        new Thread(loadTask).start();
    }

    /**
     * Creates the async task for loading booking data.
     *
     * @return configured Task for booking data loading
     */
    private Task<MyBookingsResult> createBookingLoadTask() {
        return new Task<MyBookingsResult>() {
            @Override
            protected MyBookingsResult call() throws Exception {
                return bookingsService.getAllBookings(sessionManager.getCurrentUser().getUserId());
            }
        };
    }

    /**
     * Configures success and failure handlers for the booking load task.
     *
     * @param loadTask the task to configure handlers for
     */
    private void configureTaskHandlers(Task<MyBookingsResult> loadTask) {
        loadTask.setOnSucceeded(e -> Platform.runLater(() -> handleLoadSuccess(loadTask.getValue())));
        loadTask.setOnFailed(e -> Platform.runLater(() -> handleLoadFailure()));
    }

    /**
     * Handles successful booking data loading and UI updates.
     *
     * @param result the booking load result
     */
    private void handleLoadSuccess(MyBookingsResult result) {
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
    }

    /**
     * Handles booking data loading failures with appropriate user feedback.
     */
    private void handleLoadFailure() {
        showLoadingSection(false);
        refreshButton.setDisable(false);
        showEmptyStateSection(true);
        showStatusMessage("Failed to load bookings. Please try again.", "error");
    }

    /**
     * Loads and displays booking statistics in the UI.
     * Provides overview of booking counts and total spending.
     */
    private void loadBookingStatistics() {
        if (!sessionManager.isLoggedIn()) return;

        BookingStatistics stats = bookingsService.getBookingStatistics(sessionManager.getCurrentUser().getUserId());
        updateStatisticsDisplay(stats);
    }

    /**
     * Updates the statistics display labels with current data.
     *
     * @param stats the booking statistics to display
     */
    private void updateStatisticsDisplay(BookingStatistics stats) {
        totalBookingsLabel.setText(String.valueOf(stats.getTotalBookings()));
        confirmedBookingsLabel.setText(String.valueOf(stats.getConfirmedBookings()));
        pendingBookingsLabel.setText(String.valueOf(stats.getPendingBookings()));
        cancelledBookingsLabel.setText(String.valueOf(stats.getCancelledBookings()));
        totalSpentLabel.setText("₹" + String.format("%.0f", stats.getTotalSpent()));
    }

    // -------------------------------------------------------------------------
    // Booking Display and UI Generation
    // -------------------------------------------------------------------------

    /**
     * Displays the filtered bookings in the UI container.
     * Generates dynamic booking cards or shows appropriate empty state.
     */
    private void displayBookings() {
        bookingsContainer.getChildren().clear();

        if (filteredBookings == null || filteredBookings.isEmpty()) {
            displayEmptyFilterState();
            return;
        }

        generateBookingCards();
        showStatusMessage("Showing " + filteredBookings.size() + " booking(s)", "success");
    }

    /**
     * Displays empty state message when no bookings match the current filter.
     */
    private void displayEmptyFilterState() {
        Label noBookings = new Label("No bookings match your current filter");
        noBookings.getStyleClass().add("no-bookings-label");
        bookingsContainer.getChildren().add(noBookings);
    }

    /**
     * Generates booking cards for all filtered bookings.
     */
    private void generateBookingCards() {
        for (DetailedBookingInfo bookingInfo : filteredBookings) {
            VBox bookingCard = createBookingCard(bookingInfo);
            bookingsContainer.getChildren().add(bookingCard);
        }
    }

    /**
     * Creates a comprehensive booking card UI component.
     * Includes header, journey details, passenger info, and action buttons.
     *
     * @param bookingInfo the booking information to display
     * @return configured VBox containing the booking card
     */
    private VBox createBookingCard(DetailedBookingInfo bookingInfo) {
        VBox card = new VBox(15);
        card.getStyleClass().add("booking-card");

        card.getChildren().addAll(
                createBookingHeader(bookingInfo),
                createJourneySection(bookingInfo),
                createPassengerSection(bookingInfo),
                createActionSection(bookingInfo)
        );

        return card;
    }

    /**
     * Creates the booking header with PNR, booking date, and status.
     *
     * @param bookingInfo the booking information
     * @return configured HBox containing the header
     */
    private HBox createBookingHeader(DetailedBookingInfo bookingInfo) {
        HBox header = new HBox(20);
        header.getStyleClass().add("booking-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox pnrSection = createPNRSection(bookingInfo);
        Region spacer = createHeaderSpacer();
        Label statusLabel = createStatusLabel(bookingInfo);

        header.getChildren().addAll(pnrSection, spacer, statusLabel);
        return header;
    }

    /**
     * Creates the PNR and booking date section.
     *
     * @param bookingInfo the booking information
     * @return configured VBox with PNR and date
     */
    private VBox createPNRSection(DetailedBookingInfo bookingInfo) {
        VBox pnrSection = new VBox(5);

        Label pnrLabel = new Label("PNR: " + bookingInfo.getBooking().getPnr());
        pnrLabel.getStyleClass().add("pnr-label");

        Label bookingDate = new Label("Booked: " + bookingInfo.getBooking().getBookingTime()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
        bookingDate.getStyleClass().add("booking-date");

        pnrSection.getChildren().addAll(pnrLabel, bookingDate);
        return pnrSection;
    }

    /**
     * Creates a spacer region for header layout.
     *
     * @return configured Region as spacer
     */
    private Region createHeaderSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        return spacer;
    }

    /**
     * Creates the status label with appropriate styling.
     *
     * @param bookingInfo the booking information
     * @return configured Label with status
     */
    private Label createStatusLabel(DetailedBookingInfo bookingInfo) {
        Label statusLabel = new Label(bookingsService.getFormattedStatus(bookingInfo.getBooking().getStatus()));
        statusLabel.getStyleClass().addAll("booking-status",
                bookingsService.getStatusColorClass(bookingInfo.getBooking().getStatus()));
        return statusLabel;
    }

    /**
     * Creates the journey details section with train and route information.
     *
     * @param bookingInfo the booking information
     * @return configured VBox containing journey details
     */
    private VBox createJourneySection(DetailedBookingInfo bookingInfo) {
        VBox section = new VBox(10);
        section.getStyleClass().add("journey-section");

        Label trainLabel = new Label(bookingInfo.getTrainDetails());
        trainLabel.getStyleClass().add("train-details");

        HBox routeInfo = createRouteInfoSection(bookingInfo);

        section.getChildren().addAll(trainLabel, routeInfo);
        return section;
    }

    /**
     * Creates the route information section with timeline indicators.
     *
     * @param bookingInfo the booking information
     * @return configured HBox with route details
     */
    private HBox createRouteInfoSection(DetailedBookingInfo bookingInfo) {
        HBox routeInfo = new HBox(30);
        routeInfo.setAlignment(Pos.CENTER_LEFT);

        Label routeLabel = new Label(bookingInfo.getRouteDetails());
        routeLabel.getStyleClass().add("route-details");

        Label dateLabel = new Label(bookingInfo.getFormattedJourneyDate());
        dateLabel.getStyleClass().add("journey-date");

        Label journeyTypeLabel = createJourneyTypeLabel(bookingInfo);

        routeInfo.getChildren().addAll(routeLabel, dateLabel, journeyTypeLabel);
        return routeInfo;
    }

    /**
     * Creates journey type indicator label based on timing.
     *
     * @param bookingInfo the booking information
     * @return configured Label with journey timing indicator
     */
    private Label createJourneyTypeLabel(DetailedBookingInfo bookingInfo) {
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
        return journeyTypeLabel;
    }

    /**
     * Creates the passenger details section with smart display optimization.
     *
     * @param bookingInfo the booking information
     * @return configured VBox containing passenger information
     */
    private VBox createPassengerSection(DetailedBookingInfo bookingInfo) {
        VBox section = new VBox(8);
        section.getStyleClass().add("passenger-section");

        HBox passengerHeader = createPassengerHeader(bookingInfo);
        section.getChildren().add(passengerHeader);

        if (bookingInfo.getPassengers() != null && !bookingInfo.getPassengers().isEmpty()) {
            Label passengerNamesLabel = createPassengerNamesList(bookingInfo);
            section.getChildren().add(passengerNamesLabel);
        }

        return section;
    }

    /**
     * Creates the passenger summary header with count and fare information.
     *
     * @param bookingInfo the booking information
     * @return configured HBox with passenger summary
     */
    private HBox createPassengerHeader(DetailedBookingInfo bookingInfo) {
        HBox passengerHeader = new HBox(15);
        passengerHeader.setAlignment(Pos.CENTER_LEFT);

        Label passengerCountLabel = new Label(bookingInfo.getPassengerSummary());
        passengerCountLabel.getStyleClass().add("passenger-count");

        Label fareLabel = new Label("Total Fare: ₹" + String.format("%.0f", bookingInfo.getBooking().getTotalFare()));
        fareLabel.getStyleClass().add("total-fare");

        passengerHeader.getChildren().addAll(passengerCountLabel, fareLabel);
        return passengerHeader;
    }

    /**
     * Creates an optimized passenger names list with smart truncation.
     *
     * @param bookingInfo the booking information
     * @return configured Label with passenger names
     */
    private Label createPassengerNamesList(DetailedBookingInfo bookingInfo) {
        StringBuilder passengerNames = new StringBuilder();
        List<Passenger> passengers = bookingInfo.getPassengers();

        for (int i = 0; i < passengers.size(); i++) {
            if (i > 0) passengerNames.append(", ");
            passengerNames.append(passengers.get(i).getName());

            if (i >= 2) { // Show max 3 names
                int remaining = passengers.size() - 3;
                if (remaining > 0) {
                    passengerNames.append(" +").append(remaining).append(" more");
                }
                break;
            }
        }

        Label passengerNamesLabel = new Label(passengerNames.toString());
        passengerNamesLabel.getStyleClass().add("passenger-names");
        return passengerNamesLabel;
    }

    /**
     * Creates contextual action buttons based on booking status and journey timing.
     *
     * @param bookingInfo the booking information
     * @return configured HBox containing action buttons
     */
    private HBox createActionSection(DetailedBookingInfo bookingInfo) {
        HBox actions = new HBox(15);
        actions.getStyleClass().add("action-section");
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button viewDetailsBtn = createViewDetailsButton(bookingInfo);
        actions.getChildren().add(viewDetailsBtn);

        addContextualActionButtons(actions, bookingInfo);

        return actions;
    }

    /**
     * Creates the view details button.
     *
     * @param bookingInfo the booking information
     * @return configured Button for viewing details
     */
    private Button createViewDetailsButton(DetailedBookingInfo bookingInfo) {
        Button viewDetailsBtn = new Button("View Details");
        viewDetailsBtn.getStyleClass().add("action-btn-secondary");
        viewDetailsBtn.setOnAction(e -> handleViewDetails(bookingInfo));
        return viewDetailsBtn;
    }

    /**
     * Adds contextual action buttons based on booking status.
     *
     * @param actions the HBox to add buttons to
     * @param bookingInfo the booking information
     */
    private void addContextualActionButtons(HBox actions, DetailedBookingInfo bookingInfo) {
        String status = bookingInfo.getBooking().getStatus().toLowerCase();

        if (status.equals("confirmed") || status.equals("conformed")) {
            Button downloadBtn = createDownloadButton(bookingInfo);
            actions.getChildren().add(downloadBtn);

            if (bookingInfo.isUpcomingJourney()) {
                Button cancelBtn = createCancelButton(bookingInfo);
                actions.getChildren().add(cancelBtn);
            }
        }
    }

    /**
     * Creates the download ticket button.
     *
     * @param bookingInfo the booking information
     * @return configured Button for downloading tickets
     */
    private Button createDownloadButton(DetailedBookingInfo bookingInfo) {
        Button downloadBtn = new Button("📄 Download");
        downloadBtn.getStyleClass().add("action-btn-primary");
        downloadBtn.setOnAction(e -> handleDownloadTicket(bookingInfo));
        return downloadBtn;
    }

    /**
     * Creates the cancel booking button.
     *
     * @param bookingInfo the booking information
     * @return configured Button for canceling bookings
     */
    private Button createCancelButton(DetailedBookingInfo bookingInfo) {
        Button cancelBtn = new Button("❌ Cancel");
        cancelBtn.getStyleClass().add("action-btn-danger");
        cancelBtn.setOnAction(e -> handleCancelBooking(bookingInfo));
        return cancelBtn;
    }

    // -------------------------------------------------------------------------
    // Filtering and Search Functionality
    // -------------------------------------------------------------------------

    /**
     * Filters bookings based on status selection and search text.
     * Applies real-time filtering with multiple criteria support.
     */
    @FXML
    public void filterBookings() {
        if (allBookings == null) return;

        String statusFilter = statusFilterCombo.getValue();
        String searchText = searchField.getText().toLowerCase().trim();

        filteredBookings = allBookings.stream()
                .filter(booking -> matchesStatusFilter(booking, statusFilter))
                .filter(booking -> matchesSearchFilter(booking, searchText))
                .collect(java.util.stream.Collectors.toList());

        displayBookings();
    }

    /**
     * Checks if booking matches the selected status filter.
     *
     * @param booking the booking to check
     * @param statusFilter the selected status filter
     * @return true if booking matches status filter
     */
    private boolean matchesStatusFilter(DetailedBookingInfo booking, String statusFilter) {
        if ("All Bookings".equals(statusFilter)) {
            return true;
        }
        String bookingStatus = bookingsService.getFormattedStatus(booking.getBooking().getStatus());
        return bookingStatus.equalsIgnoreCase(statusFilter);
    }

    /**
     * Checks if booking matches the search text criteria.
     *
     * @param booking the booking to check
     * @param searchText the search text
     * @return true if booking matches search criteria
     */
    private boolean matchesSearchFilter(DetailedBookingInfo booking, String searchText) {
        if (searchText.isEmpty()) {
            return true;
        }
        return booking.getBooking().getPnr().toLowerCase().contains(searchText) ||
                booking.getTrainDetails().toLowerCase().contains(searchText) ||
                booking.getRouteDetails().toLowerCase().contains(searchText);
    }

    // -------------------------------------------------------------------------
    // Action Handlers
    // -------------------------------------------------------------------------

    /**
     * Handles viewing detailed booking information.
     * Navigates to PNR status page with booking details.
     *
     * @param bookingInfo the booking to view details for
     */
    private void handleViewDetails(DetailedBookingInfo bookingInfo) {
        PNRController pnrController = SceneManager.switchScene("/fxml/PNRStatus.fxml");
        pnrController.setPNRNumber(bookingInfo.getBooking().getPnr());
    }

    /**
     * Handles ticket download functionality.
     * Currently shows placeholder message for future implementation.
     *
     * @param bookingInfo the booking to download ticket for
     */
    private void handleDownloadTicket(DetailedBookingInfo bookingInfo) {
        // TODO: Implement ticket download functionality
        showStatusMessage("Download feature will be available soon!", "info");
    }

    /**
     * Handles booking cancellation with confirmation dialog.
     * Updates booking status and refreshes display.
     *
     * @param bookingInfo the booking to cancel
     */
    private void handleCancelBooking(DetailedBookingInfo bookingInfo) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Booking");
        confirm.setHeaderText("Cancel booking for PNR: " + bookingInfo.getBooking().getPnr());
        confirm.setContentText("Are you sure you want to cancel this booking? This action cannot be undone.");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            boolean success = bookingDAO.cancelBooking(bookingInfo.getBooking().getBookingId());
            if (success) {
                showStatusMessage("Booking cancelled successfully", "success");
                loadUserBookings(); // Refresh the data
            } else {
                showStatusMessage("Failed to cancel booking. Please try again.", "error");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Navigation Handlers
    // -------------------------------------------------------------------------

    /**
     * Handles refresh action to reload booking data.
     *
     * @param event ActionEvent from refresh button
     */
    @FXML
    public void handleRefresh(ActionEvent event) {
        loadUserBookings();
    }

    /**
     * Handles navigation back to main menu.
     *
     * @param event ActionEvent from back button
     */
    @FXML
    public void handleBack(ActionEvent event) {
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    /**
     * Handles user logout with session cleanup.
     *
     * @param event ActionEvent from logout button
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        sessionManager.logout();
        SceneManager.switchScene("/fxml/Login.fxml");
    }

    /**
     * Handles navigation to book new ticket.
     * Returns to main menu for new booking initiation.
     *
     * @param event ActionEvent from book new ticket button
     */
    @FXML
    public void handleBookNewTicket(ActionEvent event) {
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    // -------------------------------------------------------------------------
    // UI State Management
    // -------------------------------------------------------------------------

    /**
     * Shows or hides the loading section with proper state management.
     *
     * @param show true to show loading section
     */
    private void showLoadingSection(boolean show) {
        if (loadingSection != null) {
            loadingSection.setVisible(show);
            loadingSection.setManaged(show);
        }
    }

    /**
     * Shows or hides the bookings section with proper state management.
     *
     * @param show true to show bookings section
     */
    private void showBookingsSection(boolean show) {
        if (bookingsSection != null) {
            bookingsSection.setVisible(show);
            bookingsSection.setManaged(show);
        }
    }

    /**
     * Shows or hides the empty state section with proper state management.
     *
     * @param show true to show empty state section
     */
    private void showEmptyStateSection(boolean show) {
        if (emptyStateSection != null) {
            emptyStateSection.setVisible(show);
            emptyStateSection.setManaged(show);
        }
    }

    /**
     * Displays a status message with appropriate styling and visibility.
     *
     * @param message the message text to display
     * @param type the message type for styling ("success", "error", "info", "warning")
     */
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