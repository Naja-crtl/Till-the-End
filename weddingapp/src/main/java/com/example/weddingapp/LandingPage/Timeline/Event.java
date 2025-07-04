package com.example.weddingapp.LandingPage.Timeline;

public class Event {
    private String date;
    private String description;
    private String time;

    // Empty constructor required for Firestore
    public Event() {}

    public Event(String date, String description, String time) {
        this.date = date;
        this.description = description;
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getTime() {
        return time;
    }
}