package trainapp;

import javafx.application.Application;
import javafx.stage.Stage;
import trainapp.util.SceneManager;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        SceneManager.init(primaryStage);
        primaryStage.setMaximized(true);
        // Load login scene first - CSS automatically loaded from FXML
        SceneManager.switchScene("/fxml/MainMenu.fxml");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
