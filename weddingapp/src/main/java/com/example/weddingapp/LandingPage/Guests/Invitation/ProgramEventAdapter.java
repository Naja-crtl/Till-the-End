package com.example.weddingapp.LandingPage.Guests.Invitation;

import android.app.TimePickerDialog;
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

public class ProgramEventAdapter extends RecyclerView.Adapter<ProgramEventAdapter.ViewHolder> {

    public interface OnDataChanged { void onChange(); }

    private final List<programEvent> events;
    private final DocumentReference invitationDoc;
    private final OnDataChanged listener;

    public ProgramEventAdapter(List<programEvent> events,
                               DocumentReference invitationDoc,
                               OnDataChanged listener) {
        this.events = events;
        this.invitationDoc = invitationDoc;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_program_event, parent, false);
        return new ViewHolder(v);
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        programEvent evt = events.get(pos);
        h.tvTime.setText(evt.getTime());
        h.tvTitle.setText(evt.getTitle());
        h.tvMessage.setText(evt.getMessage());

        // DELETE
        h.btnDelete.setOnClickListener(v -> {
            // build map as it was stored
            Map<String,Object> evMap = new HashMap<>();
            evMap.put("time",    evt.getTime());
            evMap.put("title",   evt.getTitle());
            evMap.put("message", evt.getMessage());

            invitationDoc.set(
                    Collections.singletonMap("programEvents",
                            FieldValue.arrayRemove(evMap)),
                    SetOptions.merge()
            ).addOnSuccessListener(__ -> {
                events.remove(pos);
                notifyItemRemoved(pos);
                listener.onChange();
                Toast.makeText(v.getContext(), "Event deleted", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e ->
                    Toast.makeText(v.getContext(),
                            "Delete failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show()
            );
        });

        // EDIT
        h.btnEdit.setOnClickListener(v -> showEditDialog(v.getContext(), evt, pos));
    }

    @Override public int getItemCount() { return events.size(); }

    private void showEditDialog(Context ctx, programEvent oldEvt, int pos) {
        View dv = LayoutInflater.from(ctx)
                .inflate(R.layout.dialog_program_event, null);
        EditText etTime    = dv.findViewById(R.id.tvEventTime);
        EditText etTitle   = dv.findViewById(R.id.tvEventTitle);
        EditText etMessage = dv.findViewById(R.id.tvEventMessage);

        // prefill
        etTime   .setText(oldEvt.getTime());
        etTitle  .setText(oldEvt.getTitle());
        etMessage.setText(oldEvt.getMessage());

        etTime.setOnClickListener(view -> {
            Calendar now = Calendar.getInstance();
            new TimePickerDialog(
                    ctx,
                    (p,h,m) -> etTime.setText(String.format("%02d:%02d", h, m)),
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
            ).show();
        });

        new AlertDialog.Builder(ctx)
                .setTitle("Edit Program Event")
                .setView(dv)
                .setPositiveButton("Save", (d, w) -> {
                    String newTime = etTime.getText().toString().trim();
                    String newTitle= etTitle.getText().toString().trim();
                    String newMsg  = etMessage.getText().toString().trim();

                    // remove old, add new
                    Map<String,Object> oldMap = new HashMap<>();
                    oldMap.put("time", oldEvt.getTime());
                    oldMap.put("title", oldEvt.getTitle());
                    oldMap.put("message", oldEvt.getMessage());

                    Map<String,Object> newMap = new HashMap<>();
                    newMap.put("time", newTime);
                    newMap.put("title", newTitle);
                    newMap.put("message", newMsg);

                    invitationDoc
                            .update("programEvents", FieldValue.arrayRemove(oldMap))
                            .addOnSuccessListener(__ ->
                                    invitationDoc
                                            .update("programEvents", FieldValue.arrayUnion(newMap))
                                            .addOnSuccessListener(__2 -> {
                                                // update local list & refresh
                                                oldEvt.setTime(newTime);
                                                oldEvt.setTitle(newTitle);
                                                oldEvt.setMessage(newMsg);
                                                notifyItemChanged(pos);
                                                listener.onChange();
                                                Toast.makeText(ctx, "Event updated", Toast.LENGTH_SHORT).show();
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
        TextView tvTime, tvTitle, tvMessage;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            btnEdit    = itemView.findViewById(R.id.btnEditEvent);
            btnDelete  = itemView.findViewById(R.id.btnDeleteEvent);
            tvTime     = itemView.findViewById(R.id.tvEventTime);
            tvTitle    = itemView.findViewById(R.id.tvEventTitle);
            tvMessage  = itemView.findViewById(R.id.tvEventMessage);
        }
    }
}
