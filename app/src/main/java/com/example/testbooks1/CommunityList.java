package com.example.testbooks1;

import java.util.ArrayList;

public class CommunityList {
    public String listTitle;
    public ArrayList<CommunityBook> books;

    public CommunityList() { }

    public CommunityList(String listTitle, ArrayList<CommunityBook> books) {
        this.listTitle = listTitle;
        this.books = books;
    }
}