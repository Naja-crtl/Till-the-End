package com.example.weddingapp.LandingPage.Guests;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.example.weddingapp.LandingPage.Budget.BudgetActivity;
import com.example.weddingapp.LandingPage.Category.CategoriesActivity;
import com.example.weddingapp.LandingPage.Guests.Invitation.InvitationCardActivity;
import com.example.weddingapp.LandingPage.Timeline.EventTimelineActivity;
import com.example.weddingapp.LandingPage.base.dashboard;
import com.example.weddingapp.R;
import com.example.weddingapp.auth.signin_page;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;

public class GuestsActivity extends AppCompatActivity {

    private ListView lvGuestList;
    private Button btnBride, btnGroom;
    private Button btnSendInvitation, btnCreateInvitation;

    private final ArrayList<Guest> allGuestList = new ArrayList<>();
    private final ArrayList<Guest> guestList    = new ArrayList<>();
    private ArrayAdapter<Guest> adapter;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private String userId;

    private ListenerRegistration guestListener;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guests);

        // ─── find views ───────────────────────────────────────────────────────
        lvGuestList        = findViewById(R.id.lvGuestList);
        btnBride           = findViewById(R.id.btnAttending);
        btnGroom           = findViewById(R.id.btnNotAttending);
        btnSendInvitation  = findViewById(R.id.btnSendInvitation);
        btnCreateInvitation= findViewById(R.id.btnCreateInvitation);

        // ─── setup list + adapter ──────────────────────────────────────────────
        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                guestList
        );
        lvGuestList.setAdapter(adapter);

        // ─── Firebase init ────────────────────────────────────────────────────
        firebaseAuth = FirebaseAuth.getInstance();
        firestore    = FirebaseFirestore.getInstance();
        userId       = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : null;

        // ─── filter buttons: Bride / Groom ────────────────────────────────────
        btnBride.setOnClickListener(v -> filterByTeam(true));
        btnGroom.setOnClickListener(v -> filterByTeam(false));

        // ─── configure Create/Edit Invitation button ──────────────────────────
        configureInvitationButton();

        // ─── “Send via WhatsApp” ──────────────────────────────────────────────
        btnSendInvitation.setOnClickListener(v -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
                return;
            }

            firestore.collection("users")
                    .document(user.getUid())
                    .collection("invitations")
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.isEmpty()) {
                            Toast.makeText(this, "No invitation found.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);

                        String brideName = doc.getString("brideName");
                        String groomName = doc.getString("groomName");
                        String weddingDate = doc.getString("weddingDate");
                        String venue = doc.getString("venue");
                        String customMessage = doc.getString("customMessage");
                        String userId = user.getUid();

                        String link = "https://weddinginvitation-opal.vercel.app/?userId=" + userId;

                        String message = "💍 You're Invited! 💍\n\n" +
                                "👫 Couple's Names: " + brideName + " & " + groomName + "\n" +
                                "📅 Wedding Date: " + weddingDate + "\n" +
                                "🏰 Venue: " + venue + "\n\n" +
                                "💌 Message: Please Confirm RSVP Here: " + link + "\n\n" +
                                (customMessage != null ? customMessage + "\n\n" : "") +
                                "We can't wait to celebrate with you! ❤️";

                        Intent shareIntent = new Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, message);

                        try {
                            startActivity(Intent.createChooser(shareIntent, "Share Invitation via"));
                        } catch (ActivityNotFoundException ex) {
                            Toast.makeText(
                                    this,
                                    "No app found to share invitation.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error fetching invitation: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);

// Open drawer on menu button click
        btnMenu.setOnClickListener(v -> {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START);
            } else {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        });

// Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navBudget) {
                startActivity(new Intent(this, BudgetActivity.class));
            } else if (id == R.id.navCategories) {
                startActivity(new Intent(this, CategoriesActivity.class));
            } else if (id == R.id.navHome) {
                startActivity(new Intent(this, dashboard.class));
            } else if (id == R.id.navTimeline) {
                startActivity(new Intent(this, EventTimelineActivity.class));
            } else if (id == R.id.navGuestList) {
                // no action needed already on the page
            } else if (id == R.id.navLogout) {
                firebaseAuth.signOut();
                startActivity(new Intent(this, signin_page.class));
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
        updateNavigationHeader();

    }

    private void updateNavigationHeader() {
        if (userId == null) return;

        firestore.collection("users")
                .document(userId)
                .collection("couples")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) return;
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);

                    String partner1 = doc.getString("partner1");
                    String partner2 = doc.getString("partner2");
                    String name     = partner1 + " & " + partner2;
                    String email    = firebaseAuth.getCurrentUser().getEmail();
                    String imageUrl = doc.getString("profileImageUrl");

                    View headerView   = navigationView.getHeaderView(0);
                    TextView tvName   = headerView.findViewById(R.id.tvUserName);
                    TextView tvEmail  = headerView.findViewById(R.id.tvUserEmail);
                    ImageView ivPhoto = headerView.findViewById(R.id.imgUserIcon);

                    tvName.setText(name);
                    tvEmail.setText(email);
                    Glide.with(this)
                            .load(imageUrl)
                            .into(ivPhoto);
                })
                .addOnFailureListener(e -> Log.e("GuestsActivity",
                        "Nav header fetch error", e));
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachGuestListener();
        configureInvitationButton();  // refresh button state whenever we resume
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (guestListener != null) {
            guestListener.remove();
            guestListener = null;
        }
    }

    /**
     * Checks if the user already has an invitation document.
     * Updates the button text and click logic to either create or edit.
     */
    private void configureInvitationButton() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            btnCreateInvitation.setEnabled(false);
            return;
        }

        firestore.collection("users")
                .document(user.getUid())
                .collection("invitations")
                .limit(1)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot query) {
                        if (!query.isEmpty()) {
                            // Invitation exists → switch to Edit mode
                            DocumentSnapshot doc = query.getDocuments().get(0);
                            String invId = doc.getId();

                            btnCreateInvitation.setText("Edit Invitation Card");
                            btnCreateInvitation.setOnClickListener(v -> {
                                Intent edit = new Intent(GuestsActivity.this, InvitationCardActivity.class);
                                edit.putExtra("invitationId", invId);
                                startActivity(edit);
                            });
                        } else {
                            // No invitation → Create mode
                            btnCreateInvitation.setText("Create Invitation");
                            btnCreateInvitation.setOnClickListener(v ->
                                    startActivity(
                                            new Intent(GuestsActivity.this, InvitationCardActivity.class)
                                    )
                            );
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(
                                GuestsActivity.this,
                                "Could not check invitation: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    /**
     * Attaches a real-time listener to the guests subcollection and updates counts.
     */
    private void attachGuestListener() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        guestListener = firestore
                .collection("users")
                .document(user.getUid())
                .collection("guests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(
                                this,
                                "Listen failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    allGuestList.clear();
                    int countBride = 0, countGroom = 0;

                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Guest guest = doc.toObject(Guest.class);
                            if (guest == null) continue;
                            allGuestList.add(guest);
                            if ("bride".equalsIgnoreCase(guest.getTeam())) countBride++;
                            else if ("groom".equalsIgnoreCase(guest.getTeam())) countGroom++;
                        }
                    }

                    // update tab labels
                    btnBride.setText("Bride (" + countBride + ")");
                    btnGroom.setText("Groom (" + countGroom + ")");

                    // immediately show Bride team by default
                    filterByTeam(true);
                });
    }

    /**
     * Filters the in-memory guest list by team and notifies the adapter.
     */
    private void filterByTeam(boolean isBride) {
        guestList.clear();
        for (Guest g : allGuestList) {
            if (isBride && "bride".equalsIgnoreCase(g.getTeam())) {
                guestList.add(g);
            } else if (!isBride && "groom".equalsIgnoreCase(g.getTeam())) {
                guestList.add(g);
            }
        }
        adapter.notifyDataSetChanged();
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
