package com.example.weddingapp.LandingPage.Budget;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weddingapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    private final List<Expense> expenseList;

    public ExpenseAdapter(List<Expense> expenseList) {
        this.expenseList = expenseList;
    }

    @NonNull
    @Override
    public ExpenseAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseAdapter.ViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.tvExpenseName.setText(expense.getExpenseName());
        holder.tvAmount.setText("$" + expense.getAmount());

        holder.btnDelete.setOnClickListener(v -> {
            String userId = FirebaseAuth.getInstance()
                    .getCurrentUser()
                    .getUid();
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("expenses")
                    .document(expense.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        int pos = holder.getAdapterPosition();
                        expenseList.remove(pos);
                        notifyItemRemoved(pos);

                        // If we've removed the last item, close this activity
                        if (expenseList.isEmpty()) {
                            Activity act = (Activity) holder.itemView.getContext();
                            act.finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Optional: show a Toast or log the error
                    });
        });
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvExpenseName, tvAmount;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExpenseName = itemView.findViewById(R.id.tv_expense_name);
            tvAmount      = itemView.findViewById(R.id.tv_amount);
            btnDelete     = itemView.findViewById(R.id.btn_delete);
        }
    }
}
