package com.example;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LoadingScreenController {

    private ProgressBar progressBar;
    @SuppressWarnings("unused")
    private Label label;
    @SuppressWarnings("unused")
    private ImageView imageView;

    public void showLoadingScreen(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/loadingScreen.fxml"));
            Parent root = loader.load();
            progressBar = (ProgressBar) loader.getNamespace().get("loadingBar");
            label = (Label) loader.getNamespace().get("loadingMessage");
            imageView = (ImageView) loader.getNamespace().get("backgroundPic");

            Stage loadingStage = new Stage();
            loadingStage.initStyle(StageStyle.UNDECORATED);
            loadingStage.setScene(new Scene(root));
            loadingStage.show();

            // Start a new thread for loading data
            new Thread(() -> {
                for (int i = 0; i <= 100; i++) {
                    final int progress = i;
                    Platform.runLater(() -> progressBar.setProgress(progress / 100.0));
                    try {
                        Thread.sleep(40); // Simulating loading process
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Platform.runLater(() -> {
                    loadingStage.close();
                    primaryStage.show();
                });
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
