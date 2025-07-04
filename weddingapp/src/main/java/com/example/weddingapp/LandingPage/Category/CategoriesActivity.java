package com.example.weddingapp.LandingPage.Category;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weddingapp.LandingPage.Budget.BudgetActivity;
import com.example.weddingapp.LandingPage.Guests.GuestsActivity;
import com.example.weddingapp.LandingPage.Timeline.EventTimelineActivity;
import com.example.weddingapp.LandingPage.base.DrawerHeaderView;
import com.example.weddingapp.LandingPage.base.User;
import com.example.weddingapp.LandingPage.base.dashboard;
import com.example.weddingapp.R;
import com.example.weddingapp.auth.signin_page;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList; import java.util.List;

public class CategoriesActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private final List<Category> categoryList = new ArrayList<>();
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    private static final String TAG = "CategoriesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.categories);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewCategories);
        btnMenu = findViewById(R.id.btnMenu);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        // Set up RecyclerView with a grid layout
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new CategoryAdapter(this, categoryList);
        recyclerView.setAdapter(adapter);

        // Set item click listener to open CategoryDetailActivity
        adapter.setOnItemClickListener(position -> {
            Intent intent = new Intent(CategoriesActivity.this, CategoryDetailActivity.class);
            String categoryId = categoryList.get(position).getId(); // Ensure Category has getId() method
            intent.putExtra("categoryId", categoryId);
            startActivity(intent);
        });

        // Toggle the navigation drawer using GravityCompat.START
        btnMenu.setOnClickListener(v -> {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START);
            } else {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        });

        // Set navigation item selection listener
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize Firebase components
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Fetch category data from Firestore and update the drawer header
        fetchCategories();
        fetchUserDataForDrawer();
    }

    private void fetchCategories() {
        firestore.collection("categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    categoryList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        // Convert document to a Category object (assumes fields: title, description, imageUrl)
                        Category category = document.toObject(Category.class);
                        if (category != null) {
                            category.setId(document.getId());
                            categoryList.add(category);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error fetching categories", e));
    }

    private void fetchUserDataForDrawer() {
        if (firebaseAuth.getCurrentUser() == null) return;
        String userId = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("users")
                .document(userId)
                .collection("couples")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String partner1 = doc.getString("partner1");
                        String partner2 = doc.getString("partner2");
                        String imageUrl = doc.getString("profileImageUrl");
                        String name = partner1 + " & " + partner2;
                        String email = firebaseAuth.getCurrentUser().getEmail();

                        // Update the drawer header UI with the user data
                        View headerView = navigationView.getHeaderView(0);
                        if (headerView instanceof DrawerHeaderView) {
                            ((DrawerHeaderView) headerView).setUser(new User(name, email, imageUrl));
                        } else {
                            Log.w(TAG, "Header view is not an instance of DrawerHeaderView.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error fetching user data", e));
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.navBudget) {
            startActivity(new Intent(this, BudgetActivity.class));
        } else if (id == R.id.navCategories) {
            // Already in CategoriesActivity; no action required
        } else if (id == R.id.navHome) {
            startActivity(new Intent(this, dashboard.class));
        } else if (id == R.id.navTimeline) {
            startActivity(new Intent(this, EventTimelineActivity.class));
        } else if (id == R.id.navGuestList) {
            startActivity(new Intent(this, GuestsActivity.class));
        } else if (id == R.id.navLogout) {
            firebaseAuth.signOut();
            startActivity(new Intent(this, signin_page.class));
            finish();
        }
        // Close the drawer after selection
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
}