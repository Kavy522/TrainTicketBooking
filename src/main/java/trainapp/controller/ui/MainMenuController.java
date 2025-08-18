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

/**
 * MainMenuController manages the primary application interface and train search functionality.
 * Serves as the central hub for train booking, services, and user account management.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Advanced train search with intelligent station selection</li>
 *   <li>Quick service access for PNR status, train schedules, and information</li>
 *   <li>Integrated user account management with session-aware navigation</li>
 *   <li>Searchable station combo boxes with real-time filtering</li>
 *   <li>Form validation with user-friendly error messaging</li>
 *   <li>Contextual authentication flow for protected features</li>
 * </ul>
 *
 * <p>Search Functionality:
 * <ul>
 *   <li>Multi-criteria station search (name, code, city, state)</li>
 *   <li>Real-time filtering with keyboard navigation support</li>
 *   <li>Station swapping for convenient return journey planning</li>
 *   <li>Date validation with forward-date restrictions</li>
 *   <li>Duplicate search prevention and optimization</li>
 * </ul>
 *
 * <p>User Experience Features:
 * <ul>
 *   <li>Auto-hiding error messages with timing control</li>
 *   <li>Session-aware feature access and redirection</li>
 *   <li>Quick service shortcuts for common operations</li>
 *   <li>Consistent validation feedback across all forms</li>
 *   <li>Intelligent navigation based on user authentication state</li>
 * </ul>
 */
