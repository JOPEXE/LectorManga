package com.example.lectormanga.model;

public class Manga {
    private String id;
    private String title;
    private String description;
    private String coverUrl;

    // Constructor vacío
    public Manga() {}

    // Constructor completo
    public Manga(String id, String title, String description, String coverUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.coverUrl = coverUrl;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    // Método toString para debugging
    @Override
    public String toString() {
        return "Manga{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", coverUrl='" + coverUrl + '\'' +
                '}';
    }
}