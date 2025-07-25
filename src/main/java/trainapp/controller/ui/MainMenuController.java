package trainapp.controller.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

import trainapp.model.Station;
import trainapp.dao.StationDAO;
import trainapp.util.SceneManager;

public class MainMenuController {

    @FXML
    private TextField trainFromField;
    @FXML
    private TextField trainToField;
    @FXML
    private VBox fromSuggestionsList;
    @FXML
    private VBox toSuggestionsList;

    StationDAO stationDAO = new StationDAO();

    @FXML
    public void initialize() {
        setupAutoComplete(trainFromField, fromSuggestionsList);
        setupAutoComplete(trainToField, toSuggestionsList);
    }

    private void setupAutoComplete(TextField textField, VBox suggestionsList) {
        // Listen for text changes
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            handleTextChange(textField, suggestionsList, newValue);
        });

        // Hide suggestions when focus is lost
        textField.focusedProperty().addListener((observable, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                PauseTransition delay = new PauseTransition(Duration.millis(150));
                delay.setOnFinished(e -> hideSuggestions(suggestionsList));
                delay.play();
            }
        });
    }

    private void handleTextChange(TextField textField, VBox suggestionsList, String newValue) {
        if (newValue == null || newValue.trim().isEmpty()) {
            hideSuggestions(suggestionsList);
            return;
        }

        List<Station> matchingStations = getMatchingStations(newValue.trim());

        if (matchingStations.isEmpty()) {
            hideSuggestions(suggestionsList);
        } else {
            // Limit to 5 suggestions
            List<Station> limitedStations = matchingStations.stream().limit(5).toList();
            showSuggestions(limitedStations, textField, suggestionsList);
        }
    }

    private void showSuggestions(List<Station> stations, TextField textField, VBox suggestionsList) {
        suggestionsList.getChildren().clear();

        for (Station station : stations) {
            Label suggestionLabel = new Label(station.getName()+" ("+station.getStationCode()+")");
            suggestionLabel.getStyleClass().add("suggestion-item");

            // Handle click selection
            suggestionLabel.setOnMouseClicked(event -> {
                selectSuggestion(textField, suggestionsList, station.getName()+" ("+station.getStationCode()+")");
            });

            suggestionsList.getChildren().add(suggestionLabel);
        }

        // Show the suggestions list
        suggestionsList.getStyleClass().add("show");
        suggestionsList.setVisible(true);
        suggestionsList.setManaged(true);
    }

    private void selectSuggestion(TextField textField, VBox suggestionsList, String selectedName) {
        textField.setText(selectedName);
        hideSuggestions(suggestionsList);

        Platform.runLater(() -> {
            textField.requestFocus();
            textField.positionCaret(selectedName.length());
        });
    }

    private void hideSuggestions(VBox suggestionsList) {
        suggestionsList.getStyleClass().remove("show");

        PauseTransition delay = new PauseTransition(Duration.millis(200));
        delay.setOnFinished(event -> {
            suggestionsList.setVisible(false);
            suggestionsList.setManaged(false);
            suggestionsList.getChildren().clear();
        });
        delay.play();
    }

    private List<Station> getMatchingStations(String partialName) {
        return stationDAO.getAllStations()
                .stream()
                .filter(station -> station.getName().toLowerCase().contains(partialName.toLowerCase()))
                .toList();
    }



    public void SwitchButton(ActionEvent actionEvent) {
        String fromFieldText = trainFromField.getText();
        String toFieldText = trainToField.getText();

        if (fromFieldText == null || fromFieldText.trim().isEmpty() ||
                toFieldText == null || toFieldText.trim().isEmpty()) {

            showStationEmptyAlert();
        } else {

            trainFromField.setText(toFieldText);
            trainToField.setText(fromFieldText);
        }
    }

    private void showStationEmptyAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Tailyatri - Station Switch");
        alert.setHeaderText("Cannot Switch Stations");
        alert.setContentText("Both 'From' and 'To' station fields must be filled before switching.\n\n" +
                "Please select your departure and destination stations first.");

        // Style the alert
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/MainMenu.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("custom-alert");

        // Customize button text
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().add("alert-button");

        alert.showAndWait();
    }

    @FXML
    public void handleSupportClicked(ActionEvent actionEvent) {
        // TODO: Implement support functionality
    }

    public void displayAccount(ActionEvent actionEvent) {
        SceneManager.switchScene("/fxml/Login.fxml");
    }
}