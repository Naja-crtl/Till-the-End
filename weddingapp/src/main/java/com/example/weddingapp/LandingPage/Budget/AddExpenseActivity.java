package com.example.weddingapp.LandingPage.Budget;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.weddingapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AddExpenseActivity extends AppCompatActivity {

    private EditText etExpenseDescription, etAmount;
    private Button btnSaveExpense;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String userId;
    private HuggingFaceClassifierHelper hfClassifierHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        userId = firebaseAuth.getCurrentUser().getUid();

        // Initialize the HuggingFace classifier helper
        hfClassifierHelper = new HuggingFaceClassifierHelper();

        // Find UI elements
        etExpenseDescription = findViewById(R.id.et_expense_description);
        etAmount = findViewById(R.id.et_amount);
        btnSaveExpense = findViewById(R.id.btn_save_expense);

        // Set up the Save button click listener
        btnSaveExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExpense();
            }
        });
    }

    private void saveExpense() {
        final String expenseDescription = etExpenseDescription.getText().toString().trim();
        final String amountStr = etAmount.getText().toString().trim();

        if (expenseDescription.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter description and amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use HuggingFace's API to classify the expense description.
        hfClassifierHelper.classifyExpense(expenseDescription, new HuggingFaceClassifierHelper.ClassificationCallback() {
            @Override
            public void onSuccess(String category) {
                int amount = Integer.parseInt(amountStr);
                Map<String, Object> expense = new HashMap<>();
                expense.put("description", expenseDescription);
                expense.put("category", category);
                expense.put("amount", amount);

                firestore.collection("users").document(userId).collection("expenses").add(expense)
                        .addOnSuccessListener(documentReference -> runOnUiThread(() -> {
                            Toast.makeText(AddExpenseActivity.this, "Expense Added! Category: " + category, Toast.LENGTH_SHORT).show();
                            finish();
                        }))
                        .addOnFailureListener(e -> runOnUiThread(() ->
                                Toast.makeText(AddExpenseActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(AddExpenseActivity.this, "Classification Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
}
