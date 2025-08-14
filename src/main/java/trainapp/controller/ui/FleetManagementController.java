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
import java.util.Optional;

public class FleetManagementController {

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

    // Status Controls
    @FXML private Label trainCountLabel;
    @FXML private Label routeCountLabel;
    @FXML private Label lastUpdateLabel;
    @FXML private Label messageLabel;

    // DAOs
    private TrainDAO trainDAO;
    private StationDAO stationDAO;
    private TrainScheduleDAO scheduleDAO;

    // Data
    private Train selectedTrain;
    private TrainSchedule selectedRoute;
    private ObservableList<Train> allTrains = FXCollections.observableArrayList();
    private ObservableList<Station> allStations = FXCollections.observableArrayList();
    private ObservableList<TrainSchedule> allRoutes = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        trainDAO = new TrainDAO();
        stationDAO = new StationDAO();
        scheduleDAO = new TrainScheduleDAO();

        setupTableColumns();
        setupEventHandlers();
        setupSpinners();
        loadAllData();

        // Set column resize policy
        Platform.runLater(() -> {
            trainTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            routeTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        });
    }

    private void setupTableColumns() {
        // Train table
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

        // Route table
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
            } else {
                routeTableView.setItems(allRoutes);
                enableRouteControls(false);
            }
        });
    }

    private void setupSpinners() {
        coachesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 30, 20));
        daySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 7, 1));
        sequenceSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));
    }

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

    private void setupComboBoxes() {
        setupSearchableStationCombo(sourceStationCombo, "Type to search source station...");
        setupSearchableStationCombo(destStationCombo, "Type to search destination...");
        setupSearchableStationCombo(routeStationCombo, "Type to search station...");
        setupTrainCombo();
    }

    /**
     * SEARCHABLE COMBO BOX SETUP - Core functionality restored
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
                });
                return null;
            }
        };
        new Thread(routeTask).start();
    }

    // =============== TRAIN CRUD ===============

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

    // =============== ROUTE CRUD ===============

    @FXML
    public void handleSaveRoute(ActionEvent event) {
        if (!validateRouteForm()) return;

        TrainSchedule schedule = new TrainSchedule();
        schedule.setTrainId(routeTrainCombo.getValue().getTrainId());
        schedule.setStationId(routeStationCombo.getValue().getStationId());
        schedule.setDayNumber(daySpinner.getValue());
        schedule.setSequenceOrder(sequenceSpinner.getValue());

        try {
            if (!arrivalTimeField.getText().trim().isEmpty()) {
                schedule.setArrivalTime(LocalTime.parse(arrivalTimeField.getText().trim()));
            }
            if (!departureTimeField.getText().trim().isEmpty()) {
                schedule.setDepartureTime(LocalTime.parse(departureTimeField.getText().trim()));
            }
        } catch (Exception e) {
            showMessage("Invalid time format! Use HH:MM", "error");
            return;
        }

        if (scheduleDAO.addStationToJourney(schedule)) {
            showMessage("Route saved successfully!", "success");
            clearRouteForm();
            loadRouteData();
        } else {
            showMessage("Failed to save route!", "error");
        }
    }

    @FXML
    public void handleUpdateRoute(ActionEvent event) {
        if (selectedRoute == null || !validateRouteForm()) return;

        selectedRoute.setTrainId(routeTrainCombo.getValue().getTrainId());
        selectedRoute.setStationId(routeStationCombo.getValue().getStationId());
        selectedRoute.setDayNumber(daySpinner.getValue());
        selectedRoute.setSequenceOrder(sequenceSpinner.getValue());

        try {
            selectedRoute.setArrivalTime(arrivalTimeField.getText().trim().isEmpty() ?
                    null : LocalTime.parse(arrivalTimeField.getText().trim()));
            selectedRoute.setDepartureTime(departureTimeField.getText().trim().isEmpty() ?
                    null : LocalTime.parse(departureTimeField.getText().trim()));
        } catch (Exception e) {
            showMessage("Invalid time format!", "error");
            return;
        }

        if (scheduleDAO.updateStationInJourney(selectedRoute)) {
            showMessage("Route updated successfully!", "success");
            clearRouteForm();
            loadRouteData();
        } else {
            showMessage("Failed to update route!", "error");
        }
    }

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

    // =============== EVENT HANDLERS ===============

    @FXML public void handleBack(ActionEvent event) {
        ((Stage) backButton.getScene().getWindow()).close();
    }

    @FXML public void handleRefresh(ActionEvent event) {
        loadAllData();
    }

    @FXML public void handleAddTrain(ActionEvent event) {
        clearTrainForm();
    }

    @FXML public void handleAddRoute(ActionEvent event) {
        clearRouteForm();
    }

    @FXML public void handleClearTrainForm(ActionEvent event) {
        clearTrainForm();
    }

    @FXML public void handleClearRouteForm(ActionEvent event) {
        clearRouteForm();
    }

    @FXML public void handleExportTrains(ActionEvent event) {
        showMessage("Export feature coming soon!", "");
    }

    @FXML public void handleExportRoutes(ActionEvent event) {
        showMessage("Export feature coming soon!", "");
    }

    // =============== UTILITY METHODS ===============

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

    private boolean validateRouteForm() {
        if (routeTrainCombo.getValue() == null) {
            showMessage("Please select a train!", "error");
            return false;
        }
        if (routeStationCombo.getValue() == null) {
            showMessage("Please select a station!", "error");
            return false;
        }
        return true;
    }

    private void populateTrainForm(Train train) {
        trainNumberField.setText(train.getTrainNumber());
        trainNameField.setText(train.getName());
        sourceStationCombo.setValue(getStationById(train.getSourceStationId()));
        destStationCombo.setValue(getStationById(train.getDestinationStationId()));
        coachesSpinner.getValueFactory().setValue(train.getTotalCoaches());
    }

    private void populateRouteForm(TrainSchedule schedule) {
        routeTrainCombo.setValue(getTrainById(schedule.getTrainId()));
        routeStationCombo.setValue(getStationById(schedule.getStationId()));
        arrivalTimeField.setText(schedule.getArrivalTime() != null ? schedule.getArrivalTime().toString() : "");
        departureTimeField.setText(schedule.getDepartureTime() != null ? schedule.getDepartureTime().toString() : "");
        daySpinner.getValueFactory().setValue(schedule.getDayNumber());
        sequenceSpinner.getValueFactory().setValue(schedule.getSequenceOrder());
    }

    private void clearTrainForm() {
        trainNumberField.clear();
        trainNameField.clear();
        sourceStationCombo.setValue(null);
        destStationCombo.setValue(null);
        coachesSpinner.getValueFactory().setValue(20);
        selectedTrain = null;
    }

    private void clearRouteForm() {
        routeStationCombo.setValue(null);
        arrivalTimeField.clear();
        departureTimeField.clear();
        daySpinner.getValueFactory().setValue(1);
        sequenceSpinner.getValueFactory().setValue(1);
        selectedRoute = null;
    }

    private void filterRoutesByTrain(Train train) {
        ObservableList<TrainSchedule> filtered = FXCollections.observableArrayList();
        for (TrainSchedule route : allRoutes) {
            if (route.getTrainId() == train.getTrainId()) {
                filtered.add(route);
            }
        }
        routeTableView.setItems(filtered);
    }

    private void enableRouteControls(boolean enable) {
        routeStationCombo.setDisable(!enable);
        arrivalTimeField.setDisable(!enable);
        departureTimeField.setDisable(!enable);
        daySpinner.setDisable(!enable);
        sequenceSpinner.setDisable(!enable);
        saveRouteBtn.setDisable(!enable);
    }

    private Station getStationById(int stationId) {
        return allStations.stream().filter(s -> s.getStationId() == stationId).findFirst().orElse(null);
    }

    private Train getTrainById(int trainId) {
        return allTrains.stream().filter(t -> t.getTrainId() == trainId).findFirst().orElse(null);
    }

    private void updateStatus() {
        trainCountLabel.setText(allTrains.size() + " trains");
        routeCountLabel.setText(allRoutes.size() + " routes");
        lastUpdateLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

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