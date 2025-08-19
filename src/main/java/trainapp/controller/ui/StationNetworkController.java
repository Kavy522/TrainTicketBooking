package trainapp.controller.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import trainapp.dao.StationDAO;
import trainapp.model.Station;
import trainapp.util.SceneManager;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * StationNetworkController manages the complete station network administration interface.
 * Provides comprehensive station management with CRUD operations, filtering, and analytics.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Complete station network overview with real-time statistics</li>
 *   <li>Advanced filtering by state, city, and search criteria</li>
 *   <li>Full CRUD operations for station management (Create, Read, Update, Delete)</li>
 *   <li>Dynamic form validation with real-time input checking</li>
 *   <li>Statistical analysis with state and city distribution</li>
 *   <li>Comprehensive search functionality across all station attributes</li>
 * </ul>
 *
 * <p>Administrative Operations:
 * <ul>
 *   <li>Station creation with validation and duplicate prevention</li>
 *   <li>Station modification with real-time form validation</li>
 *   <li>Safe station deletion with confirmation dialogs</li>
 *   <li>Bulk filtering and search operations</li>
 *   <li>Data refresh and synchronization capabilities</li>
 * </ul>
 *
 * <p>Data Management Features:
 * <ul>
 *   <li>Real-time statistics calculation (total stations, states, cities)</li>
 *   <li>Dynamic filter option loading from database</li>
 *   <li>Cross-field validation and data consistency checks</li>
 *   <li>Error handling with user-friendly feedback messages</li>
 *   <li>Responsive table layout with action buttons</li>
 * </ul>
 */
