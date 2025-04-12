package com.example;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**Content For The Help Window */
public class Help {
    private static void alertBuilder(String title, String content) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);

        VBox vbox = new VBox();

        // Create a Label for the title bar
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10;");

        // Create a TextArea for the content and wrap it in a ScrollPane
        TextArea contentArea = new TextArea(content);
        contentArea.setWrapText(true);
        contentArea.setEditable(false);
        // contentArea.setPrefWidth(true);
        contentArea.setPrefHeight(350);
        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        // Add the title and scroll pane to the VBox
        vbox.getChildren().addAll(titleLabel, scrollPane);

        // Set up the scene and show the dialog
        Scene scene = new Scene(vbox, 500, 400);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    public static void About() {
        alertBuilder("About Daily Roundup", about);
    }

    static String about = ("Welcome to the Daily Roundup News Aggregator Application! This is a simple news aggregator application built with JavaFX by Group 13. This app allows you to stay updated with the latest news articles from various categories. Use this guide to learn how to navigate and make the most of the app.\n\n"
            +
            "Main Features:\n"
            + "1. Fetch and Display News Articles: Automatically fetches and displays the latest news articles from various categories.\n"
            + "2. Bookmark Articles: Save articles to be read later and quick access.\n"
            + "3. Keyword Preferences: Customize news articles displayed based on your preferred keywords.\n"
            + "4. Search Functionality: Search for news articles using specific keywords.\n"
            + "5. Light/Dark Mode: Switch between light and dark themes.\n"
            + "6. Browser Navigation: Navigate through news articles using the built-in browser.\n\n"
            +
            "Navigation and Buttons:\n"
            + "- Sidebar: Expand or collapse the sidebar to view menus and settings.\n"
            + "- Search Bar: Type keywords to search for news articles.\n"
            + "- Refresh Button: Refresh the news articles to get the latest updates.\n"
            + "- Back/Forward Buttons: Navigate backward or forward in the browser view.\n"
            + "- Reload Button: Reload the current page in the browser view.\n"
            + "- Bookmark Button: Bookmark the selected article for later.\n"
            + "- Help Menu: Access navigation guide and about information.\n\n"
            +
            "Tabs:\n"
            + "- Home Tab: Displays top news articles based on your preferred keywords.\n"
            + "- Category Tabs (Business, Technology, etc.): View articles specific to selected categories.\n"
            + "- Bookmarks Tab: View and manage your bookmarked articles.\n\n"
            +
            "Article Interaction:\n"
            + "- View Article: Click on an article from the list to view it in the built-in browser.\n"
            + "- Bookmark Article: Click the bookmark button(star beneath the article) to save the article for later reading.\n"
            + "- Share Article: Copy the article URL to the clipboard for sharing.\n\n"
            +
            "Preferences:\n"
            + "- Keyword Preferences: Enter your preferred keywords in the provided fields. Click 'Save' to update or 'Clear' to remove keywords.\n"
            + "- Light/Dark Mode: Switch between light and dark themes using the 'Switch Themes' button.\n\n"
            +
            "Alerts and Notifications:\n"
            + "- The application will provide alerts for actions like bookmarking articles, copying URLs to the clipboard, and updating preferences.\n\n"
            +
            "Settings and Exit:\n"
            + "- Close Button: Close the application.\n"
            + "- Minimize Button: Minimize the application window.\n"
            + "- Maximize Button: Maximize or restore the application window.\n\n"
            +
            "This guide provides an overview of the main functionalities to help you get started with the News Aggregator Application. Enjoy staying updated with the latest news articles tailored to your preferences!");

}
