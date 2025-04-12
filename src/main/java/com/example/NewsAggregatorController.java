package com.example;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Controller class for the News Aggregator application.
 * Handles user interactions and manages the news fetching and display.
 */

public class NewsAggregatorController {
    @FXML
    private VBox sideBar;
    boolean islightTheme;
    @FXML
    private VBox mainContainer;
    @FXML
    private Button reopenTabButton;
    @FXML
    private MenuButton reopenMenuButton;
    @FXML
    private TextField searchBar;
    @FXML
    private Button refreshButton, backButton, forwardButton, reloadButton;
    @FXML
    private ListView<Article> newsListView;
    @FXML
    private Label statusLabel, themeStatus, titlelabel;
    @FXML
    private TabPane tabPane;
    @FXML
    private MenuButton help;
    @FXML
    ProgressBar progressBar;
    @FXML
    private Tab homeTab, articleTab, businessTab, technologyTab, entertainmentTab, sportsTab, healthTab, scienceTab,
            localTab, bookmarksTab;
    @FXML
    private GridPane keywordGrid;
    @FXML
    private Button saveButton;
    @FXML
    private Button clearButton;
    @FXML
    private Button closeButton;
    @FXML
    private TextField keywordField;
    @FXML
    private Stage stage;
    @FXML
    private WebView webView;
    @FXML
    private ProgressIndicator progressIndicator;

    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setOnHidden(event -> {
            saveBookmarksToFile();
        });
        stage.setOnCloseRequest(event -> {
            saveBookmarksToFile();
        });
    }

    private final NewsService newsService = new NewsService();
    private List<Article> originalArticles;
    private List<Article> bookmarkedArticles = new ArrayList<>();
    Article currentArticle;
    private static final int MAX_RETRIES = 3;
    private List<Tab> closedTabs = new ArrayList<>();
    private PauseTransition pause = new PauseTransition(Duration.seconds(2));

    private static final String BOOKMARKS_DIR_PATH = System.getProperty("user.home") + "/news_aggregator/bookmarks";
    private static final String BOOKMARKS_FILE_PATH = BOOKMARKS_DIR_PATH + "/bookmarked_articles.ser";

    private final List<TextField> keywordFields = new ArrayList<>();
    private final List<String> Prefkeywords = new ArrayList<>();

    /**
     * Initializes the controller and sets up event handlers for UI elements.
     */
    @FXML
    private void initialize() {
        progressIndicator.setVisible(false);
        webViewLoader();
        // Set uptextfields for preferences keywords
        for (int i = 0; i < 6; i++) {
            TextField keywordField = new TextField();
            keywordField.setPrefHeight(38);
            keywordField.setPromptText("Keyword " + (i + 1));
            keywordGrid.add(keywordField, i % 2, i / 2);
            keywordFields.add(keywordField);
        }
        // Load stored keywords
        List<String> storedKeywords = loadKeywords();
        for (int i = 0; i < storedKeywords.size() && i < keywordFields.size(); i++) {
            keywordFields.get(i).setText(storedKeywords.get(i));
        }
        Prefkeywords.addAll(storedKeywords);

        // Set button for preferences window/actions
        saveButton.setOnAction(event -> saveKeywordsAction());
        clearButton.setOnAction(event -> clearKeywordsAction());
        closeButton.setOnAction(event -> setPrefsVisibility());

        // Initially hide the preferencs window
        mainContainer.setVisible(false);

        // / Event handler to track closed tabs
        tabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    closedTabs.addAll(change.getRemoved());
                    updateReopenMenuButton(); // Update the reopen menu whenever a tab is closed
                }
            }
        });
        // Initially hide progress bar
        progressBar.setVisible(false);

        // Create a new directory in users home directory to store program
        // files(Bookmarked Aricles)
        File bookmarksDir = new File(BOOKMARKS_DIR_PATH);
        if (!bookmarksDir.exists()) {
            bookmarksDir.mkdirs();
        }
        // Set Default theme
        islightTheme = true;

        // Hide Side Bar/MenuBar
        collapseSideBar();

        // Event listner for search textfield
        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            pause.setOnFinished(event -> {
                if (newValue.isEmpty()) {
                    newsListView.getItems().setAll(originalArticles);
                } else {
                    searchNews(newValue);
                }
            });
            pause.playFromStart();
        });

        // Methods For Refresh and Navigation Buttons
        refreshButton.setOnAction(event -> refreshNews());
        backButton.setOnAction(event -> webView.getEngine().executeScript("history.back()"));
        forwardButton.setOnAction(event -> webView.getEngine().executeScript("history.forward()"));
        reloadButton.setOnAction(event -> webView.getEngine().reload());

        // Fetch and display news articles.
        refreshNews();

        // Open news articles from the listview in the browser under the ArticleTab
        newsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                webView.getEngine().load(newValue.getUrl());
                tabPane.getSelectionModel().select(articleTab);
                currentArticle = newValue;
                titlelabel.setText(currentArticle.getTitle());
            }
        });

        // Show/hide navigation buttons based on selected tab
        updateNavigationButtonsVisibility(false);
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            updateNavigationButtonsVisibility(newTab == articleTab);
        });

        // Load bookmarks when the application starts
        loadBookmarksFromFile();

        // Update bookmarks tab on selection to display bookmarked articles in
        // realtime
        bookmarksTab.setOnSelectionChanged(event -> {
            if (bookmarksTab.isSelected()) {
                displayBookmarkedArticles();
            }
        });
    }

    // ------------------------END OF INITIALIZE--------------------------

    // Create a loading indicator and display it when WebView is loading
    private void webViewLoader() {
        WebEngine webEngine = webView.getEngine();

        // Add listener to the webEngine's loadWorker state property
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.RUNNING) {
                progressIndicator.setVisible(true);
            } else if (newValue == Worker.State.SUCCEEDED) {
                progressIndicator.setVisible(false);
            }
        });

        // Add listener to the webEngine's loadWorker progress property
        webEngine.getLoadWorker().progressProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 0.6) {
                progressIndicator.setVisible(false);
            }
        });
    }

    private void saveKeywordsAction() {
        Prefkeywords.clear(); // Clear existing keywords to avoid duplication
        for (TextField field : keywordFields) {
            if (!field.getText().trim().isEmpty()) {
                Prefkeywords.add(field.getText().trim());
            }
        }
        if (!Prefkeywords.isEmpty()) {
            saveKeywords(Prefkeywords);
        }
        setPrefsVisibility();
        alertBuilder("Preferences Updated", "Refresh To See Changes");
    }

    private void clearKeywordsAction() {
        for (TextField field : keywordFields) {
            field.clear();
        }
        Prefkeywords.clear(); // Clear the keywords list
        saveKeywords(Prefkeywords); // Save the cleared keywords
    }

    private void saveKeywords(List<String> keywords) {
        Preferences prefs = Preferences.userNodeForPackage(NewsAggregatorController.class);
        for (int i = 0; i < keywords.size(); i++) {
            prefs.put("keyword" + i, keywords.get(i));
        }
        for (int i = keywords.size(); i < 6; i++) {
            prefs.remove("keyword" + i); // Clear any previously saved keywords beyond the current list size
        }
    }

    private List<String> loadKeywords() {
        Preferences prefs = Preferences.userNodeForPackage(NewsAggregatorController.class);
        List<String> keywords = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String keyword = prefs.get("keyword" + i, null);
            if (keyword != null) {
                keywords.add(keyword);
            }
        }
        System.out.println(keywords);
        return keywords;
    }

    // Store closed buttons as menu Items so they can be reopened later
    private void updateReopenMenuButton() {
        reopenMenuButton.getItems().clear();
        for (Tab closedTab : closedTabs) {
            MenuItem menuItem = new MenuItem(closedTab.getText());
            menuItem.setOnAction(event -> {
                closedTabs.remove(closedTab);
                tabPane.getTabs().add(closedTab);
                updateReopenMenuButton(); // Refresh the menu items after reopening
            });
            reopenMenuButton.getItems().add(menuItem);
        }
    }

    @FXML
    // Adding articles to bookmarks from the list view
    private void addToBookmarks() {
        if (currentArticle != null && !bookmarkedArticles.contains(currentArticle)) {
            bookmarkedArticles.add(currentArticle);
            displayBookmarkedArticles();
            String title = "Article Added to Bookmarks";
            String content = "The article has been added to your bookmarks.";
            alertBuilder(title, content);
        } else if (bookmarkedArticles.contains(currentArticle)) {
            String title = "Article Already in Bookmarks";
            String content = "The article is already in your bookmarks";
            alertBuilder(title, content);
        } else {
            String title = "No Article Selected";
            String content = "Please Click on an Article Before Clicking 'Bookmark Article'";
            alertBuilder(title, content);
        }
    }

    // Actuall copy to clipboard method
    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    // Copy to clipboard method for our fxml elements
    @FXML
    private void copyUrlToClipboard() {
        String urlToCopy = webView.getEngine().getLocation();
        if (urlToCopy != null && !urlToCopy.equals("about:blank")) {
            copyToClipboard(urlToCopy);
            String title = "Copied to Clipboard";
            String alertcontent = "You Can Now Share The Article, The URL has been copied to your clipboard 😁";
            alertBuilder(title, alertcontent);
        } else {
            String title = "Failed To Copy To Clipboard";
            String alertcontent = "No Article Selected, Please Click on an Article Before Pressing The Share Button";
            alertBuilder(title, alertcontent);
        }
    }

    // Side Bar's Display and Hide Methods For Our FXML elements
    @FXML
    private void expandSideBar() {
        TranslateTransition expandSideBar = new TranslateTransition(Duration.millis(80), sideBar);
        expandSideBar.setFromX(-sideBar.getPrefWidth() - 10);
        expandSideBar.setToX(0);
        expandSideBar.play();
    }

    @FXML
    private void collapseSideBar() {
        TranslateTransition collapseSideBar = new TranslateTransition(Duration.millis(80), sideBar);
        collapseSideBar.setFromX(0);
        collapseSideBar.setToX(-sideBar.getPrefWidth() - 10);
        collapseSideBar.play();
    }

    // Method to simplify displaying of alerts
    public static void alertBuilder(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();

        // Create a PauseTransition to hide the alert after 4 seconds
        PauseTransition delay = new PauseTransition(Duration.seconds(4));
        delay.setOnFinished(event -> alert.close());
        delay.play();
    }

    // Display About This Application
    @FXML
    private void about() {
        Help.About();
    }

    // closeButton
    @FXML
    private void closeButton() {
        stage.close();
        System.exit(0);
    }

    // minimizeButton
    @FXML
    private void minimizeButton() {
        stage.setIconified(true);
    }

    // maximizeButton
    @FXML
    private void maximizeButton() {
        if (stage.isMaximized()) {
            stage.setMaximized(false);
        } else {
            stage.setMaximized(true);
        }
    }

    // Swiching Between Light and Dark Mode Themes
    @FXML
    private void lightmode() {
        Scene scene = stage.getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/com/example/light_theme.css").toExternalForm());
    }

    @FXML
    private void darkmode() {
        Scene scene = stage.getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/com/example/dark_theme.css").toExternalForm());
    }

    @FXML
    private void switchThemes() {
        if (islightTheme) {
            darkmode();
            themeStatus.setText("Light Mode");
            islightTheme = false;
        } else {
            lightmode();
            themeStatus.setText("Dark Mode");
            islightTheme = true;
        }

    }

    // Load tabs on selection
    private void loadOtherTabsOnSelection(Tab tab, String category, String keywords) {
        tab.setOnSelectionChanged(event -> {
            if (tab.isSelected()) {
                tab.setContent(createPage(category, keywords, false));
            }
        });
    }

    // Create the page in the various tabs
    public ScrollPane createPage(String category, String keywords, boolean isHome) {
        VBox page = new VBox();
        page.setPadding(new Insets(10));
        page.setSpacing(5);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        page.getChildren().add(gridPane);

        if (!Prefkeywords.isEmpty() && isHome)
            loadPrefPage(gridPane, 0, category, Prefkeywords, isHome);
        else
            loadPage(gridPane, 0, category, keywords, isHome);

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    // Uses API call to fetch news articles and fills the page with it
    private void loadPage(GridPane gridPane, int retries, String category, String keywords, boolean isHome) {
        CompletableFuture<List<Article>> fetchNewsTask;
        if (isHome) {
            fetchNewsTask = keywords.equals("") ? newsService.fetchNews(category, keywords)
                    : newsService.searchNews(keywords);
            statusLabel.setText("Fectching News");
        } else {
            fetchNewsTask = keywords.equals("") ? newsService.fetchNews(category, keywords)
                    : newsService.searchNews(keywords);
            statusLabel.setText("Loading " + category + " news");
        }
        progressBar.setVisible(true);

        fetchNewsTask.thenAccept(articles -> {
            Platform.runLater(() -> {
                // Remove invalid articles
                articles.removeIf(article -> article == null || article.getUrl() == null || article.getUrl().isEmpty()
                        || article.getUrlToImage() == null || article.getUrlToImage().isEmpty());
                if (articles.isEmpty()) {
                    statusLabel.setText("No articles fetched. Check your internet connection.");
                } else {
                    statusLabel.setText("");
                }
                progressBar.setVisible(false);
                populateGrid(gridPane, articles, isHome);
            });
        }).exceptionally(ex -> {
            if (retries < MAX_RETRIES) {
                Platform.runLater(() -> {
                    statusLabel.setText("Retrying... (" + (retries + 1) + "/" + MAX_RETRIES + ")");
                    loadPage(gridPane, retries + 1, category, keywords, isHome);
                });
            } else {
                Platform.runLater(() -> statusLabel
                        .setText("Failed to fetch news after " + MAX_RETRIES + " attempts"));
            }
            return null;
        });
    }

    // Populates the given GridPane with news articles
    private void populateGrid(GridPane gridPane, List<Article> articles, boolean isHomeTab) {
        gridPane.getChildren().clear(); // Clear existing children
        int column = 0;
        int row = 0;

        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);
            VBox articleBox;

            if (i == 0 && isHomeTab) {
                articleBox = createArticleBox(article, true);
                gridPane.add(articleBox, column, row, 2, 2);
                column += 2;
                row += 1;
            } else {
                articleBox = createArticleBox(article, false);
                gridPane.add(articleBox, column, row);
                column++;
                if (column == 3) {
                    column = 0;
                    row++;
                }
            }
        }
    }

    // Creating Article Boxes in the Gridview of Tabs
    public VBox createArticleBox(Article article, boolean isHeadArticle) {
        VBox articleBox = new VBox();
        articleBox.setAlignment(Pos.CENTER);
        articleBox.setPadding(new Insets(0, 5, 0, 5));
        articleBox.setSpacing(5);
        articleBox.getStyleClass().add("article-box");

        articleBox.setOnMouseEntered(event -> articleBox.getStyleClass().add("article-box-hover"));
        articleBox.setOnMouseExited(event -> articleBox.getStyleClass().remove("article-box-hover"));

        ImageView imageView = new ImageView();

        if (article == null) {
            return articleBox;
        }

        if (isHeadArticle) {
            ImageHandler.loadImageAsync(imageView, article.getUrlToImage(), 520, 290);
        } else {
            ImageHandler.loadImageAsync(imageView, article.getUrlToImage(), 250, 140);
        }

        Label titleLabel = new Label(article.getTitle());
        titleLabel.getStyleClass().add("title-label");
        if (isHeadArticle) {
            titleLabel.getStyleClass().add("head-article");
        }
        titleLabel.setWrapText(true);
        titleLabel.setTextAlignment(TextAlignment.JUSTIFY);
        String fullTitle = titleLabel.getText().replaceAll("\\s+", " ");
        titleLabel.setText(fullTitle.length() > 90 ? fullTitle.substring(0, 90) + "..." : fullTitle);

        HBox buBox = new HBox();
        buBox.setAlignment(Pos.BOTTOM_RIGHT);
        buBox.setPadding(new Insets(0, 0, 0, 0));

        Button bookmarkButton = new Button("✩");
        bookmarkButton.getStyleClass().add("bookmark-button");
        bookmarkButton.setOnAction(event -> bookmarkArticle(article, articleBox));
        bookmarkButton.setText(bookmarkedArticles.contains(article) ? "★" : "✩");
        buBox.getChildren().addAll(bookmarkButton);

        articleBox.getChildren().addAll(imageView, titleLabel, buBox);

        articleBox.setOnMouseClicked(event -> {
            webView.getEngine().load(article.getUrl());
            tabPane.getSelectionModel().select(articleTab);
            currentArticle = article;
            titlelabel.setText(currentArticle.getTitle());
        });

        return articleBox;
    }

    // Bookmark articles from the categories tabs
    private synchronized void bookmarkArticle(Article article, VBox articleBox) {
        if (!bookmarkedArticles.contains(article)) {
            bookmarkedArticles.add(article);
            ((Button) ((HBox) articleBox.getChildren().get(2)).getChildren().get(0)).setText("★");
        } else {
            bookmarkedArticles.remove(article);
            ((Button) ((HBox) articleBox.getChildren().get(2)).getChildren().get(0)).setText("✩");
        }
        displayBookmarkedArticles();
    }

    // Creating Bookmark Page of the bookmark tab
    private void displayBookmarkedArticles() {
        VBox bookmarksBox = new VBox();
        bookmarksBox.setPadding(new Insets(10));
        bookmarksBox.setSpacing(5);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(15);
        gridPane.setVgap(15);
        bookmarksBox.getChildren().add(gridPane);
        bookmarkedArticles = new ArrayList<>(bookmarkedArticles.stream()
                .collect(Collectors.toMap(
                        Article::getUrl, // Key mapper
                        article -> article, // Value mapper
                        (existing, replacement) -> existing, // Merge function to handle duplicates
                        LinkedHashMap::new // Supplier of the resulting Map
                ))
                .values());

        for (int i = 0; i < bookmarkedArticles.size(); i++) {
            Article article = bookmarkedArticles.get(i);
            VBox articleBox = createArticleBoxForBookmarksTab(article);
            gridPane.add(articleBox, i % 3, i / 3);
        }

        ScrollPane scrollPane = new ScrollPane(bookmarksBox);
        scrollPane.setFitToWidth(true);
        bookmarksTab.setContent(scrollPane);
    }

    // Article Boxes for bookmark tab to display bookmarked articles
    private VBox createArticleBoxForBookmarksTab(Article article) {
        VBox articleBox = createArticleBox(article, false);
        // Remove the bookmark button
        articleBox.getChildren()
                .removeIf(node -> node instanceof HBox && ((HBox) node).getChildren().stream()
                        .anyMatch(btn -> btn instanceof Button && ((Button) btn).getText().equals("✩")
                                || ((Button) btn).getText().equals("★")));

        // Add delete button
        HBox deleteBox = new HBox();
        deleteBox.setAlignment(Pos.BOTTOM_RIGHT);
        Button deleteButton = new Button("Delete");

        deleteButton.setOnAction(event -> {
            bookmarkedArticles.remove(article);
            displayBookmarkedArticles();
        });
        deleteBox.getChildren().add(deleteButton);

        articleBox.getChildren().add(deleteBox);

        return articleBox;
    }

    // Saving Bookmarked Articles into a file
    public void saveBookmarksToFile() {
        try (FileOutputStream fileOut = new FileOutputStream(BOOKMARKS_FILE_PATH);
                ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(bookmarkedArticles);
            System.out.println("Bookmarks have been serialized and saved.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Loading Bookmarked Articles From the saved file
    @SuppressWarnings("unchecked")
    private void loadBookmarksFromFile() {
        CompletableFuture.runAsync(() -> {
            File file = new File(BOOKMARKS_FILE_PATH);
            if (file.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(BOOKMARKS_FILE_PATH))) {
                    Object readObject = ois.readObject();
                    if (readObject instanceof List) {
                        bookmarkedArticles = (List<Article>) readObject;
                    } else {
                        bookmarkedArticles = new ArrayList<>();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    bookmarkedArticles = new ArrayList<>();
                }
            } else {
                bookmarkedArticles = new ArrayList<>();
            }
        }).thenRun(() -> Platform.runLater(this::displayBookmarkedArticles));
    }

    // Refreshes the news articles displayed in the list view and tabs
    private void refreshNews() {
        statusLabel.setText("Fetching news...");
        progressBar.setVisible(true);
        CompletableFuture<List<Article>> fetchNewsTask = newsService.searchNews("general");
        fetchNewsTask.thenAccept(articles -> {
            Platform.runLater(() -> {
                // Remove invalid articles
                articles.removeIf(article -> article == null || article.getUrl() == null || article.getUrl().isEmpty()
                        || article.getUrlToImage() == null || article.getUrlToImage().isEmpty());
                if (articles.isEmpty()) {
                    statusLabel.setText("No articles fetched. Check your internet connection.");
                } else {
                    statusLabel.setText("");
                }
                progressBar.setVisible(false);
                originalArticles = articles;
                newsListView.getItems().setAll(articles);
                homeTab.setContent(createPage("", "", true));
                loadOtherTabsOnSelection(localTab, "", "ghana");
                loadOtherTabsOnSelection(businessTab, "business", "" );
                loadOtherTabsOnSelection(technologyTab, "technology", "" );
                loadOtherTabsOnSelection(entertainmentTab, "entertainment", "" );
                loadOtherTabsOnSelection(sportsTab, "sports", "" );
                loadOtherTabsOnSelection(healthTab, "health", "" );
                loadOtherTabsOnSelection(scienceTab, "science", "" );
            });
        }).exceptionally(ex -> {
            Platform.runLater(
                    () -> statusLabel.setText("No internet connection. Please connect to the internet and refresh"));
            progressBar.setVisible(false);
            return null;
        });
    }

    // Searches for news based on user input and displays results in the list view
    private void searchNews(String query) {
        statusLabel.setText("Searching news...");
        progressBar.setVisible(true);
        CompletableFuture<List<Article>> searchNewsTask = newsService.searchNews(query);
        searchNewsTask.thenAccept(articles -> {
            Platform.runLater(() -> {
                articles.removeIf(article -> article == null || article.getUrl() == null || article.getUrl().isEmpty());
                if (articles.isEmpty()) {
                    statusLabel.setText("No articles found for your search query.");
                } else {
                    statusLabel.setText("");
                }
                progressBar.setVisible(false);
                newsListView.getItems().setAll(articles);
            });
        }).exceptionally(ex -> {
            Platform.runLater(
                    () -> statusLabel.setText("No internet connection. Please connect to the internet and try again"));
            progressBar.setVisible(false);
            return null;
        });
    }

    // Shows the navigation buttons when article tab is selected
    private void updateNavigationButtonsVisibility(boolean visible) {
        backButton.setVisible(visible);
        forwardButton.setVisible(visible);
        reloadButton.setVisible(visible);
    }

    // Hides or displays preferences window
    @FXML
    public void setPrefsVisibility() {
        mainContainer
                .setVisible(
                        mainContainer.isVisible() ? false : true);
    }

    // Load page when preferences keywords are not empty
    private void loadPrefPage(GridPane gridPane, int retries, String category, List<String> keywords, boolean isHome) {
        List<CompletableFuture<List<Article>>> fetchNewsTasks = new ArrayList<>();
        for (String keyword : keywords) {
            fetchNewsTasks.add(newsService.fetchPrefNews(keyword.trim()));
        }

        statusLabel.setText("Fetching News");
        progressBar.setVisible(true);

        CompletableFuture.allOf(fetchNewsTasks.toArray(new CompletableFuture[0]))
                .thenApply(v -> fetchNewsTasks.stream()
                        .map(task -> {
                            try {
                                return task.get(); // Using get() to handle exceptions
                            } catch (Exception e) {
                                e.printStackTrace(); // Log individual task errors
                                return Collections.<Article>emptyList(); // Return empty list on failure
                            }
                        })
                        .flatMap(List::stream)
                        .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Article::getUrl)))))
                .thenAccept(uniqueCombinedArticles -> {
                    List<Article> sortedFilteredArticles = uniqueCombinedArticles.stream()
                            .filter(article -> article != null && article.getUrl() != null
                                    && !article.getUrl().isEmpty()
                                    && article.getUrlToImage() != null && !article.getUrlToImage().isEmpty())
                            .sorted((a, b) -> b.getpublishedAt().compareTo(a.getpublishedAt()))
                            .collect(Collectors.toList());

                    Platform.runLater(() -> {
                        if (sortedFilteredArticles.isEmpty()) {
                            statusLabel.setText("No articles fetched. Check your internet connection.");
                        } else {
                            statusLabel.setText("");
                        }
                        progressBar.setVisible(false);
                        populateGrid(gridPane, sortedFilteredArticles, isHome);
                    });
                })
                .exceptionally(ex -> {
                    ex.printStackTrace(); // Log the exception
                    if (retries < MAX_RETRIES) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Retrying... (" + (retries + 1) + "/" + MAX_RETRIES + ")");
                            loadPrefPage(gridPane, retries + 1, category, keywords, isHome);
                        });
                    } else {
                        Platform.runLater(
                                () -> statusLabel.setText("Failed to fetch news after " + MAX_RETRIES + " attempts"));
                    }
                    return null;
                });
    }

}