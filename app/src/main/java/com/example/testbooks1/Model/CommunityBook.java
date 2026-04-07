package com.example.testbooks1.Model;

import android.os.Parcel;
import android.os.Parcelable;

public class CommunityBook implements Parcelable {

    public String bookId;
    public String bookTitle;
    public String imageUrl;

    public String title;
    public String image;

    public String author, category, description;
    public String snippet;

    public CommunityBook() {}

    public CommunityBook(String bookId, String title, String author,
                         String image, String category, String description, String snippet) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.image = image;
        this.category = category;
        this.description = description;
        this.snippet = snippet;

        this.bookTitle = title;
        this.imageUrl = image;
    }

    public void normalize() {
        if (title == null && bookTitle != null) {
            title = bookTitle;
        }
        if (image == null && imageUrl != null) {
            image = imageUrl;
        }
    }

    protected CommunityBook(Parcel in) {
        bookId = in.readString();
        title = in.readString();
        author = in.readString();
        image = in.readString();
        category = in.readString();
        description = in.readString();
        snippet = in.readString();
        bookTitle = in.readString();
        imageUrl = in.readString();
    }

    public static final Creator<CommunityBook> CREATOR = new Creator<CommunityBook>() {
        @Override
        public CommunityBook createFromParcel(Parcel in) {
            return new CommunityBook(in);
        }

        @Override
        public CommunityBook[] newArray(int size) {
            return new CommunityBook[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(bookId);
        parcel.writeString(title);
        parcel.writeString(author);
        parcel.writeString(image);
        parcel.writeString(category);
        parcel.writeString(description);
        parcel.writeString(snippet);
        parcel.writeString(bookTitle);
        parcel.writeString(imageUrl);
    }
}