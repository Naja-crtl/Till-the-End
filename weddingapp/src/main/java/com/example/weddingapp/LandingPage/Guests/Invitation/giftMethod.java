package com.example.weddingapp.LandingPage.Guests.Invitation;

import com.google.firebase.Timestamp;

import java.util.Map;

/**
 * Model class representing a gift method entry.
 */
public class giftMethod {
    private String type;
    private Map<String, Object> details;
    private int order;
    private Timestamp createdAt;

    // No-argument constructor required for Firestore
    public giftMethod() {}

    public giftMethod(String type, Map<String, Object> details, int order, Timestamp createdAt) {
        this.type = type;
        this.details = details;
        this.order = order;
        this.createdAt = createdAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
