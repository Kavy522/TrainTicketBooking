package trainapp.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneManager {
    private static Stage primaryStage;

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static <T> T switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.setMaximized(true);
            return loader.getController();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load FXML: " + fxmlPath, ex);
        }
    }


    /**
     * Get the primary stage reference
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}