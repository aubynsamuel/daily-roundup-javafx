package com.example;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * This class provides methods for fetching news articles from the News API.
 * It uses multiple API keys and handles basic error scenarios.
 * It also implements a cache to store fetched articles for faster retrieval.
 */
public class NewsService {
    private static final Logger logger = LoggerFactory.getLogger(NewsService.class);

    private static final String[] API_KEYS = {
            "e83e808f55ce4c62a8afe1c33d13d28b",
            "8126ae9100ff429ebec68fbf3fc4cd4d",
            "3065a421504844f18f7091cafe9c42c7"
    };
    private static int currentKeyIndex = 0;

    // URLs for searching and fetching news articles using the News API

    // HTTP client for making requests to the News API
    private final CloseableHttpClient httpClient;

    // Cache to store fetched articles
    private final Cache<String, List<Article>> articleCache;

    public NewsService() {
        this.httpClient = HttpClients.createDefault();
        // Create a cache with a maximum size of 300 entries and an expiration time of
        // 30 minutes
        this.articleCache = CacheBuilder.newBuilder()
                .maximumSize(300)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }

    // Fetches news articles for a given category from the News API
    public CompletableFuture<List<Article>> fetchNews(String category, String keywords) {
        String CAT_API_URL = "https://newsapi.org/v2/top-headlines?category=%s&q=%s&language=en&apiKey="
                + API_KEYS[currentKeyIndex]
                + "&pageSize=45&page=1&sortBy=publishedAt";
        String encodedKeywords = URLEncoder.encode(keywords, StandardCharsets.UTF_8);
        String url = String.format(CAT_API_URL, category, encodedKeywords);
        return CompletableFuture.supplyAsync(() -> fetchArticlesFromCacheOrApi(url));
    }

    public CompletableFuture<List<Article>> fetchPrefNews(String query) {
        String SEARCH_PREF_API_URL = "https://newsapi.org/v2/everything?q=%s&language=en&apiKey=%s&pageSize=9&page=1&sortBy=publishedAt";
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String apiUrl = String.format(SEARCH_PREF_API_URL, encodedQuery, API_KEYS[currentKeyIndex]);
        return CompletableFuture.supplyAsync(() -> fetchArticlesFromCacheOrApi(apiUrl));
    }

    // Searches for news articles based on a given query using the News API
    public CompletableFuture<List<Article>> searchNews(String query) {
        String SEARCH_API_URL = "https://newsapi.org/v2/everything?q=%s&language=en&apiKey=%s&pageSize=30&page=1&sortBy=publishedAt";
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String apiUrl = String.format(SEARCH_API_URL, encodedQuery, API_KEYS[currentKeyIndex]);
        return CompletableFuture.supplyAsync(() -> fetchArticlesFromCacheOrApi(apiUrl));
    }

    // Fetches news articles from the cache or the API, depending on availability
    private List<Article> fetchArticlesFromCacheOrApi(String apiUrl) {
        // Check if the articles are already in the cache
        List<Article> cachedArticles = articleCache.getIfPresent(apiUrl);
        if (cachedArticles != null) {
            logger.info("Retrieved articles from cache for URL: {}", apiUrl);
            return cachedArticles;
        } else {
            // Fetch articles from the API and store them in the cache
            List<Article> articles = fetchArticlesFromApi(apiUrl);
            articleCache.put(apiUrl, articles);
            logger.info("Fetched articles from API and stored in cache for URL: {}", apiUrl);
            return articles;
        }
    }

    // Fetches news articles from the News API using the provided URL
    private List<Article> fetchArticlesFromApi(String apiUrl) {
        List<Article> newsArticles = new ArrayList<>();
        boolean retry;
        int attempts = 0;

        do {
            retry = false;
            try {
                HttpGet request = new HttpGet(apiUrl);
                request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getCode();
                    logger.info("Response status code: {}", statusCode);

                    if (statusCode == 200) {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root = mapper.readTree(response.getEntity().getContent());

                        // Logging response for debugging
                        // logger.debug("API Response: {}", root.toString());

                        root.path("articles").forEach(article -> {
                            String title = article.path("title").asText();
                            String url = article.path("url").asText();
                            String urlToImage = article.path("urlToImage").asText(null);
                            String description = article.path("description").asText();
                            String publishedAt = article.path("publishedAt").asText();

                            // Only add articles with images
                            if (urlToImage != null && !urlToImage.isEmpty()) {
                                newsArticles.add(new Article(title, description, url, urlToImage, publishedAt));
                            }
                        });
                    } else if (statusCode == 429) {
                        logger.error("Rate limit exceeded. Trying a new API key.");
                        attempts++;
                        if (attempts < API_KEYS.length) {
                            currentKeyIndex = (currentKeyIndex + 1) % API_KEYS.length;
                            apiUrl = apiUrl.replaceFirst("apiKey=[^&]+", "apiKey=" + API_KEYS[currentKeyIndex]);
                            retry = true;
                        } else {
                            throw new RuntimeException("All API keys have reached their rate limits.");
                        }
                    } else {
                        logger.error("Failed to fetch articles. HTTP status code: {}", statusCode);
                    }
                }
            } catch (UnknownHostException e) {
                logger.error("No internet connection. Please connect to the internet and refresh", e);
                throw new RuntimeException(e);
            } catch (IOException e) {
                logger.error("Error fetching articles: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        } while (retry);

        return newsArticles;
    }
}
