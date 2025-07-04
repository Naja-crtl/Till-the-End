package com.example.weddingapp.LandingPage.base;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.weddingapp.LandingPage.Budget.BudgetActivity;
import com.example.weddingapp.LandingPage.Category.CategoriesActivity;
import com.example.weddingapp.LandingPage.Guests.GuestsActivity;
import com.example.weddingapp.LandingPage.Timeline.EventTimelineActivity;
import com.example.weddingapp.R;
import com.example.weddingapp.auth.signin_page;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.navigation.NavigationView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class dashboard extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvWelcomeMessage, tvCountdown;
    private ImageButton btnMenu, btnProfile;
    private LinearLayout llBudget, llCategories, llGuestList, llTimeline;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private AdView adView;  // Banner AdView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1. Initialize the Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {});

        // 2. Find and load your AdView
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        // Initialize Firebase instances
        firebaseAuth = FirebaseAuth.getInstance();
        firestore    = FirebaseFirestore.getInstance();

        // Initialize UI components
        drawerLayout     = findViewById(R.id.drawerLayout);
        navigationView   = findViewById(R.id.navigationView);
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);
        tvCountdown      = findViewById(R.id.tvCountdown);
        btnMenu          = findViewById(R.id.btnMenu);
        btnProfile       = findViewById(R.id.btnProfile);

        llBudget     = findViewById(R.id.llBudget);
        llCategories = findViewById(R.id.llCategories);
        llGuestList  = findViewById(R.id.llGuestList);
        llTimeline   = findViewById(R.id.llTimeline);

        // Set up navigation menu and button actions
        setupNavigationMenu();
        setupButtonNavigation();

        // Load user data from Firestore
        fetchUserDataFromFirestore();

        // Profile button handler
        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(dashboard.this, profile.class))
        );
    }

    private void setupNavigationMenu() {
        btnMenu.setOnClickListener(v -> {
            if (!drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.openDrawer(navigationView);
            } else {
                drawerLayout.closeDrawer(navigationView);
            }
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navLogout) {
                logout();
            } else if (id == R.id.navBudget) {
                startActivity(new Intent(this, BudgetActivity.class));
            } else if (id == R.id.navCategories) {
                startActivity(new Intent(this, CategoriesActivity.class));
            } else if (id == R.id.navGuestList) {
                startActivity(new Intent(this, GuestsActivity.class));
            } else if (id == R.id.navTimeline) {
                startActivity(new Intent(this, EventTimelineActivity.class));
            }
            drawerLayout.closeDrawer(navigationView);
            return true;
        });
    }

    private void fetchUserDataFromFirestore() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        firestore.collection("users")
                .document(userId)
                .collection("couples")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String partner1    = doc.getString("partner1");
                        String partner2    = doc.getString("partner2");
                        String weddingDate = doc.getString("weddingDate");

                        tvWelcomeMessage.setText("Welcome " + partner1 + " and " + partner2 + "!");
                        calculateCountdown(weddingDate);

                        View headerView = navigationView.getHeaderView(0);
                        if (headerView instanceof DrawerHeaderView) {
                            String email    = currentUser.getEmail();
                            String imageUrl = doc.getString("profileImageUrl");
                            ((DrawerHeaderView) headerView)
                                    .setUser(new User(partner1 + " & " + partner2, email, imageUrl));
                        }
                    } else {
                        Toast.makeText(this, "No data found for the user!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to fetch data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void calculateCountdown(String weddingDate) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date wedding = dateFormat.parse(weddingDate);
            Date today   = new Date();

            long diffInMillis  = wedding.getTime() - today.getTime();
            long daysRemaining = TimeUnit.MILLISECONDS.toDays(diffInMillis);

            if (daysRemaining >= 0) {
                tvCountdown.setText("Days until the wedding: " + daysRemaining + " days");
            } else {
                tvCountdown.setText("The wedding date has passed!");
            }
        } catch (ParseException e) {
            tvCountdown.setText("Invalid wedding date!");
        }
    }

    private void setupButtonNavigation() {
        llBudget.setOnClickListener(v ->
                startActivity(new Intent(this, BudgetActivity.class))
        );
        llCategories.setOnClickListener(v ->
                startActivity(new Intent(this, CategoriesActivity.class))
        );
        llGuestList.setOnClickListener(v ->
                startActivity(new Intent(this, GuestsActivity.class))
        );
        llTimeline.setOnClickListener(v ->
                startActivity(new Intent(this, EventTimelineActivity.class))
        );
    }

    private void logout() {
        firebaseAuth.signOut();
        Intent intent = new Intent(this, signin_page.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
