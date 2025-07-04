package com.example.weddingapp.LandingPage.Category;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.weddingapp.R;

import java.util.List;

public class VendorAdapter extends RecyclerView.Adapter<VendorAdapter.VendorViewHolder> {

    private List<Vendor> vendorList;
    private OnItemClickListener listener;
    private static final int REQUEST_CALL_PHONE_PERMISSION = 101;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public VendorAdapter(Context context, List<Vendor> vendorList) {
        this.vendorList = vendorList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VendorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vendor, parent, false);
        return new VendorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VendorViewHolder holder, int position) {
        Vendor vendor = vendorList.get(position);
        Context context = holder.itemView.getContext();

        List<String> imageUrls = vendor.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            holder.startImageSlideshow(imageUrls);
        } else {
            Glide.with(context).load(vendor.getImageUrls()).into(holder.vendorImage);
        }

        holder.vendorName.setText(vendor.getName());
        holder.vendorNotes.setText(vendor.getNotes());
        holder.ratingBar.setRating((float) vendor.getRating());
        holder.ratingText.setText(String.format("(%.1f)", vendor.getRating()));
        holder.priceRange.setText(vendor.getPriceRange());
        holder.vendorLocation.setText(vendor.getAddress());

        holder.mapButton.setOnClickListener(v -> holder.handleMapNavigation(context, vendor));
        holder.callButton.setOnClickListener(v -> holder.handleCallClick(context, vendor));
        holder.emailButton.setOnClickListener(v -> holder.handleEmailClick(context, vendor));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(pos);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return vendorList != null ? vendorList.size() : 0;
    }

    @Override
    public void onViewRecycled(@NonNull VendorViewHolder holder) {
        holder.stopSlideshow();
        super.onViewRecycled(holder);
    }

    public void updateVendors(List<Vendor> newVendors) {
        vendorList = newVendors;
        notifyDataSetChanged();
    }

    public static class VendorViewHolder extends RecyclerView.ViewHolder {
        ImageView vendorImage;
        TextView vendorName, vendorNotes, ratingText, priceRange, vendorLocation;
        RatingBar ratingBar;
        Button mapButton, callButton, emailButton;

        private Handler imageHandler;
        private Runnable imageRunnable;
        private int currentImageIndex = 0;
        private List<String> currentImageUrls;

        public VendorViewHolder(@NonNull View itemView) {
            super(itemView);
            vendorImage = itemView.findViewById(R.id.vendorImage);
            vendorName = itemView.findViewById(R.id.vendorName);
            vendorNotes = itemView.findViewById(R.id.vendorNotes);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            ratingText = itemView.findViewById(R.id.ratingText);
            priceRange = itemView.findViewById(R.id.priceRange);
            vendorLocation = itemView.findViewById(R.id.vendorLocation);
            mapButton = itemView.findViewById(R.id.mapButton);
            callButton = itemView.findViewById(R.id.callButton);
            emailButton = itemView.findViewById(R.id.emailButton);
        }

        public void startImageSlideshow(List<String> imageUrls) {
            stopSlideshow();
            this.currentImageUrls = imageUrls;
            currentImageIndex = 0;
            imageHandler = new Handler();
            imageRunnable = new Runnable() {
                @Override
                public void run() {
                    if (currentImageUrls != null && !currentImageUrls.isEmpty()) {
                        Animation slideOut = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.slide_out_left);
                        vendorImage.startAnimation(slideOut);
                        slideOut.setAnimationListener(new Animation.AnimationListener() {
                            @Override public void onAnimationStart(Animation animation) {}
                            @Override public void onAnimationRepeat(Animation animation) {}
                            @Override public void onAnimationEnd(Animation animation) {
                                Glide.with(itemView.getContext())
                                        .load(currentImageUrls.get(currentImageIndex))
                                        .into(vendorImage);
                                Animation slideIn = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.slide_in_right);
                                vendorImage.startAnimation(slideIn);
                                currentImageIndex = (currentImageIndex + 1) % currentImageUrls.size();
                                imageHandler.postDelayed(imageRunnable, 5000);
                            }
                        });
                    }
                }
            };
            imageHandler.post(imageRunnable);
        }

        public void stopSlideshow() {
            if (imageHandler != null && imageRunnable != null) {
                imageHandler.removeCallbacks(imageRunnable);
            }
        }

        public void handleMapNavigation(Context context, Vendor vendor) {
            double lat = vendor.getLatitude();
            double lng = vendor.getLongitude();
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            } else {
                String url = "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng;
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        }

        public void handleCallClick(Context context, Vendor vendor) {
            PackageManager pm = context.getPackageManager();
            boolean hasTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
            if (hasTelephony) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + vendor.getContact()));
                    context.startActivity(intent);
                } else if (context instanceof Activity) {
                    ActivityCompat.requestPermissions(
                            (Activity) context,
                            new String[]{Manifest.permission.CALL_PHONE},
                            REQUEST_CALL_PHONE_PERMISSION
                    );
                }
            } else {
                Toast.makeText(context, "This device doesn't support phone calls", Toast.LENGTH_SHORT).show();
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Vendor Phone", vendor.getContact());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Phone number copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        }

        public void handleEmailClick(Context context, Vendor vendor) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + vendor.getEmail()));
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
