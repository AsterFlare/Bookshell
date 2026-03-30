package com.example.testbooks1.Model;

import java.util.ArrayList;

public class User {

    public String firstName;
    public String lastName;
    public String email;
    public Stats stats;
    public ArrayList<String> badges;

    public User() {}

    public User(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.stats = new Stats(0,0);

        this.badges = new ArrayList<>();
    }
}