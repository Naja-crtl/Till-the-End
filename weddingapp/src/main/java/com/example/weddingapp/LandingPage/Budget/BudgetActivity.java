package com.example.weddingapp.LandingPage.Budget;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.weddingapp.LandingPage.Category.CategoriesActivity;
import com.example.weddingapp.LandingPage.Guests.GuestsActivity;
import com.example.weddingapp.LandingPage.Timeline.EventTimelineActivity;
import com.example.weddingapp.LandingPage.base.dashboard;
import com.example.weddingapp.R;
import com.example.weddingapp.auth.signin_page;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "BudgetActivity";

    private TextView tvTotalBudget, tvRemainingBudget;
    private RecyclerView rvBudgetList;
    private FloatingActionButton btnAddExpense;
    private BudgetAdapter budgetAdapter;
    private final List<BudgetItem> budgetItems = new ArrayList<>();

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String userId;

    private int totalBudget = 0;
    private int remainingBudget = 0;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private PieChart pieChart;

    // Listener registration so we can detach in onDestroy()
    private ListenerRegistration expenseListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.budget_activity);

        // --- UI setup ---
        drawerLayout    = findViewById(R.id.drawerLayout);
        navigationView  = findViewById(R.id.navigationView);
        btnMenu         = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        navigationView.setNavigationItemSelectedListener(this);

        tvTotalBudget     = findViewById(R.id.tv_total_budget);
        tvRemainingBudget = findViewById(R.id.tv_remaining_budget);
        rvBudgetList      = findViewById(R.id.rv_budget_list);
        btnAddExpense     = findViewById(R.id.btn_add_expense);
        pieChart          = findViewById(R.id.pieChart);

        budgetAdapter = new BudgetAdapter(budgetItems, totalBudget);
        rvBudgetList.setLayoutManager(new LinearLayoutManager(this));
        rvBudgetList.setAdapter(budgetAdapter);

        btnAddExpense.setOnClickListener(v ->
                startActivity(new Intent(BudgetActivity.this, AddExpenseActivity.class))
        );

        configurePieChart();

        // --- Firebase setup ---
        firebaseAuth = FirebaseAuth.getInstance();
        firestore    = FirebaseFirestore.getInstance();
        userId       = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : null;

        updateNavigationHeader();
        fetchTotalBudgetAndListenExpenses();
    }

    private void updateNavigationHeader() {
        if (userId == null) return;

        firestore.collection("users")
                .document(userId)
                .collection("couples")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        String partner1 = doc.getString("partner1");
                        String partner2 = doc.getString("partner2");
                        String imageUrl  = doc.getString("profileImageUrl");
                        String name      = partner1 + " & " + partner2;
                        String email     = firebaseAuth.getCurrentUser().getEmail();

                        View headerView  = navigationView.getHeaderView(0);
                        TextView tvName  = headerView.findViewById(R.id.tvUserName);
                        TextView tvEmail = headerView.findViewById(R.id.tvUserEmail);
                        ImageView ivProfile = headerView.findViewById(R.id.imgUserIcon);

                        tvName.setText(name);
                        tvEmail.setText(email);
                        Glide.with(this).load(imageUrl).into(ivProfile);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Nav header fetch error", e));
    }

    private void fetchTotalBudgetAndListenExpenses() {
        if (userId == null) return;

        // First, get the total budget from the 'couples' doc
        firestore.collection("users")
                .document(userId)
                .collection("couples")
                .limit(1)
                .get()
                .addOnSuccessListener(coupleSnap -> {
                    if (!coupleSnap.isEmpty()) {
                        DocumentSnapshot coupleDoc = coupleSnap.getDocuments().get(0);
                        totalBudget = coupleDoc.getLong("budget") != null
                                ? coupleDoc.getLong("budget").intValue()
                                : 0;
                        remainingBudget = totalBudget;
                        tvTotalBudget.setText("Total Budget: $" + totalBudget);

                        // Now start listening to all expense changes
                        attachExpenseListener();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Couple fetch error", e));
    }

    private void attachExpenseListener() {
        // Clean up any existing listener
        if (expenseListener != null) {
            expenseListener.remove();
        }

        expenseListener = firestore.collection("users")
                .document(userId)
                .collection("expenses")
                .addSnapshotListener((expSnap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Expenses listen error", e);
                        return;
                    }
                    if (expSnap == null) return;

                    // Reset and recalc
                    budgetItems.clear();
                    Map<String, Integer> catTotals = new HashMap<>();
                    remainingBudget = totalBudget;

                    for (DocumentSnapshot doc : expSnap.getDocuments()) {
                        String cat = doc.getString("category");
                        int amt = doc.getLong("amount") != null
                                ? doc.getLong("amount").intValue()
                                : 0;
                        if (cat != null) {
                            catTotals.put(cat, catTotals.getOrDefault(cat, 0) + amt);
                            remainingBudget -= amt;
                        }
                    }

                    // Build the list
                    for (Map.Entry<String,Integer> entry : catTotals.entrySet()) {
                        budgetItems.add(new BudgetItem(entry.getKey(), entry.getValue()));
                    }

                    // Update UI
                    budgetAdapter.setTotalBudget(totalBudget);
                    tvRemainingBudget.setText("Remaining: $" + remainingBudget);
                    updatePieChart();
                    budgetAdapter.notifyDataSetChanged();
                });
    }

    private void configurePieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setDrawEntryLabels(false);

        // Legend configuration for multi-line wrapping
        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);
        legend.setXEntrySpace(8f);
        legend.setYEntrySpace(4f);
        legend.setTextColor(Color.BLACK);
        legend.setTextSize(12f);
    }

    private void updatePieChart() {
        List<PieEntry> entries = new ArrayList<>();
        for (BudgetItem item : budgetItems) {
            entries.add(new PieEntry(item.getAmount(), item.getCategory()));
        }

        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(ColorTemplate.COLORFUL_COLORS);
        ds.setValueTextColor(Color.BLACK);
        ds.setValueTextSize(12f);
        ds.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        ds.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        ds.setValueLinePart1OffsetPercentage(80f);
        ds.setValueLinePart1Length(0.4f);
        ds.setValueLinePart2Length(0.4f);
        ds.setValueLineColor(Color.BLACK);

        PieData data = new PieData(ds);
        data.setValueFormatter(new PercentFormatter(pieChart));
        pieChart.setData(data);
        pieChart.setDrawEntryLabels(false);  // no labels on slices
        pieChart.invalidate();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.navHome) {
            startActivity(new Intent(this, dashboard.class));
        } else if (id == R.id.navCategories) {
            startActivity(new Intent(this, CategoriesActivity.class));
        } else if (id == R.id.navGuestList) {
            startActivity(new Intent(this, GuestsActivity.class));
        } else if (id == R.id.navTimeline) {
            startActivity(new Intent(this, EventTimelineActivity.class));
        } else if (id == R.id.navLogout) {
            firebaseAuth.signOut();
            startActivity(new Intent(this, signin_page.class));
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (expenseListener != null) {
            expenseListener.remove();
        }
        super.onDestroy();
    }
}