public class StationNetworkController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // Search and Filter Controls
    @FXML private TextField searchField;
    @FXML private ComboBox<String> stateFilterCombo;
    @FXML private ComboBox<String> cityFilterCombo;

    // Statistics Display
    @FXML private Label totalStationsLabel;
    @FXML private Label statesCountLabel;
    @FXML private Label citiesCountLabel;

    // Data Table and Columns
    @FXML private TableView<Station> stationsTable;
    @FXML private TableColumn<Station, Number> colId;
    @FXML private TableColumn<Station, String> colCode;
    @FXML private TableColumn<Station, String> colName;
    @FXML private TableColumn<Station, String> colCity;
    @FXML private TableColumn<Station, String> colState;
    @FXML private TableColumn<Station, Void> colActions;

    // Status and Messaging
    @FXML private Label messageLabel;

    // -------------------------------------------------------------------------
    // Data Access and State Management
    // -------------------------------------------------------------------------

    private final StationDAO stationDAO = new StationDAO();
    private ObservableList<Station> stationsList = FXCollections.observableArrayList();
    private ObservableList<Station> filteredStations = FXCollections.observableArrayList();

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the station network management interface with full-screen layout.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        configureWindowLayout();
        setupTableColumns();
        setupComboBoxes();
        loadStations();
        updateStatistics();
    }

    /**
     * Configures the window for optimal data display with maximized layout.
     */
    private void configureWindowLayout() {
        Platform.runLater(() -> {
            Stage stage = (Stage) stationsTable.getScene().getWindow();
            if (stage != null) {
                stage.setMaximized(true);
            }
        });
    }

    /**
     * Sets up table columns with data binding and action buttons.
     */
    private void setupTableColumns() {
        configureDataColumns();
        configureActionColumn();
        stationsTable.setItems(filteredStations);
    }

    /**
     * Configures basic data columns with property binding.
     */
    private void configureDataColumns() {
        colId.setCellValueFactory(cellData -> cellData.getValue().stationIdProperty());
        colCode.setCellValueFactory(cellData -> cellData.getValue().stationCodeProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colCity.setCellValueFactory(cellData -> cellData.getValue().cityProperty());
        colState.setCellValueFactory(cellData -> cellData.getValue().stateProperty());
    }

    /**
     * Configures the action column with edit and delete buttons.
     */
    private void configureActionColumn() {
        colActions.setCellFactory(column -> new TableCell<Station, Void>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox actionBox = new HBox(8);

            {
                configureActionButtons();
                setupActionHandlers();
            }

            private void configureActionButtons() {
                editBtn.getStyleClass().add("action-btn-edit");
                deleteBtn.getStyleClass().add("action-btn-delete");

                actionBox.setAlignment(Pos.CENTER);
                actionBox.getChildren().addAll(editBtn, deleteBtn);
            }

            private void setupActionHandlers() {
                editBtn.setOnAction(e -> {
                    Station station = getTableView().getItems().get(getIndex());
                    handleEditStation(station);
                });

                deleteBtn.setOnAction(e -> {
                    Station station = getTableView().getItems().get(getIndex());
                    handleDeleteStation(station);
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
     * Sets up filter combo boxes with default values and options loading.
     */
    private void setupComboBoxes() {
        initializeComboBoxDefaults();
        loadFilterOptions();
    }

    /**
     * Initializes combo boxes with default "All" options.
     */
    private void initializeComboBoxDefaults() {
        stateFilterCombo.getItems().add("All States");
        stateFilterCombo.setValue("All States");

        cityFilterCombo.getItems().add("All Cities");
        cityFilterCombo.setValue("All Cities");
    }

    // -------------------------------------------------------------------------
    // Data Loading and Management
    // -------------------------------------------------------------------------

    /**
     * Loads filter options from database for dynamic filtering.
     */
    private void loadFilterOptions() {
        try {
            loadStateFilterOptions();
            loadCityFilterOptions();
        } catch (Exception e) {
            showMessage("Error loading filter options: " + e.getMessage(), "error");
        }
    }

    /**
     * Loads unique states for filter combo box.
     */
    private void loadStateFilterOptions() {
        try {
            List<String> states = stationDAO.getAllUniqueStates();
            stateFilterCombo.getItems().clear();
            stateFilterCombo.getItems().add("All States");
            stateFilterCombo.getItems().addAll(states);
            stateFilterCombo.setValue("All States");
        } catch (Exception e) {
            showMessage("Error loading states: " + e.getMessage(), "error");
        }
    }

    /**
     * Loads unique cities for filter combo box.
     */
    private void loadCityFilterOptions() {
        try {
            List<String> cities = stationDAO.getAllUniqueCities();
            cityFilterCombo.getItems().clear();
            cityFilterCombo.getItems().add("All Cities");
            cityFilterCombo.getItems().addAll(cities);
            cityFilterCombo.setValue("All Cities");
        } catch (Exception e) {
            showMessage("Error loading cities: " + e.getMessage(), "error");
        }
    }

    /**
     * Loads all stations from database and updates display.
     */
    private void loadStations() {
        try {
            List<Station> stations = stationDAO.getAllStations();
            stationsList.setAll(stations);
            filteredStations.setAll(stations);

            displayLoadingResults(stations);
        } catch (Exception e) {
            showMessage("Error loading stations: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    /**
     * Displays results of station loading operation.
     *
     * @param stations the loaded stations list
     */
    private void displayLoadingResults(List<Station> stations) {
        if (stations.isEmpty()) {
            showMessage("No stations found in the database.", "info");
        } else {
            showMessage("Loaded " + stations.size() + " stations successfully.", "success");
        }
    }

    /**
     * Updates station network statistics display.
     */
    private void updateStatistics() {
        try {
            StationStatistics stats = calculateStationStatistics();
            updateStatisticsDisplay(stats);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculates comprehensive station network statistics.
     *
     * @return StationStatistics with calculated values
     */
    private StationStatistics calculateStationStatistics() {
        int totalStations = stationsList.size();
        long uniqueStates = stationsList.stream()
                .map(Station::getState)
                .distinct()
                .count();
        long uniqueCities = stationsList.stream()
                .map(Station::getCity)
                .distinct()
                .count();

        return new StationStatistics(totalStations, uniqueStates, uniqueCities);
    }

    /**
     * Updates statistics display with calculated values.
     *
     * @param stats the calculated statistics
     */
    private void updateStatisticsDisplay(StationStatistics stats) {
        Platform.runLater(() -> {
            totalStationsLabel.setText(String.valueOf(stats.totalStations));
            statesCountLabel.setText(String.valueOf(stats.uniqueStates));
            citiesCountLabel.setText(String.valueOf(stats.uniqueCities));
        });
    }

    // -------------------------------------------------------------------------
    // Search and Filter Operations
    // -------------------------------------------------------------------------

    /**
     * Handles search functionality with current input.
     */
    @FXML
    public void handleSearch() {
        String searchText = searchField.getText().trim().toLowerCase();
        applyFilters(searchText);
    }

    /**
     * Clears all search and filter criteria.
     */
    @FXML
    public void handleClearSearch() {
        searchField.clear();
        stateFilterCombo.setValue("All States");
        cityFilterCombo.setValue("All Cities");
        filteredStations.setAll(stationsList);
        showMessage("Filters cleared.", "info");
    }

    /**
     * Applies filter criteria without search text.
     */
    @FXML
    public void handleApplyFilter() {
        String searchText = searchField.getText().trim().toLowerCase();
        applyFilters(searchText);
    }

    /**
     * Applies comprehensive filtering based on search text and combo box selections.
     *
     * @param searchText the text to search for across station attributes
     */
    private void applyFilters(String searchText) {
        String selectedState = stateFilterCombo.getValue();
        String selectedCity = cityFilterCombo.getValue();

        List<Station> filtered = stationsList.stream()
                .filter(station -> matchesAllCriteria(station, searchText, selectedState, selectedCity))
                .collect(Collectors.toList());

        filteredStations.setAll(filtered);
        showMessage("Found " + filtered.size() + " stations matching criteria.", "info");
    }

    /**
     * Checks if a station matches all filter criteria.
     *
     * @param station the station to check
     * @param searchText the search text
     * @param selectedState the selected state filter
     * @param selectedCity the selected city filter
     * @return true if station matches all criteria
     */
    private boolean matchesAllCriteria(Station station, String searchText, String selectedState, String selectedCity) {
        return matchesSearchCriteria(station, searchText) &&
                matchesStateFilter(station, selectedState) &&
                matchesCityFilter(station, selectedCity);
    }

    /**
     * Checks if station matches search text criteria.
     *
     * @param station the station to check
     * @param searchText the search text
     * @return true if station matches search criteria
     */
    private boolean matchesSearchCriteria(Station station, String searchText) {
        return searchText.isEmpty() ||
                station.getName().toLowerCase().contains(searchText) ||
                station.getStationCode().toLowerCase().contains(searchText) ||
                station.getCity().toLowerCase().contains(searchText) ||
                station.getState().toLowerCase().contains(searchText);
    }

    /**
     * Checks if station matches state filter.
     *
     * @param station the station to check
     * @param selectedState the selected state filter
     * @return true if station matches state filter
     */
    private boolean matchesStateFilter(Station station, String selectedState) {
        return "All States".equals(selectedState) || station.getState().equals(selectedState);
    }

    /**
     * Checks if station matches city filter.
     *
     * @param station the station to check
     * @param selectedCity the selected city filter
     * @return true if station matches city filter
     */
    private boolean matchesCityFilter(Station station, String selectedCity) {
        return "All Cities".equals(selectedCity) || station.getCity().equals(selectedCity);
    }

    // -------------------------------------------------------------------------
    // Station CRUD Operations
    // -------------------------------------------------------------------------

    /**
     * Handles adding a new station.
     */
    @FXML
    public void handleAddStation() {
        showStationDialog(null);
    }

    /**
     * Handles editing an existing station.
     *
     * @param station the station to edit
     */
    private void handleEditStation(Station station) {
        showStationDialog(station);
    }

    /**
     * Handles deleting a station with confirmation.
     *
     * @param station the station to delete
     */
    private void handleDeleteStation(Station station) {
        Alert confirmDialog = createDeleteConfirmationDialog(station);

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            processStationDeletion(station);
        }
    }

    /**
     * Creates delete confirmation dialog.
     *
     * @param station the station to be deleted
     * @return configured Alert dialog
     */
    private Alert createDeleteConfirmationDialog(Station station) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Station");
        confirmDialog.setHeaderText("Delete Station: " + station.getName());
        confirmDialog.setContentText("Are you sure you want to delete this station? This action cannot be undone.");
        return confirmDialog;
    }

    /**
     * Processes station deletion operation.
     *
     * @param station the station to delete
     */
    private void processStationDeletion(Station station) {
        try {
            if (stationDAO.deleteStation(station.getStationId())) {
                showMessage("Station deleted successfully", "success");
                refreshAllData();
            } else {
                showMessage("Failed to delete station", "error");
            }
        } catch (Exception e) {
            showMessage("Error deleting station: " + e.getMessage(), "error");
        }
    }

    // -------------------------------------------------------------------------
    // Station Dialog Management
    // -------------------------------------------------------------------------

    /**
     * Shows station add/edit dialog with form validation.
     *
     * @param station the station to edit, or null for new station
     */
    private void showStationDialog(Station station) {
        Dialog<Station> dialog = createStationDialog(station);

        GridPane form = createStationForm(station);
        dialog.getDialogPane().setContent(form);

        configureDialogButtons(dialog, form);
        setupDialogResultConverter(dialog);

        Optional<Station> result = dialog.showAndWait();
        result.ifPresent(this::saveStation);
    }

    /**
     * Creates the station dialog with appropriate title and header.
     *
     * @param station the station being edited, or null for new station
     * @return configured Dialog
     */
    private Dialog<Station> createStationDialog(Station station) {
        Dialog<Station> dialog = new Dialog<>();
        dialog.setTitle(station == null ? "Add New Station" : "Edit Station");
        dialog.setHeaderText(station == null ? "Enter station details" : "Update station details");
        return dialog;
    }

    /**
     * Creates the station form with input fields.
     *
     * @param station the station to pre-populate, or null for empty form
     * @return configured GridPane containing the form
     */
    private GridPane createStationForm(Station station) {
        TextField codeField = new TextField();
        TextField nameField = new TextField();
        TextField cityField = new TextField();
        TextField stateField = new TextField();

        // Pre-populate if editing
        if (station != null) {
            codeField.setText(station.getStationCode());
            nameField.setText(station.getName());
            cityField.setText(station.getCity());
            stateField.setText(station.getState());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        grid.add(new Label("Station Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Station Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("City:"), 0, 2);
        grid.add(cityField, 1, 2);
        grid.add(new Label("State:"), 0, 3);
        grid.add(stateField, 1, 3);

        // Store references for validation
        grid.setUserData(new TextField[]{codeField, nameField, cityField, stateField});

        return grid;
    }

    /**
     * Configures dialog buttons with validation.
     *
     * @param dialog the dialog to configure
     * @param form the form containing input fields
     */
    private void configureDialogButtons(Dialog<Station> dialog, GridPane form) {
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        setupFormValidation(saveButton, form);
    }

    /**
     * Sets up real-time form validation.
     *
     * @param saveButton the save button to enable/disable
     * @param form the form containing input fields
     */
    private void setupFormValidation(Button saveButton, GridPane form) {
        TextField[] fields = (TextField[]) form.getUserData();

        Runnable validateInput = () -> {
            boolean valid = true;
            for (TextField field : fields) {
                if (field.getText().trim().isEmpty()) {
                    valid = false;
                    break;
                }
            }
            saveButton.setDisable(!valid);
        };

        for (TextField field : fields) {
            field.textProperty().addListener((obs, oldVal, newVal) -> validateInput.run());
        }
    }

    /**
     * Sets up dialog result converter.
     *
     * @param dialog the dialog to configure
     */
    private void setupDialogResultConverter(Dialog<Station> dialog) {
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                GridPane form = (GridPane) dialog.getDialogPane().getContent();
                TextField[] fields = (TextField[]) form.getUserData();

                Station station = dialog.getTitle().contains("Edit") ?
                        getSelectedStation() : new Station();

                // FIX: Access individual TextField elements from the array
                station.setStationCode(fields[0].getText().trim().toUpperCase());  // codeField
                station.setName(fields[1].getText().trim());                       // nameField
                station.setCity(fields[2].getText().trim());                       // cityField
                station.setState(fields[3].getText().trim());                      // stateField

                return station;
            }
            return null;
        });
    }


    /**
     * Gets the currently selected station from the table.
     *
     * @return selected Station or new Station if none selected
     */
    private Station getSelectedStation() {
        Station selected = stationsTable.getSelectionModel().getSelectedItem();
        return selected != null ? selected : new Station();
    }

    /**
     * Saves station to database with appropriate add or update operation.
     *
     * @param station the station to save
     */
    private void saveStation(Station station) {
        try {
            boolean success;
            if (station.getStationId() == 0) {
                success = stationDAO.addStation(station);
                showMessage(success ? "Station added successfully" : "Failed to add station",
                        success ? "success" : "error");
            } else {
                success = stationDAO.updateStation(station);
                showMessage(success ? "Station updated successfully" : "Failed to update station",
                        success ? "success" : "error");
            }

            if (success) {
                refreshAllData();
            }
        } catch (Exception e) {
            showMessage("Error saving station: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Data Refresh and Window Management
    // -------------------------------------------------------------------------

    /**
     * Handles data refresh operation.
     */
    @FXML
    public void handleRefresh() {
        refreshAllData();
        showMessage("Data refreshed successfully", "success");
    }

    /**
     * Refreshes all data and updates displays.
     */
    private void refreshAllData() {
        loadStations();
        updateStatistics();
        loadFilterOptions();
    }

    /**
     * Closes the station network management window.
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
     * StationStatistics holds calculated statistical information for display.
     */
    private static class StationStatistics {
        final int totalStations;
        final long uniqueStates;
        final long uniqueCities;

        StationStatistics(int totalStations, long uniqueStates, long uniqueCities) {
            this.totalStations = totalStations;
            this.uniqueStates = uniqueStates;
            this.uniqueCities = uniqueCities;
        }
    }
}
