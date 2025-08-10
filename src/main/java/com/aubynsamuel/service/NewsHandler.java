package com.aubynsamuel.service;

import com.aubynsamuel.model.Article;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NewsHandler {
    private final NewsService newsService = new NewsService();

    public CompletableFuture<List<Article>> refreshNews() {
        return newsService.searchNews("general");
    }

    public CompletableFuture<List<Article>> searchNews(String query) {
        return newsService.searchNews(query);
    }

    public CompletableFuture<List<Article>> fetchNews(String category, String keywords) {
        return newsService.fetchNews(category, keywords);
    }

    public CompletableFuture<List<Article>> fetchPrefNews(String query) {
        return newsService.fetchPrefNews(query);
    }
}
