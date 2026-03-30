package com.example.testbooks1;

public class CommunityBook {
    public String userId;
    public String bookTitle;
    public String listTitle;
    public String image;
    public long timestamp;

    public CommunityBook() {}

    public CommunityBook(String userId, String bookTitle, String listTitle, String image, long timestamp) {
        this.userId = userId;
        this.bookTitle = bookTitle;
        this.listTitle = listTitle;
        this.image = image;
        this.timestamp = timestamp;
    }
}