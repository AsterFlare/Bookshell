package com.example.testbooks1.Model;

public class UserBook {
    public String userId;
    public String bookTitle;
    public String status;
    public String imageUrl;
    public long timestamp;
    public String author;
    public String description;
    public String publisher;
    public String category;
    public String snippet;

    public UserBook() {}

    public UserBook(String userId, String bookTitle, String status, String imageUrl,
                    long timestamp, String author, String description,
                    String publisher, String category, String snippet) {
        this.userId = userId;
        this.bookTitle = bookTitle;
        this.status = status;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.author = author;
        this.description = description;
        this.publisher = publisher;
        this.category = category;
        this.snippet = snippet;
    }
}