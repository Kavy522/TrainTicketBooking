package trainapp.controller.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;
import javafx.util.StringConverter;
import trainapp.model.Station;
import trainapp.dao.StationDAO;
import trainapp.service.SessionManager;
import trainapp.util.SceneManager;

import java.time.LocalDate;
import java.util.List;

public class MainMenuController {

    // Main booking form ComboBoxes
    @FXML private ComboBox<Station> trainFromCombo;
    @FXML private ComboBox<Station> trainToCombo;
    @FXML private DatePicker trainDatePicker;
    @FXML private Button searchTrainsButton;
    @FXML private Label errorMessageLabel;
    @FXML private Label bookingStatus;

    // Quick services ComboBoxes
    @FXML private ComboBox<Station> fromStationCombo;
    @FXML private ComboBox<Station> toStationCombo;
    @FXML private DatePicker trainsDatePicker;

    // Service input fields
    @FXML private TextField trainStatusField;
    @FXML private TextField pnrStatusField;
    @FXML private TextField timeTableField;

    // Services and DAOs
    private final StationDAO stationDAO = new StationDAO();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private ObservableList<Station> allStations = FXCollections.observableArrayList();

    LoginController loginController = new LoginController();

    @FXML
    public void initialize() {
        loadStations();
        setupComboBoxes();

        // Set default date to today
        if (trainDatePicker != null) {
            trainDatePicker.setValue(LocalDate.now());
        }
        if (trainsDatePicker != null) {
            trainsDatePicker.setValue(LocalDate.now());
        }
    }

    private void loadStations() {
        try {
            List<Station> stations = stationDAO.getAllStations();
            allStations.setAll(stations);
        } catch (Exception e) {
            showErrorMessage("Failed to load stations. Please refresh the page.");
        }
    }

    private void setupComboBoxes() {
        // Setup main booking form ComboBoxes
        setupSearchableStationCombo(trainFromCombo, "Select departure station");
        setupSearchableStationCombo(trainToCombo, "Select destination station");

        // Setup quick services ComboBoxes
        setupSearchableStationCombo(fromStationCombo, "From station");
        setupSearchableStationCombo(toStationCombo, "To station");
    }

    /**
     * Setup searchable ComboBox with the same functionality as Fleet Management
     */
    private void setupSearchableStationCombo(ComboBox<Station> comboBox, String promptText) {
        comboBox.setItems(allStations);
        comboBox.setEditable(true);
        comboBox.setPromptText(promptText);

        // String converter for proper display
        comboBox.setConverter(new StringConverter<Station>() {
            @Override
            public String toString(Station station) {
                return station != null ? station.getName() + " (" + station.getStationCode() + ")" : null;
            }

            @Override
            public Station fromString(String string) {
                return findStationByText(string);
            }
        });

        // Cell factory for dropdown display
        comboBox.setCellFactory(param -> new ListCell<Station>() {
            @Override
            protected void updateItem(Station item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.getName() + " (" + item.getStationCode() + ") - " + item.getCity() + ", " + item.getState());
            }
        });

        comboBox.setButtonCell(new ListCell<Station>() {
            @Override
            protected void updateItem(Station item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (" + item.getStationCode() + ")");
            }
        });

        // Add search functionality
        TextField editor = comboBox.getEditor();

