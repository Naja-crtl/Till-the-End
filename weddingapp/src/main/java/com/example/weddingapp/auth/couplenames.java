package com.example.weddingapp.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.weddingapp.LandingPage.Budget.BudgetActivity;
import com.example.weddingapp.LandingPage.base.dashboard;
import com.example.weddingapp.R;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class couplenames extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText etPartner1, etPartner2, etBudget;
    private Button btnWeddingDate, btnNext, btnUploadProfilePicture;
    private ImageView ivProfilePicture;

    private String selectedWeddingDate = "";
    private Uri profileImageUri;

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_couplenames);

        // Initialize Firebase components
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize UI components
        etPartner1 = findViewById(R.id.etPartner1);
        etPartner2 = findViewById(R.id.etPartner2);
        etBudget = findViewById(R.id.etBudget);
        btnWeddingDate = findViewById(R.id.btnWeddingDate);
        btnNext = findViewById(R.id.btnNext);
        btnUploadProfilePicture = findViewById(R.id.btnUploadProfilePicture);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);

        // Set click listeners
        btnWeddingDate.setOnClickListener(v -> showDatePicker());
        btnUploadProfilePicture.setOnClickListener(v -> selectProfileImage());
        btnNext.setOnClickListener(v -> saveDataAndProceed());
    }

    private void showDatePicker() {
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
        builder.setTitleText("Select Wedding Date");

        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        builder.setCalendarConstraints(constraintsBuilder.build());

        MaterialDatePicker<Long> datePicker = builder.build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            selectedWeddingDate = sdf.format(selection);
            btnWeddingDate.setText("Wedding Date: " + selectedWeddingDate);
        });

        datePicker.show(getSupportFragmentManager(), "WEDDING_DATE_PICKER");
    }

    private void selectProfileImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            profileImageUri = data.getData();
            ivProfilePicture.setImageURI(profileImageUri);
        }
    }

    private void saveDataAndProceed() {
        String partner1 = etPartner1.getText().toString().trim();
        String partner2 = etPartner2.getText().toString().trim();
        String budgetText = etBudget.getText().toString().trim();

        if (partner1.isEmpty() || partner2.isEmpty() || budgetText.isEmpty() || selectedWeddingDate.isEmpty()) {
            Toast.makeText(couplenames.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double budget;
        try {
            budget = Double.parseDouble(budgetText);
        } catch (NumberFormatException e) {
            Toast.makeText(couplenames.this, "Invalid budget amount", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            if (profileImageUri != null) {
                uploadProfileImage(userId, partner1, partner2, budget);
            } else {
                saveCoupleData(userId, partner1, partner2, budget, null);
            }
        }
    }

    private void uploadProfileImage(String userId, String partner1, String partner2, double budget) {
        StorageReference profileImageRef = storageReference.child("profile_images/"
                + UUID.randomUUID().toString() + ".jpg");

        profileImageRef.putFile(profileImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            saveCoupleData(userId, partner1, partner2, budget, imageUrl);
                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(couplenames.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveCoupleData(String userId, String partner1, String partner2, double budget, @Nullable String imageUrl) {
        Map<String, Object> coupleData = new HashMap<>();
        coupleData.put("partner1", partner1);
        coupleData.put("partner2", partner2);
        coupleData.put("weddingDate", selectedWeddingDate);
        coupleData.put("budget", budget);
        if (imageUrl != null) {
            coupleData.put("profileImageUrl", imageUrl);
        }

        firestore.collection("users").document(userId).collection("couples")
                .add(coupleData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(couplenames.this, "Data saved successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(couplenames.this, dashboard.class);
                    intent.putExtra("partner1", partner1);
                    intent.putExtra("partner2", partner2);
                    intent.putExtra("weddingDate", selectedWeddingDate);
                    intent.putExtra("budget", budget);
                    if (imageUrl != null) {
                        intent.putExtra("profileImageUrl", imageUrl);
                    }
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(couplenames.this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // This overload is preserved if you need to pass budget as a String elsewhere
    private void saveCoupleData(String userId, String partner1, String partner2, String budget, @Nullable String imageUrl) {
        Map<String, Object> coupleData = new HashMap<>();
        coupleData.put("partner1", partner1);
        coupleData.put("partner2", partner2);
        coupleData.put("weddingDate", selectedWeddingDate);
        coupleData.put("budget", budget);
        if (imageUrl != null) {
            coupleData.put("profileImageUrl", imageUrl);
        }

        firestore.collection("users").document(userId).collection("couples")
                .add(coupleData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(couplenames.this, "Data saved successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(couplenames.this, BudgetActivity.class);
                    intent.putExtra("partner1", partner1);
                    intent.putExtra("partner2", partner2);
                    intent.putExtra("weddingDate", selectedWeddingDate);
                    intent.putExtra("budget", budget);
                    if (imageUrl != null) {
                        intent.putExtra("profileImageUrl", imageUrl);
                    }
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(couplenames.this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
