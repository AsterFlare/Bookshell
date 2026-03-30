package com.example.testbooks1.Model;

public class UserBook {
    public String userId;
    public String bookTitle;
    public String status; // "Want to Read", "Currently Reading", "Completed"
    public String imageUrl;
    public long timestamp;

    public UserBook() {}

    public UserBook(String userId, String bookTitle, String status, String imageUrl, long timestamp) {
        this.userId = userId;
        this.bookTitle = bookTitle;
        this.status = status;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }
}