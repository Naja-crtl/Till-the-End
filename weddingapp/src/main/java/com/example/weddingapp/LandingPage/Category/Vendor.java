package com.example.weddingapp.LandingPage.Category;

import java.util.List;

public class Vendor {
    private String id;
    private String name;
    private String category;
    private String contact;
    private String email;
    private String address;
    private String priceRange;
    private double rating;
    private String notes;
    private List<String> imageUrls;
    private double latitude;
    private double longitude;

    public Vendor() {}

    public Vendor(String id, String name, String category, String contact, String email,
                  String address, String priceRange, double rating, String notes,
                  List<String> imageUrls, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.contact = contact;
        this.email = email;
        this.address = address;
        this.priceRange = priceRange;
        this.rating = rating;
        this.notes = notes;
        this.imageUrls = imageUrls;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getContact() { return contact; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public String getPriceRange() { return priceRange; }
    public double getRating() { return rating; }
    public String getNotes() { return notes; }
    public List<String> getImageUrls() { return imageUrls; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setContact(String contact) { this.contact = contact; }
    public void setEmail(String email) { this.email = email; }
    public void setAddress(String address) { this.address = address; }
    public void setPriceRange(String priceRange) { this.priceRange = priceRange; }
    public void setRating(double rating) { this.rating = rating; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
