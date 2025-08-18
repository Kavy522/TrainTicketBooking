package trainapp.controller.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import trainapp.dao.TrainDAO;
import trainapp.dao.StationDAO;
import trainapp.dao.TrainScheduleDAO;
import trainapp.model.Train;
import trainapp.model.TrainSchedule;
import trainapp.model.Station;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * FleetManagementController manages comprehensive train fleet and route operations.
 * Provides CRUD operations for trains and intelligent route management with auto-sequencing.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Complete train fleet management with CRUD operations</li>
 *   <li>Intelligent route management with auto-sequence positioning</li>
 *   <li>Advanced searchable station selection with real-time filtering</li>
 *   <li>Dynamic sequence management preventing conflicts and gaps</li>
 *   <li>Real-time validation and duplicate prevention</li>
 *   <li>Asynchronous data loading with progress feedback</li>
 * </ul>
 *
 * <p>Advanced Route Management:
 * <ul>
 *   <li>Auto-sequence insertion with intelligent shift management</li>
 *   <li>Dynamic sequence spinner limits based on existing routes</li>
 *   <li>Station conflict detection and resolution</li>
 *   <li>Time-based scheduling with flexible arrival/departure times</li>
 *   <li>Multi-day journey support with day numbering</li>
 * </ul>
 *
 * <p>User Experience Features:
 * <ul>
 *   <li>Searchable combo boxes with multi-criteria filtering</li>
 *   <li>Real-time form validation with descriptive error messages</li>
 *   <li>Contextual button enabling/disabling based on selection state</li>
 *   <li>Auto-refresh statistics and status indicators</li>
 *   <li>Confirmation dialogs for destructive operations</li>
 * </ul>
 */
