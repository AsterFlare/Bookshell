package com.example.testbooks1.Model;

public class Book {
    String id;
    String title;
    String imageUrl;
    String author;
    String description;
    String publisher;

    public Book(String id, String title, String imageUrl, String author, String description, String publisher) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.author = author;
        this.description = description;
        this.publisher = publisher;
    }


    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public String getPublisher() {
        return publisher;
    }
}