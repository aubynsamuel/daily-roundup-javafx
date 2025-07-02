package com.example;

import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.web.WebView;

public class UIHandler {
    private boolean islightTheme = true;
    private final Stage stage;

    public UIHandler(Stage stage) {
        this.stage = stage;
    }

    public void expandSideBar(VBox sideBar) {
        TranslateTransition expandSideBar = new TranslateTransition(Duration.millis(80), sideBar);
        expandSideBar.setFromX(-sideBar.getPrefWidth() - 10);
        expandSideBar.setToX(0);
        expandSideBar.play();
    }

    public void collapseSideBar(VBox sideBar) {
        TranslateTransition collapseSideBar = new TranslateTransition(Duration.millis(80), sideBar);
        collapseSideBar.setFromX(0);
        collapseSideBar.setToX(-sideBar.getPrefWidth() - 10);
        collapseSideBar.play();
    }

    public static void alertBuilder(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
        PauseTransition delay = new PauseTransition(Duration.seconds(4));
        delay.setOnFinished(event -> alert.close());
        delay.play();
    }

    public void about() {
        Help.About();
    }

    public void closeButton() {
        stage.close();
        System.exit(0);
    }

    public void minimizeButton() {
        stage.setIconified(true);
    }

    public void maximizeButton() {
        if (stage.isMaximized()) {
            stage.setMaximized(false);
        } else {
            stage.setMaximized(true);
        }
    }

    public void switchThemes(Label themeStatus) {
        Scene scene = stage.getScene();
        if (islightTheme) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/com/example/dark_theme.css").toExternalForm());
            themeStatus.setText("Light Mode");
            islightTheme = false;
        } else {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/com/example/light_theme.css").toExternalForm());
            themeStatus.setText("Dark Mode");
            islightTheme = true;
        }
    }

    public void copyUrlToClipboard(WebView webView) {
        String urlToCopy = webView.getEngine().getLocation();
        if (urlToCopy != null && !urlToCopy.equals("about:blank")) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(urlToCopy);
            clipboard.setContent(content);
            alertBuilder("Copied to Clipboard", "You Can Now Share The Article, The URL has been copied to your clipboard 😁");
        } else {
            alertBuilder("Failed To Copy To Clipboard", "No Article Selected, Please Click on an Article Before Pressing The Share Button");
        }
    }
}