public class MainMenuController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // Main Booking Form Controls
    @FXML private ComboBox<Station> trainFromCombo;
    @FXML private ComboBox<Station> trainToCombo;
    @FXML private DatePicker trainDatePicker;
    @FXML private Button searchTrainsButton;
    @FXML private Label errorMessageLabel;
    @FXML private Label bookingStatus;

    // Quick Services Form Controls
    @FXML private ComboBox<Station> fromStationCombo;
    @FXML private ComboBox<Station> toStationCombo;
    @FXML private DatePicker trainsDatePicker;

    // Service Input Fields
    @FXML private TextField trainStatusField;
    @FXML private TextField pnrStatusField;
    @FXML private TextField timeTableField;

    // -------------------------------------------------------------------------
    // Services and Data Management
    // -------------------------------------------------------------------------

    private final StationDAO stationDAO = new StationDAO();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private ObservableList<Station> allStations = FXCollections.observableArrayList();

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the main menu interface with station data and default values.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        loadStations();
        setupComboBoxes();
        initializeDefaultValues();
    }

    /**
     * Loads all available stations from the database.
     * Populates the stations list for use in combo boxes.
     */
    private void loadStations() {
        try {
            List<Station> stations = stationDAO.getAllStations();
            allStations.setAll(stations);
        } catch (Exception e) {
            showErrorMessage("Failed to load stations. Please refresh the page.");
            e.printStackTrace();
        }
    }

    /**
     * Sets up all station selection combo boxes with search functionality.
     */
    private void setupComboBoxes() {
        setupSearchableStationCombo(trainFromCombo, "Select departure station");
        setupSearchableStationCombo(trainToCombo, "Select destination station");
        setupSearchableStationCombo(fromStationCombo, "From station");
        setupSearchableStationCombo(toStationCombo, "To station");
    }

    /**
     * Initializes default values for date pickers and other controls.
     */
    private void initializeDefaultValues() {
        LocalDate today = LocalDate.now();
        if (trainDatePicker != null) {
            trainDatePicker.setValue(today);
        }
        if (trainsDatePicker != null) {
            trainsDatePicker.setValue(today);
        }
    }

    // -------------------------------------------------------------------------
    // Advanced Station Search Setup
    // -------------------------------------------------------------------------

    /**
     * Configures a station combo box with advanced search and filtering capabilities.
     * Provides real-time filtering based on station name, code, city, and state.
     *
     * @param comboBox the station combo box to configure
     * @param promptText placeholder text for the combo box
     */
    private void setupSearchableStationCombo(ComboBox<Station> comboBox, String promptText) {
        comboBox.setItems(allStations);
        comboBox.setEditable(true);
        comboBox.setPromptText(promptText);

        configureComboBoxConverter(comboBox);
        configureComboBoxDisplay(comboBox);
        setupComboBoxSearchBehavior(comboBox);
    }

    /**
     * Configures the string converter for proper station display and parsing.
     *
     * @param comboBox the combo box to configure
     */
    private void configureComboBoxConverter(ComboBox<Station> comboBox) {
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
    }

    /**
     * Configures the display formatting for dropdown and selected values.
     *
     * @param comboBox the combo box to configure
     */
    private void configureComboBoxDisplay(ComboBox<Station> comboBox) {
        // Cell factory for detailed dropdown display
        comboBox.setCellFactory(param -> new ListCell<Station>() {
            @Override
            protected void updateItem(Station item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.getName() + " (" + item.getStationCode() + ") - " + item.getCity() + ", " + item.getState());
            }
        });

        // Button cell for compact selected value display
        comboBox.setButtonCell(new ListCell<Station>() {
            @Override
            protected void updateItem(Station item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (" + item.getStationCode() + ")");
            }
        });
    }

    /**
     * Sets up search behavior for station combo boxes including text filtering and keyboard handling.
     *
     * @param comboBox the combo box to configure search behavior for
     */
    private void setupComboBoxSearchBehavior(ComboBox<Station> comboBox) {
        TextField editor = comboBox.getEditor();

        setupTextFilteringBehavior(comboBox, editor);
        setupSelectionHandling(comboBox);
        setupFocusHandling(comboBox, editor);
        setupKeyboardHandling(comboBox, editor);
    }

    /**
     * Sets up real-time text filtering for station search.
     *
     * @param comboBox the combo box being configured
     * @param editor the text editor component
     */
    private void setupTextFilteringBehavior(ComboBox<Station> comboBox, TextField editor) {
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
    }

    /**
     * Sets up selection handling for combo box interactions.
     *
     * @param comboBox the combo box being configured
     */
    private void setupSelectionHandling(ComboBox<Station> comboBox) {
        comboBox.setOnAction(event -> {
            Station selected = comboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Platform.runLater(() -> {
                    comboBox.getEditor().setText(selected.getName() + " (" + selected.getStationCode() + ")");
                    comboBox.hide();
                });
            }
        });
    }

    /**
     * Sets up focus handling for text selection and validation.
     *
     * @param comboBox the combo box being configured
     * @param editor the text editor component
     */
    private void setupFocusHandling(ComboBox<Station> comboBox, TextField editor) {
        editor.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                Platform.runLater(() -> editor.selectAll());
            } else {
                validateAndSetSelection(comboBox, editor);
            }
        });
    }

    /**
     * Validates and sets the selection when focus is lost.
     *
     * @param comboBox the combo box being validated
     * @param editor the text editor component
     */
    private void validateAndSetSelection(ComboBox<Station> comboBox, TextField editor) {
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

    /**
     * Sets up keyboard navigation and shortcuts.
     *
     * @param comboBox the combo box being configured
     * @param editor the text editor component
     */
    private void setupKeyboardHandling(ComboBox<Station> comboBox, TextField editor) {
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

    // -------------------------------------------------------------------------
    // Search and Filtering Utilities
    // -------------------------------------------------------------------------

    /**
     * Checks if a station matches the given search criteria.
     * Searches across name, station code, city, and state fields.
     *
     * @param station the station to check
     * @param searchText the search text to match against
     * @return true if station matches search criteria
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
     * Finds a station by text input using multiple matching strategies.
     * Attempts exact matches by code and name, then partial matches.
     *
     * @param text the text to search for
     * @return matching Station object or null if not found
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

        return findStationByExactOrPartialMatch(searchText);
    }

    /**
     * Attempts to find station by exact or partial text matching.
     *
     * @param searchText the normalized search text
     * @return matching Station object or null if not found
     */
    private Station findStationByExactOrPartialMatch(String searchText) {
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

    // -------------------------------------------------------------------------
    // Main Search Functionality
    // -------------------------------------------------------------------------

    /**
     * Handles the main train search functionality with comprehensive validation.
     * Validates all required fields and initiates the search process.
     *
     * @param event ActionEvent from search button click
     */
    @FXML
    public void handleSearchClick(ActionEvent event) {
        clearErrorMessage();

        Station fromStation = trainFromCombo.getValue();
        Station toStation = trainToCombo.getValue();
        LocalDate journeyDate = trainDatePicker.getValue();

        if (!validateSearchInputs(fromStation, toStation, journeyDate)) {
            return;
        }

        initiateTrainSearch(fromStation, toStation, journeyDate);
    }

    /**
     * Validates all search input parameters with user-friendly error messages.
     *
     * @param fromStation selected departure station
     * @param toStation selected destination station
     * @param journeyDate selected journey date
     * @return true if all inputs are valid
     */
    private boolean validateSearchInputs(Station fromStation, Station toStation, LocalDate journeyDate) {
        if (fromStation == null) {
            showAlert("Please select a departure station.", "Validation Error");
            return false;
        }
        if (toStation == null) {
            showAlert("Please select a destination station.", "Validation Error");
            return false;
        }
        if (journeyDate == null || journeyDate.isBefore(LocalDate.now())) {
            showAlert("Please select a valid departure date (today or later).", "Validation Error");
            return false;
        }
        if (fromStation.equals(toStation)) {
            showAlert("Departure and destination stations cannot be the same.", "Validation Error");
            return false;
        }
        return true;
    }

    /**
     * Initiates the train search by navigating to search results page.
     *
     * @param fromStation departure station
     * @param toStation destination station
     * @param journeyDate journey date
     */
    private void initiateTrainSearch(Station fromStation, Station toStation, LocalDate journeyDate) {
        SearchTrainController searchController = SceneManager.switchScene("/fxml/TrainSearch.fxml");
        searchController.setSearchData(fromStation.getName(), toStation.getName(), journeyDate);
    }

    /**
     * Handles station swapping for convenient return journey planning.
     * Validates that both stations are selected before swapping.
     *
     * @param event ActionEvent from swap stations button
     */
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

    // -------------------------------------------------------------------------
    // User Account and Session Management
    // -------------------------------------------------------------------------

    /**
     * Handles user account access with intelligent navigation based on authentication state.
     * Redirects to appropriate interface based on user role or login requirement.
     *
     * @param event ActionEvent from my account button
     */
    @FXML
    public void handleMyAccount(ActionEvent event) {
        try {
            clearErrorMessage();

            if (!isSessionManagerAvailable()) {
                return;
            }

            if (!sessionManager.isLoggedIn()) {
                SceneManager.switchScene("/fxml/Login.fxml");
                return;
            }

            navigateToUserInterface();

        } catch (Exception e) {
            showErrorMessage("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Checks if the session manager is available and functional.
     *
     * @return true if session manager is available
     */
    private boolean isSessionManagerAvailable() {
        if (sessionManager == null) {
            showErrorMessage("Session manager not available. Please restart the application.");
            return false;
        }
        return true;
    }

    /**
     * Navigates to the appropriate user interface based on user role.
     */
    private void navigateToUserInterface() {
        if (sessionManager.isAdmin()) {
            SceneManager.switchScene("/fxml/AdminProfile.fxml");
        } else if (sessionManager.isUser()) {
            SceneManager.switchScene("/fxml/UserProfile.fxml");
        } else {
            showErrorMessage("Unable to determine user type. Please contact support.");
        }
    }

    /**
     * Handles user logout with session cleanup.
     *
     * @param event ActionEvent from logout button
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        if (sessionManager != null) {
            sessionManager.logout();
        }
        SceneManager.switchScene("/fxml/Login.fxml");
    }

    /**
     * Handles booking history access with authentication check.
     * Redirects to login if not authenticated, with appropriate context.
     *
     * @param event ActionEvent from view bookings button
     */
    @FXML
    public void handleViewBookings(ActionEvent event) {
        if (!sessionManager.isLoggedIn()) {
            LoginController loginController = SceneManager.switchScene("/fxml/Login.fxml");
            loginController.setLoginMessage("You need to login to view your bookings");
            loginController.setRedirectAfterLogin("/fxml/MyBookings.fxml");
        } else {
            MyBookingsController bookingsController = SceneManager.switchScene("/fxml/MyBookings.fxml");
            // The controller will automatically load bookings for the current user
        }
    }

    // -------------------------------------------------------------------------
    // Quick Service Handlers
    // -------------------------------------------------------------------------

    /**
     * Handles train status inquiry with input validation.
     *
     * @param event ActionEvent from train status button
     */
    @FXML
    public void handleTrainStatus(ActionEvent event) {
        String trainInfo = trainStatusField.getText();
        if (trainInfo == null || trainInfo.trim().isEmpty()) {
            showAlert("Please enter train name or number.", "Input Required");
            return;
        }
        showAlert("Train status feature will be available soon!", "Coming Soon");
    }

    /**
     * Handles PNR status inquiry with validation and navigation to PNR status page.
     *
     * @param event ActionEvent from PNR status button
     */
    @FXML
    public void handlePNRStatus(ActionEvent event) {
        String pnr = pnrStatusField.getText();

        if (!validatePNRInput(pnr)) {
            return;
        }

        navigateToPNRStatus(pnr.trim());
    }

    /**
     * Validates PNR input format and requirements.
     *
     * @param pnr the PNR input to validate
     * @return true if PNR input is valid
     */
    private boolean validatePNRInput(String pnr) {
        if (pnr == null || pnr.trim().isEmpty()) {
            showAlert("Please enter your PNR number.", "Input Required");
            return false;
        }

        if (pnr.length() != 10) {
            showAlert("PNR must be exactly 10 digits.", "Invalid PNR");
            return false;
        }

        return true;
    }

    /**
     * Navigates to PNR status page with the provided PNR number.
     *
     * @param pnr the validated PNR number
     */
    private void navigateToPNRStatus(String pnr) {
        try {
            PNRController pnrController = SceneManager.switchScene("/fxml/PNRStatus.fxml");
            pnrController.setPNRNumber(pnr);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to open PNR status window: " + e.getMessage(), "Error");
        }
    }

    /**
     * Handles train schedule inquiry with input validation.
     *
     * @param event ActionEvent from train schedule button
     */
    @FXML
    public void handleTrainSchedule(ActionEvent event) {
        String trainInfo = timeTableField.getText();
        if (trainInfo == null || trainInfo.trim().isEmpty()) {
            showAlert("Please enter train name or number.", "Input Required");
            return;
        }
        showAlert("Train schedule feature will be available soon!", "Coming Soon");
    }

    /**
     * Handles quick train search from the services section.
     * Validates inputs and delegates to main search functionality.
     *
     * @param event ActionEvent from find trains button
     */
    @FXML
    public void handleFindTrains(ActionEvent event) {
        Station fromStation = fromStationCombo.getValue();
        Station toStation = toStationCombo.getValue();
        LocalDate journeyDate = trainsDatePicker.getValue();

        if (!validateQuickSearchInputs(fromStation, toStation, journeyDate)) {
            return;
        }

        // Sync with main form and use main search functionality
        synchronizeWithMainForm(fromStation, toStation, journeyDate);
        handleSearchClick(event);
    }

    /**
     * Validates quick search inputs from the services section.
     *
     * @param fromStation departure station
     * @param toStation destination station
     * @param journeyDate journey date
     * @return true if inputs are valid
     */
    private boolean validateQuickSearchInputs(Station fromStation, Station toStation, LocalDate journeyDate) {
        if (fromStation == null || toStation == null) {
            showAlert("Please select both departure and destination stations.", "Input Required");
            return false;
        }
        if (journeyDate == null) {
            showAlert("Please select a journey date.", "Input Required");
            return false;
        }
        if (fromStation.equals(toStation)) {
            showAlert("Departure and destination stations cannot be the same.", "Validation Error");
            return false;
        }
        return true;
    }

    /**
     * Synchronizes quick search values with the main search form.
     *
     * @param fromStation departure station
     * @param toStation destination station
     * @param journeyDate journey date
     */
    private void synchronizeWithMainForm(Station fromStation, Station toStation, LocalDate journeyDate) {
        trainFromCombo.setValue(fromStation);
        trainToCombo.setValue(toStation);
        trainDatePicker.setValue(journeyDate);
    }

    // -------------------------------------------------------------------------
    // User Interface and Messaging
    // -------------------------------------------------------------------------

    /**
     * Displays an alert dialog with custom styling and proper error handling.
     *
     * @param message the message to display
     * @param title the dialog title
     */
    private void showAlert(String message, String title) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title != null ? title : "Information");
        alert.setHeaderText(null);
        alert.setContentText(message);

        applyCustomStyling(alert);
        alert.showAndWait();
    }

    /**
     * Applies custom CSS styling to alert dialogs.
     *
     * @param alert the alert dialog to style
     */
    private void applyCustomStyling(Alert alert) {
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/MainMenu.css").toExternalForm());
            alert.getDialogPane().getStyleClass().add("custom-alert");
        } catch (Exception e) {
            // CSS loading failed, continue without styling
        }
    }

    /**
     * Displays an error message with auto-hide functionality.
     *
     * @param message the error message to display
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
     * Clears the error message display.
     */
    private void clearErrorMessage() {
        if (errorMessageLabel != null) {
            errorMessageLabel.setVisible(false);
            errorMessageLabel.setManaged(false);
        }
    }
}