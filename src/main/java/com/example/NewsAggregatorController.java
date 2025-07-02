package com.example;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.web.WebView;

public class NewsAggregatorController {
    @FXML
    VBox sideBar;
    @FXML
    VBox mainContainer;
    @FXML
    Button reopenTabButton;
    @FXML
    MenuButton reopenMenuButton;
    @FXML
    TextField searchBar;
    @FXML
    Button refreshButton, backButton, forwardButton, reloadButton;
    @FXML
    ListView<Article> newsListView;
    @FXML
    Label statusLabel, themeStatus, titlelabel;
    @FXML
    TabPane tabPane;
    @FXML
    MenuButton help;
    @FXML
    ProgressBar progressBar;
    @FXML
    Tab homeTab, articleTab, businessTab, technologyTab, entertainmentTab, sportsTab, healthTab, scienceTab,
            localTab, bookmarksTab;
    @FXML
    GridPane keywordGrid;
    @FXML
    Button saveButton;
    @FXML
    Button clearButton;
    @FXML
    Button closeButton;
    @FXML
    TextField keywordField;
    @FXML
    Stage stage;
    @FXML
    WebView webView;
    @FXML
    ProgressIndicator progressIndicator;

    public void setStage(Stage stage) {
        this.stage = stage;
        this.uiHandler = new UIHandler(stage);
        stage.setOnHidden(event -> {
            bookmarkHandler.saveBookmarksToFile();
        });
        stage.setOnCloseRequest(event -> {
            bookmarkHandler.saveBookmarksToFile();
        });
    }

    List<Article> originalArticles;
    Article currentArticle;
    private List<Tab> closedTabs = new ArrayList<>();
    private final PauseTransition pause = new PauseTransition(Duration.seconds(2));
    private final List<TextField> keywordFields = new ArrayList<>();

    UIHandler uiHandler;
    PreferencesHandler preferencesHandler;
    BookmarkHandler bookmarkHandler;
    NewsHandler newsHandler;

    @FXML
    private void initialize() {
        progressIndicator.setVisible(false);
        webViewLoader();

        for (int i = 0; i < 6; i++) {
            TextField keywordField = new TextField();
            keywordField.setPrefHeight(38);
            keywordField.setPromptText("Keyword " + (i + 1));
            keywordGrid.add(keywordField, i % 2, i / 2);
            keywordFields.add(keywordField);
        }

        preferencesHandler = new PreferencesHandler(keywordFields);
        newsHandler = new NewsHandler(this);
        bookmarkHandler = new BookmarkHandler(newsHandler);
        uiHandler = new UIHandler(stage);

        List<String> storedKeywords = preferencesHandler.loadKeywords();
        for (int i = 0; i < storedKeywords.size() && i < keywordFields.size(); i++) {
            keywordFields.get(i).setText(storedKeywords.get(i));
        }

        saveButton.setOnAction(event -> {
            preferencesHandler.saveKeywordsAction();
            setPrefsVisibility();
        });
        clearButton.setOnAction(event -> preferencesHandler.clearKeywordsAction());
        closeButton.setOnAction(event -> setPrefsVisibility());

        mainContainer.setVisible(false);

        tabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    closedTabs.addAll(change.getRemoved());
                    updateReopenMenuButton();
                }
            }
        });

        progressBar.setVisible(false);
        collapseSideBar();

        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            pause.setOnFinished(event -> {
                if (newValue.isEmpty()) {
                    newsListView.getItems().setAll(originalArticles);
                } else {
                    newsHandler.searchNews(newValue);
                }
            });
            pause.playFromStart();
        });

        refreshButton.setOnAction(event -> newsHandler.refreshNews());
        backButton.setOnAction(event -> webView.getEngine().executeScript("history.back()"));
        forwardButton.setOnAction(event -> webView.getEngine().executeScript("history.forward()"));
        reloadButton.setOnAction(event -> webView.getEngine().reload());

        newsHandler.refreshNews();

        newsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                webView.getEngine().load(newValue.getUrl());
                tabPane.getSelectionModel().select(articleTab);
                currentArticle = newValue;
                titlelabel.setText(currentArticle.getTitle());
            }
        });

        updateNavigationButtonsVisibility(false);
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            updateNavigationButtonsVisibility(newTab == articleTab);
        });

        bookmarksTab.setOnSelectionChanged(event -> {
            if (bookmarksTab.isSelected()) {
                bookmarkHandler.displayBookmarkedArticles(bookmarksTab);
            }
        });
    }

    private void webViewLoader() {
        Worker<Void> worker = webView.getEngine().getLoadWorker();
        worker.stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.RUNNING) {
                progressIndicator.setVisible(true);
            } else if (newValue == Worker.State.SUCCEEDED) {
                progressIndicator.setVisible(false);
            }
        });

        worker.progressProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 0.6) {
                progressIndicator.setVisible(false);
            }
        });
    }

    private void updateReopenMenuButton() {
        reopenMenuButton.getItems().clear();
        for (Tab closedTab : closedTabs) {
            MenuItem menuItem = new MenuItem(closedTab.getText());
            menuItem.setOnAction(event -> {
                closedTabs.remove(closedTab);
                tabPane.getTabs().add(closedTab);
                updateReopenMenuButton();
            });
            reopenMenuButton.getItems().add(menuItem);
        }
    }

    @FXML
    private void addToBookmarks() {
        bookmarkHandler.addToBookmarks(currentArticle);
    }

    @FXML
    private void copyUrlToClipboard() {
        uiHandler.copyUrlToClipboard(webView);
    }

    @FXML
    private void expandSideBar() {
        uiHandler.expandSideBar(sideBar);
    }

    @FXML
    private void collapseSideBar() {
        uiHandler.collapseSideBar(sideBar);
    }

    @FXML
    private void about() {
        uiHandler.about();
    }

    @FXML
    private void closeButton() {
        uiHandler.closeButton();
    }

    @FXML
    private void minimizeButton() {
        uiHandler.minimizeButton();
    }

    @FXML
    private void maximizeButton() {
        uiHandler.maximizeButton();
    }

    @FXML
    private void switchThemes() {
        uiHandler.switchThemes(themeStatus);
    }

    private void updateNavigationButtonsVisibility(boolean visible) {
        backButton.setVisible(visible);
        forwardButton.setVisible(visible);
        reloadButton.setVisible(visible);
    }

    @FXML
    public void setPrefsVisibility() {
        mainContainer.setVisible(!mainContainer.isVisible());
    }
}
