package com.example.weddingapp.LandingPage.Guests.Invitation;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weddingapp.R;
import com.example.weddingapp.LandingPage.Guests.GuestsActivity;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InvitationCardActivity extends AppCompatActivity {

    private EditText etBrideName, etGroomName, etWeddingDate, etWeddingVenue, etCustomMessage;
    private Button btnAddEvent, btnAddGiftMethod, btnSave;
    private RecyclerView rvProgramEvents, rvGiftMethods;

    private ProgramEventAdapter eventAdapter;
    private GiftMethodAdapter giftAdapter;
    private final List<programEvent> eventList = new ArrayList<>();
    private final List<giftMethod>  giftList  = new ArrayList<>();

    private FirebaseFirestore firestore;
    private String userId, invId;

    enum LoadState { IDLE, LOADING }
    private LoadState loadState = LoadState.IDLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitation_card);

        // bind views
        etBrideName     = findViewById(R.id.etBrideName);
        etGroomName     = findViewById(R.id.etGroomName);
        etWeddingDate   = findViewById(R.id.etWeddingDate);
        etWeddingVenue  = findViewById(R.id.etWeddingVenue);
        etCustomMessage = findViewById(R.id.etCustomMessage);

        btnAddEvent     = findViewById(R.id.btnAddEvent);
        btnAddGiftMethod= findViewById(R.id.btnAddGiftMethod);
        btnSave         = findViewById(R.id.btnSendCard);

        rvProgramEvents = findViewById(R.id.rvProgramEvents);
        rvGiftMethods   = findViewById(R.id.rvGiftMethods);

        // date-picker hookup
        etWeddingDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Wedding Date")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .setCalendarConstraints(
                            new CalendarConstraints.Builder()
                                    .setValidator(DateValidatorPointForward.now())
                                    .build()
                    )
                    .build();

            picker.addOnPositiveButtonClickListener(selection -> {
                // format the picked timestamp
                String formatted = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(new Date(selection));
                etWeddingDate.setText(formatted);
            });
            picker.show(getSupportFragmentManager(), "WEDDING_DATE_PICKER");
        });

        // init Firestore & Auth
        firestore = FirebaseFirestore.getInstance();
        userId    = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // find or create invitation doc
        firestore.collection("users").document(userId)
                .collection("invitations")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) {
                        DocumentSnapshot snap = qs.getDocuments().get(0);
                        invId = snap.getId();
                    } else {
                        invId = firestore.collection("users").document(userId)
                                .collection("invitations")
                                .document().getId();
                        firestore.collection("users").document(userId)
                                .collection("invitations")
                                .document(invId)
                                .set(Collections.singletonMap("userId", userId),
                                        SetOptions.merge());
                    }
                    setupAdapters();
                    setupButtons();
                    loadInvitation();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error loading invitation: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }
    private void setupAdapters() {
        eventAdapter = new ProgramEventAdapter(
                eventList,
                firestore.collection("users").document(userId)
                        .collection("invitations").document(invId),
                this::loadInvitation
        );
        rvProgramEvents.setLayoutManager(new LinearLayoutManager(this));
        rvProgramEvents.setAdapter(eventAdapter);

        giftAdapter = new GiftMethodAdapter(
                giftList,
                firestore.collection("users").document(userId)
                        .collection("invitations").document(invId),
                this::loadInvitation
        );
        rvGiftMethods.setLayoutManager(new LinearLayoutManager(this));
        rvGiftMethods.setAdapter(giftAdapter);
    }

    private void setupButtons() {
        btnAddEvent     .setOnClickListener(v -> showAddEventDialog());
        btnAddGiftMethod.setOnClickListener(v -> showAddGiftDialog());
        btnSave         .setOnClickListener(v -> saveInvitation());
    }

    private void loadInvitation() {
        if (loadState == LoadState.LOADING) return;
        loadState = LoadState.LOADING;

        firestore.collection("users").document(userId)
                .collection("invitations")
                .document(invId)
                .get()
                .addOnSuccessListener(this::onInvitationLoaded)
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to load invitation: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    loadState = LoadState.IDLE;
                });
    }

    private void onInvitationLoaded(DocumentSnapshot doc) {
        loadState = LoadState.IDLE;
        if (!doc.exists()) return;

        if (doc.contains("brideName"))    etBrideName .setText(doc.getString("brideName"));
        if (doc.contains("groomName"))    etGroomName .setText(doc.getString("groomName"));
        if (doc.contains("weddingDate"))  etWeddingDate.setText(doc.getString("weddingDate"));
        if (doc.contains("venue"))        etWeddingVenue.setText(doc.getString("venue"));
        if (doc.contains("customMessage"))etCustomMessage.setText(doc.getString("customMessage"));

        // load program events
        List<Map<String,Object>> evs = (List<Map<String,Object>>) doc.get("programEvents");
        eventList.clear();
        if (evs != null) {
            for (Map<String,Object> m : evs) {
                programEvent pe = new programEvent();
                pe.setTime((String) m.get("time"));
                pe.setTitle((String) m.get("title"));
                pe.setMessage((String) m.get("message"));
                eventList.add(pe);
            }
            Collections.sort(eventList, (a,b) -> a.getTime().compareTo(b.getTime()));
        }
        eventAdapter.notifyDataSetChanged();

        // load gift methods
        List<Map<String,Object>> gfs = (List<Map<String,Object>>) doc.get("giftMethods");
        giftList.clear();
        if (gfs != null) {
            for (Map<String,Object> m : gfs) {
                giftMethod gm = new giftMethod();
                gm.setType((String) m.get("type"));
                gm.setDetails((Map<String,Object>) m.get("details"));
                giftList.add(gm);
            }
        }
        giftAdapter.notifyDataSetChanged();
    }

    private void showAddEventDialog() {
        View dv = getLayoutInflater().inflate(R.layout.dialog_program_event, null);
        EditText etT  = dv.findViewById(R.id.tvEventTime);
        EditText etTi = dv.findViewById(R.id.tvEventTitle);
        EditText etM  = dv.findViewById(R.id.tvEventMessage);

        etT.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            new TimePickerDialog(
                    this,
                    (picker,h,m) -> etT.setText(
                            String.format(Locale.getDefault(), "%02d:%02d", h, m)
                    ),
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
            ).show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Add Program Event")
                .setView(dv)
                .setPositiveButton("Add", (d,w) -> {
                    String time  = etT .getText().toString().trim();
                    String title = etTi.getText().toString().trim();
                    String msg   = etM .getText().toString().trim();
                    if (time.isEmpty() || title.isEmpty()) {
                        Toast.makeText(this, "Time & title required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String,Object> ev = new HashMap<>();
                    ev.put("time",    time);
                    ev.put("title",   title);
                    ev.put("message", msg);

                    firestore.collection("users").document(userId)
                            .collection("invitations").document(invId)
                            .set(Collections.singletonMap(
                                    "programEvents",
                                    FieldValue.arrayUnion(ev)
                            ), SetOptions.merge())
                            .addOnSuccessListener(__ -> loadInvitation())
                            .addOnFailureListener(ex ->
                                    Toast.makeText(this,
                                            "Failed to add event: " + ex.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddGiftDialog() {
        View dv = getLayoutInflater().inflate(R.layout.dialog_add_gift, null);
        EditText etType    = dv.findViewById(R.id.etGiftType);
        EditText etDetails = dv.findViewById(R.id.etGiftDetails);

        new AlertDialog.Builder(this)
                .setTitle("Add Gift Method")
                .setView(dv)
                .setPositiveButton("Add", (d,w) -> {
                    String type = etType.getText().toString().trim();
                    if (type.isEmpty()) {
                        Toast.makeText(this, "Type required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String,Object> gm = new HashMap<>();
                    gm.put("type",    type);
                    gm.put("details", Collections.singletonMap("info", etDetails.getText().toString().trim()));

                    firestore.collection("users").document(userId)
                            .collection("invitations").document(invId)
                            .set(Collections.singletonMap(
                                    "giftMethods",
                                    FieldValue.arrayUnion(gm)
                            ), SetOptions.merge())
                            .addOnSuccessListener(__ -> loadInvitation())
                            .addOnFailureListener(ex ->
                                    Toast.makeText(this,
                                            "Failed to add gift: " + ex.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveInvitation() {
        String bride = etBrideName    .getText().toString().trim();
        String groom = etGroomName    .getText().toString().trim();
        String date  = etWeddingDate  .getText().toString().trim();
        String venue = etWeddingVenue .getText().toString().trim();
        String msg   = etCustomMessage.getText().toString().trim();

        if (bride.isEmpty() || groom.isEmpty() || date.isEmpty() || venue.isEmpty()) {
            Toast.makeText(this,
                    "Please fill bride, groom, date & venue",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Map<String,Object> data = new HashMap<>();
        data.put("brideName",    bride);
        data.put("groomName",    groom);
        data.put("weddingDate",  date);
        data.put("venue",        venue);
        data.put("customMessage",msg);
        data.put("updatedAt",    FieldValue.serverTimestamp());

        firestore.collection("users").document(userId)
                .collection("invitations").document(invId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(__ -> {
                    Toast.makeText(this, "Invitation saved!", Toast.LENGTH_SHORT).show();
                    startActivity(
                            new Intent(InvitationCardActivity.this, GuestsActivity.class)
                    );
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Save failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }
}
