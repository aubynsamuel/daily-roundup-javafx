package com.aubynsamuel.ui.controller;

import com.aubynsamuel.model.Article;
import com.aubynsamuel.service.BookmarkHandler;
import com.aubynsamuel.service.ImageHandler;
import com.aubynsamuel.service.NewsHandler;
import com.aubynsamuel.service.PreferencesHandler;
import com.aubynsamuel.ui.util.UIHandler;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.scene.text.TextAlignment;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;

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
    ImageHandler imageHandler;

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
        newsHandler = new NewsHandler();
        bookmarkHandler = new BookmarkHandler();
        uiHandler = new UIHandler(stage);
        imageHandler = new ImageHandler();

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
                    searchNews(newValue);
                }
            });
            pause.playFromStart();
        });

        refreshButton.setOnAction(event -> refreshNews());
        backButton.setOnAction(event -> webView.getEngine().executeScript("history.back()"));
        forwardButton.setOnAction(event -> webView.getEngine().executeScript("history.forward()"));
        reloadButton.setOnAction(event -> webView.getEngine().reload());

        refreshNews();

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
                displayBookmarkedArticles(bookmarksTab);
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
        int result = bookmarkHandler.addToBookmarks(currentArticle);
        if (result == 0) {
            UIHandler.alertBuilder("Article Added to Bookmarks", "The article has been added to your bookmarks.");
        } else if (result == 1) {
            UIHandler.alertBuilder("Article Already in Bookmarks", "The article is already in your bookmarks");
        } else {
            UIHandler.alertBuilder("No Article Selected",
                    "Please Click on an Article Before Clicking 'Bookmark Article'");
        }
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

    public void displayBookmarkedArticles(Tab bookmarksTab) {
        VBox bookmarksBox = new VBox();
        bookmarksBox.setPadding(new Insets(10));
        bookmarksBox.setSpacing(5);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(15);
        gridPane.setVgap(15);
        bookmarksBox.getChildren().add(gridPane);
        List<Article> bookmarkedArticles = new ArrayList<>(bookmarkHandler.getBookmarkedArticles().stream()
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

    private VBox createArticleBoxForBookmarksTab(Article article) {
        VBox articleBox = createArticleBox(article, false);
        articleBox.getChildren()
                .removeIf(node -> node instanceof HBox && ((HBox) node).getChildren().stream()
                        .anyMatch(btn -> btn instanceof Button && (((Button) btn).getText().equals("✩")
                                || ((Button) btn).getText().equals("★"))));

        HBox deleteBox = new HBox();
        deleteBox.setAlignment(Pos.BOTTOM_RIGHT);
        Button deleteButton = new Button("Delete");

        deleteButton.setOnAction(event -> {
            bookmarkHandler.removeBookmark(article);
            displayBookmarkedArticles(bookmarksTab);
        });
        deleteBox.getChildren().add(deleteButton);

        articleBox.getChildren().add(deleteBox);

        return articleBox;
    }

    private void refreshNews() {
        statusLabel.setText("Fetching news...");
        progressBar.setVisible(true);
        newsHandler.refreshNews().thenAccept(articles -> {
            Platform.runLater(() -> {
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
                loadOtherTabsOnSelection(businessTab, "business", "");
                loadOtherTabsOnSelection(technologyTab, "technology", "");
                loadOtherTabsOnSelection(entertainmentTab, "entertainment", "");
                loadOtherTabsOnSelection(sportsTab, "sports", "");
                loadOtherTabsOnSelection(healthTab, "health", "");
                loadOtherTabsOnSelection(scienceTab, "science", "");
            });
        }).exceptionally(ex -> {
            Platform.runLater(
                    () -> statusLabel.setText("No internet connection. Please connect to the internet and refresh"));
            progressBar.setVisible(false);
            return null;
        });
    }

    private void searchNews(String query) {
        statusLabel.setText("Searching news...");
        progressBar.setVisible(true);
        newsHandler.searchNews(query).thenAccept(articles -> {
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

    private void loadOtherTabsOnSelection(Tab tab, String category, String keywords) {
        tab.setOnSelectionChanged(event -> {
            if (tab.isSelected()) {
                tab.setContent(createPage(category, keywords, false));
            }
        });
    }

    public ScrollPane createPage(String category, String keywords, boolean isHome) {
        VBox page = new VBox();
        page.setPadding(new Insets(10));
        page.setSpacing(5);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        page.getChildren().add(gridPane);

        if (!preferencesHandler.getPrefKeywords().isEmpty() && isHome)
            loadPrefPage(gridPane, 0, category, preferencesHandler.getPrefKeywords(), isHome);
        else
            loadPage(gridPane, 0, category, keywords, isHome);

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private void loadPage(GridPane gridPane, int retries, String category, String keywords, boolean isHome) {
        CompletableFuture<List<Article>> fetchNewsTask;
        if (isHome) {
            fetchNewsTask = keywords.equals("") ? newsHandler.fetchNews(category, keywords)
                    : newsHandler.searchNews(keywords);
            statusLabel.setText("Fectching News");
        } else {
            fetchNewsTask = keywords.equals("") ? newsHandler.fetchNews(category, keywords)
                    : newsHandler.searchNews(keywords);
            statusLabel.setText("Loading " + category + " news");
        }
        progressBar.setVisible(true);

        fetchNewsTask.thenAccept(articles -> {
            Platform.runLater(() -> {
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
            if (retries < 3) {
                Platform.runLater(() -> {
                    statusLabel.setText("Retrying... (" + (retries + 1) + "/" + 3 + ")");
                    loadPage(gridPane, retries + 1, category, keywords, isHome);
                });
            } else {
                Platform.runLater(() -> statusLabel
                        .setText("Failed to fetch news after " + 3 + " attempts"));
            }
            return null;
        });
    }

    private void populateGrid(GridPane gridPane, List<Article> articles, boolean isHomeTab) {
        gridPane.getChildren().clear();
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
            imageHandler.loadImageAsync(imageView, article.getUrlToImage(), 520, 290);
        } else {
            imageHandler.loadImageAsync(imageView, article.getUrlToImage(), 250, 140);
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
        bookmarkButton.setText(bookmarkHandler.getBookmarkedArticles().contains(article) ? "★" : "✩");
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

    private synchronized void bookmarkArticle(Article article, VBox articleBox) {
        if (!bookmarkHandler.getBookmarkedArticles().contains(article)) {
            bookmarkHandler.addToBookmarks(article);
            ((Button) ((HBox) articleBox.getChildren().get(2)).getChildren().get(0)).setText("★");
        } else {
            bookmarkHandler.getBookmarkedArticles().remove(article);
            ((Button) ((HBox) articleBox.getChildren().get(2)).getChildren().get(0)).setText("✩");
        }
        displayBookmarkedArticles(bookmarksTab);
    }

    private void loadPrefPage(GridPane gridPane, int retries, String category, List<String> keywords, boolean isHome) {
        List<CompletableFuture<List<Article>>> fetchNewsTasks = new ArrayList<>();
        for (String keyword : keywords) {
            fetchNewsTasks.add(newsHandler.fetchPrefNews(keyword.trim()));
        }

        statusLabel.setText("Fetching News");
        progressBar.setVisible(true);

        CompletableFuture.allOf(fetchNewsTasks.toArray(new CompletableFuture[0]))
                .thenApply(v -> fetchNewsTasks.stream()
                        .map(task -> {
                            try {
                                return task.get();
                            } catch (Exception e) {
                                e.printStackTrace();
                                return Collections.<Article>emptyList();
                            }
                        })
                        .flatMap(list -> list.stream())
                        .filter(obj -> obj instanceof Article)
                        .map(obj -> (Article) obj)
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
                    ex.printStackTrace();
                    if (retries < 3) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Retrying... (" + (retries + 1) + "/" + 3 + ")");
                            loadPrefPage(gridPane, retries + 1, category, keywords, isHome);
                        });
                    } else {
                        Platform.runLater(() -> statusLabel.setText("Failed to fetch news after " + 3 + " attempts"));
                    }
                    return null;
                });
    }
}