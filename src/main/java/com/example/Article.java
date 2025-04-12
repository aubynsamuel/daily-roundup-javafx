package com.example;

import java.io.Serializable;

/**Creates the structure and fields for news articles and also makes them 
 * serializable so that they can be saved and loaded into and from a file*/
public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private String description;
    private String url;
    private String urlToImage;
    private String publishedAt;

    // Constructors, getters, and setters
    public Article() {
    }

    public Article(String title, String description, String url, String urlToImage, String publishedAt) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.urlToImage = urlToImage;
        this.publishedAt = publishedAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrlToImage() {
        return urlToImage;
    }

    public void setUrlToImage(String urlToImage) {
        this.urlToImage = urlToImage;
    }

    public String getpublishedAt() {
        return publishedAt;
    }

    public void setpublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    @Override
    public String toString() {
        return title;
    }
}