package com.example;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**Loads image using their URLs, stores them in cache and clears them after a certain time period */
public class ImageHandler {
    private static final Map<String, CachedImage> imageCache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRATION_TIME = 1800000; // 30 minutes in milliseconds
    private static final ScheduledExecutorService cacheClearScheduler = Executors.newSingleThreadScheduledExecutor();

    static {
        // Schedule cache clearing every 30 minutes
        cacheClearScheduler.scheduleAtFixedRate(ImageHandler::clearExpiredCacheEntries, 30, 30,
                TimeUnit.MINUTES);
    }

    private static class CachedImage {
        final Image image;
        final long timestamp;

        CachedImage(Image image) {
            this.image = image;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static void loadImageAsync(ImageView imageView, String imageUrl, double width, double height) {

        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Check if the image is already in the cache and loads the image asynchronously
            if (imageCache.containsKey(imageUrl)) {
                Platform.runLater(() -> {
                    imageView.setImage(imageCache.get(imageUrl).image);
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);
                    imageView.setFitWidth(width);
                    imageView.setFitHeight(height);
                });
                return;
            }

            CompletableFuture.runAsync(() -> {
                Image image = new Image(imageUrl, width * 1.4, height * 1.4, true, true);

                // Store the image in the cache
                imageCache.put(imageUrl, new CachedImage(image));

                Platform.runLater(() -> {
                    // Update the ImageView with the loaded image
                    imageView.setImage(image);
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);
                    imageView.setFitWidth(width);
                    imageView.setFitHeight(height);
                });
            });
        }
    }

    private static void clearExpiredCacheEntries() {
        long currentTime = System.currentTimeMillis();
        imageCache.entrySet().removeIf(entry -> currentTime - entry.getValue().timestamp > CACHE_EXPIRATION_TIME);
    }
}
