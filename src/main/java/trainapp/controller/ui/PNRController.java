package trainapp.controller.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import trainapp.dao.BookingDAO;
import trainapp.model.Passenger;
import trainapp.service.PNRService;
import trainapp.service.PNRService.PNRStatusInfo;
import trainapp.service.PNRService.PNRStatusResult;
import trainapp.util.SceneManager;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * PNRController manages PNR status lookup and booking details display.
 * Provides real-time PNR inquiry, booking lookup, and related actions for users.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Auto-search and manual PNR lookup with instant feedback</li>
 *   <li>Detailed PNR and booking information display</li>
 *   <li>Status-based styling and context-aware messaging</li>
 *   <li>Passenger summary in dynamic card layout</li>
 *   <li>Booking cancellation with confirmation</li>
 *   <li>Graceful fallback states and comprehensive error handling</li>
 * </ul>
 */
public class PNRController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // Header
    @FXML private Button backButton;
    @FXML private Label titleLabel;

    // PNR Input
    @FXML private TextField pnrSearchField;
    @FXML private Button searchButton;

    // Loading & Status Sections
    @FXML private VBox loadingSection;
    @FXML private VBox pnrDetailsSection;
    @FXML private VBox notFoundSection;
    @FXML private Label statusMessage;

    // PNR & Booking Details
    @FXML private Label pnrNumberLabel;
    @FXML private Label bookingStatusLabel;
    @FXML private Label trainDetailsLabel;
    @FXML private Label routeDetailsLabel;
    @FXML private Label journeyDateLabel;
    @FXML private Label bookingTimeLabel;
    @FXML private Label totalFareLabel;
    @FXML private Label paymentStatusLabel;

    // Passenger Details
    @FXML private VBox passengerListContainer;
    @FXML private Label passengerCountLabel;

    @FXML private Button CancleButton;

    // -------------------------------------------------------------------------
    // Services and Data Management
    // -------------------------------------------------------------------------

    private final PNRService pnrService = new PNRService();
    private PNRStatusInfo currentPNRInfo;
    private final BookingDAO bookingDAO = new BookingDAO();

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the PNR status UI.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        showLoadingSection(false);
        showPNRDetailsSection(false);
        showNotFoundSection(false);
    }

    // -------------------------------------------------------------------------
    // PNR Search Input and Trigger
    // -------------------------------------------------------------------------

    /**
     * Sets the PNR number programmatically and auto-triggers lookup.
     *
     * @param pnr the PNR to be looked up
     */
    public void setPNRNumber(String pnr) {
        pnrSearchField.setText(pnr);
        Platform.runLater(() -> searchPNR(pnr));
    }

    /**
     * Handles manual PNR search when triggered by user input.
     *
     * @param event ActionEvent from search button
     */
    @FXML
    public void handleSearch(ActionEvent event) {
        String pnr = pnrSearchField.getText().trim();
        if (pnr.isEmpty()) {
            showStatusMessage("Please enter a PNR number", "error");
            return;
        }

        if (pnr.length() != 10) {
            showStatusMessage("PNR must be exactly 10 digits", "error");
            return;
        }

        hideStatusMessage();
        searchPNR(pnr);
    }

    /**
     * Searches for PNR status asynchronously and updates the UI.
     *
     * @param pnr the PNR to be searched
     */
    private void searchPNR(String pnr) {
        showLoadingSection(true);
        showPNRDetailsSection(false);
        showNotFoundSection(false);
        searchButton.setDisable(true);

        Task<PNRStatusResult> searchTask = new Task<PNRStatusResult>() {
            @Override
            protected PNRStatusResult call() throws Exception {
                return pnrService.getPNRStatus(pnr);
            }
        };

        searchTask.setOnSucceeded(e -> Platform.runLater(() -> {
            PNRStatusResult result = searchTask.getValue();
            showLoadingSection(false);
            searchButton.setDisable(false);

            if (result.isSuccess()) {
                currentPNRInfo = result.getData();
                displayPNRDetails(currentPNRInfo);
                showPNRDetailsSection(true);
            } else {
                showNotFoundSection(true);
                showStatusMessage(result.getMessage(), "error");
            }
        }));

        searchTask.setOnFailed(e -> Platform.runLater(() -> {
            showLoadingSection(false);
            searchButton.setDisable(false);
            showNotFoundSection(true);
            showStatusMessage("Failed to retrieve PNR status. Please try again.", "error");
        }));

        new Thread(searchTask).start();
    }

    // -------------------------------------------------------------------------
    // PNR Details Display
    // -------------------------------------------------------------------------

    /**
     * Displays all PNR status information in the UI.
     *
     * @param pnrInfo the retrieved PNR status and booking data
     */
    private void displayPNRDetails(PNRStatusInfo pnrInfo) {
        // Basic PNR and status
        pnrNumberLabel.setText(pnrInfo.getPnr());

        String status = pnrService.getFormattedStatus(pnrInfo.getBooking().getStatus());
        bookingStatusLabel.setText(status);
        bookingStatusLabel.getStyleClass().removeAll("status-confirmed", "status-pending", "status-cancelled");
        bookingStatusLabel.getStyleClass().add(pnrService.getStatusColorClass(status));

        if (CancleButton != null) {
            CancleButton.setDisable("CANCELLED".equalsIgnoreCase(pnrInfo.getBooking().getStatus()));
        }

        // Train and route/journey
        trainDetailsLabel.setText(pnrInfo.getTrainDetails());
        routeDetailsLabel.setText(pnrInfo.getRouteDetails());
        journeyDateLabel.setText(pnrInfo.getFormattedJourneyDate());

        // Booking date and total fare
        if (pnrInfo.getBooking().getBookingTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
            bookingTimeLabel.setText(pnrInfo.getBooking().getBookingTime().format(formatter));
        }
        totalFareLabel.setText("₹" + String.format("%.2f", pnrInfo.getBooking().getTotalFare()));

        // Payment status
        if (pnrInfo.getPayment() != null) {
            paymentStatusLabel.setText(pnrInfo.getPayment().getStatus().toUpperCase());
        } else {
            paymentStatusLabel.setText("PENDING");
        }

        displayPassengerDetails(pnrInfo.getPassengers());
    }

    /**
     * Displays detailed passenger information in a dynamic, card-based layout.
     *
     * @param passengers the list of passengers for this PNR
     */
    private void displayPassengerDetails(List<Passenger> passengers) {
        passengerListContainer.getChildren().clear();

        if (passengers == null || passengers.isEmpty()) {
            Label noPassengers = new Label("No passenger information available");
            noPassengers.getStyleClass().add("no-data-label");
            passengerListContainer.getChildren().add(noPassengers);
            passengerCountLabel.setText("0 Passengers");
            return;
        }

        passengerCountLabel.setText(passengers.size() + " Passenger" + (passengers.size() > 1 ? "s" : ""));

        for (int i = 0; i < passengers.size(); i++) {
            Passenger passenger = passengers.get(i);
            VBox passengerCard = createPassengerCard(passenger, i + 1);
            passengerListContainer.getChildren().add(passengerCard);
        }
    }

    /**
     * Creates a UI card view for an individual passenger.
     *
     * @param passenger the passenger data
     * @param serialNo the passenger's serial number
     * @return VBox containing the passenger card
     */
    private VBox createPassengerCard(Passenger passenger, int serialNo) {
        VBox card = new VBox(8);
        card.getStyleClass().add("passenger-card");

        // Header
        HBox header = new HBox(10);
        header.getStyleClass().add("passenger-header");
        Label serialLabel = new Label("Passenger " + serialNo);
        serialLabel.getStyleClass().add("passenger-serial");
        Label nameLabel = new Label(passenger.getName());
        nameLabel.getStyleClass().add("passenger-name");
        header.getChildren().addAll(serialLabel, nameLabel);

        // Details row
        HBox details = new HBox(20);
        details.getStyleClass().add("passenger-details");
        Label ageLabel = new Label("Age: " + passenger.getAge());
        Label genderLabel = new Label("Gender: " + passenger.getGender());
        Label seatLabel = new Label("Seat: " + (passenger.getSeatNumber() != null ? passenger.getSeatNumber() : "Not assigned"));
        Label coachLabel = new Label("Coach: " + (passenger.getCoachType() != null ? passenger.getCoachType() : "N/A"));
        details.getChildren().addAll(ageLabel, genderLabel, seatLabel, coachLabel);

        card.getChildren().addAll(header, details);
        return card;
    }

    // -------------------------------------------------------------------------
    // Actions and Handlers
    // -------------------------------------------------------------------------

    /**
     * Handles refresh of current PNR status.
     *
     * @param event ActionEvent from refresh button
     */
    @FXML
    public void handleRefresh(ActionEvent event) {
        String pnr = pnrSearchField.getText().trim();
        if (!pnr.isEmpty()) {
            searchPNR(pnr);
        }
    }

    /**
     * Handles ticket download request for current PNR.
     *
     * @param event ActionEvent from download button
     */
    @FXML
    public void handleDownloadTicket(ActionEvent event) {
        if (currentPNRInfo != null) {
            // TODO: Implement PDF download functionality
            showStatusMessage("Download feature will be available soon!", "info");
        }
    }

    /**
     * Handles booking cancellation with confirmation dialog.
     *
     * @param event ActionEvent from cancel booking button
     */
    @FXML
    public void handleCancelBooking(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Booking");
        confirm.setHeaderText("Cancel booking for PNR: " + currentPNRInfo.getPnr());
        confirm.setContentText("Are you sure you want to cancel this booking? This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean canceled = bookingDAO.cancelBooking(currentPNRInfo.getBooking().getBookingId());
            if (canceled) {
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Success");
                success.setHeaderText(null);
                success.setContentText("Booking canceled successfully.");
                success.showAndWait();
                handleRefresh(new ActionEvent());
            } else {
                Alert failure = new Alert(Alert.AlertType.ERROR);
                failure.setTitle("Error");
                failure.setHeaderText(null);
                failure.setContentText("Failed to cancel booking. Please try again.");
                failure.showAndWait();
            }
        }
    }

    /**
     * Handles navigation back to the main menu.
     *
     * @param event ActionEvent from back button
     */
    @FXML
    public void handleBack(ActionEvent event) {
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    // -------------------------------------------------------------------------
    // UI State and Messaging Helpers
    // -------------------------------------------------------------------------

    /**
     * Shows or hides loading section.
     * @param show true to show loading
     */
    private void showLoadingSection(boolean show) {
        if (loadingSection != null) {
            loadingSection.setVisible(show);
            loadingSection.setManaged(show);
        }
    }

    /**
     * Shows or hides booking details section.
     * @param show true to show details
     */
    private void showPNRDetailsSection(boolean show) {
        if (pnrDetailsSection != null) {
            pnrDetailsSection.setVisible(show);
            pnrDetailsSection.setManaged(show);
        }
    }

    /**
     * Shows or hides not found section.
     * @param show true to show not found state
     */
    private void showNotFoundSection(boolean show) {
        if (notFoundSection != null) {
            notFoundSection.setVisible(show);
            notFoundSection.setManaged(show);
        }
    }

    /**
     * Displays a status message with appropriate styling.
     *
     * @param message the text to display
     * @param type style class (e.g., "success", "error", "info")
     */
    private void showStatusMessage(String message, String type) {
        if (statusMessage != null) {
            statusMessage.setText(message);
            statusMessage.getStyleClass().removeAll("success", "error", "info");
            statusMessage.getStyleClass().add(type);
            statusMessage.setVisible(true);
            statusMessage.setManaged(true);
        }
    }

    /**
     * Hides the status message.
     */
    private void hideStatusMessage() {
        if(statusMessage != null){
            statusMessage.setVisible(false);
            statusMessage.setManaged(false);
        }
    }
}