public class FleetManagementController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // Navigation Controls
    @FXML private Button backButton;

    // Train Management Controls
    @FXML private TextField trainNumberField;
    @FXML private TextField trainNameField;
    @FXML private ComboBox<Station> sourceStationCombo;
    @FXML private ComboBox<Station> destStationCombo;
    @FXML private Spinner<Integer> coachesSpinner;
    @FXML private TableView<Train> trainTableView;
    @FXML private TableColumn<Train, String> trainNumberCol;
    @FXML private TableColumn<Train, String> trainNameCol;
    @FXML private TableColumn<Train, String> sourceStationCol;
    @FXML private TableColumn<Train, String> destStationCol;
    @FXML private TableColumn<Train, Integer> totalCoachesCol;

    // Route Management Controls
    @FXML private ComboBox<Train> routeTrainCombo;
    @FXML private ComboBox<Station> routeStationCombo;
    @FXML private TextField arrivalTimeField;
    @FXML private TextField departureTimeField;
    @FXML private Spinner<Integer> daySpinner;
    @FXML private Spinner<Integer> sequenceSpinner;
    @FXML private Button saveRouteBtn;
    @FXML private Button updateRouteBtn;
    @FXML private Button deleteRouteBtn;
    @FXML private TableView<TrainSchedule> routeTableView;
    @FXML private TableColumn<TrainSchedule, String> routeTrainCol;
    @FXML private TableColumn<TrainSchedule, String> routeStationCol;
    @FXML private TableColumn<TrainSchedule, String> arrivalCol;
    @FXML private TableColumn<TrainSchedule, String> departureCol;
    @FXML private TableColumn<TrainSchedule, Integer> dayCol;
    @FXML private TableColumn<TrainSchedule, Integer> sequenceCol;

    // Status and Statistics
    @FXML private Label trainCountLabel;
    @FXML private Label routeCountLabel;
    @FXML private Label lastUpdateLabel;
    @FXML private Label messageLabel;

    // -------------------------------------------------------------------------
    // Data Access and State Management
    // -------------------------------------------------------------------------

    private TrainDAO trainDAO;
    private StationDAO stationDAO;
    private TrainScheduleDAO scheduleDAO;

    private Train selectedTrain;
    private TrainSchedule selectedRoute;
    private ObservableList<Train> allTrains = FXCollections.observableArrayList();
    private ObservableList<Station> allStations = FXCollections.observableArrayList();
    private ObservableList<TrainSchedule> allRoutes = FXCollections.observableArrayList();

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the fleet management interface with data loading and UI setup.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        initializeServices();
        setupTableColumns();
        setupEventHandlers();
        setupSpinners();
        loadAllData();
        configureTableResizing();
    }

    /**
     * Initializes data access services for fleet management operations.
     */
    private void initializeServices() {
        trainDAO = new TrainDAO();
        stationDAO = new StationDAO();
        scheduleDAO = new TrainScheduleDAO();
    }

    /**
     * Configures table column value factories and custom cell value providers.
     * Sets up data binding between model objects and table display.
     */
    private void setupTableColumns() {
        // Train table configuration
        trainNumberCol.setCellValueFactory(new PropertyValueFactory<>("trainNumber"));
        trainNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        sourceStationCol.setCellValueFactory(cellData -> {
            Station station = getStationById(cellData.getValue().getSourceStationId());
            return new SimpleStringProperty(station != null ? station.getName() : "Unknown");
        });
        destStationCol.setCellValueFactory(cellData -> {
            Station station = getStationById(cellData.getValue().getDestinationStationId());
            return new SimpleStringProperty(station != null ? station.getName() : "Unknown");
        });
        totalCoachesCol.setCellValueFactory(new PropertyValueFactory<>("totalCoaches"));

        // Route table configuration
        routeTrainCol.setCellValueFactory(cellData -> {
            Train train = getTrainById(cellData.getValue().getTrainId());
            return new SimpleStringProperty(train != null ? train.getTrainNumber() : "Unknown");
        });
        routeStationCol.setCellValueFactory(cellData -> {
            Station station = getStationById(cellData.getValue().getStationId());
            return new SimpleStringProperty(station != null ? station.getName() : "Unknown");
        });
        arrivalCol.setCellValueFactory(cellData -> {
            LocalTime time = cellData.getValue().getArrivalTime();
            return new SimpleStringProperty(time != null ? time.toString() : "-");
        });
        departureCol.setCellValueFactory(cellData -> {
            LocalTime time = cellData.getValue().getDepartureTime();
            return new SimpleStringProperty(time != null ? time.toString() : "-");
        });
        dayCol.setCellValueFactory(new PropertyValueFactory<>("dayNumber"));
        sequenceCol.setCellValueFactory(new PropertyValueFactory<>("sequenceOrder"));
    }

    /**
     * Sets up event handlers for table selections and combo box changes.
     * Manages form population and button state based on user selections.
     */
    private void setupEventHandlers() {
        trainTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedTrain = newVal;
            if (newVal != null) {
                populateTrainForm(newVal);
            }
        });

        routeTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedRoute = newVal;
            if (newVal != null) {
                populateRouteForm(newVal);
                deleteRouteBtn.setDisable(false);
                updateRouteBtn.setDisable(false);
            } else {
                deleteRouteBtn.setDisable(true);
                updateRouteBtn.setDisable(true);
            }
        });

        routeTrainCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                filterRoutesByTrain(newVal);
                enableRouteControls(true);
                updateSequenceSpinnerLimits();
            } else {
                routeTableView.setItems(allRoutes);
                enableRouteControls(false);
                updateSequenceSpinnerLimits();
            }
        });
    }

    /**
     * Configures spinner controls with appropriate value ranges and defaults.
     */
    private void setupSpinners() {
        coachesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 30, 20));
        daySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 7, 1));
        sequenceSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));
    }

    /**
     * Configures table column resizing policy for optimal display.
     */
    private void configureTableResizing() {
        Platform.runLater(() -> {
            trainTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            routeTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        });
    }

    // -------------------------------------------------------------------------
    // Data Loading and Management
    // -------------------------------------------------------------------------

    /**
     * Loads all station and train data asynchronously.
     * Updates UI components and sets up combo boxes after data loading completes.
     */
    private void loadAllData() {
        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<Station> stations = stationDAO.getAllStations();
                List<Train> trains = trainDAO.getAllTrains();

                Platform.runLater(() -> {
                    allStations.setAll(stations);
                    allTrains.setAll(trains);
                    setupComboBoxes();
                    trainTableView.setItems(allTrains);
                    loadRouteData();
                    updateStatus();
                });
                return null;
            }
        };
        new Thread(loadTask).start();
    }

    /**
     * Loads route data for all trains asynchronously.
     * Updates route table and sequence spinner limits after completion.
     */
    private void loadRouteData() {
        Task<Void> routeTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                ObservableList<TrainSchedule> routes = FXCollections.observableArrayList();
                for (Train train : allTrains) {
                    routes.addAll(trainDAO.getTrainSchedule(train.getTrainId()));
                }
                Platform.runLater(() -> {
                    allRoutes.setAll(routes);
                    routeTableView.setItems(allRoutes);
                    updateSequenceSpinnerLimits();
                });
                return null;
            }
        };
        new Thread(routeTask).start();
    }

    // -------------------------------------------------------------------------
    // Combo Box Setup and Search Functionality
    // -------------------------------------------------------------------------

    /**
     * Sets up all combo boxes with appropriate data and search functionality.
     */
    private void setupComboBoxes() {
        setupSearchableStationCombo(sourceStationCombo, "Type to search source station...");
        setupSearchableStationCombo(destStationCombo, "Type to search destination...");
        setupSearchableStationCombo(routeStationCombo, "Type to search station...");
        setupTrainCombo();
    }

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

        // String converter for proper display and parsing
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

        setupComboBoxSearchBehavior(comboBox);
    }

    /**
     * Sets up search behavior for station combo boxes including text filtering and keyboard handling.
     *
     * @param comboBox the combo box to configure search behavior for
     */
    private void setupComboBoxSearchBehavior(ComboBox<Station> comboBox) {
        TextField editor = comboBox.getEditor();

        // Real-time search filtering
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

        setupComboBoxFocusAndKeyboardHandling(comboBox, editor);
    }

    /**
     * Sets up focus and keyboard handling for searchable combo boxes.
     *
     * @param comboBox the combo box to configure
     * @param editor the text editor component of the combo box
     */
    private void setupComboBoxFocusAndKeyboardHandling(ComboBox<Station> comboBox, TextField editor) {
        // Focus handling for text selection and validation
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

        // Keyboard navigation and shortcuts
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
     * Configures the train combo box with proper display formatting.
     */
    private void setupTrainCombo() {
        routeTrainCombo.setItems(allTrains);
        routeTrainCombo.setCellFactory(param -> new ListCell<Train>() {
            @Override
            protected void updateItem(Train item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTrainNumber() + " - " + item.getName());
            }
        });

        routeTrainCombo.setButtonCell(new ListCell<Train>() {
            @Override
            protected void updateItem(Train item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTrainNumber() + " - " + item.getName());
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
    // Train CRUD Operations
    // -------------------------------------------------------------------------

    /**
     * Handles saving a new train to the database.
     * Validates form data and creates new train record.
     *
     * @param event ActionEvent from save train button
     */
    @FXML
    public void handleSaveTrain(ActionEvent event) {
        if (!validateTrainForm()) return;

        Train train = new Train();
        train.setTrainNumber(trainNumberField.getText().trim());
        train.setName(trainNameField.getText().trim());
        train.setSourceStationId(sourceStationCombo.getValue().getStationId());
        train.setDestinationStationId(destStationCombo.getValue().getStationId());
        train.setTotalCoaches(coachesSpinner.getValue());

        if (trainDAO.addTrain(train)) {
            showMessage("Train saved successfully!", "success");
            clearTrainForm();
            loadAllData();
        } else {
            showMessage("Failed to save train!", "error");
        }
    }

    /**
     * Handles updating an existing train record.
     * Validates form data and updates selected train.
     *
     * @param event ActionEvent from update train button
     */
    @FXML
    public void handleUpdateTrain(ActionEvent event) {
        if (selectedTrain == null || !validateTrainForm()) return;

        selectedTrain.setTrainNumber(trainNumberField.getText().trim());
        selectedTrain.setName(trainNameField.getText().trim());
        selectedTrain.setSourceStationId(sourceStationCombo.getValue().getStationId());
        selectedTrain.setDestinationStationId(destStationCombo.getValue().getStationId());
        selectedTrain.setTotalCoaches(coachesSpinner.getValue());

        if (trainDAO.updateTrain(selectedTrain)) {
            showMessage("Train updated successfully!", "success");
            clearTrainForm();
            loadAllData();
        } else {
            showMessage("Failed to update train!", "error");
        }
    }

    /**
     * Handles deleting a train with confirmation dialog.
     * Warns user about associated route deletion.
     *
     * @param event ActionEvent from delete train button
     */
    @FXML
    public void handleDeleteTrain(ActionEvent event) {
        if (selectedTrain == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Train");
        alert.setHeaderText("Delete " + selectedTrain.getTrainNumber() + "?");
        alert.setContentText("This will delete all associated routes!");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (trainDAO.deleteTrain(selectedTrain.getTrainId())) {
                showMessage("Train deleted successfully!", "success");
                clearTrainForm();
                loadAllData();
            } else {
                showMessage("Failed to delete train!", "error");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Route CRUD Operations with Auto-Sequencing
    // -------------------------------------------------------------------------

    /**
     * Handles saving a new route with intelligent auto-sequencing.
     * Automatically manages sequence positioning and station shifting.
     *
     * @param event ActionEvent from save route button
     */
    @FXML
    public void handleSaveRoute(ActionEvent event) {
        if (!validateRouteForm()) return;

        TrainSchedule schedule = createScheduleFromForm();
        if (schedule == null) return;

        Integer sequence = sequenceSpinner.getValue();
        if (sequence == null || sequence <= 0) {
            // Auto-assign to end of route
            handleAutoSequenceAppend(schedule);
        } else {
            // User-specified sequence with auto-adjustment
            handleUserSpecifiedSequence(schedule, sequence);
        }
    }

    /**
     * Creates a TrainSchedule object from current form values.
     *
     * @return configured TrainSchedule object or null if time parsing fails
     */
    private TrainSchedule createScheduleFromForm() {
        TrainSchedule schedule = new TrainSchedule();
        schedule.setTrainId(routeTrainCombo.getValue().getTrainId());
        schedule.setStationId(routeStationCombo.getValue().getStationId());
        schedule.setDayNumber(daySpinner.getValue());

        try {
            schedule.setArrivalTime(arrivalTimeField.getText().trim().isEmpty() ?
                    null : LocalTime.parse(arrivalTimeField.getText().trim()));
            schedule.setDepartureTime(departureTimeField.getText().trim().isEmpty() ?
                    null : LocalTime.parse(departureTimeField.getText().trim()));
            return schedule;
        } catch (Exception e) {
            showMessage("Invalid time format! Use HH:MM", "error");
            return null;
        }
    }

    /**
     * Handles auto-sequence append to end of route.
     *
     * @param schedule the schedule to save
     */
    private void handleAutoSequenceAppend(TrainSchedule schedule) {
        try {
            int maxSeq = scheduleDAO.getCurrentMaxSequence(schedule.getTrainId());
            schedule.setSequenceOrder(maxSeq + 1);
        } catch (Exception e) {
            schedule.setSequenceOrder(1);
        }

        if (scheduleDAO.addStationToJourney(schedule)) {
            showMessage("Route saved successfully!", "success");
            clearRouteForm();
            loadRouteData();
        } else {
            showMessage("Failed to save route!", "error");
        }
    }

    /**
     * Handles user-specified sequence with automatic adjustment.
     *
     * @param schedule the schedule to save
     * @param sequence the desired sequence position
     */
    private void handleUserSpecifiedSequence(TrainSchedule schedule, int sequence) {
        schedule.setSequenceOrder(sequence);

        if (scheduleDAO.addStationToJourneyWithSequenceAdjustment(schedule)) {
            showMessage("Route saved successfully! Existing stations shifted down.", "success");
            clearRouteForm();
            loadRouteData();
        } else {
            showMessage("Failed to save route!", "error");
        }
    }

    /**
     * Handles updating an existing route with sequence change management.
     *
     * @param event ActionEvent from update route button
     */
    @FXML
    public void handleUpdateRoute(ActionEvent event) {
        if (selectedRoute == null || !validateRouteForm()) return;

        int oldSequence = selectedRoute.getSequenceOrder();
        int newSequence = sequenceSpinner.getValue();

        updateSelectedRouteFromForm();

        if (oldSequence != newSequence) {
            handleSequenceChangeUpdate(oldSequence, newSequence);
        } else {
            handleSimpleRouteUpdate();
        }
    }

    /**
     * Updates the selected route object from current form values.
     */
    private void updateSelectedRouteFromForm() {
        selectedRoute.setTrainId(routeTrainCombo.getValue().getTrainId());
        selectedRoute.setStationId(routeStationCombo.getValue().getStationId());
        selectedRoute.setDayNumber(daySpinner.getValue());

        try {
            selectedRoute.setArrivalTime(arrivalTimeField.getText().trim().isEmpty() ?
                    null : LocalTime.parse(arrivalTimeField.getText().trim()));
            selectedRoute.setDepartureTime(departureTimeField.getText().trim().isEmpty() ?
                    null : LocalTime.parse(departureTimeField.getText().trim()));
        } catch (Exception e) {
            showMessage("Invalid time format!", "error");
        }
    }

    /**
     * Handles route update with sequence position change.
     *
     * @param oldSequence the original sequence position
     * @param newSequence the new sequence position
     */
    private void handleSequenceChangeUpdate(int oldSequence, int newSequence) {
        if (handleSequenceChange(selectedRoute, oldSequence, newSequence, selectedRoute.getTrainId())) {
            showMessage("Route updated successfully! Sequences adjusted.", "success");
            clearRouteForm();
            loadRouteData();
        } else {
            showMessage("Failed to update route sequence!", "error");
        }
    }

    /**
     * Handles simple route update without sequence change.
     */
    private void handleSimpleRouteUpdate() {
        selectedRoute.setSequenceOrder(sequenceSpinner.getValue());
        if (scheduleDAO.updateStationInJourney(selectedRoute)) {
            showMessage("Route updated successfully!", "success");
            clearRouteForm();
            loadRouteData();
        } else {
            showMessage("Failed to update route!", "error");
        }
    }

    /**
     * Handles complex sequence changes during route updates.
     * Uses delete-and-recreate strategy for sequence repositioning.
     *
     * @param schedule the schedule being updated
     * @param oldSeq the original sequence position
     * @param newSeq the new sequence position
     * @param trainId the train ID for the route
     * @return true if sequence change was successful
     */
    private boolean handleSequenceChange(TrainSchedule schedule, int oldSeq, int newSeq, int trainId) {
        try {
            if (!trainDAO.deleteJourney(schedule.getScheduleId())) {
                return false;
            }
            schedule.setSequenceOrder(newSeq);
            return scheduleDAO.addStationToJourneyWithSequenceAdjustment(schedule);
        } catch (Exception e) {
            System.err.println("Error handling sequence change: " + e.getMessage());
            return false;
        }
    }

    /**
     * Handles deleting a route with confirmation dialog.
     *
     * @param event ActionEvent from delete route button
     */
    @FXML
    public void handleDeleteRoute(ActionEvent event) {
        if (selectedRoute == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Route");
        alert.setHeaderText("Delete this route entry?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (trainDAO.deleteJourney(selectedRoute.getScheduleId())) {
                showMessage("Route deleted successfully!", "success");
                clearRouteForm();
                loadRouteData();
            } else {
                showMessage("Failed to delete route!", "error");
            }
        }
    }

    // -------------------------------------------------------------------------
    // UI Action Handlers
    // -------------------------------------------------------------------------

    /**
     * Handles back button click to close the fleet management window.
     *
     * @param event ActionEvent from back button
     */
    @FXML public void handleBack(ActionEvent event) {
        ((Stage) backButton.getScene().getWindow()).close();
    }

    /**
     * Handles refresh button click to reload all data.
     *
     * @param event ActionEvent from refresh button
     */
    @FXML public void handleRefresh(ActionEvent event) {
        loadAllData();
    }

    /**
     * Handles add train button click to clear form for new train entry.
     *
     * @param event ActionEvent from add train button
     */
    @FXML public void handleAddTrain(ActionEvent event) {
        clearTrainForm();
    }

    /**
     * Handles add route button click to clear form for new route entry.
     *
     * @param event ActionEvent from add route button
     */
    @FXML public void handleAddRoute(ActionEvent event) {
        clearRouteForm();
    }

    /**
     * Handles clear train form button click.
     *
     * @param event ActionEvent from clear train form button
     */
    @FXML public void handleClearTrainForm(ActionEvent event) {
        clearTrainForm();
    }

    /**
     * Handles clear route form button click.
     *
     * @param event ActionEvent from clear route form button
     */
    @FXML public void handleClearRouteForm(ActionEvent event) {
        clearRouteForm();
    }

    /**
     * Placeholder handler for train export functionality.
     *
     * @param event ActionEvent from export trains button
     */
    @FXML public void handleExportTrains(ActionEvent event) {
        showMessage("Export feature coming soon!", "");
    }

    /**
     * Placeholder handler for route export functionality.
     *
     * @param event ActionEvent from export routes button
     */
    @FXML public void handleExportRoutes(ActionEvent event) {
        showMessage("Export feature coming soon!", "");
    }

    // -------------------------------------------------------------------------
    // Form Validation
    // -------------------------------------------------------------------------

    /**
     * Validates train form data for completeness and logical consistency.
     *
     * @return true if form data is valid
     */
    private boolean validateTrainForm() {
        if (trainNumberField.getText().trim().isEmpty()) {
            showMessage("Train number is required!", "error");
            return false;
        }
        if (trainNameField.getText().trim().isEmpty()) {
            showMessage("Train name is required!", "error");
            return false;
        }
        if (sourceStationCombo.getValue() == null) {
            showMessage("Source station is required!", "error");
            return false;
        }
        if (destStationCombo.getValue() == null) {
            showMessage("Destination station is required!", "error");
            return false;
        }
        if (sourceStationCombo.getValue().equals(destStationCombo.getValue())) {
            showMessage("Source and destination cannot be the same!", "error");
            return false;
        }
        return true;
    }

    /**
     * Validates route form data including duplicate station checking.
     *
     * @return true if form data is valid
     */
    private boolean validateRouteForm() {
        if (routeTrainCombo.getValue() == null) {
            showMessage("Please select a train!", "error");
            return false;
        }
        if (routeStationCombo.getValue() == null) {
            showMessage("Please select a station!", "error");
            return false;
        }

        // Check for duplicate stations in the same train route
        int trainId = routeTrainCombo.getValue().getTrainId();
        int stationId = routeStationCombo.getValue().getStationId();

        if (scheduleDAO.stationExistsInRoute(trainId, stationId)) {
            // Allow if we're updating the same record
            if (selectedRoute == null || selectedRoute.getStationId() != stationId) {
                showMessage("This station already exists in this train's route!", "error");
                return false;
            }
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Form Management
    // -------------------------------------------------------------------------

    /**
     * Populates the train form with data from the selected train.
     *
     * @param train the train data to populate form with
     */
    private void populateTrainForm(Train train) {
        trainNumberField.setText(train.getTrainNumber());
        trainNameField.setText(train.getName());
        sourceStationCombo.setValue(getStationById(train.getSourceStationId()));
        destStationCombo.setValue(getStationById(train.getDestinationStationId()));
        coachesSpinner.getValueFactory().setValue(train.getTotalCoaches());
    }

    /**
     * Populates the route form with data from the selected route.
     * Updates sequence spinner limits before setting the value.
     *
     * @param schedule the route data to populate form with
     */
    private void populateRouteForm(TrainSchedule schedule) {
        routeTrainCombo.setValue(getTrainById(schedule.getTrainId()));
        routeStationCombo.setValue(getStationById(schedule.getStationId()));
        arrivalTimeField.setText(schedule.getArrivalTime() != null ? schedule.getArrivalTime().toString() : "");
        departureTimeField.setText(schedule.getDepartureTime() != null ? schedule.getDepartureTime().toString() : "");
        daySpinner.getValueFactory().setValue(schedule.getDayNumber());

        updateSequenceSpinnerLimits();
        sequenceSpinner.getValueFactory().setValue(schedule.getSequenceOrder());
    }

    /**
     * Clears the train form and resets selection state.
     */
    private void clearTrainForm() {
        trainNumberField.clear();
        trainNameField.clear();
        sourceStationCombo.setValue(null);
        destStationCombo.setValue(null);
        coachesSpinner.getValueFactory().setValue(20);
        selectedTrain = null;
    }

    /**
     * Clears the route form and updates sequence spinner limits.
     * Resets button states for route operations.
     */
    private void clearRouteForm() {
        routeStationCombo.setValue(null);
        arrivalTimeField.clear();
        departureTimeField.clear();
        daySpinner.getValueFactory().setValue(1);

        updateSequenceSpinnerLimits();

        selectedRoute = null;
        updateRouteBtn.setDisable(true);
        deleteRouteBtn.setDisable(true);
    }

    // -------------------------------------------------------------------------
    // Dynamic Sequence Management
    // -------------------------------------------------------------------------

    /**
     * Updates sequence spinner limits based on the selected train's current route structure.
     * Dynamically adjusts maximum allowed sequence to prevent gaps and conflicts.
     */
    private void updateSequenceSpinnerLimits() {
        if (routeTrainCombo.getValue() != null) {
            try {
                int maxSeq = scheduleDAO.getCurrentMaxSequence(routeTrainCombo.getValue().getTrainId());
                int maxAllowed = maxSeq + 1;

                sequenceSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxAllowed, maxAllowed)
                );

                System.out.println("Updated sequence limits: 1 to " + maxAllowed + " for train " +
                        routeTrainCombo.getValue().getTrainNumber());
            } catch (Exception e) {
                // Fallback for new trains with no routes
                sequenceSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1)
                );
            }
        } else {
            // No train selected - reset to default
            sequenceSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1)
            );
        }
    }

    // -------------------------------------------------------------------------
    // Utility Methods
    // -------------------------------------------------------------------------

    /**
     * Filters route table to show only routes for the selected train.
     *
     * @param train the train to filter routes for
     */
    private void filterRoutesByTrain(Train train) {
        ObservableList<TrainSchedule> filtered = FXCollections.observableArrayList();
        for (TrainSchedule route : allRoutes) {
            if (route.getTrainId() == train.getTrainId()) {
                filtered.add(route);
            }
        }
        routeTableView.setItems(filtered);
    }

    /**
     * Enables or disables route control inputs based on train selection.
     *
     * @param enable true to enable controls, false to disable
     */
    private void enableRouteControls(boolean enable) {
        routeStationCombo.setDisable(!enable);
        arrivalTimeField.setDisable(!enable);
        departureTimeField.setDisable(!enable);
        daySpinner.setDisable(!enable);
        sequenceSpinner.setDisable(!enable);
        saveRouteBtn.setDisable(!enable);
    }

    /**
     * Finds a station by ID from the loaded stations list.
     *
     * @param stationId the station ID to search for
     * @return Station object or null if not found
     */
    private Station getStationById(int stationId) {
        return allStations.stream().filter(s -> s.getStationId() == stationId).findFirst().orElse(null);
    }

    /**
     * Finds a train by ID from the loaded trains list.
     *
     * @param trainId the train ID to search for
     * @return Train object or null if not found
     */
    private Train getTrainById(int trainId) {
        return allTrains.stream().filter(t -> t.getTrainId() == trainId).findFirst().orElse(null);
    }

    /**
     * Updates status labels with current data counts and timestamp.
     */
    private void updateStatus() {
        trainCountLabel.setText(allTrains.size() + " trains");
        routeCountLabel.setText(allRoutes.size() + " routes");
        lastUpdateLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    /**
     * Displays a message with styling and auto-hide functionality.
     * Updates status after displaying the message.
     *
     * @param message the message text to display
     * @param type the message type for styling ("success", "error", or "")
     */
    private void showMessage(String message, String type) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.getStyleClass().removeAll("success", "error", "warning");
            if (!type.isEmpty()) {
                messageLabel.getStyleClass().add(type);
            }
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);

            PauseTransition hideDelay = new PauseTransition(Duration.seconds(3));
            hideDelay.setOnFinished(e -> {
                messageLabel.setVisible(false);
                messageLabel.setManaged(false);
            });
            hideDelay.play();
        }
        updateStatus();
    }
}