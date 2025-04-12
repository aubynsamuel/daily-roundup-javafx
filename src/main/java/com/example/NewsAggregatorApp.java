package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class NewsAggregatorApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            LoadingScreenController loadingScreen = new LoadingScreenController();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/NewsAggregatorLayout.fxml"));
            VBox root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/example/light_theme.css").toExternalForm());

            NewsAggregatorController controller = loader.getController();
            controller.setStage(primaryStage);

            primaryStage.setTitle("Daily Roundup");

            VBoxResizer.apply(primaryStage, root);

            primaryStage.initStyle(StageStyle.UNDECORATED);
            Image icon = new Image("/com/example/daily-roundup-high-resolution-logo.jpeg");
            primaryStage.getIcons().add(icon);
            primaryStage.setScene(scene);

            primaryStage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.F11) { // Check if F11 key is pressed
                    primaryStage.setFullScreen(!primaryStage.isFullScreen()); // Toggle fullscreen
                    event.consume(); // Prevent event from being handled by other nodes
                }
            });
            primaryStage.setFullScreenExitHint("");

            // Show loading screen while main application loads in the background
            loadingScreen.showLoadingScreen(primaryStage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
