package com.example.weddingapp.LandingPage.Budget;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weddingapp.R;

import java.util.List;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.ViewHolder> {

    private final List<BudgetItem> budgetItems;
    private int totalBudget;

    public BudgetAdapter(List<BudgetItem> budgetItems, int totalBudget) {
        this.budgetItems = budgetItems;
        // compute grand total from items
        this.totalBudget = 0;
        for (BudgetItem bi : budgetItems) {
            this.totalBudget += bi.getAmount();
        }
    }

    public void setTotalBudget(int totalBudget) {
        // recompute in case items changed
        this.totalBudget = 0;
        for (BudgetItem bi : budgetItems) {
            this.totalBudget += bi.getAmount();
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BudgetItem item = budgetItems.get(position);
        holder.tvCategory.setText(item.getCategory());
        holder.tvAmount.setText("$" + item.getAmount() + " spent");

        int progress = totalBudget > 0
                ? (int) ((item.getAmount() / (float) totalBudget) * 100)
                : 0;
        holder.progressBudget.setProgress(progress);

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, ExpenseDetailsActivity.class);
            intent.putExtra("category", item.getCategory());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return budgetItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCategory, tvAmount;
        final ProgressBar progressBudget;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            progressBudget = itemView.findViewById(R.id.progress_budget);
        }
    }
}