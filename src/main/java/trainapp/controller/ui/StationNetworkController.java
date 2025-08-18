package trainapp.controller.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import trainapp.dao.StationDAO;
import trainapp.model.Station;
import trainapp.util.SceneManager;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StationNetworkController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> stateFilterCombo;
    @FXML private ComboBox<String> cityFilterCombo;

    @FXML private Label totalStationsLabel;
    @FXML private Label statesCountLabel;
    @FXML private Label citiesCountLabel;

    @FXML private TableView<Station> stationsTable;
    @FXML private TableColumn<Station, Number> colId;
    @FXML private TableColumn<Station, String> colCode;
    @FXML private TableColumn<Station, String> colName;
    @FXML private TableColumn<Station, String> colCity;
    @FXML private TableColumn<Station, String> colState;
    @FXML private TableColumn<Station, Void> colActions;

    @FXML private Label messageLabel;

    private final StationDAO stationDAO = new StationDAO();
    private ObservableList<Station> stationsList = FXCollections.observableArrayList();
    private ObservableList<Station> filteredStations = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Maximize window
        Platform.runLater(() -> {
            Stage stage = (Stage) stationsTable.getScene().getWindow();
            if (stage != null) {
                stage.setMaximized(true);
            }
        });

        setupTableColumns();
        setupComboBoxes();
        loadStations();
        updateStatistics();
    }

    private void setupTableColumns() {
        // Configure table columns
        colId.setCellValueFactory(cellData -> cellData.getValue().stationIdProperty());
        colCode.setCellValueFactory(cellData -> cellData.getValue().stationCodeProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colCity.setCellValueFactory(cellData -> cellData.getValue().cityProperty());
        colState.setCellValueFactory(cellData -> cellData.getValue().stateProperty());

        // Actions column
        colActions.setCellFactory(column -> new TableCell<Station, Void>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox actionBox = new HBox(8);

            {
                editBtn.getStyleClass().add("action-btn-edit");
                deleteBtn.getStyleClass().add("action-btn-delete");

                actionBox.setAlignment(Pos.CENTER);
                actionBox.getChildren().addAll(editBtn, deleteBtn);

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

        stationsTable.setItems(filteredStations);
    }

    private void setupComboBoxes() {
        // Setup state filter combo
        stateFilterCombo.getItems().add("All States");
        stateFilterCombo.setValue("All States");

        // Setup city filter combo
        cityFilterCombo.getItems().add("All Cities");
        cityFilterCombo.setValue("All Cities");

        // Load unique states and cities
        loadFilterOptions();
    }

    private void loadFilterOptions() {
        try {
            // Load unique states
            List<String> states = stationDAO.getAllUniqueStates();
            stateFilterCombo.getItems().clear();
            stateFilterCombo.getItems().add("All States");
            stateFilterCombo.getItems().addAll(states);
            stateFilterCombo.setValue("All States");

            // Load unique cities
            List<String> cities = stationDAO.getAllUniqueCities();
            cityFilterCombo.getItems().clear();
            cityFilterCombo.getItems().add("All Cities");
            cityFilterCombo.getItems().addAll(cities);
            cityFilterCombo.setValue("All Cities");
        } catch (Exception e) {
            showMessage("Error loading filter options: " + e.getMessage(), "error");
        }
    }

    private void loadStations() {
        try {
            List<Station> stations = stationDAO.getAllStations();
            stationsList.setAll(stations);
            filteredStations.setAll(stations);

            if (stations.isEmpty()) {
                showMessage("No stations found in the database.", "info");
            } else {
                showMessage("Loaded " + stations.size() + " stations successfully.", "success");
            }
        } catch (Exception e) {
            showMessage("Error loading stations: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    private void updateStatistics() {
        try {
            int totalStations = stationsList.size();
            long uniqueStates = stationsList.stream()
                    .map(Station::getState)
                    .distinct()
                    .count();
            long uniqueCities = stationsList.stream()
                    .map(Station::getCity)
                    .distinct()
                    .count();

            Platform.runLater(() -> {
                totalStationsLabel.setText(String.valueOf(totalStations));
                statesCountLabel.setText(String.valueOf(uniqueStates));
                citiesCountLabel.setText(String.valueOf(uniqueCities));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSearch() {
        String searchText = searchField.getText().trim().toLowerCase();
        applyFilters(searchText);
    }

    @FXML
    public void handleClearSearch() {
        searchField.clear();
        stateFilterCombo.setValue("All States");
        cityFilterCombo.setValue("All Cities");
        filteredStations.setAll(stationsList);
        showMessage("Filters cleared.", "info");
    }

    @FXML
    public void handleApplyFilter() {
        String searchText = searchField.getText().trim().toLowerCase();
        applyFilters(searchText);
    }

    private void applyFilters(String searchText) {
        String selectedState = stateFilterCombo.getValue();
        String selectedCity = cityFilterCombo.getValue();

        List<Station> filtered = stationsList.stream()
                .filter(station -> {
                    // Search filter
                    boolean matchesSearch = searchText.isEmpty() ||
                            station.getName().toLowerCase().contains(searchText) ||
                            station.getStationCode().toLowerCase().contains(searchText) ||
                            station.getCity().toLowerCase().contains(searchText) ||
                            station.getState().toLowerCase().contains(searchText);

                    // State filter
                    boolean matchesState = "All States".equals(selectedState) ||
                            station.getState().equals(selectedState);

                    // City filter
                    boolean matchesCity = "All Cities".equals(selectedCity) ||
                            station.getCity().equals(selectedCity);

                    return matchesSearch && matchesState && matchesCity;
                })
                .collect(Collectors.toList());

        filteredStations.setAll(filtered);
        showMessage("Found " + filtered.size() + " stations matching criteria.", "info");
    }

    @FXML
    public void handleAddStation() {
        showStationDialog(null);
    }

    private void handleEditStation(Station station) {
        showStationDialog(station);
    }

    private void handleDeleteStation(Station station) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Station");
        confirmDialog.setHeaderText("Delete Station: " + station.getName());
        confirmDialog.setContentText("Are you sure you want to delete this station? This action cannot be undone.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (stationDAO.deleteStation(station.getStationId())) {
                    showMessage("Station deleted successfully", "success");
                    loadStations();
                    updateStatistics();
                    loadFilterOptions(); // Refresh filter options
                } else {
                    showMessage("Failed to delete station", "error");
                }
            } catch (Exception e) {
                showMessage("Error deleting station: " + e.getMessage(), "error");
            }
        }
    }

    private void showStationDialog(Station station) {
        Dialog<Station> dialog = new Dialog<>();
        dialog.setTitle(station == null ? "Add New Station" : "Edit Station");
        dialog.setHeaderText(station == null ? "Enter station details" : "Update station details");

        // Create form fields
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

        // Create form layout
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

        dialog.getDialogPane().setContent(grid);

        // Add buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Enable/disable save button based on input validation
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        // Validation
        Runnable validateInput = () -> {
            boolean valid = !codeField.getText().trim().isEmpty() &&
                    !nameField.getText().trim().isEmpty() &&
                    !cityField.getText().trim().isEmpty() &&
                    !stateField.getText().trim().isEmpty();
            saveButton.setDisable(!valid);
        };

        codeField.textProperty().addListener((obs, oldVal, newVal) -> validateInput.run());
        nameField.textProperty().addListener((obs, oldVal, newVal) -> validateInput.run());
        cityField.textProperty().addListener((obs, oldVal, newVal) -> validateInput.run());
        stateField.textProperty().addListener((obs, oldVal, newVal) -> validateInput.run());

        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Station newStation = station != null ? station : new Station();
                newStation.setStationCode(codeField.getText().trim().toUpperCase());
                newStation.setName(nameField.getText().trim());
                newStation.setCity(cityField.getText().trim());
                newStation.setState(stateField.getText().trim());
                return newStation;
            }
            return null;
        });

        Optional<Station> result = dialog.showAndWait();
        result.ifPresent(this::saveStation);
    }

    private void saveStation(Station station) {
        try {
            boolean success;
            if (station.getStationId() == 0) {
                // Adding new station
                success = stationDAO.addStation(station);
                if (success) {
                    showMessage("Station added successfully", "success");
                }
            } else {
                // Updating existing station
                success = stationDAO.updateStation(station);
                if (success) {
                    showMessage("Station updated successfully", "success");
                }
            }

            if (success) {
                loadStations();
                updateStatistics();
                loadFilterOptions();
            } else {
                showMessage("Failed to save station", "error");
            }
        } catch (Exception e) {
            showMessage("Error saving station: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleRefresh() {
        loadStations();
        updateStatistics();
        loadFilterOptions();
        showMessage("Data refreshed successfully", "success");
    }

    @FXML
    public void handleClose() {
        Stage stage = (Stage) stationsTable.getScene().getWindow();
        stage.close();
    }

    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);

        // Clear existing style classes
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
}