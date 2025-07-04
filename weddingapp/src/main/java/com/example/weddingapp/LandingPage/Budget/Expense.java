package com.example.weddingapp.LandingPage.Budget;

public class Expense {
    private String id; // Firestore document ID
    private String expenseName;
    private int amount;

    public Expense() {
    }

    public Expense(String id, String expenseName, int amount) {
        this.id = id;
        this.expenseName = expenseName;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExpenseName() {
        return expenseName;
    }

    public void setExpenseName(String expenseName) {
        this.expenseName = expenseName;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
