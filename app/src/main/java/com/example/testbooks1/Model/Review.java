package com.example.testbooks1.Model;

public class Review {
    public String userId;
    public String userName;
    public int rating;
    public String comment;
    public long timestamp;

    public Review() {}

    public Review(String userId, String userName, int rating, String comment, long timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }
}
