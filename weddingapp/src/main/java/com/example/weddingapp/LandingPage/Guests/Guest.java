package com.example.weddingapp.LandingPage.Guests;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Guest {
    private String name;
    private boolean attending;
    private String message;
    private String team;
    @ServerTimestamp
    private Date createdAt;

    // No-arg constructor for Firestore
    public Guest() { }

    public Guest(String name, boolean attending, String message, String team) {
        this.name = name;
        this.attending = attending;
        this.message = message;
        this.team = team;

    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isAttending() { return attending; }
    public void setAttending(boolean attending) { this.attending = attending; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        String status = attending ? "Attending" : "Not attending";
        return name + " - " + status + (message != null ? " (" + message + ")" : "");
    }
}