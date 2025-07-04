package com.example.weddingapp.LandingPage.base;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.weddingapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class profile extends AppCompatActivity {

    private EditText etPartner1, etPartner2, etBudget;
    private Button btnWeddingDate, btnSaveProfile, btnChangeProfilePicture;
    private ImageView imgProfilePicture;
    private String selectedWeddingDate;
    private String documentId;
    private Uri imageUri;

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    private static final int PICK_IMAGE_REQUEST = 1;
    // Fixed square size in dp
    private static final int SQUARE_SIZE_DP = 230;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase instances
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        // Initialize UI elements
        etPartner1 = findViewById(R.id.etPartner1);
        etPartner2 = findViewById(R.id.etPartner2);
        etBudget = findViewById(R.id.etBudget);
        btnWeddingDate = findViewById(R.id.btnWeddingDate);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnChangeProfilePicture = findViewById(R.id.btnChangeProfilePicture);
        imgProfilePicture = findViewById(R.id.imgProfilePicture);

        // Load existing profile data from Firestore
        loadProfileData();

        // Set up wedding date selection
        btnWeddingDate.setOnClickListener(v -> showDatePickerDialog());

        // Set up change profile picture button
        btnChangeProfilePicture.setOnClickListener(v -> openFileChooser());

        // Set up save button action
        btnSaveProfile.setOnClickListener(v -> saveProfileData());
    }

    // Open the image picker
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            // Display selected image using Glide with fixed square dimensions
            Glide.with(this)
                    .load(imageUri)
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            imgProfilePicture.setImageDrawable(resource);
                            ViewGroup.LayoutParams params = imgProfilePicture.getLayoutParams();
                            int squareSize = (int) (SQUARE_SIZE_DP * getResources().getDisplayMetrics().density + 0.5f);
                            params.width = squareSize;
                            params.height = squareSize;
                            imgProfilePicture.setLayoutParams(params);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) { }
                    });
        }
    }

    // Display DatePicker dialog for wedding date selection
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) -> {
                    month1++; // Adjust zero-based month
                    selectedWeddingDate = dayOfMonth + "/" + month1 + "/" + year1;
                    btnWeddingDate.setText(selectedWeddingDate);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    // Load profile data from Firestore
    private void loadProfileData() {
        String userId = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("users")
                .document(userId)
                .collection("couples")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        documentId = document.getId();
                        etPartner1.setText(document.getString("partner1"));
                        etPartner2.setText(document.getString("partner2"));

                        Double budgetDouble = document.getDouble("budget");
                        if (budgetDouble != null) {
                            etBudget.setText(String.valueOf(budgetDouble));
                        }
                        selectedWeddingDate = document.getString("weddingDate");
                        if (selectedWeddingDate != null) {
                            btnWeddingDate.setText(selectedWeddingDate);
                        }

                        String profileImageUrl = document.getString("profileImageUrl");
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .into(new CustomTarget<Drawable>() {
                                        @Override
                                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                            imgProfilePicture.setImageDrawable(resource);
                                            ViewGroup.LayoutParams params = imgProfilePicture.getLayoutParams();
                                            int squareSize = (int) (SQUARE_SIZE_DP * getResources().getDisplayMetrics().density + 0.5f);
                                            params.width = squareSize;
                                            params.height = squareSize;
                                            imgProfilePicture.setLayoutParams(params);
                                        }

                                        @Override
                                        public void onLoadCleared(@Nullable Drawable placeholder) { }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(profile.this, "Failed to load data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("profile", "Failed to load data: " + e.getMessage());
                });
    }

    // Save profile data and upload new profile image (if selected) to Firebase Storage
    private void saveProfileData() {
        String partner1 = etPartner1.getText().toString().trim();
        String partner2 = etPartner2.getText().toString().trim();
        String budgetString = etBudget.getText().toString().trim();

        if (partner1.isEmpty() || partner2.isEmpty() || budgetString.isEmpty() || selectedWeddingDate.isEmpty()) {
            Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        double budget;
        try {
            budget = Double.parseDouble(budgetString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid budget amount!", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadProfileImage(new UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                String userId = firebaseAuth.getCurrentUser().getUid();
                Map<String, Object> coupleData = new HashMap<>();
                coupleData.put("partner1", partner1);
                coupleData.put("partner2", partner2);
                coupleData.put("weddingDate", selectedWeddingDate);
                coupleData.put("budget", budget);

                if (downloadUrl != null) {
                    coupleData.put("profileImageUrl", downloadUrl);
                }

                if (!documentId.isEmpty()) {
                    firestore.collection("users").document(userId)
                            .collection("couples").document(documentId)
                            .update(coupleData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(profile.this, "Profile updated!", Toast.LENGTH_SHORT).show();
                                navigateToDashboard();
                            })
                            .addOnFailureListener(e -> Toast.makeText(profile.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(profile.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Upload profile image to Firebase Storage
    private void uploadProfileImage(final UploadCallback callback) {
        if (imageUri != null) {
            String userId = firebaseAuth.getCurrentUser().getUid();
            StorageReference imageRef = storageReference.child("profileImages/" + userId + ".jpg");

            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                String downloadUrl = uri.toString();
                                callback.onSuccess(downloadUrl);
                            }).addOnFailureListener(callback::onFailure)
                    ).addOnFailureListener(callback::onFailure);
        } else {
            callback.onSuccess(null); // No new image, continue without updating image URL
        }
    }

    interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(profile.this, dashboard.class);
        startActivity(intent);
        finish();
    }
}
