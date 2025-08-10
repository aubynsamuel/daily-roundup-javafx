package com.aubynsamuel.service;

import com.aubynsamuel.model.Article;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BookmarkHandler {
    private List<Article> bookmarkedArticles = new ArrayList<>();
    private static final String BOOKMARKS_DIR_PATH = System.getProperty("user.home") + "/news_aggregator/bookmarks";
    private static final String BOOKMARKS_FILE_PATH = BOOKMARKS_DIR_PATH + "/bookmarked_articles.ser";

    public BookmarkHandler() {
        File bookmarksDir = new File(BOOKMARKS_DIR_PATH);
        if (!bookmarksDir.exists()) {
            bookmarksDir.mkdirs();
        }
        loadBookmarksFromFile();
    }

    public int addToBookmarks(Article currentArticle) {
        if (currentArticle != null && !bookmarkedArticles.contains(currentArticle)) {
            bookmarkedArticles.add(currentArticle);
            return 0;
        } else if (bookmarkedArticles.contains(currentArticle)) {
            return 1;
        } else {
            return -1;
        }
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
    public void loadBookmarksFromFile() {
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
    }

    public List<Article> getBookmarkedArticles() {
        return bookmarkedArticles;
    }

    public void removeBookmark(Article article) {
        bookmarkedArticles.remove(article);
    }
}
