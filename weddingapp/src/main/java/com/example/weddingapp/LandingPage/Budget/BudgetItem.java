package com.example.weddingapp.LandingPage.Budget;

public class BudgetItem {
    private String category;
    private int amount;

    public BudgetItem() { }

    public BudgetItem(String category, int amount) {
        this.category = category;
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public int getAmount() {
        return amount;
    }
}
