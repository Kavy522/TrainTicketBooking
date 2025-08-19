package trainapp.controller.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import trainapp.dao.BookingDAO;
import trainapp.dao.StationDAO;
import trainapp.dao.TrainDAO;
import trainapp.dao.UserDAO;
import trainapp.model.Booking;
import trainapp.model.Train;
import trainapp.model.User;
import trainapp.model.Station;
import trainapp.util.SceneManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ReservationsController manages comprehensive booking administration and oversight.
 * Provides powerful filtering, statistics, and administrative operations for all system bookings.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Comprehensive booking overview with detailed table display</li>
 *   <li>Advanced filtering by status, date, and search criteria</li>
 *   <li>Real-time booking statistics and revenue tracking</li>
 *   <li>Administrative actions (view, update, delete) for each booking</li>
 *   <li>Bulk operations and report generation capabilities</li>
 *   <li>Dynamic data loading with station and user information enrichment</li>
 * </ul>
 *
 * <p>Administrative Operations:
 * <ul>
 *   <li>Status management with real-time updates (waiting, confirmed, cancelled)</li>
 *   <li>Detailed booking inspection with complete journey information</li>
 *   <li>Revenue analytics with confirmed booking calculations</li>
 *   <li>Search and filter operations across multiple criteria</li>
 *   <li>Data export and reporting functionalities</li>
 * </ul>
 *
 * <p>Data Integration Features:
 * <ul>
 *   <li>Cross-referential data loading (users, trains, stations)</li>
 *   <li>Dynamic route calculation from station information</li>
 *   <li>Real-time statistics computation and display</li>
 *   <li>Styled status indicators with contextual color coding</li>
 *   <li>Responsive table layout with action buttons</li>
 * </ul>
 */
