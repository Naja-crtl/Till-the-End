package com.example.weddingapp.LandingPage.base;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.weddingapp.R; // ensure you import your app's R

public class DrawerHeaderView extends LinearLayout {
    private ImageView imgUserIcon;
    private TextView tvUserName, tvUserEmail;

    public DrawerHeaderView(Context context) {
        super(context);
        init(context);
    }

    public DrawerHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DrawerHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Inflate the content layout that contains the ImageView and TextViews
        LayoutInflater.from(context).inflate(R.layout.drawer_header_content, this, true);
        imgUserIcon = findViewById(R.id.imgUserIcon);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
    }


    // Updates the header with user data.
    public void setUser(User user) {
        tvUserName.setText(user.getName());
        tvUserEmail.setText(user.getEmail());
        Glide.with(getContext())
                .load(user.getImageUrl())
                .placeholder(R.drawable.icon) // fallback icon
                .into(imgUserIcon);
    }
}
