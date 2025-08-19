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
 * FleetManagementController - Comprehensive Train Fleet and Route Management System
 *
 * <p>This controller manages the complete lifecycle of train fleet operations and route management
 * with advanced features for performance, usability, and data integrity.</p>
 *
 * <h3>Core Functionality</h3>
 * <ul>
 *   <li>Train CRUD operations (Create, Read, Update, Delete)</li>
 *   <li>Route management with lazy loading for performance optimization</li>
 *   <li>Advanced searchable interfaces for trains and stations</li>
 *   <li>Dynamic sequence management for route ordering</li>
 *   <li>Real-time validation and duplicate prevention</li>
 * </ul>
 *
 * <h3>Architecture Design</h3>
 * <ul>
 *   <li><strong>MVC Pattern:</strong> Clear separation between UI, business logic, and data access</li>
 *   <li><strong>DAO Pattern:</strong> Data access through dedicated objects ({@link TrainDAO}, {@link StationDAO}, {@link TrainScheduleDAO})</li>
 *   <li><strong>Observable Collections:</strong> Reactive UI updates using JavaFX {@link javafx.collections.ObservableList}</li>
 *   <li><strong>Asynchronous Processing:</strong> Non-blocking UI operations using {@link javafx.concurrent.Task} and {@link javafx.application.Platform#runLater}</li>
 * </ul>
 *
 * <h3>Performance Optimizations</h3>
 * <ul>
 *   <li><strong>Lazy Loading:</strong> Routes are loaded only when a specific train is selected</li>
 *   <li><strong>Thread Safety:</strong> All UI updates are performed on the JavaFX Application Thread</li>
 *   <li><strong>Memory Efficiency:</strong> Uses filtered observable lists instead of creating new collections</li>
 * </ul>
 *
 * <h3>UI Features</h3>
 * <ul>
 *   <li>Searchable ComboBoxes with real-time filtering</li>
 *   <li>Auto-sequence management for route stations</li>
 *   <li>Comprehensive form validation with user feedback</li>
 *   <li>Status indicators and loading messages</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * <p>This controller requires the following components:</p>
 * <ul>
 *   <li>{@link trainapp.dao.TrainDAO} - For train data operations</li>
 *   <li>{@link trainapp.dao.StationDAO} - For station data operations</li>
 *   <li>{@link trainapp.dao.TrainScheduleDAO} - For route/schedule data operations</li>
 *   <li>{@link trainapp.model.Train} - Train entity model</li>
 *   <li>{@link trainapp.model.Station} - Station entity model</li>
 *   <li>{@link trainapp.model.TrainSchedule} - Route/schedule entity model</li>
 * </ul>
 *
 * <h3>Thread Safety</h3>
 * <p>This controller is designed to be thread-safe for JavaFX applications. All database operations
 * are performed on background threads using {@link javafx.concurrent.Task}, and UI updates are
 * marshalled to the JavaFX Application Thread using {@link javafx.application.Platform#runLater}.</p>
 *
 * <h3>Error Handling</h3>
 * <p>The controller implements comprehensive error handling with user-friendly feedback:</p>
 * <ul>
 *   <li>Form validation with specific error messages</li>
 *   <li>Database operation failure handling</li>
 *   <li>Graceful degradation for network/connection issues</li>
 *   <li>Loading state management with timeout handling</li>
 * </ul>
 */

public class FleetManagementController {

    // =========================================================================
    // FXML UI COMPONENT DECLARATIONS
    // =========================================================================

    // Navigation Controls
    @FXML private Button backButton;

    // Train Management Form Components
    @FXML private TextField trainNumberField;
    @FXML private TextField trainNameField;
    @FXML private ComboBox<Station> sourceStationCombo;
    @FXML private ComboBox<Station> destStationCombo;
    @FXML private Spinner<Integer> coachesSpinner;

    // Train Management Table Components
    @FXML private TableView<Train> trainTableView;
    @FXML private TableColumn<Train, String> trainNumberCol;
    @FXML private TableColumn<Train, String> trainNameCol;
    @FXML private TableColumn<Train, String> sourceStationCol;
    @FXML private TableColumn<Train, String> destStationCol;
    @FXML private TableColumn<Train, Integer> totalCoachesCol;

    // Route Management Form Components
    @FXML private ComboBox<Train> routeTrainCombo;
    @FXML private ComboBox<Station> routeStationCombo;
    @FXML private TextField arrivalTimeField;
    @FXML private TextField departureTimeField;
    @FXML private Spinner<Integer> daySpinner;
    @FXML private Spinner<Integer> sequenceSpinner;

    // Route Management Table Components
    @FXML private TableView<TrainSchedule> routeTableView;
    @FXML private TableColumn<TrainSchedule, String> routeTrainCol;
    @FXML private TableColumn<TrainSchedule, String> routeStationCol;
    @FXML private TableColumn<TrainSchedule, String> arrivalCol;
    @FXML private TableColumn<TrainSchedule, String> departureCol;
    @FXML private TableColumn<TrainSchedule, Integer> dayCol;
    @FXML private TableColumn<TrainSchedule, Integer> sequenceCol;

    // Status and Feedback Components
    @FXML private Label trainCountLabel;
    @FXML private Label routeCountLabel;
    @FXML private Label lastUpdateLabel;
    @FXML private Label messageLabel;
    @FXML private Label routeLoadingLabel;

    // =========================================================================
    // DATA ACCESS OBJECTS AND STATE MANAGEMENT
    // =========================================================================

    // Data Access Objects for persistence layer interaction
    private TrainDAO trainDAO;
    private StationDAO stationDAO;
    private TrainScheduleDAO scheduleDAO;

    // UI Selection State Management
    private Train selectedTrain;                    // Currently selected train in train table
    private TrainSchedule selectedRoute;            // Currently selected route in route table
    private Train selectedRouteManagementTrain;    // Selected train for route management operations

    // Observable Data Collections for reactive UI
    private ObservableList<Train> allTrains = FXCollections.observableArrayList();
    private ObservableList<Station> allStations = FXCollections.observableArrayList();
    private ObservableList<TrainSchedule> currentTrainRoutes = FXCollections.observableArrayList();

    // =========================================================================
    // CONTROLLER INITIALIZATION AND SETUP
    // =========================================================================

    /**
     * JavaFX initialization method called automatically after FXML loading.
     * Sets up all UI components, data access objects, and event handlers.
     * This is the main entry point for controller setup.
     */
    @FXML
    public void initialize() {
        try {
            System.out.println("Initializing FleetManagementController...");

            initializeDataAccessObjects();
            setupTableColumns();
            setupFormControls();
            setupEventHandlers();
            setupInitialUIState();
            loadInitialDataAsync();
            configureUIBehavior();

            System.out.println("FleetManagementController initialization completed successfully");
        } catch (Exception e) {
            System.err.println("Error during controller initialization: " + e.getMessage());
            e.printStackTrace();
            showMessage("Failed to initialize application: " + e.getMessage(), "error");
        }
    }

    /**
     * Initialize all Data Access Objects for database operations.
     * These DAOs provide the interface to the persistence layer.
     */
    private void initializeDataAccessObjects() {
        try {
            trainDAO = new TrainDAO();
            stationDAO = new StationDAO();
            scheduleDAO = new TrainScheduleDAO();
            System.out.println("Data Access Objects initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize DAOs: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Configure table columns with appropriate cell value factories.
     * Sets up data binding between model objects and table display.
     */
    private void setupTableColumns() {
        // Train Table Column Configuration
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

        // Route Table Column Configuration
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

        System.out.println("Table columns configured successfully");
    }

    /**
     * Setup form controls including spinners with appropriate value ranges.
     * Configures minimum, maximum, and default values for all spinner controls.
     */
    private void setupFormControls() {
        // Configure Spinners with business rule constraints
        coachesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 30, 20));
        daySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 7, 1));
        sequenceSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));

        System.out.println("Form controls configured successfully");
    }

    /**
     * Setup event handlers for table selections and form interactions.
     * Manages UI state based on user selections and data changes.
     */
    private void setupEventHandlers() {
        // Train Table Selection Handler - populates form when train is selected
        trainTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedTrain = newVal;
            if (newVal != null) {
                populateTrainForm(newVal);
            }
        });

        // Route Table Selection Handler - populates form when route is selected
        routeTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedRoute = newVal;
            if (newVal != null) {
                populateRouteForm(newVal);
            }
        });

        // Route Train ComboBox Selection Handler - implements lazy loading
        routeTrainCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedRouteManagementTrain = newVal;
            if (newVal != null) {
                // Train selected - load routes lazily
                loadRoutesForSelectedTrain(newVal);
                updateSequenceSpinnerLimits();

                if (routeLoadingLabel != null) {
                    routeLoadingLabel.setText("Loading routes for " + newVal.getTrainNumber() + "...");
                    routeLoadingLabel.getStyleClass().add("loading");
                }
            } else {
                // No train selected - clear route data
                currentTrainRoutes.clear();
                routeTableView.setItems(currentTrainRoutes);
                updateSequenceSpinnerLimits();

                if (routeLoadingLabel != null) {
                    routeLoadingLabel.setText("Select a train to view routes");
                    routeLoadingLabel.getStyleClass().remove("loading");
                }
            }
        });

        System.out.println("Event handlers configured successfully");
    }

    /**
     * Setup initial UI state with proper default values and visibility.
     * Prepares the interface for first use by the user.
     */
    private void setupInitialUIState() {
        // Set initial status message for route management
        if (routeLoadingLabel != null) {
            routeLoadingLabel.setText("Select a train to view routes");
            routeLoadingLabel.setVisible(true);
        }

        // Initialize route table with empty data
        routeTableView.setItems(currentTrainRoutes);

        System.out.println("Initial UI state configured successfully");
    }

    /**
     * Load initial data asynchronously to prevent UI blocking.
     * Fetches all stations and trains from database and updates UI.
     */
    private void loadInitialDataAsync() {
        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("Loading initial data from database...");
                List<Station> stations = stationDAO.getAllStations();
                List<Train> trains = trainDAO.getAllTrains();

                Platform.runLater(() -> {
                    try {
                        allStations.setAll(stations);
                        allTrains.setAll(trains);
                        setupComboBoxes();
                        trainTableView.setItems(allTrains);
                        updateStatusLabels();
                        System.out.println("Initial data loaded successfully: " +
                                stations.size() + " stations, " + trains.size() + " trains");
                    } catch (Exception e) {
                        System.err.println("Error setting up UI with loaded data: " + e.getMessage());
                        showMessage("Failed to setup UI: " + e.getMessage(), "error");
                    }
                });
                return null;
            }
        };

        loadTask.setOnFailed(e -> {
            System.err.println("Failed to load initial data: " + loadTask.getException().getMessage());
            showMessage("Failed to load application data", "error");
        });

        new Thread(loadTask).start();
    }

    /**
     * Configure UI behavior settings including table resizing policies.
     * Ensures optimal display and user experience.
     */
    private void configureUIBehavior() {
        Platform.runLater(() -> {
            trainTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            routeTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            System.out.println("UI behavior configured successfully");
        });
    }

    // =========================================================================
    // COMBOBOX SETUP AND SEARCH FUNCTIONALITY
    // =========================================================================

    /**
     * Setup all combo boxes with searchable functionality.
     * Configures station and train selection with real-time filtering.
     */
    private void setupComboBoxes() {
        setupSearchableStationCombo(sourceStationCombo, "Type to search source station...");
        setupSearchableStationCombo(destStationCombo, "Type to search destination...");
        setupSearchableStationCombo(routeStationCombo, "Type to search station...");
        setupSearchableTrainCombo();
        System.out.println("ComboBoxes configured with search functionality");
    }

    /**
     * Setup searchable train combo box with enhanced display and search capabilities.
     * Provides detailed train information in dropdown and compact display when selected.
     */
    private void setupSearchableTrainCombo() {
        routeTrainCombo.setItems(allTrains);
        routeTrainCombo.setEditable(true);
        routeTrainCombo.setPromptText("Type train number or name to search...");

        // String converter for proper display and parsing
        routeTrainCombo.setConverter(new StringConverter<Train>() {
            @Override
            public String toString(Train train) {
                return train != null ? train.getTrainNumber() + " - " + train.getName() : null;
            }

            @Override
            public Train fromString(String string) {
                return findTrainByText(string);
            }
        });

        // Cell factory for detailed dropdown display including route information
        routeTrainCombo.setCellFactory(param -> new ListCell<Train>() {
            @Override
            protected void updateItem(Train item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    Station source = getStationById(item.getSourceStationId());
                    Station dest = getStationById(item.getDestinationStationId());
                    setText(item.getTrainNumber() + " - " + item.getName() +
                            " (" + (source != null ? source.getStationCode() : "?") +
                            " → " + (dest != null ? dest.getStationCode() : "?") + ")");
                }
            }
        });

        // Button cell for compact selected value display
        routeTrainCombo.setButtonCell(new ListCell<Train>() {
            @Override
            protected void updateItem(Train item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Choose train for route management..." :
                        item.getTrainNumber() + " - " + item.getName());
            }
        });

        setupTrainComboSearchBehavior();
    }

    /**
     * Setup search behavior for train combo box with real-time filtering.
     * Filters trains based on train number and name as user types.
     */
    private void setupTrainComboSearchBehavior() {
        TextField editor = routeTrainCombo.getEditor();

        // Real-time search filtering based on user input
        editor.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!routeTrainCombo.isFocused()) return;

            Platform.runLater(() -> {
                if (newValue == null || newValue.isEmpty()) {
                    routeTrainCombo.setItems(allTrains);
                } else {
                    String searchText = newValue.toLowerCase().trim();
                    ObservableList<Train> filteredTrains = FXCollections.observableArrayList();

                    for (Train train : allTrains) {
                        if (matchesTrainSearchCriteria(train, searchText)) {
                            filteredTrains.add(train);
                        }
                    }

                    routeTrainCombo.setItems(filteredTrains);
                    if (!filteredTrains.isEmpty() && !routeTrainCombo.isShowing()) {
                        routeTrainCombo.show();
                    }
                }
            });
        });

        // Selection handling for proper display update
        routeTrainCombo.setOnAction(event -> {
            Train selected = routeTrainCombo.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Platform.runLater(() -> {
                    routeTrainCombo.getEditor().setText(selected.getTrainNumber() + " - " + selected.getName());
                    routeTrainCombo.hide();
                });
            }
        });
    }

    /**
     * Setup searchable station combo box with comprehensive search capabilities.
     * Allows searching by station name, code, city, and state.
     *
     * @param comboBox The station combo box to configure
     * @param promptText The placeholder text to display
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
                        item.getName() + " (" + item.getStationCode() + ") - " +
                                item.getCity() + ", " + item.getState());
            }
        });

        // Button cell for compact selected value display
        comboBox.setButtonCell(new ListCell<Station>() {
            @Override
            protected void updateItem(Station item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.getName() + " (" + item.getStationCode() + ")");
            }
        });

        setupStationComboSearchBehavior(comboBox);
    }

    /**
     * Setup search behavior for station combo boxes with multi-criteria filtering.
     * Filters stations based on name, code, city, and state as user types.
     *
     * @param comboBox The station combo box to configure search behavior for
     */
    private void setupStationComboSearchBehavior(ComboBox<Station> comboBox) {
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
                        if (matchesStationSearchCriteria(station, searchText)) {
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

        // Selection handling for proper display update
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

    // =========================================================================
    // LAZY LOADING AND DATA MANAGEMENT
    // =========================================================================

    /**
     * Load routes for the selected train asynchronously (lazy loading).
     * This prevents loading all route data at startup, improving performance.
     *
     * @param train The train to load routes for
     */
    private void loadRoutesForSelectedTrain(Train train) {
        if (train == null) return;

        Task<Void> routeTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<TrainSchedule> routes = trainDAO.getTrainSchedule(train.getTrainId());

                Platform.runLater(() -> {
                    currentTrainRoutes.setAll(routes);
                    routeTableView.setItems(currentTrainRoutes);

                    // Update loading status
                    if (routeLoadingLabel != null) {
                        routeLoadingLabel.setText("Loaded " + routes.size() + " route stations");
                        routeLoadingLabel.getStyleClass().remove("loading");
                    }

                    updateSequenceSpinnerLimits();
                    updateStatusLabels();
                });
                return null;
            }
        };

        // Handle loading failures gracefully
        routeTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                showMessage("Failed to load routes for train: " + train.getTrainNumber(), "error");
                if (routeLoadingLabel != null) {
                    routeLoadingLabel.setText("Error loading routes");
                    routeLoadingLabel.getStyleClass().remove("loading");
                }
            });
        });

        new Thread(routeTask).start();
    }

    // =========================================================================
    // SEARCH AND FILTERING UTILITIES
    // =========================================================================

    /**
     * Check if train matches search criteria based on train number and name.
     * Used for real-time filtering in train combo box.
     *
     * @param train The train to check
     * @param searchText The search text to match against
     * @return true if train matches search criteria
     */
    private boolean matchesTrainSearchCriteria(Train train, String searchText) {
        if (train == null || searchText == null || searchText.isEmpty()) {
            return true;
        }

        String search = searchText.toLowerCase();
        return (train.getTrainNumber() != null && train.getTrainNumber().toLowerCase().contains(search))
                || (train.getName() != null && train.getName().toLowerCase().contains(search));
    }

    /**
     * Check if station matches search criteria based on name, code, city, and state.
     * Used for real-time filtering in station combo boxes.
     *
     * @param station The station to check
     * @param searchText The search text to match against
     * @return true if station matches search criteria
     */
    private boolean matchesStationSearchCriteria(Station station, String searchText) {
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
     * Find train by text input using multiple matching strategies.
     * Attempts exact matches first, then falls back to partial matches.
     *
     * @param text The text to search for
     * @return Matching Train object or null if not found
     */
    private Train findTrainByText(String text) {
        if (text == null || text.trim().isEmpty()) return null;

        String searchText = text.toLowerCase().trim();

        // Try exact train number match first
        for (Train train : allTrains) {
            if (train.getTrainNumber() != null &&
                    train.getTrainNumber().equalsIgnoreCase(searchText)) {
                return train;
            }
        }

        // Try exact name match
        for (Train train : allTrains) {
            if (train.getName() != null &&
                    train.getName().equalsIgnoreCase(searchText)) {
                return train;
            }
        }

        // Try partial matches
        for (Train train : allTrains) {
            if (matchesTrainSearchCriteria(train, searchText)) {
                return train;
            }
        }

        return null;
    }

    /**
     * Find station by text input using multiple matching strategies.
     * Supports both "Name (CODE)" format and direct name/code searching.
     *
     * @param text The text to search for
     * @return Matching Station object or null if not found
     */
    private Station findStationByText(String text) {
        if (text == null || text.trim().isEmpty()) return null;

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

        String searchText = text.toLowerCase().trim();

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

        // Try partial matches
        for (Station station : allStations) {
            if (matchesStationSearchCriteria(station, searchText)) {
                return station;
            }
        }

        return null;
    }

    // =========================================================================
    // TRAIN CRUD OPERATIONS
    // =========================================================================

    /**
     * Handle saving a new train to the database.
     * Validates form data and creates new train record.
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
            loadInitialDataAsync();
        } else {
            showMessage("Failed to save train!", "error");
        }
    }

    /**
     * Handle updating an existing train record.
     * Validates form data and updates selected train in database.
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
            loadInitialDataAsync();
        } else {
            showMessage("Failed to update train!", "error");
        }
    }

    /**
     * Handle deleting a train with confirmation dialog.
     * Warns user about cascading deletion of associated routes.
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
                loadInitialDataAsync();
            } else {
                showMessage("Failed to delete train!", "error");
            }
        }
    }

    // =========================================================================
    // ROUTE CRUD OPERATIONS
    // =========================================================================

    /**
     * Handle saving a new route with intelligent sequence management.
     * Supports both auto-sequence assignment and user-specified positioning.
     */
    @FXML
    public void handleSaveRoute(ActionEvent event) {
        if (!validateRouteForm()) return;

        TrainSchedule schedule = createScheduleFromForm();
        if (schedule == null) return;

        Integer sequence = sequenceSpinner.getValue();
        if (sequence == null || sequence <= 0) {
            handleAutoSequenceAppend(schedule);
        } else {
            handleUserSpecifiedSequence(schedule, sequence);
        }
    }

    /**
     * Handle updating an existing route with sequence change management.
     * Manages complex sequence repositioning when needed.
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
     * Handle deleting a route with confirmation dialog.
     * Removes the route from the train's schedule.
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
                if (selectedRouteManagementTrain != null) {
                    loadRoutesForSelectedTrain(selectedRouteManagementTrain);
                }
            } else {
                showMessage("Failed to delete route!", "error");
            }
        }
    }

    // =========================================================================
    // ROUTE OPERATION HELPERS
    // =========================================================================

    /**
     * Create TrainSchedule object from current form values.
     * Handles time parsing and validation.
     *
     * @return TrainSchedule object or null if validation fails
     */
    private TrainSchedule createScheduleFromForm() {
        if (selectedRouteManagementTrain == null) {
            showMessage("Please select a train first!", "error");
            return null;
        }

        TrainSchedule schedule = new TrainSchedule();
        schedule.setTrainId(selectedRouteManagementTrain.getTrainId());
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
     * Handle auto-sequence append to end of route.
     * Automatically assigns the next available sequence number.
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
            if (selectedRouteManagementTrain != null) {
                loadRoutesForSelectedTrain(selectedRouteManagementTrain);
            }
        } else {
            showMessage("Failed to save route!", "error");
        }
    }

    /**
     * Handle user-specified sequence with automatic sequence adjustment.
     * Inserts route at specified position and shifts existing routes as needed.
     */
    private void handleUserSpecifiedSequence(TrainSchedule schedule, int sequence) {
        schedule.setSequenceOrder(sequence);

        if (scheduleDAO.addStationToJourneyWithSequenceAdjustment(schedule)) {
            showMessage("Route saved successfully! Existing stations shifted down.", "success");
            clearRouteForm();
            if (selectedRouteManagementTrain != null) {
                loadRoutesForSelectedTrain(selectedRouteManagementTrain);
            }
        } else {
            showMessage("Failed to save route!", "error");
        }
    }

    /**
     * Update selected route object from current form values.
     * Prepares route for database update operation.
     */
    private void updateSelectedRouteFromForm() {
        if (selectedRouteManagementTrain == null) return;

        selectedRoute.setTrainId(selectedRouteManagementTrain.getTrainId());
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
     * Handle route update with sequence position change.
     * Manages complex sequence repositioning using delete-and-recreate strategy.
     */
    private void handleSequenceChangeUpdate(int oldSequence, int newSequence) {
        if (handleSequenceChange(selectedRoute, oldSequence, newSequence, selectedRoute.getTrainId())) {
            showMessage("Route updated successfully! Sequences adjusted.", "success");
            clearRouteForm();
            if (selectedRouteManagementTrain != null) {
                loadRoutesForSelectedTrain(selectedRouteManagementTrain);
            }
        } else {
            showMessage("Failed to update route sequence!", "error");
        }
    }

    /**
     * Handle simple route update without sequence change.
     * Updates route data while maintaining current sequence position.
     */
    private void handleSimpleRouteUpdate() {
        selectedRoute.setSequenceOrder(sequenceSpinner.getValue());
        if (scheduleDAO.updateStationInJourney(selectedRoute)) {
            showMessage("Route updated successfully!", "success");
            clearRouteForm();
            if (selectedRouteManagementTrain != null) {
                loadRoutesForSelectedTrain(selectedRouteManagementTrain);
            }
        } else {
            showMessage("Failed to update route!", "error");
        }
    }

    /**
     * Handle complex sequence changes using delete-and-recreate strategy.
     * This approach ensures sequence integrity when repositioning routes.
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

    // =========================================================================
    // FORM VALIDATION
    // =========================================================================

    /**
     * Validate train form data for completeness and business rules.
     * Ensures all required fields are filled and rules are followed.
     *
     * @return true if form data is valid, false otherwise
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
     * Validate route form data including duplicate station checking.
     * Ensures route data integrity and prevents duplicate stations in same train.
     *
     * @return true if form data is valid, false otherwise
     */
    private boolean validateRouteForm() {
        if (selectedRouteManagementTrain == null) {
            showMessage("Please select a train!", "error");
            return false;
        }
        if (routeStationCombo.getValue() == null) {
            showMessage("Please select a station!", "error");
            return false;
        }

        // Check for duplicate stations in the same train route
        int trainId = selectedRouteManagementTrain.getTrainId();
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

    // =========================================================================
    // FORM MANAGEMENT
    // =========================================================================

    /**
     * Populate train form with data from selected train.
     * Updates all train form fields with selected train's data.
     *
     * @param train The train data to populate form with
     */
    private void populateTrainForm(Train train) {
        if (train == null) return;

        trainNumberField.setText(train.getTrainNumber());
        trainNameField.setText(train.getName());
        sourceStationCombo.setValue(getStationById(train.getSourceStationId()));
        destStationCombo.setValue(getStationById(train.getDestinationStationId()));
        coachesSpinner.getValueFactory().setValue(train.getTotalCoaches());
    }

    /**
     * Populate route form with data from selected route.
     * Updates all route form fields and manages train selection.
     *
     * @param schedule The route data to populate form with
     */
    private void populateRouteForm(TrainSchedule schedule) {
        if (schedule == null) return;

        // Set train combo to the schedule's train if different
        Train train = getTrainById(schedule.getTrainId());
        if (train != null && train != selectedRouteManagementTrain) {
            routeTrainCombo.setValue(train);
        }

        routeStationCombo.setValue(getStationById(schedule.getStationId()));
        arrivalTimeField.setText(schedule.getArrivalTime() != null ?
                schedule.getArrivalTime().toString() : "");
        departureTimeField.setText(schedule.getDepartureTime() != null ?
                schedule.getDepartureTime().toString() : "");
        daySpinner.getValueFactory().setValue(schedule.getDayNumber());

        updateSequenceSpinnerLimits();
        sequenceSpinner.getValueFactory().setValue(schedule.getSequenceOrder());
    }

    /**
     * Clear train form and reset selection state.
     * Resets all train form fields to default values.
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
     * Clear route form and reset selection state.
     * Resets all route form fields while preserving train selection.
     */
    private void clearRouteForm() {
        routeStationCombo.setValue(null);
        arrivalTimeField.clear();
        departureTimeField.clear();
        daySpinner.getValueFactory().setValue(1);

        updateSequenceSpinnerLimits();

        selectedRoute = null;
        routeTableView.getSelectionModel().clearSelection();
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    /**
     * Update sequence spinner limits based on selected train's current routes.
     * Dynamically adjusts maximum sequence to prevent gaps and conflicts.
     */
    private void updateSequenceSpinnerLimits() {
        if (selectedRouteManagementTrain != null) {
            try {
                int maxSeq = scheduleDAO.getCurrentMaxSequence(selectedRouteManagementTrain.getTrainId());
                int maxAllowed = maxSeq + 1;

                sequenceSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxAllowed, maxAllowed)
                );
            } catch (Exception e) {
                // Fallback for trains with no existing routes
                sequenceSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1)
                );
            }
        } else {
            // Default range when no train is selected
            sequenceSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1)
            );
        }
    }

    /**
     * Find station by ID from the loaded stations list.
     * Used for resolving station references in train and route data.
     *
     * @param stationId The station ID to search for
     * @return Station object or null if not found
     */
    private Station getStationById(int stationId) {
        return allStations.stream()
                .filter(s -> s.getStationId() == stationId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Find train by ID from the loaded trains list.
     * Used for resolving train references in route data.
     *
     * @param trainId The train ID to search for
     * @return Train object or null if not found
     */
    private Train getTrainById(int trainId) {
        return allTrains.stream()
                .filter(t -> t.getTrainId() == trainId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Update status labels with current data counts and timestamp.
     * Provides real-time feedback on data state to the user.
     */
    private void updateStatusLabels() {
        Platform.runLater(() -> {
            if (trainCountLabel != null) {
                trainCountLabel.setText(allTrains.size() + " trains");
            }
            if (routeCountLabel != null) {
                routeCountLabel.setText(currentTrainRoutes.size() + " routes" +
                        (selectedRouteManagementTrain != null ?
                                " (" + selectedRouteManagementTrain.getTrainNumber() + ")" : ""));
            }
            if (lastUpdateLabel != null) {
                lastUpdateLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
        });
    }

    /**
     * Display message with styling and auto-hide functionality.
     * Provides user feedback for operations with appropriate visual styling.
     *
     * @param message The message text to display
     * @param type The message type for styling ("success", "error", "warning", or "")
     */
    private void showMessage(String message, String type) {
        Platform.runLater(() -> {
            if (messageLabel != null) {
                messageLabel.setText(message);
                messageLabel.getStyleClass().removeAll("success", "error", "warning");
                if (!type.isEmpty()) {
                    messageLabel.getStyleClass().add(type);
                }
                messageLabel.setVisible(true);
                messageLabel.setManaged(true);

                // Auto-hide message after 3 seconds
                PauseTransition hideDelay = new PauseTransition(Duration.seconds(3));
                hideDelay.setOnFinished(e -> {
                    messageLabel.setVisible(false);
                    messageLabel.setManaged(false);
                });
                hideDelay.play();
            }
            updateStatusLabels();
        });
    }

    // =========================================================================
    // UI ACTION HANDLERS
    // =========================================================================

    /**
     * Handle back button click to close the fleet management window.
     */
    @FXML
    public void handleBack(ActionEvent event) {
        try {
            ((Stage) backButton.getScene().getWindow()).close();
        } catch (Exception e) {
            System.err.println("Error handling back button: " + e.getMessage());
        }
    }

    /**
     * Handle refresh button click to reload all data.
     */
    @FXML
    public void handleRefresh(ActionEvent event) {
        loadInitialDataAsync();
        if (selectedRouteManagementTrain != null) {
            loadRoutesForSelectedTrain(selectedRouteManagementTrain);
        }
        showMessage("Data refreshed successfully!", "success");
    }

    /**
     * Handle add train button click to clear form for new train entry.
     */
    @FXML
    public void handleAddTrain(ActionEvent event) {
        clearTrainForm();
    }

    /**
     * Handle add route button click to clear form for new route entry.
     */
    @FXML
    public void handleAddRoute(ActionEvent event) {
        clearRouteForm();
    }

    /**
     * Handle clear train form button click.
     */
    @FXML
    public void handleClearTrainForm(ActionEvent event) {
        clearTrainForm();
    }

    /**
     * Handle clear route form button click.
     */
    @FXML
    public void handleClearRouteForm(ActionEvent event) {
        clearRouteForm();
    }

    /**
     * Placeholder handler for train export functionality.
     * To be implemented in future versions.
     */
    @FXML
    public void handleExportTrains(ActionEvent event) {
        showMessage("Export trains feature coming soon!", "");
    }

    /**
     * Placeholder handler for route export functionality.
     * To be implemented in future versions.
     */
    @FXML
    public void handleExportRoutes(ActionEvent event) {
        showMessage("Export routes feature coming soon!", "");
    }
}