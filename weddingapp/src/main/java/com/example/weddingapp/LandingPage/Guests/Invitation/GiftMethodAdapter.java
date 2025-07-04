package com.example.weddingapp.LandingPage.Guests.Invitation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weddingapp.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import java.util.*;

public class GiftMethodAdapter extends RecyclerView.Adapter<GiftMethodAdapter.ViewHolder> {

    public interface OnDataChanged { void onChange(); }

    private final List<giftMethod>      gifts;
    private final DocumentReference     invitationDoc;
    private final OnDataChanged         listener;

    // Updated constructor to accept the listener lambda
    public GiftMethodAdapter(List<giftMethod> gifts,
                             DocumentReference invitationDoc,
                             OnDataChanged listener) {
        this.gifts         = gifts;
        this.invitationDoc = invitationDoc;
        this.listener      = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gift_method, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        giftMethod gm = gifts.get(pos);
        h.tvType   .setText(gm.getType());
        h.tvDetails.setText(gm.getDetails().get("info").toString());

        // DELETE
        h.btnDelete.setOnClickListener(v -> {
            Map<String,Object> old = new HashMap<>();
            old.put("type",    gm.getType());
            old.put("details", gm.getDetails());

            invitationDoc.set(
                    Collections.singletonMap("giftMethods",
                            FieldValue.arrayRemove(old)),
                    SetOptions.merge()
            ).addOnSuccessListener(__ -> {
                gifts.remove(pos);
                notifyItemRemoved(pos);
                listener.onChange();      // notify activity to refresh if needed
                Toast.makeText(v.getContext(), "Gift removed", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e ->
                    Toast.makeText(v.getContext(),
                            "Delete failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show()
            );
        });

        // EDIT
        h.btnEdit.setOnClickListener(v ->
                showEditDialog(v.getContext(), gm, pos)
        );
    }

    @Override public int getItemCount() { return gifts.size(); }

    private void showEditDialog(Context ctx, giftMethod oldGm, int pos) {
        View dv = LayoutInflater.from(ctx)
                .inflate(R.layout.dialog_add_gift, null);
        EditText etType    = dv.findViewById(R.id.etGiftType);
        EditText etDetails = dv.findViewById(R.id.etGiftDetails);

        etType   .setText(oldGm.getType());
        etDetails.setText(oldGm.getDetails().get("info").toString());

        new AlertDialog.Builder(ctx)
                .setTitle("Edit Gift Method")
                .setView(dv)
                .setPositiveButton("Save", (d, w) -> {
                    String newType = etType.getText().toString().trim();
                    String newInfo = etDetails.getText().toString().trim();

                    Map<String,Object> old = new HashMap<>();
                    old.put("type",    oldGm.getType());
                    old.put("details", oldGm.getDetails());

                    Map<String,Object> nw = new HashMap<>();
                    nw.put("type",    newType);
                    nw.put("details", Collections.singletonMap("info", newInfo));

                    invitationDoc
                            .update("giftMethods", FieldValue.arrayRemove(old))
                            .addOnSuccessListener(__ ->
                                    invitationDoc
                                            .update("giftMethods", FieldValue.arrayUnion(nw))
                                            .addOnSuccessListener(__2 -> {
                                                oldGm.setType(newType);
                                                oldGm.setDetails(Collections.singletonMap("info", newInfo));
                                                notifyItemChanged(pos);
                                                listener.onChange();
                                                Toast.makeText(ctx, "Gift updated", Toast.LENGTH_SHORT).show();
                                            })
                            ).addOnFailureListener(e ->
                                    Toast.makeText(ctx,
                                            "Update failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show()
                            );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView btnEdit, btnDelete;
        TextView tvType, tvDetails;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            btnEdit    = itemView.findViewById(R.id.btnEditGift);
            btnDelete  = itemView.findViewById(R.id.btnDeleteGift);
            tvType     = itemView.findViewById(R.id.tvGiftType);
            tvDetails  = itemView.findViewById(R.id.tvGiftDetails);
        }
    }
}