        editor.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!comboBox.isFocused()) return;

            Platform.runLater(() -> {
                if (newValue == null || newValue.isEmpty()) {
                    comboBox.setItems(allStations);
                } else {
                    String searchText = newValue.toLowerCase().trim();
                    ObservableList<Station> filteredStations = FXCollections.observableArrayList();

                    for (Station station : allStations) {
                        if (matchesSearchCriteria(station, searchText)) {
                            filteredStations.add(station);
                        }
                    }

                    comboBox.setItems(filteredStations);

                    if (!filteredStations.isEmpty() && !comboBox.isShowing()) {
                        comboBox.show();
                    }
                }
            });
        });

        // Selection handling
        comboBox.setOnAction(event -> {
            Station selected = comboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Platform.runLater(() -> {
                    comboBox.getEditor().setText(selected.getName() + " (" + selected.getStationCode() + ")");
                    comboBox.hide();
                });
            }
        });

        // Focus handling
        editor.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                Platform.runLater(() -> editor.selectAll());
            } else {
                Station currentValue = comboBox.getValue();
                String editorText = editor.getText();

                if (currentValue == null && !editorText.isEmpty()) {
                    Station match = findStationByText(editorText);
                    if (match != null) {
                        comboBox.setValue(match);
                    } else {
                        editor.clear();
                        comboBox.setValue(null);
                    }
                } else if (currentValue != null) {
                    editor.setText(currentValue.getName() + " (" + currentValue.getStationCode() + ")");
                }
            }
        });

        // Keyboard handling
        editor.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER:
                    if (!comboBox.getItems().isEmpty()) {
                        comboBox.setValue(comboBox.getItems().get(0));
                    }
                    event.consume();
                    break;
                case ESCAPE:
                    editor.clear();
                    comboBox.hide();
                    event.consume();
                    break;
                case DOWN:
                    if (!comboBox.isShowing()) {
                        comboBox.show();
                    }
                    break;
            }
        });
    }

    /**
     * Check if station matches search criteria
     */
    private boolean matchesSearchCriteria(Station station, String searchText) {
        if (station == null || searchText == null || searchText.isEmpty()) {
            return true;
        }

        String search = searchText.toLowerCase();

        return (station.getName() != null && station.getName().toLowerCase().contains(search))
                || (station.getStationCode() != null && station.getStationCode().toLowerCase().contains(search))
                || (station.getCity() != null && station.getCity().toLowerCase().contains(search))
                || (station.getState() != null && station.getState().toLowerCase().contains(search));
    }

    /**
     * Find station by text input for search functionality
     */
    private Station findStationByText(String text) {
        if (text == null || text.trim().isEmpty()) return null;

        String searchText = text.toLowerCase().trim();

        // Extract station code if format is "Name (CODE)"
        if (text.contains("(") && text.contains(")")) {
            int start = text.lastIndexOf('(');
            int end = text.lastIndexOf(')');
            if (start < end) {
                String code = text.substring(start + 1, end).trim();
                for (Station station : allStations) {
                    if (station.getStationCode() != null &&
                            station.getStationCode().equalsIgnoreCase(code)) {
                        return station;
                    }
                }
            }
        }

        // Try exact name match
        for (Station station : allStations) {
            if (station.getName() != null &&
                    station.getName().equalsIgnoreCase(searchText)) {
                return station;
            }
        }

        // Try exact code match
        for (Station station : allStations) {
            if (station.getStationCode() != null &&
                    station.getStationCode().equalsIgnoreCase(searchText)) {
                return station;
            }
        }

        // Try partial name match
        for (Station station : allStations) {
            if (station.getName() != null &&
                    station.getName().toLowerCase().contains(searchText)) {
                return station;
            }
        }

        return null;
    }

    @FXML
    public void handleSwapStations(ActionEvent event) {
        Station fromStation = trainFromCombo.getValue();
        Station toStation = trainToCombo.getValue();

        if (fromStation == null || toStation == null) {
            showAlert("Please select both departure and destination stations before swapping.", "Warning");
        } else {
            trainFromCombo.setValue(toStation);
            trainToCombo.setValue(fromStation);
        }
    }

    @FXML
    public void handleSearchClick(ActionEvent event) {
        // Clear any previous error messages
        clearErrorMessage();

        Station fromStation = trainFromCombo.getValue();
        Station toStation = trainToCombo.getValue();
        LocalDate journeyDate = trainDatePicker.getValue();

        // Validation
        if (fromStation == null) {
            showAlert("Please select a departure station.", "Validation Error");
            return;
        }
        if (toStation == null) {
            showAlert("Please select a destination station.", "Validation Error");
            return;
        }
        if (journeyDate == null || journeyDate.isBefore(LocalDate.now())) {
            showAlert("Please select a valid departure date (today or later).", "Validation Error");
            return;
        }
        if (fromStation.equals(toStation)) {
            showAlert("Departure and destination stations cannot be the same.", "Validation Error");
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/TrainSearch.fxml"));
            javafx.scene.Parent root = loader.load();

            trainapp.controller.ui.SearchTrainController searchController = loader.getController();
            searchController.setSearchData(fromStation.getName(), toStation.getName(), journeyDate);

            javafx.stage.Stage stage = (javafx.stage.Stage) trainFromCombo.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Internal error occurred while loading search results.", "Error");
        }
    }

    /**
     * Handle My Account click
     */
    @FXML
    public void handleMyAccount(ActionEvent event) {
        try {
            clearErrorMessage();

            if (sessionManager == null) {
                showErrorMessage("Session manager not available. Please restart the application.");
                return;
            }

            if (!sessionManager.isLoggedIn()) {
                try {
                    SceneManager.switchScene("/fxml/Login.fxml");
                } catch (Exception e) {
                    showErrorMessage("Failed to load login page.");
                }
                return;
            }

            try {
                if (sessionManager.isAdmin()) {
                    SceneManager.switchScene("/fxml/AdminProfile.fxml");
                } else if (sessionManager.isUser()) {
                    SceneManager.switchScene("/fxml/UserProfile.fxml");
                } else {
                    showErrorMessage("Unable to determine user type. Please contact support.");
                }
            } catch (Exception e) {
                showErrorMessage("Failed to load profile page: " + e.getMessage());
            }

        } catch (Exception e) {
            showErrorMessage("An unexpected error occurred: " + e.getMessage());
        }
    }

    // Additional service button handlers
    @FXML
    public void handleTrainStatus(ActionEvent event) {
        String trainInfo = trainStatusField.getText();
        if (trainInfo == null || trainInfo.trim().isEmpty()) {
            showAlert("Please enter train name or number.", "Input Required");
            return;
        }
        showAlert("Train status feature will be available soon!", "Coming Soon");
    }

    @FXML
    public void handlePNRStatus(ActionEvent event) {
        String pnr = pnrStatusField.getText();
        if (pnr == null || pnr.trim().isEmpty()) {
            showAlert("Please enter your PNR number.", "Input Required");
            return;
        }
        if (pnr.length() != 10) {
            showAlert("PNR must be exactly 10 digits.", "Invalid PNR");
            return;
        }
        showAlert("PNR status feature will be available soon!", "Coming Soon");
    }

    @FXML
    public void handleTrainSchedule(ActionEvent event) {
        String trainInfo = timeTableField.getText();
        if (trainInfo == null || trainInfo.trim().isEmpty()) {
            showAlert("Please enter train name or number.", "Input Required");
            return;
        }
        showAlert("Train schedule feature will be available soon!", "Coming Soon");
    }

    @FXML
    public void handleFindTrains(ActionEvent event) {
        Station fromStation = fromStationCombo.getValue();
        Station toStation = toStationCombo.getValue();
        LocalDate journeyDate = trainsDatePicker.getValue();

        if (fromStation == null || toStation == null) {
            showAlert("Please select both departure and destination stations.", "Input Required");
            return;
        }
        if (journeyDate == null) {
            showAlert("Please select a journey date.", "Input Required");
            return;
        }
        if (fromStation.equals(toStation)) {
            showAlert("Departure and destination stations cannot be the same.", "Validation Error");
            return;
        }

        // Use the same search functionality as main form
        trainFromCombo.setValue(fromStation);
        trainToCombo.setValue(toStation);
        trainDatePicker.setValue(journeyDate);
        handleSearchClick(event);
    }

    /**
     * Handle logout
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            if (sessionManager != null) {
                sessionManager.logout();
            }
            SceneManager.switchScene("/fxml/Login.fxml");
        } catch (Exception e) {
            showErrorMessage("Failed to logout properly.");
        }
    }

    /**
     * Handle view bookings
     */
    @FXML
    public void handleViewBookings(ActionEvent event) {
        try {
            if (!sessionManager.isLoggedIn()) {
                loginController.setRedirectAfterLogin("/fxml/MyBookings.fxml");
                SceneManager.switchScene("/fxml/Login.fxml");
            } else {
                SceneManager.switchScene("/fxml/MyBookings.fxml");
            }
        } catch (Exception e) {
            showErrorMessage("Failed to load bookings page.");
        }
    }

    // Utility methods
    private void showAlert(String message, String title) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title != null ? title : "Information");
        alert.setHeaderText(null);
        alert.setContentText(message);

        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/MainMenu.css").toExternalForm());
            alert.getDialogPane().getStyleClass().add("custom-alert");
        } catch (Exception e) {
            // CSS loading failed, continue without styling
        }

        alert.showAndWait();
    }

    /**
     * Show error message to user
     */
    private void showErrorMessage(String message) {
        if (errorMessageLabel != null) {
            errorMessageLabel.setText(message);
            errorMessageLabel.setVisible(true);
            errorMessageLabel.setManaged(true);

            // Auto-hide after 5 seconds
            PauseTransition hideDelay = new PauseTransition(Duration.seconds(5));
            hideDelay.setOnFinished(e -> clearErrorMessage());
            hideDelay.play();
        }
    }

    /**
     * Clear error message
     */
    private void clearErrorMessage() {
        if (errorMessageLabel != null) {
            errorMessageLabel.setVisible(false);
            errorMessageLabel.setManaged(false);
        }
    }
}