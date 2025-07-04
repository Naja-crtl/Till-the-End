package com.example.weddingapp.LandingPage.Budget;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weddingapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDetailsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ExpenseAdapter expenseAdapter;
    private List<Expense> expenseList;
    private FirebaseFirestore db;
    private String userId;
    private String category;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make sure the layout file name matches the one you've set up for this activity.
        setContentView(R.layout.activity_expense_details);

        // Get the category passed from the BudgetAdapter.
        category = getIntent().getStringExtra("category");
        setTitle(category + " Expenses");

        recyclerView = findViewById(R.id.recycler_view);
        expenseList = new ArrayList<>();
        expenseAdapter = new ExpenseAdapter(expenseList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(expenseAdapter);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        fetchCategoryExpenses();
    }

    private void fetchCategoryExpenses() {
        db.collection("users")
                .document(userId)
                .collection("expenses")
                .whereEqualTo("category", category)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            expenseList.clear();
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                String id = doc.getId();
                                // Use "description" key instead of "expenseName" for consistency
                                String expenseName = doc.getString("description");
                                Long amountLong = doc.getLong("amount");
                                int amount = (amountLong != null) ? amountLong.intValue() : 0;
                                expenseList.add(new Expense(id, expenseName, amount));
                            }
                            expenseAdapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.e("ExpenseDetails", "Error fetching expenses", task.getException());
                    }
                });
    }
}
