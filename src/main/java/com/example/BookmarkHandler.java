package com.example;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BookmarkHandler {
    private List<Article> bookmarkedArticles = new ArrayList<>();
    private static final String BOOKMARKS_DIR_PATH = System.getProperty("user.home") + "/news_aggregator/bookmarks";
    private static final String BOOKMARKS_FILE_PATH = BOOKMARKS_DIR_PATH + "/bookmarked_articles.ser";
    private final NewsHandler newsHandler;

    public BookmarkHandler(NewsHandler newsHandler) {
        this.newsHandler = newsHandler;
        File bookmarksDir = new File(BOOKMARKS_DIR_PATH);
        if (!bookmarksDir.exists()) {
            bookmarksDir.mkdirs();
        }
        loadBookmarksFromFile();
    }

    public void addToBookmarks(Article currentArticle) {
        if (currentArticle != null && !bookmarkedArticles.contains(currentArticle)) {
            bookmarkedArticles.add(currentArticle);
            UIHandler.alertBuilder("Article Added to Bookmarks", "The article has been added to your bookmarks.");
        } else if (bookmarkedArticles.contains(currentArticle)) {
            UIHandler.alertBuilder("Article Already in Bookmarks", "The article is already in your bookmarks");
        } else {
            UIHandler.alertBuilder("No Article Selected", "Please Click on an Article Before Clicking 'Bookmark Article'");
        }
    }

    public void displayBookmarkedArticles(Tab bookmarksTab) {
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

    private VBox createArticleBoxForBookmarksTab(Article article) {
        VBox articleBox = newsHandler.createArticleBox(article, false);
        articleBox.getChildren()
                .removeIf(node -> node instanceof HBox && ((HBox) node).getChildren().stream()
                        .anyMatch(btn -> btn instanceof Button && (((Button) btn).getText().equals("✩")
                                || ((Button) btn).getText().equals("★"))));

        HBox deleteBox = new HBox();
        deleteBox.setAlignment(Pos.BOTTOM_RIGHT);
        Button deleteButton = new Button("Delete");

        deleteButton.setOnAction(event -> {
            bookmarkedArticles.remove(article);
            displayBookmarkedArticles(newsHandler.getController().bookmarksTab);
        });
        deleteBox.getChildren().add(deleteButton);

        articleBox.getChildren().add(deleteBox);

        return articleBox;
    }

    public void saveBookmarksToFile() {
        try (FileOutputStream fileOut = new FileOutputStream(BOOKMARKS_FILE_PATH);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(bookmarkedArticles);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadBookmarksFromFile() {
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
        Platform.runLater(() -> displayBookmarkedArticles(newsHandler.getController().bookmarksTab));
    }

    public List<Article> getBookmarkedArticles() {
        return bookmarkedArticles;
    }
}
