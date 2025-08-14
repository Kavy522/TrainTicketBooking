package trainapp.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class SceneManager {
    private static Stage primaryStage;
    private static boolean debugMode = true; // Set to false in production

    /**
     * Initialize SceneManager with primary stage reference
     */
    public static void init(Stage stage) {
        primaryStage = stage;
        if (debugMode) {
            System.out.println("✅ SceneManager initialized with primary stage");
        }
    }

    /**
     * Switch scene and return controller (your existing method enhanced)
     */
    public static <T> T switchScene(String fxmlPath) {
        try {
            if (debugMode) {
                System.out.println("🔄 Attempting to load FXML: " + fxmlPath);
            }

            // Validate primary stage
            if (primaryStage == null) {
                throw new IllegalStateException("SceneManager not initialized. Call SceneManager.init(stage) first.");
            }

            // Check if resource exists
            URL resourceUrl = SceneManager.class.getResource(fxmlPath);
            if (resourceUrl == null) {
                throw new IOException("FXML resource not found: " + fxmlPath);
            }

            if (debugMode) {
                System.out.println("✅ FXML resource found: " + resourceUrl);
            }

            // Load FXML
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            // Create and set scene
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.setMaximized(true);

            if (debugMode) {
                T controller = loader.getController();
                System.out.println("✅ Scene loaded successfully: " + fxmlPath);
                System.out.println("📋 Controller: " + (controller != null ? controller.getClass().getSimpleName() : "null"));
            }

            return loader.getController();

        } catch (IOException ex) {
            System.err.println("❌ FXML Loading Error: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Failed to load FXML: " + fxmlPath, ex);
        } catch (Exception ex) {
            System.err.println("❌ Scene Switch Error: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Failed to switch scene: " + fxmlPath, ex);
        }
    }

    /**
     * Switch scene without returning controller (simpler version)
     */
    public static void switchSceneSimple(String fxmlPath) {
        switchScene(fxmlPath);
    }

    /**
     * Switch scene with custom stage settings
     */
    public static <T> T switchScene(String fxmlPath, boolean maximized, boolean centerOnScreen) {
        T controller = switchScene(fxmlPath);

        if (maximized) {
            primaryStage.setMaximized(true);
        } else {
            primaryStage.setMaximized(false);
        }

        if (centerOnScreen) {
            primaryStage.centerOnScreen();
        }

        return controller;
    }

    /**
     * Get the primary stage reference
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Check if SceneManager is properly initialized
     */
    public static boolean isInitialized() {
        return primaryStage != null;
    }

    /**
     * Enable/disable debug mode
     */
    public static void setDebugMode(boolean debug) {
        debugMode = debug;
    }

    /**
     * Verify FXML resource exists without loading
     */
    public static boolean resourceExists(String fxmlPath) {
        return SceneManager.class.getResource(fxmlPath) != null;
    }

    /**
     * Debug method to check all required FXML files
     */
    public static void debugResourcePaths() {
        System.out.println("🔍 Checking FXML resources...");

        String[] requiredFiles = {
                "/fxml/Login.fxml",
                "/fxml/MainMenu.fxml",
                "/fxml/UserProfile.fxml",
                "/fxml/AdminProfile.fxml"
        };

        for (String path : requiredFiles) {
            if (resourceExists(path)) {
                System.out.println("✅ " + path + " - Found");
            } else {
                System.out.println("❌ " + path + " - NOT FOUND");
            }
        }

        System.out.println("Primary Stage: " + (primaryStage != null ? "Initialized" : "NULL"));
    }

    /**
     * Get current scene title (if set)
     */
    public static String getCurrentSceneTitle() {
        return primaryStage != null ? primaryStage.getTitle() : null;
    }

    /**
     * Set stage title
     */
    public static void setStageTitle(String title) {
        if (primaryStage != null) {
            primaryStage.setTitle(title);
        }
    }

    /**
     * Emergency fallback - load login scene
     */
    public static void loadLoginScene() {
        try {
            System.out.println("🚨 Loading emergency login scene...");
            switchScene("/fxml/Login.fxml");
        } catch (Exception e) {
            System.err.println("❌ Failed to load login scene: " + e.getMessage());
            // At this point, something is seriously wrong
            throw new RuntimeException("Critical error: Cannot load any scenes", e);
        }
    }
}