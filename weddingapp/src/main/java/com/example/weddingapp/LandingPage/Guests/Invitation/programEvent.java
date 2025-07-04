package com.example.weddingapp.LandingPage.Guests.Invitation;

public class programEvent {
    private String time;
    private String title;
    private String message;
    // + serverTimestamp, order, etc.

    public programEvent() {}  // required for Firestore

    public String getTime()    { return time;    }
    public String getTitle()   { return title;   }
    public String getMessage() { return message; }

    public void setTime(String t)      { time = t; }
    public void setTitle(String t)     { title = t; }
    public void setMessage(String m)   { message = m; }
}
