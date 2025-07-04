package com.example.weddingapp.LandingPage.base;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.weddingapp.R;
import com.example.weddingapp.auth.signin_page;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class starter_screen extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private static final String TAG = "StarterScreen";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starter_screen);

        firebaseAuth = FirebaseAuth.getInstance();

        // Root view for background animation
        View rootView = findViewById(android.R.id.content);
        // Retrieve ImageViews for the logo and title
        ImageView logoImage = findViewById(R.id.logoImage);
        ImageView logoTitle = findViewById(R.id.logoTitle);

        startAnimations(rootView, logoImage, logoTitle);
    }

    private void startAnimations(View background, ImageView logoImage, ImageView logoTitle) {
        Log.d(TAG, "Starting animations...");

        // Animate background color from light pink to vibrant pink
        ValueAnimator colorAnimator = ValueAnimator.ofObject(
                new ArgbEvaluator(),
                Color.parseColor("#FFEBEE"),
                Color.parseColor("#E91E63")
        );
        colorAnimator.setDuration(2000);
        colorAnimator.addUpdateListener(animator -> background.setBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimator.start();

        // Diagonal slide-in and zoom effect for the logo image
        TranslateAnimation slideAnimation = new TranslateAnimation(-500, 0, 500, 0);
        slideAnimation.setDuration(2000);
        slideAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        slideAnimation.setFillAfter(true);
        slideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                logoImage.setScaleX(0.5f);
                logoImage.setScaleY(0.5f);
                logoImage.animate().scaleX(1.2f).scaleY(1.2f).setDuration(2000).start();
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                navigateToNextScreen();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // No action required on repeat
            }
        });
        logoImage.startAnimation(slideAnimation);

        // Fade-in effect for the logo title
        logoTitle.setAlpha(0f);
        logoTitle.animate()
                .alpha(1f)
                .setDuration(3000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void navigateToNextScreen() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        Intent intent = (currentUser != null) ?
                new Intent(this, dashboard.class) :
                new Intent(this, signin_page.class);
        startActivity(intent);
        finish();
    }
}
