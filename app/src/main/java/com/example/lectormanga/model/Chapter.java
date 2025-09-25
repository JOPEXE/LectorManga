package com.example.lectormanga.model;

public class Chapter {
    private String id;
    private String title;
    private String chapterNumber;
    private String pages;
    private String publishedAt;
    private String mangaId;

    // Constructor vacío
    public Chapter() {}

    // Constructor completo
    public Chapter(String id, String title, String chapterNumber, String pages, String publishedAt, String mangaId) {
        this.id = id;
        this.title = title;
        this.chapterNumber = chapterNumber;
        this.pages = pages;
        this.publishedAt = publishedAt;
        this.mangaId = mangaId;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(String chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        this.pages = pages;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getMangaId() {
        return mangaId;
    }

    public void setMangaId(String mangaId) {
        this.mangaId = mangaId;
    }

    @Override
    public String toString() {
        return "Chapter{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", chapterNumber='" + chapterNumber + '\'' +
                ", pages='" + pages + '\'' +
                '}';
    }
}