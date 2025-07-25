package trainapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainMenu.fxml"));
            Parent root = loader.load();
            primaryStage.setTitle("Indian Railway Booking System");
            Scene scene = new Scene(root, 800, 600);
            primaryStage.setScene(scene);

            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/MainMenu.css")).toExternalForm());

            primaryStage.setMaximized(true);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();  // 👈 will tell the exact reason
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
