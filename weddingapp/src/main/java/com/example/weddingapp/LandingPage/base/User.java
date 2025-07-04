package com.example.weddingapp.LandingPage.base;

public class User {
    private String name;
    private String email;
    private String imageUrl; // URL for the user's profile image

    // Default constructor is required for Firebase data mapping.
    public User() { }

    public User(String name, String email, String imageUrl) {
        this.name = name;
        this.email = email;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
