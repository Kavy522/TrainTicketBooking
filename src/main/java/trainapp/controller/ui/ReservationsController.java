package trainapp.controller.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
import trainapp.service.SessionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReservationsController {

    // Search and Filter Controls
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> dateFilterCombo;

    // Statistics Labels
    @FXML private Label totalBookingsLabel;
    @FXML private Label confirmedBookingsLabel;
    @FXML private Label pendingBookingsLabel;
    @FXML private Label cancelledBookingsLabel;
    @FXML private Label totalRevenueLabel;

    // Table and Columns
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

    @FXML private Label messageLabel;

    // DAOs
    private final BookingDAO bookingDAO = new BookingDAO();
    private final UserDAO userDAO = new UserDAO();
    private final TrainDAO trainDAO = new TrainDAO();
    private final StationDAO stationDAO = new StationDAO();

    // Data
    private ObservableList<Booking> allBookings = FXCollections.observableArrayList();
    private ObservableList<Booking> filteredBookings = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            Stage stage = (Stage) bookingTable.getScene().getWindow();
            if (stage != null) {
                stage.setMaximized(true);
            }
        });

        setupTableColumns();
        setupFilters();
        loadBookings();
        updateStatistics();
    }

    private void setupTableColumns() {
        // Using your existing Booking model properties
        colId.setCellValueFactory(cellData -> cellData.getValue().bookingIdProperty());
        colPNR.setCellValueFactory(cellData -> cellData.getValue().pnrProperty());

        // User name - will be populated when loading data
        colUserName.setCellValueFactory(cellData -> cellData.getValue().userNameProperty());

        // Train number - will be populated when loading data
        colTrainNumber.setCellValueFactory(cellData -> cellData.getValue().trainNumberProperty());

        // Route - build from source and destination station IDs
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

        // Journey date - using journey ID (simplified as booking date for now)
        colJourneyDate.setCellValueFactory(cellData -> {
            LocalDateTime bookingTime = cellData.getValue().getBookingTime();
            String formattedDate = bookingTime != null ?
                    bookingTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
            return new javafx.beans.property.SimpleStringProperty(formattedDate);
        });

        // Booking date
        colBookingDate.setCellValueFactory(cellData -> {
            LocalDateTime bookingTime = cellData.getValue().getBookingTime();
            String formattedDateTime = bookingTime != null ?
                    bookingTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) : "";
            return new javafx.beans.property.SimpleStringProperty(formattedDateTime);
        });

        // Class - simplified as "Standard" for now since it's not in your model
        colClass.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty("Standard"));

        // Passengers - simplified as 1 for now since it's not in your model
        colPassengers.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(1));

        // Amount - using totalFare
        colAmount.setCellValueFactory(cellData -> cellData.getValue().totalFareProperty());

        // Status column with styling
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

        // Payment status - simplified as "Paid" for confirmed, "Pending" for others
        colPaymentStatus.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            String paymentStatus = "confirmed".equals(status) ? "Paid" : "Pending";
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

        // Actions column
        colActions.setCellFactory(column -> new TableCell<Booking, Void>() {
            private final Button viewBtn = new Button("View");
            private final Button updateBtn = new Button("Update");
            private final Button deleteBtn = new Button("Delete");
            private final HBox actionBox = new HBox(5);

            {
                viewBtn.getStyleClass().add("action-btn-view");
                updateBtn.getStyleClass().add("action-btn-approve");
                deleteBtn.getStyleClass().add("action-btn-cancel");

                actionBox.setAlignment(Pos.CENTER);
                actionBox.getChildren().addAll(viewBtn, updateBtn, deleteBtn);

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

        bookingTable.setItems(filteredBookings);
    }

    private void setupFilters() {
        // Using your status values: waiting, confirmed, cancelled
        statusFilterCombo.getItems().addAll("All Status", "waiting", "confirmed", "cancelled");
        statusFilterCombo.setValue("All Status");

        dateFilterCombo.getItems().addAll("All Dates", "Today", "This Week", "This Month");
        dateFilterCombo.setValue("All Dates");

        statusFilterCombo.setOnAction(e -> applyFilters());
        dateFilterCombo.setOnAction(e -> applyFilters());
    }

    private void loadBookings() {
        try {
            List<Booking> bookings = bookingDAO.getAllBookings();

            // Populate additional fields for display
            for (Booking booking : bookings) {
                // Load and set user name
                try {
                    User user = userDAO.getUserById(booking.getUserId());
                    if (user != null) {
                        booking.setUserName(user.getName());
                    } else {
                        booking.setUserName("Unknown User");
                    }
                } catch (Exception e) {
                    booking.setUserName("Unknown User");
                }

                // Load and set train number
                try {
                    Train train = trainDAO.getTrainById(booking.getTrainId());
                    if (train != null) {
                        booking.setTrainNumber(train.getTrainNumber());
                    } else {
                        booking.setTrainNumber("Unknown Train");
                    }
                } catch (Exception e) {
                    booking.setTrainNumber("Unknown Train");
                }
            }

            allBookings.setAll(bookings);
            filteredBookings.setAll(bookings);

            if (bookings.isEmpty()) {
                showMessage("No reservations found.", "info");
            } else {
                showMessage("Loaded " + bookings.size() + " reservations.", "success");
            }
        } catch (Exception e) {
            showMessage("Error loading reservations: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    private void updateStatistics() {
        try {
            int total = allBookings.size();
            long confirmed = allBookings.stream().filter(b -> "confirmed".equals(b.getStatus())).count();
            long pending = allBookings.stream().filter(b -> "waiting".equals(b.getStatus())).count();
            long cancelled = allBookings.stream().filter(b -> "cancelled".equals(b.getStatus())).count();

            double totalRevenue = allBookings.stream()
                    .filter(b -> "confirmed".equals(b.getStatus()))
                    .mapToDouble(Booking::getTotalFare)
                    .sum();

            Platform.runLater(() -> {
                totalBookingsLabel.setText(String.valueOf(total));
                confirmedBookingsLabel.setText(String.valueOf(confirmed));
                pendingBookingsLabel.setText(String.valueOf(pending));
                cancelledBookingsLabel.setText(String.valueOf(cancelled));
                totalRevenueLabel.setText("₹" + String.format("%,.2f", totalRevenue));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSearch() {
        applyFilters();
    }

    @FXML
    public void handleClearSearch() {
        searchField.clear();
        statusFilterCombo.setValue("All Status");
        dateFilterCombo.setValue("All Dates");
        filteredBookings.setAll(allBookings);
        showMessage("Filters cleared.", "info");
    }

    @FXML
    public void handleRefresh() {
        loadBookings();
        updateStatistics();
        showMessage("Data refreshed.", "success");
    }

    private void applyFilters() {
        String searchText = searchField.getText().trim().toLowerCase();
        String selectedStatus = statusFilterCombo.getValue();

        List<Booking> filtered = allBookings.stream()
                .filter(booking -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                            booking.getPnr().toLowerCase().contains(searchText) ||
                            booking.getUserName().toLowerCase().contains(searchText) ||
                            booking.getTrainNumber().toLowerCase().contains(searchText);

                    boolean matchesStatus = "All Status".equals(selectedStatus) ||
                            booking.getStatus().equals(selectedStatus);

                    return matchesSearch && matchesStatus;
                })
                .collect(Collectors.toList());

        filteredBookings.setAll(filtered);
        showMessage("Found " + filtered.size() + " reservations.", "info");
    }

    @FXML
    public void handleExportData() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Data");
        alert.setHeaderText("Export Feature - Future Implementation");
        alert.setContentText("Export functionality will be implemented soon.\n\nCurrently showing " +
                filteredBookings.size() + " reservations.");
        alert.showAndWait();
    }

    @FXML
    public void handleGenerateReport() {
        try {
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

            Alert reportDialog = new Alert(Alert.AlertType.INFORMATION);
            reportDialog.setTitle("Reservations Report");
            reportDialog.setHeaderText("Statistics Report");
            reportDialog.setContentText(report.toString());
            reportDialog.getDialogPane().setPrefWidth(400);
            reportDialog.showAndWait();

        } catch (Exception e) {
            showMessage("Error generating report: " + e.getMessage(), "error");
        }
    }

    @FXML
    public void handleBulkActions() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Bulk Actions");
        alert.setHeaderText("Bulk Actions - Future Implementation");
        alert.setContentText("Bulk actions will be implemented in future updates.");
        alert.showAndWait();
    }

    private void viewBookingDetails(Booking booking) {
        try {
            Alert detailsDialog = new Alert(Alert.AlertType.INFORMATION);
            detailsDialog.setTitle("Booking Details");
            detailsDialog.setHeaderText("PNR: " + booking.getPnr());

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

            detailsDialog.setContentText(details.toString());
            detailsDialog.getDialogPane().setPrefWidth(400);
            detailsDialog.showAndWait();

        } catch (Exception e) {
            showMessage("Error loading details: " + e.getMessage(), "error");
        }
    }

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
        result.ifPresent(newStatus -> {
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
        });
    }

    private void deleteBooking(Booking booking) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Booking");
        confirmDialog.setHeaderText("Delete PNR: " + booking.getPnr());
        confirmDialog.setContentText("Are you sure? This action cannot be undone.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
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
    }

    @FXML
    public void handleClose() {
        Stage stage = (Stage) bookingTable.getScene().getWindow();
        stage.close();
    }

    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);

        messageLabel.getStyleClass().removeAll("success", "error", "info");
        messageLabel.getStyleClass().add(type);

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
}
