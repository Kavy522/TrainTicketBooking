package trainapp.controller.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import trainapp.model.Passenger;
import trainapp.service.PNRService;
import trainapp.service.PNRService.PNRStatusInfo;
import trainapp.service.PNRService.PNRStatusResult;
import trainapp.util.SceneManager;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class PNRController {

    // Header
    @FXML private Button backButton;
    @FXML private Label titleLabel;

    // PNR Input Section
    @FXML private TextField pnrSearchField;
    @FXML private Button searchButton;

    // Loading and Status
    @FXML private VBox loadingSection;
    @FXML private VBox pnrDetailsSection;
    @FXML private VBox notFoundSection;
    @FXML private Label statusMessage;

    // PNR Details
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

    // Services
    private final PNRService pnrService = new PNRService();
    private PNRStatusInfo currentPNRInfo;

    @FXML
    public void initialize() {
        // Hide all sections initially
        showLoadingSection(false);
        showPNRDetailsSection(false);
        showNotFoundSection(false);
    }

    /**
     * Set PNR number from MainMenu and search automatically
     */
    public void setPNRNumber(String pnr) {
        pnrSearchField.setText(pnr);
        Platform.runLater(() -> searchPNR(pnr));
    }

    /**
     * Handle manual PNR search
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
     * Search for PNR status
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

        searchTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
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
            });
        });

        searchTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                showLoadingSection(false);
                searchButton.setDisable(false);
                showNotFoundSection(true);
                showStatusMessage("Failed to retrieve PNR status. Please try again.", "error");
            });
        });

        new Thread(searchTask).start();
    }

    /**
     * Display PNR details in the UI
     */
    private void displayPNRDetails(PNRStatusInfo pnrInfo) {
        // Basic details
        pnrNumberLabel.setText(pnrInfo.getPnr());

        // Booking status with styling
        String status = pnrService.getFormattedStatus(pnrInfo.getBooking().getStatus());
        bookingStatusLabel.setText(status);
        bookingStatusLabel.getStyleClass().removeAll("status-confirmed", "status-pending", "status-cancelled");
        bookingStatusLabel.getStyleClass().add(pnrService.getStatusColorClass(status));

        // Train and journey details
        trainDetailsLabel.setText(pnrInfo.getTrainDetails());
        routeDetailsLabel.setText(pnrInfo.getRouteDetails());
        journeyDateLabel.setText(pnrInfo.getFormattedJourneyDate());

        // Booking and payment details
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

        // Passenger details
        displayPassengerDetails(pnrInfo.getPassengers());
    }

    /**
     * Display passenger information
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
     * Create passenger card UI
     */
    private VBox createPassengerCard(Passenger passenger, int serialNo) {
        VBox card = new VBox(8);
        card.getStyleClass().add("passenger-card");

        // Header with serial number and name
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

    /**
     * Handle refresh PNR status
     */
    @FXML
    public void handleRefresh(ActionEvent event) {
        String pnr = pnrSearchField.getText().trim();
        if (!pnr.isEmpty()) {
            searchPNR(pnr);
        }
    }

    /**
     * Handle download ticket
     */
    @FXML
    public void handleDownloadTicket(ActionEvent event) {
        if (currentPNRInfo != null) {
            // TODO: Implement PDF download
            showStatusMessage("Download feature will be available soon!", "info");
        }
    }

    /**
     * Handle cancel booking
     */
    @FXML
    public void handleCancelBooking(ActionEvent event) {
        if (currentPNRInfo != null) {
            // TODO: Implement booking cancellation
            showStatusMessage("Cancellation feature will be available soon!", "info");
        }
    }

    /**
     * Handle back to main menu
     */
    @FXML
    public void handleBack(ActionEvent event) {
        SceneManager.switchScene("/fxml/MainMenu.fxml");
    }

    // UI Helper methods
    private void showLoadingSection(boolean show) {
        if (loadingSection != null) {
            loadingSection.setVisible(show);
            loadingSection.setManaged(show);
        }
    }

    private void showPNRDetailsSection(boolean show) {
        if (pnrDetailsSection != null) {
            pnrDetailsSection.setVisible(show);
            pnrDetailsSection.setManaged(show);
        }
    }

    private void showNotFoundSection(boolean show) {
        if (notFoundSection != null) {
            notFoundSection.setVisible(show);
            notFoundSection.setManaged(show);
        }
    }

    private void showStatusMessage(String message, String type) {
        if (statusMessage != null) {
            statusMessage.setText(message);
            statusMessage.getStyleClass().removeAll("success", "error", "info");
            statusMessage.getStyleClass().add(type);
            statusMessage.setVisible(true);
            statusMessage.setManaged(true);
        }
    }

    private void hideStatusMessage(){
        if(statusMessage != null){
            statusMessage.setVisible(false);
            statusMessage.setManaged(false);
        }
    }
}
