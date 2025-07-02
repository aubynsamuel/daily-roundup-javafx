package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class NewsHandler {
    private final NewsService newsService = new NewsService();
    private final NewsAggregatorController controller;
    private static final int MAX_RETRIES = 3;

    public NewsHandler(NewsAggregatorController controller) {
        this.controller = controller;
    }

    public void refreshNews() {
        controller.statusLabel.setText("Fetching news...");
        controller.progressBar.setVisible(true);
        CompletableFuture<List<Article>> fetchNewsTask = newsService.searchNews("general");
        fetchNewsTask.thenAccept(articles -> {
            Platform.runLater(() -> {
                articles.removeIf(article -> article == null || article.getUrl() == null || article.getUrl().isEmpty()
                        || article.getUrlToImage() == null || article.getUrlToImage().isEmpty());
                if (articles.isEmpty()) {
                    controller.statusLabel.setText("No articles fetched. Check your internet connection.");
                } else {
                    controller.statusLabel.setText("");
                }
                controller.progressBar.setVisible(false);
                controller.originalArticles = articles;
                controller.newsListView.getItems().setAll(articles);
                controller.homeTab.setContent(createPage("", "", true));
                loadOtherTabsOnSelection(controller.localTab, "", "ghana");
                loadOtherTabsOnSelection(controller.businessTab, "business", "");
                loadOtherTabsOnSelection(controller.technologyTab, "technology", "");
                loadOtherTabsOnSelection(controller.entertainmentTab, "entertainment", "");
                loadOtherTabsOnSelection(controller.sportsTab, "sports", "");
                loadOtherTabsOnSelection(controller.healthTab, "health", "");
                loadOtherTabsOnSelection(controller.scienceTab, "science", "");
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> controller.statusLabel.setText("No internet connection. Please connect to the internet and refresh"));
            controller.progressBar.setVisible(false);
            return null;
        });
    }

    public void searchNews(String query) {
        controller.statusLabel.setText("Searching news...");
        controller.progressBar.setVisible(true);
        CompletableFuture<List<Article>> searchNewsTask = newsService.searchNews(query);
        searchNewsTask.thenAccept(articles -> {
            Platform.runLater(() -> {
                articles.removeIf(article -> article == null || article.getUrl() == null || article.getUrl().isEmpty());
                if (articles.isEmpty()) {
                    controller.statusLabel.setText("No articles found for your search query.");
                } else {
                    controller.statusLabel.setText("");
                }
                controller.progressBar.setVisible(false);
                controller.newsListView.getItems().setAll(articles);
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> controller.statusLabel.setText("No internet connection. Please connect to the internet and try again"));
            controller.progressBar.setVisible(false);
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

        if (!controller.preferencesHandler.getPrefKeywords().isEmpty() && isHome)
            loadPrefPage(gridPane, 0, category, controller.preferencesHandler.getPrefKeywords(), isHome);
        else
            loadPage(gridPane, 0, category, keywords, isHome);

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private void loadPage(GridPane gridPane, int retries, String category, String keywords, boolean isHome) {
        CompletableFuture<List<Article>> fetchNewsTask;
        if (isHome) {
            fetchNewsTask = keywords.equals("") ? newsService.fetchNews(category, keywords)
                    : newsService.searchNews(keywords);
            controller.statusLabel.setText("Fectching News");
        } else {
            fetchNewsTask = keywords.equals("") ? newsService.fetchNews(category, keywords)
                    : newsService.searchNews(keywords);
            controller.statusLabel.setText("Loading " + category + " news");
        }
        controller.progressBar.setVisible(true);

        fetchNewsTask.thenAccept(articles -> {
            Platform.runLater(() -> {
                articles.removeIf(article -> article == null || article.getUrl() == null || article.getUrl().isEmpty()
                        || article.getUrlToImage() == null || article.getUrlToImage().isEmpty());
                if (articles.isEmpty()) {
                    controller.statusLabel.setText("No articles fetched. Check your internet connection.");
                } else {
                    controller.statusLabel.setText("");
                }
                controller.progressBar.setVisible(false);
                populateGrid(gridPane, articles, isHome);
            });
        }).exceptionally(ex -> {
            if (retries < MAX_RETRIES) {
                Platform.runLater(() -> {
                    controller.statusLabel.setText("Retrying... (" + (retries + 1) + "/" + MAX_RETRIES + ")");
                    loadPage(gridPane, retries + 1, category, keywords, isHome);
                });
            } else {
                Platform.runLater(() -> controller.statusLabel
                        .setText("Failed to fetch news after " + MAX_RETRIES + " attempts"));
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
        bookmarkButton.setText(controller.bookmarkHandler.getBookmarkedArticles().contains(article) ? "★" : "✩");
        buBox.getChildren().addAll(bookmarkButton);

        articleBox.getChildren().addAll(imageView, titleLabel, buBox);

        articleBox.setOnMouseClicked(event -> {
            controller.webView.getEngine().load(article.getUrl());
            controller.tabPane.getSelectionModel().select(controller.articleTab);
            controller.currentArticle = article;
            controller.titlelabel.setText(controller.currentArticle.getTitle());
        });

        return articleBox;
    }

    private synchronized void bookmarkArticle(Article article, VBox articleBox) {
        if (!controller.bookmarkHandler.getBookmarkedArticles().contains(article)) {
            controller.bookmarkHandler.addToBookmarks(article);
            ((Button) ((HBox) articleBox.getChildren().get(2)).getChildren().get(0)).setText("★");
        } else {
            controller.bookmarkHandler.getBookmarkedArticles().remove(article);
            ((Button) ((HBox) articleBox.getChildren().get(2)).getChildren().get(0)).setText("✩");
        }
        controller.bookmarkHandler.displayBookmarkedArticles(controller.bookmarksTab);
    }

    private void loadPrefPage(GridPane gridPane, int retries, String category, List<String> keywords, boolean isHome) {
        List<CompletableFuture<List<Article>>> fetchNewsTasks = new ArrayList<>();
        for (String keyword : keywords) {
            fetchNewsTasks.add(newsService.fetchPrefNews(keyword.trim()));
        }

        controller.statusLabel.setText("Fetching News");
        controller.progressBar.setVisible(true);

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
                            controller.statusLabel.setText("No articles fetched. Check your internet connection.");
                        } else {
                            controller.statusLabel.setText("");
                        }
                        controller.progressBar.setVisible(false);
                        populateGrid(gridPane, sortedFilteredArticles, isHome);
                    });
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    if (retries < MAX_RETRIES) {
                        Platform.runLater(() -> {
                            controller.statusLabel.setText("Retrying... (" + (retries + 1) + "/" + MAX_RETRIES + ")");
                            loadPrefPage(gridPane, retries + 1, category, keywords, isHome);
                        });
                    } else {
                        Platform.runLater(() -> controller.statusLabel.setText("Failed to fetch news after " + MAX_RETRIES + " attempts"));
                    }
                    return null;
                });
    }

    public NewsAggregatorController getController() {
        return controller;
    }
}