public class ReservationsController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // Search and Filter Controls
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> dateFilterCombo;

    // Statistics Display
    @FXML private Label totalBookingsLabel;
    @FXML private Label confirmedBookingsLabel;
    @FXML private Label pendingBookingsLabel;
    @FXML private Label cancelledBookingsLabel;
    @FXML private Label totalRevenueLabel;

    // Data Table and Columns
    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, Number> colId;
    @FXML private TableColumn<Booking, String> colPNR;
    @FXML private TableColumn<Booking, String> colUserName;
    @FXML private TableColumn<Booking, String> colTrainNumber;
    @FXML private TableColumn<Booking, String> colRoute;
    @FXML private TableColumn<Booking, String> colJourneyDate;
    @FXML private TableColumn<Booking, String> colBookingDate;
    @FXML private TableColumn<Booking, String> colClass;
    @FXML private TableColumn<Booking, Number> colPassengers;
    @FXML private TableColumn<Booking, Number> colAmount;
    @FXML private TableColumn<Booking, String> colStatus;
    @FXML private TableColumn<Booking, String> colPaymentStatus;
    @FXML private TableColumn<Booking, Void> colActions;

    // Status and Messaging
    @FXML private Label messageLabel;

    // -------------------------------------------------------------------------
    // Data Access and State Management
    // -------------------------------------------------------------------------

    private final BookingDAO bookingDAO = new BookingDAO();
    private final UserDAO userDAO = new UserDAO();
    private final TrainDAO trainDAO = new TrainDAO();
    private final StationDAO stationDAO = new StationDAO();

    private ObservableList<Booking> allBookings = FXCollections.observableArrayList();
    private ObservableList<Booking> filteredBookings = FXCollections.observableArrayList();

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the reservations management interface with full-screen layout.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        configureWindowLayout();
        setupTableColumns();
        setupFilters();
        loadBookings();
        updateStatistics();
    }

    /**
     * Configures the window for optimal data display with maximized layout.
     */
    private void configureWindowLayout() {
        Platform.runLater(() -> {
            Stage stage = (Stage) bookingTable.getScene().getWindow();
            if (stage != null) {
                stage.setMaximized(true);
            }
        });
    }

    /**
     * Sets up comprehensive table column configuration with data binding and styling.
     */
    private void setupTableColumns() {
        configureBasicColumns();
        configureComputedColumns();
        configureStyledColumns();
        configureActionColumn();

        bookingTable.setItems(filteredBookings);
    }

    /**
     * Configures basic table columns with direct property binding.
     */
    private void configureBasicColumns() {
        colId.setCellValueFactory(cellData -> cellData.getValue().bookingIdProperty());
        colPNR.setCellValueFactory(cellData -> cellData.getValue().pnrProperty());
        colUserName.setCellValueFactory(cellData -> cellData.getValue().userNameProperty());
        colTrainNumber.setCellValueFactory(cellData -> cellData.getValue().trainNumberProperty());
        colAmount.setCellValueFactory(cellData -> cellData.getValue().totalFareProperty());
    }

    /**
     * Configures computed columns that require data transformation or lookup.
     */
    private void configureComputedColumns() {
        setupRouteColumn();
        setupDateColumns();
        setupClassAndPassengerColumns();
    }

    /**
     * Sets up route column with station name lookup and formatting.
     */
    private void setupRouteColumn() {
        colRoute.setCellValueFactory(cellData -> {
            try {
                int sourceStationId = cellData.getValue().getSourceStationId();
                int destStationId = cellData.getValue().getDestStationId();

                Station sourceStation = stationDAO.getStationById(sourceStationId);
                Station destStation = stationDAO.getStationById(destStationId);

                String sourceName = sourceStation != null ? sourceStation.getName() : "Unknown";
                String destName = destStation != null ? destStation.getName() : "Unknown";

                return new javafx.beans.property.SimpleStringProperty(sourceName + " → " + destName);
            } catch (Exception e) {
                return new javafx.beans.property.SimpleStringProperty("Unknown Route");
            }
        });
    }

    /**
     * Sets up date columns with proper formatting.
     */
    private void setupDateColumns() {
        colJourneyDate.setCellValueFactory(cellData -> {
            LocalDateTime bookingTime = cellData.getValue().getBookingTime();
            String formattedDate = bookingTime != null ?
                    bookingTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
            return new javafx.beans.property.SimpleStringProperty(formattedDate);
        });

        colBookingDate.setCellValueFactory(cellData -> {
            LocalDateTime bookingTime = cellData.getValue().getBookingTime();
            String formattedDateTime = bookingTime != null ?
                    bookingTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) : "";
            return new javafx.beans.property.SimpleStringProperty(formattedDateTime);
        });
    }

    /**
     * Sets up class and passenger columns with default values.
     */
    private void setupClassAndPassengerColumns() {
        colClass.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty("Standard"));
        colPassengers.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(1));
    }

    /**
     * Configures columns with custom styling and formatting.
     */
    private void configureStyledColumns() {
        setupStatusColumn();
        setupPaymentStatusColumn();
    }

    /**
     * Sets up booking status column with contextual styling.
     */
    private void setupStatusColumn() {
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        colStatus.setCellFactory(column -> new TableCell<Booking, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label statusLabel = new Label(status);
                    statusLabel.getStyleClass().add("status-" + status.toLowerCase().replace(" ", "-"));
                    setGraphic(statusLabel);
                    setText(null);
                }
            }
        });
    }

    /**
     * Sets up payment status column with conditional styling.
     */
    private void setupPaymentStatusColumn() {
        colPaymentStatus.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            String paymentStatus = "conformed".equals(status) ? "Paid" : "Pending";
            return new javafx.beans.property.SimpleStringProperty(paymentStatus);
        });

        colPaymentStatus.setCellFactory(column -> new TableCell<Booking, String>() {
            @Override
            protected void updateItem(String paymentStatus, boolean empty) {
                super.updateItem(paymentStatus, empty);
                if (empty || paymentStatus == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label paymentLabel = new Label(paymentStatus);
                    paymentLabel.getStyleClass().add("payment-" + paymentStatus.toLowerCase().replace(" ", "-"));
                    setGraphic(paymentLabel);
                    setText(null);
                }
            }
        });
    }

    /**
     * Configures action column with view, update, and delete buttons.
     */
    private void configureActionColumn() {
        colActions.setCellFactory(column -> new TableCell<Booking, Void>() {
            private final Button viewBtn = new Button("View");
            private final Button updateBtn = new Button("Update");
            private final Button deleteBtn = new Button("Delete");
            private final HBox actionBox = new HBox(5);

            {
                configureActionButtons();
                setupActionHandlers();
            }

            private void configureActionButtons() {
                viewBtn.getStyleClass().add("action-btn-view");
                updateBtn.getStyleClass().add("action-btn-approve");
                deleteBtn.getStyleClass().add("action-btn-cancel");

                actionBox.setAlignment(Pos.CENTER);
                actionBox.getChildren().addAll(viewBtn, updateBtn, deleteBtn);
            }

            private void setupActionHandlers() {
                viewBtn.setOnAction(e -> {
                    Booking booking = getTableView().getItems().get(getIndex());
                    viewBookingDetails(booking);
                });

                updateBtn.setOnAction(e -> {
                    Booking booking = getTableView().getItems().get(getIndex());
                    updateBookingStatus(booking);
                });

                deleteBtn.setOnAction(e -> {
                    Booking booking = getTableView().getItems().get(getIndex());
                    deleteBooking(booking);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionBox);
                }
            }
        });
    }

    /**
     * Sets up filter controls with status and date options.
     */
    private void setupFilters() {
        configureStatusFilter();
        configureDateFilter();
        setupFilterHandlers();
    }

    /**
     * Configures status filter combo box with available booking statuses.
     */
    private void configureStatusFilter() {
        statusFilterCombo.getItems().addAll("All Status", "waiting", "confirmed", "cancelled");
        statusFilterCombo.setValue("All Status");
    }

    /**
     * Configures date filter combo box with time range options.
     */
    private void configureDateFilter() {
        dateFilterCombo.getItems().addAll("All Dates", "Today", "This Week", "This Month");
        dateFilterCombo.setValue("All Dates");
    }

    /**
     * Sets up event handlers for filter controls.
     */
    private void setupFilterHandlers() {
        statusFilterCombo.setOnAction(e -> applyFilters());
        dateFilterCombo.setOnAction(e -> applyFilters());
    }

    // -------------------------------------------------------------------------
    // Data Loading and Management
    // -------------------------------------------------------------------------

    /**
     * Loads all bookings with enriched data from related entities.
     * Populates user names and train numbers for complete display.
     */
    private void loadBookings() {
        try {
            List<Booking> bookings = bookingDAO.getAllBookings();
            enrichBookingData(bookings);

            allBookings.setAll(bookings);
            filteredBookings.setAll(bookings);

            displayLoadingResults(bookings);
        } catch (Exception e) {
            showMessage("Error loading reservations: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    /**
     * Enriches booking data with user names and train numbers.
     *
     * @param bookings list of bookings to enrich
     */
    private void enrichBookingData(List<Booking> bookings) {
        for (Booking booking : bookings) {
            enrichUserInformation(booking);
            enrichTrainInformation(booking);
        }
    }

    /**
     * Enriches booking with user information.
     *
     * @param booking the booking to enrich
     */
    private void enrichUserInformation(Booking booking) {
        try {
            User user = userDAO.getUserById(booking.getUserId());
            booking.setUserName(user != null ? user.getName() : "Unknown User");
        } catch (Exception e) {
            booking.setUserName("Unknown User");
        }
    }

    /**
     * Enriches booking with train information.
     *
     * @param booking the booking to enrich
     */
    private void enrichTrainInformation(Booking booking) {
        try {
            Train train = trainDAO.getTrainById(booking.getTrainId());
            booking.setTrainNumber(train != null ? train.getTrainNumber() : "Unknown Train");
        } catch (Exception e) {
            booking.setTrainNumber("Unknown Train");
        }
    }

    /**
     * Displays results of booking data loading operation.
     *
     * @param bookings the loaded bookings
     */
    private void displayLoadingResults(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            showMessage("No reservations found.", "info");
        } else {
            showMessage("Loaded " + bookings.size() + " reservations.", "success");
        }
    }

    /**
     * Updates booking statistics with current data calculations.
     */
    private void updateStatistics() {
        try {
            BookingStatistics stats = calculateBookingStatistics();
            updateStatisticsDisplay(stats);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculates comprehensive booking statistics.
     *
     * @return BookingStatistics object with calculated values
     */
    private BookingStatistics calculateBookingStatistics() {
        int total = allBookings.size();
        long confirmed = allBookings.stream().filter(b -> "confirmed".equals(b.getStatus())).count();
        long pending = allBookings.stream().filter(b -> "waiting".equals(b.getStatus())).count();
        long cancelled = allBookings.stream().filter(b -> "cancelled".equals(b.getStatus())).count();

        double totalRevenue = allBookings.stream()
                .filter(b -> "confirmed".equals(b.getStatus()))
                .mapToDouble(Booking::getTotalFare)
                .sum();

        return new BookingStatistics(total, confirmed, pending, cancelled, totalRevenue);
    }

    /**
     * Updates statistics display with calculated values.
     *
     * @param stats the calculated statistics
     */
    private void updateStatisticsDisplay(BookingStatistics stats) {
        Platform.runLater(() -> {
            totalBookingsLabel.setText(String.valueOf(stats.total));
            confirmedBookingsLabel.setText(String.valueOf(stats.confirmed));
            pendingBookingsLabel.setText(String.valueOf(stats.pending));
            cancelledBookingsLabel.setText(String.valueOf(stats.cancelled));
            totalRevenueLabel.setText("₹" + String.format("%,.2f", stats.totalRevenue));
        });
    }

    // -------------------------------------------------------------------------
    // Search and Filter Operations
    // -------------------------------------------------------------------------

    /**
     * Handles search functionality with current filter criteria.
     */
    @FXML
    public void handleSearch() {
        applyFilters();
    }

    /**
     * Clears all search and filter criteria and resets display.
     */
    @FXML
    public void handleClearSearch() {
        searchField.clear();
        statusFilterCombo.setValue("All Status");
        dateFilterCombo.setValue("All Dates");
        filteredBookings.setAll(allBookings);
        showMessage("Filters cleared.", "info");
    }

    /**
     * Applies current filter criteria to booking display.
     */
    private void applyFilters() {
        String searchText = searchField.getText().trim().toLowerCase();
        String selectedStatus = statusFilterCombo.getValue();

        List<Booking> filtered = allBookings.stream()
                .filter(booking -> matchesSearchCriteria(booking, searchText))
                .filter(booking -> matchesStatusFilter(booking, selectedStatus))
                .collect(Collectors.toList());

        filteredBookings.setAll(filtered);
        showMessage("Found " + filtered.size() + " reservations.", "info");
    }

    /**
     * Checks if booking matches search criteria.
     *
     * @param booking the booking to check
     * @param searchText the search text
     * @return true if booking matches search criteria
     */
    private boolean matchesSearchCriteria(Booking booking, String searchText) {
        return searchText.isEmpty() ||
                booking.getPnr().toLowerCase().contains(searchText) ||
                booking.getUserName().toLowerCase().contains(searchText) ||
                booking.getTrainNumber().toLowerCase().contains(searchText);
    }

    /**
     * Checks if booking matches status filter.
     *
     * @param booking the booking to check
     * @param selectedStatus the selected status filter
     * @return true if booking matches status filter
     */
    private boolean matchesStatusFilter(Booking booking, String selectedStatus) {
        return "All Status".equals(selectedStatus) || booking.getStatus().equals(selectedStatus);
    }

    // -------------------------------------------------------------------------
    // Administrative Actions
    // -------------------------------------------------------------------------

    /**
     * Displays detailed information for a specific booking.
     *
     * @param booking the booking to view details for
     */
    private void viewBookingDetails(Booking booking) {
        try {
            Alert detailsDialog = new Alert(Alert.AlertType.INFORMATION);
            detailsDialog.setTitle("Booking Details");
            detailsDialog.setHeaderText("PNR: " + booking.getPnr());

            String details = buildBookingDetailsText(booking);

            detailsDialog.setContentText(details);
            detailsDialog.getDialogPane().setPrefWidth(400);
            detailsDialog.showAndWait();

        } catch (Exception e) {
            showMessage("Error loading details: " + e.getMessage(), "error");
        }
    }

    /**
     * Builds detailed text information for booking display.
     *
     * @param booking the booking to build details for
     * @return formatted booking details string
     */
    private String buildBookingDetailsText(Booking booking) {
        StringBuilder details = new StringBuilder();
        details.append("Booking ID: ").append(booking.getBookingId()).append("\n");
        details.append("User: ").append(booking.getUserName()).append("\n");
        details.append("Train: ").append(booking.getTrainNumber()).append("\n");
        details.append("Journey ID: ").append(booking.getJourneyId()).append("\n");

        // Get station names
        try {
            Station sourceStation = stationDAO.getStationById(booking.getSourceStationId());
            Station destStation = stationDAO.getStationById(booking.getDestStationId());
            String sourceName = sourceStation != null ? sourceStation.getName() : "Unknown";
            String destName = destStation != null ? destStation.getName() : "Unknown";
            details.append("Route: ").append(sourceName).append(" → ").append(destName).append("\n");
        } catch (Exception e) {
            details.append("Route: Unknown\n");
        }

        details.append("Fare: ₹").append(String.format("%.2f", booking.getTotalFare())).append("\n");
        details.append("Status: ").append(booking.getStatus()).append("\n");
        details.append("Booking Time: ").append(booking.getBookingTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))).append("\n");

        return details.toString();
    }

    /**
     * Updates booking status with admin confirmation.
     *
     * @param booking the booking to update
     */
    private void updateBookingStatus(Booking booking) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Update Status");
        dialog.setHeaderText("Update status for PNR: " + booking.getPnr());

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("waiting", "confirmed", "cancelled");
        statusCombo.setValue(booking.getStatus());

        dialog.getDialogPane().setContent(statusCombo);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return statusCombo.getValue();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newStatus -> processStatusUpdate(booking, newStatus));
    }

    /**
     * Processes the booking status update operation.
     *
     * @param booking the booking to update
     * @param newStatus the new status to set
     */
    private void processStatusUpdate(Booking booking, String newStatus) {
        try {
            if (bookingDAO.updateBookingStatus(booking.getBookingId(), newStatus)) {
                booking.setStatus(newStatus);
                bookingTable.refresh();
                updateStatistics();
                showMessage("Status updated successfully", "success");
            } else {
                showMessage("Failed to update status", "error");
            }
        } catch (Exception e) {
            showMessage("Error updating status: " + e.getMessage(), "error");
        }
    }

    /**
     * Deletes a booking with admin confirmation.
     *
     * @param booking the booking to delete
     */
    private void deleteBooking(Booking booking) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Booking");
        confirmDialog.setHeaderText("Delete PNR: " + booking.getPnr());
        confirmDialog.setContentText("Are you sure? This action cannot be undone.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            processBookingDeletion(booking);
        }
    }

    /**
     * Processes the booking deletion operation.
     *
     * @param booking the booking to delete
     */
    private void processBookingDeletion(Booking booking) {
        try {
            if (bookingDAO.deleteBooking(booking.getBookingId())) {
                allBookings.remove(booking);
                filteredBookings.remove(booking);
                updateStatistics();
                showMessage("Booking deleted successfully", "success");
            } else {
                showMessage("Failed to delete booking", "error");
            }
        } catch (Exception e) {
            showMessage("Error deleting booking: " + e.getMessage(), "error");
        }
    }

    // -------------------------------------------------------------------------
    // Data Operations and Reports
    // -------------------------------------------------------------------------

    /**
     * Refreshes all booking data and statistics.
     */
    @FXML
    public void handleRefresh() {
        loadBookings();
        updateStatistics();
        showMessage("Data refreshed.", "success");
    }

    /**
     * Handles data export functionality (placeholder for future implementation).
     */
    @FXML
    public void handleExportData() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Data");
        alert.setHeaderText("Export Feature - Future Implementation");
        alert.setContentText("Export functionality will be implemented soon.\n\nCurrently showing " +
                filteredBookings.size() + " reservations.");
        alert.showAndWait();
    }

    /**
     * Generates and displays comprehensive booking statistics report.
     */
    @FXML
    public void handleGenerateReport() {
        try {
            String report = buildStatisticsReport();
            displayStatisticsReport(report);
        } catch (Exception e) {
            showMessage("Error generating report: " + e.getMessage(), "error");
        }
    }

    /**
     * Builds comprehensive statistics report text.
     *
     * @return formatted report string
     */
    private String buildStatisticsReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== RESERVATIONS REPORT ===\n\n");
        report.append("Total Reservations: ").append(filteredBookings.size()).append("\n");

        long confirmed = filteredBookings.stream().filter(b -> "confirmed".equals(b.getStatus())).count();
        long pending = filteredBookings.stream().filter(b -> "waiting".equals(b.getStatus())).count();
        long cancelled = filteredBookings.stream().filter(b -> "cancelled".equals(b.getStatus())).count();

        report.append("Confirmed: ").append(confirmed).append("\n");
        report.append("Waiting: ").append(pending).append("\n");
        report.append("Cancelled: ").append(cancelled).append("\n\n");

        double totalRevenue = filteredBookings.stream()
                .filter(b -> "confirmed".equals(b.getStatus()))
                .mapToDouble(Booking::getTotalFare)
                .sum();

        report.append("Total Revenue: ₹").append(String.format("%.2f", totalRevenue)).append("\n");
        report.append("\nGenerated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));

        return report.toString();
    }

    /**
     * Displays the statistics report in a dialog.
     *
     * @param report the report text to display
     */
    private void displayStatisticsReport(String report) {
        Alert reportDialog = new Alert(Alert.AlertType.INFORMATION);
        reportDialog.setTitle("Reservations Report");
        reportDialog.setHeaderText("Statistics Report");
        reportDialog.setContentText(report);
        reportDialog.getDialogPane().setPrefWidth(400);
        reportDialog.showAndWait();
    }

    /**
     * Handles bulk actions functionality (placeholder for future implementation).
     */
    @FXML
    public void handleBulkActions() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Bulk Actions");
        alert.setHeaderText("Bulk Actions - Future Implementation");
        alert.setContentText("Bulk actions will be implemented in future updates.");
        alert.showAndWait();
    }

    // -------------------------------------------------------------------------
    // Navigation and Window Management
    // -------------------------------------------------------------------------

    /**
     * Closes the reservations management window.
     */
    @FXML
    public void handleClose() {
        SceneManager.switchScene("/fxml/AdminProfile.fxml");
    }

    // -------------------------------------------------------------------------
    // UI State Management and Messaging
    // -------------------------------------------------------------------------

    /**
     * Displays status message with automatic hiding after delay.
     *
     * @param message the message to display
     * @param type the message type for styling
     */
    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);

        messageLabel.getStyleClass().removeAll("success", "error", "info");
        messageLabel.getStyleClass().add(type);

        // Auto-hide message after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    messageLabel.setVisible(false);
                    messageLabel.setManaged(false);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // -------------------------------------------------------------------------
    // Inner Classes for Data Management
    // -------------------------------------------------------------------------

    /**
     * BookingStatistics holds calculated statistical information for display.
     */
    private static class BookingStatistics {
        final int total;
        final long confirmed;
        final long pending;
        final long cancelled;
        final double totalRevenue;

        BookingStatistics(int total, long confirmed, long pending, long cancelled, double totalRevenue) {
            this.total = total;
            this.confirmed = confirmed;
            this.pending = pending;
            this.cancelled = cancelled;
            this.totalRevenue = totalRevenue;
        }
    }
}