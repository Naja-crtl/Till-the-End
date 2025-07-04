package com.example.weddingapp.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.weddingapp.LandingPage.base.dashboard;
import com.example.weddingapp.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class signin_page extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private GoogleSignInClient mGoogleSignInClient;

    private EditText emailInput, passwordInput;
    private Button signInButton, googleSignInButton;
    private TextView signUpLink, forgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signinpage);

        // Initialize Firebase Auth and Firestore
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI components
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        signInButton = findViewById(R.id.signInButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        signUpLink = findViewById(R.id.signUpLink);
        forgotPassword = findViewById(R.id.forgotPassword);

        // Set up the listeners
        setSignInButtonListener();
        setupSignUpLink();
        setupForgotPasswordLink();

        // Configure Google Sign-In options using your Web Client ID from strings.xml
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set up Google Sign-In button listener
        googleSignInButton.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    private void setSignInButtonListener() {
        signInButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(signin_page.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Email and password authentication using FirebaseAuth
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                Log.d("FirebaseAuth", "Login successful for user: " + user.getEmail());
                                fetchCoupleData(user.getUid());
                            }
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Sign-In Failed";
                            Toast.makeText(signin_page.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void fetchCoupleData(String userId) {
        // Reference to the user's "couples" subcollection in Firestore
        CollectionReference couplesRef = firestore.collection("users").document(userId).collection("couples");
        couplesRef.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // If couple data exists, retrieve it and navigate to the dashboard
                        DocumentSnapshot coupleDoc = querySnapshot.getDocuments().get(0);
                        String partner1 = coupleDoc.getString("partner1");
                        String partner2 = coupleDoc.getString("partner2");
                        String weddingDate = coupleDoc.getString("weddingDate");

                        Intent intent = new Intent(signin_page.this, dashboard.class);
                        intent.putExtra("partner1", partner1);
                        intent.putExtra("partner2", partner2);
                        intent.putExtra("weddingDate", weddingDate);
                        startActivity(intent);
                        finish();
                    } else {
                        // If no couple data is found, navigate to the couple names page
                        Intent intent = new Intent(signin_page.this, couplenames.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(signin_page.this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void setupSignUpLink() {
        // Create a clickable "Sign Up" link
        String text = "Don't have an Account? Sign Up";
        SpannableString spannable = new SpannableString(text);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(signin_page.this, signup_page.class);
                startActivity(intent);
            }
        };
        int signUpStartIndex = text.indexOf("Sign Up");
        spannable.setSpan(clickableSpan, signUpStartIndex, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(Color.BLUE), signUpStartIndex, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        signUpLink.setText(spannable);
        signUpLink.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setupForgotPasswordLink() {
        // Create a clickable "Forgot Password?" link
        SpannableString forgotSpan = new SpannableString("Forgot Password?");
        ClickableSpan forgotClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                String email = emailInput.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(signin_page.this, "Please enter your email to reset your password", Toast.LENGTH_SHORT).show();
                    return;
                }
                firebaseAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(signin_page.this, "Password reset email sent. Check your inbox.", Toast.LENGTH_LONG).show();
                            } else {
                                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email.";
                                Toast.makeText(signin_page.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        };
        forgotSpan.setSpan(forgotClickableSpan, 0, forgotSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        forgotSpan.setSpan(new ForegroundColorSpan(Color.BLUE), 0, forgotSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        forgotPassword.setText(forgotSpan);
        forgotPassword.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            // Retrieve the GoogleSignInAccount from the Intent data
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w("GoogleSignIn", "Google sign in failed", e);
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        // Use the Google ID token to authenticate with Firebase
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        Toast.makeText(signin_page.this, "Google Sign-In successful", Toast.LENGTH_SHORT).show();
                        if (user != null) {
                            Log.d("GoogleAuth", "Google Sign-In successful for user: " + user.getEmail());
                            fetchCoupleData(user.getUid());
                        }
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Authentication Failed";
                        Toast.makeText(signin_page.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
