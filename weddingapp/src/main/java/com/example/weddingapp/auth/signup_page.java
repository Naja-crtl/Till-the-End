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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.weddingapp.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class signup_page extends AppCompatActivity {

    private EditText firstName, lastName, emailEditText, passwordEditText, confirmPasswordEditText, contactNumberEditText;
    private Spinner countryCodeSpinner;
    private Button signUpButton;
    private CheckBox termsCheckBox;
    private TextView signinLink;

    // Firebase instances
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    // Google Sign-In client and request code
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_page);

        // Initialize Firebase Authentication and Firestore
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI components
        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        confirmPasswordEditText = findViewById(R.id.confirmPassword);
        contactNumberEditText = findViewById(R.id.contactNumber);
        countryCodeSpinner = findViewById(R.id.countryCode);
        signUpButton = findViewById(R.id.signUpButton);
        termsCheckBox = findViewById(R.id.termsCheckBox);
        signinLink = findViewById(R.id.signinlink);

        setupCountryCodeSpinner();
        setSignUpButtonListener();
        setupSignInLink();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                // Make sure default_web_client_id is correctly set in your strings.xml file
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set Google Sign-In button listener
        findViewById(R.id.googleSignInButton).setOnClickListener(v -> signInWithGoogle());
    }

    private void setupCountryCodeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.country_codes,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countryCodeSpinner.setAdapter(adapter);
    }

    private void setSignUpButtonListener() {
        signUpButton.setOnClickListener(v -> {
            String fName = firstName.getText().toString().trim();
            String lName = lastName.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();
            String contactNumber = contactNumberEditText.getText().toString().trim();
            String countryCode = countryCodeSpinner.getSelectedItem().toString();

            // Input validation
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || contactNumber.isEmpty()) {
                Toast.makeText(signup_page.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else if (!password.equals(confirmPassword)) {
                Toast.makeText(signup_page.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            } else if (!termsCheckBox.isChecked()) {
                Toast.makeText(signup_page.this, "You must agree to the terms and conditions", Toast.LENGTH_SHORT).show();
            } else {
                String fullPhoneNumber = countryCode + " " + contactNumber;

                // Sign up the user with Firebase Authentication
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Sign-up successful
                                String userId = firebaseAuth.getCurrentUser().getUid();
                                saveUserDataToFirestore(userId, fName, lName, email, fullPhoneNumber, countryCode);
                            } else {
                                // Handle errors during sign-up
                                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                    Toast.makeText(signup_page.this, "This email is already registered", Toast.LENGTH_SHORT).show();
                                } else {
                                    String errorMessage = task.getException() != null ? task.getException().getMessage() : "Sign-Up Failed";
                                    Toast.makeText(signup_page.this, errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }

    private void saveUserDataToFirestore(String userId, String firstName, String lastName, String email, String phoneNumber, String countryCode) {
        // Create a map to store user data
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("email", email);
        userData.put("phoneNumber", phoneNumber);
        userData.put("countryCode", countryCode);
        userData.put("initialized", true);

        // Add the data to Firestore under the 'users' collection
        firestore.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(signup_page.this, "Sign-Up Successful!", Toast.LENGTH_SHORT).show();
                    // Navigate to Sign-In or Dashboard as needed
                    Intent intent = new Intent(signup_page.this, signin_page.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(signup_page.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSignInLink() {
        String text = "Already have an Account? Sign In";
        SpannableString spannable = new SpannableString(text);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // Navigate to the Sign-In page
                Intent intent = new Intent(signup_page.this, signin_page.class);
                startActivity(intent);
            }
        };

        int signInStartIndex = text.indexOf("Sign In");
        spannable.setSpan(clickableSpan, signInStartIndex, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(Color.BLUE), signInStartIndex, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        signinLink.setText(spannable);
        signinLink.setMovementMethod(LinkMovementMethod.getInstance());
    }

    // Google Sign-In flow
    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w("GoogleSignIn", "Google sign in failed", e);
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        Toast.makeText(signup_page.this, "Google sign in successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(signup_page.this, couplenames.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(signup_page.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
