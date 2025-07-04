package com.example.weddingapp.LandingPage.Category;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weddingapp.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CategoryDetailActivity extends AppCompatActivity {

    private TextView tvCategoryTitle, tvCategoryDescription;
    private RecyclerView recyclerViewVendors;
    private VendorAdapter vendorAdapter;
    private List<Vendor> vendorList;
    private FirebaseFirestore firestore;
    private String categoryId, categoryTitle;
    private SearchView searchViewVendors;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final String TAG = "CategoryDetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_detail);

        firestore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        categoryId = getIntent().getStringExtra("categoryId");

        // Bind views
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        tvCategoryDescription = findViewById(R.id.tvCategoryDescription);
        recyclerViewVendors = findViewById(R.id.recyclerViewVendors);
        searchViewVendors = findViewById(R.id.searchViewVendor);

        recyclerViewVendors.setLayoutManager(new LinearLayoutManager(this));
        vendorList = new ArrayList<>();
        vendorAdapter = new VendorAdapter(this, new ArrayList<>());
        recyclerViewVendors.setAdapter(vendorAdapter);

        searchViewVendors.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterVendors(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterVendors(newText);
                return true;
            }
        });

        fetchCategoryInfoAndLocation();
    }

    private void fetchCategoryInfoAndLocation() {
        firestore.collection("categories")
                .document(categoryId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        categoryTitle = documentSnapshot.getString("title");
                        String description = documentSnapshot.getString("description");

                        tvCategoryTitle.setText(categoryTitle != null ? categoryTitle : "Category");
                        tvCategoryDescription.setText(description != null ? description : "Browse vendors in this category");

                        getCurrentLocationAndFetchVendors();
                    } else {
                        Toast.makeText(this, "Category not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load category info", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Category info error: " + e.getMessage());
                });
    }

    private void getCurrentLocationAndFetchVendors() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double userLat = location.getLatitude();
                double userLng = location.getLongitude();
                fetchVendorsSortedByDistance(userLat, userLng);
            } else {
                Toast.makeText(this, "Could not fetch location", Toast.LENGTH_SHORT).show();
                fetchVendorsWithoutLocation();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            fetchVendorsWithoutLocation();
        });
    }

    private void fetchVendorsSortedByDistance(double userLat, double userLng) {
        firestore.collection("categories")
                .document(categoryId)
                .collection("vendors")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<VendorWithDistance> tempList = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        Vendor vendor = doc.toObject(Vendor.class);
                        if (vendor != null) {
                            float[] result = new float[1];
                            Location.distanceBetween(userLat, userLng,
                                    vendor.getLatitude(), vendor.getLongitude(), result);
                            tempList.add(new VendorWithDistance(vendor, result[0]));
                        }
                    }

                    Collections.sort(tempList, Comparator.comparingDouble(v -> v.distance));

                    vendorList.clear();
                    for (VendorWithDistance v : tempList) {
                        vendorList.add(v.vendor);
                    }

                    vendorAdapter.updateVendors(vendorList);
                });
    }

    private void fetchVendorsWithoutLocation() {
        firestore.collection("categories")
                .document(categoryId)
                .collection("vendors")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    vendorList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Vendor vendor = doc.toObject(Vendor.class);
                        if (vendor != null) {
                            vendorList.add(vendor);
                        }
                    }
                    vendorAdapter.updateVendors(vendorList);
                });
    }

    private void filterVendors(String query) {
        String q = query.toLowerCase();
        List<Vendor> filteredList = new ArrayList<>();
        for (Vendor vendor : vendorList) {
            boolean matchesName = vendor.getName() != null
                    && vendor.getName().toLowerCase().contains(q);
            boolean matchesAddress = vendor.getAddress() != null
                    && vendor.getAddress().toLowerCase().contains(q);
            if (matchesName || matchesAddress) {
                filteredList.add(vendor);
            }
        }
        vendorAdapter.updateVendors(filteredList);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocationAndFetchVendors();
        } else {
            Toast.makeText(this, "Location permission denied. Showing unsorted vendors.", Toast.LENGTH_SHORT).show();
            fetchVendorsWithoutLocation();
        }
    }

    // Helper inner class to pair vendor with its distance
    private static class VendorWithDistance {
        Vendor vendor;
        float distance;

        VendorWithDistance(Vendor vendor, float distance) {
            this.vendor = vendor;
            this.distance = distance;
        }
    }
}
