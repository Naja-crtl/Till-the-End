package com.example.weddingapp.LandingPage.Timeline;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.weddingapp.LandingPage.Budget.BudgetActivity;
import com.example.weddingapp.LandingPage.Category.CategoriesActivity;
import com.example.weddingapp.LandingPage.Guests.GuestsActivity;
import com.example.weddingapp.LandingPage.base.DrawerHeaderView;
import com.example.weddingapp.R;
import com.example.weddingapp.LandingPage.base.User;
import com.example.weddingapp.LandingPage.base.dashboard;
import com.example.weddingapp.auth.signin_page;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import android.Manifest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EventTimelineActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Timeline and event views
    private MaterialCalendarView calendarView;
    private ListView eventsListView;
    private EditText eventEditText;
    private Button addEventButton;
    private Map<String, List<String>> eventsMap;
    private ArrayAdapter<String> eventsAdapter;
    private String currentSelectedDate;

    // Navigation drawer views
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;

    // Firebase instances
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    // Notification permission request code
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "EventTimelineActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_timeline);

        // Initialize Firebase instances
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Check and request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        }

        // Setup navigation drawer views
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            if (!drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.openDrawer(navigationView);
            } else {
                drawerLayout.closeDrawer(navigationView);
            }
        });
        navigationView.setNavigationItemSelectedListener(this);

        // Setup timeline views
        calendarView = findViewById(R.id.calendarView);
        eventsListView = findViewById(R.id.eventsListView);
        calendarView = findViewById(R.id.calendarView);
        eventsListView = findViewById(R.id.eventsListView);


        eventsMap = new HashMap<>();

        // Create the custom ArrayAdapter with an overridden getView method
        eventsAdapter = new ArrayAdapter<String>(this, R.layout.item_event, R.id.eventDescription, new ArrayList<>()) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                // Get the event description (expected format: "Description - Time")
                String eventDescription = getItem(position);

                // Use ImageButton since the layout uses ImageButtons for delete and edit
                ImageButton deleteButton = view.findViewById(R.id.deleteButton);
                ImageButton editButton = view.findViewById(R.id.editButton);
                TextView eventTime = view.findViewById(R.id.eventTime);

                if ("No events for this date".equals(eventDescription)) {
                    deleteButton.setVisibility(View.GONE);
                    editButton.setVisibility(View.GONE);
                    eventTime.setVisibility(View.GONE);
                } else {
                    deleteButton.setVisibility(View.VISIBLE);
                    editButton.setVisibility(View.VISIBLE);
                    eventTime.setVisibility(View.VISIBLE);

                    // Set the event time from the event details (assumed format "Description - Time")
                    List<String> eventsForDate = eventsMap.get(currentSelectedDate);
                    if (eventsForDate != null && position < eventsForDate.size()) {
                        String eventDetails = eventsForDate.get(position);
                        String[] parts = eventDetails.split(" - ");
                        if (parts.length == 2) {
                            eventTime.setText(parts[1]);
                        }
                    }

                    // Delete button functionality
                    deleteButton.setOnClickListener(v -> {
                        List<String> eventsList = eventsMap.get(currentSelectedDate);
                        if (eventsList != null) {
                            eventsList.remove(eventDescription);
                            eventsMap.put(currentSelectedDate, eventsList);
                            loadEventsForDate(currentSelectedDate);
                            updateCalendarEvents();

                            String[] parts = eventDescription.split(" - ");
                            if (parts.length == 2) {
                                String description = parts[0];
                                String time = parts[1];
                                deleteEventFromFirestore(description, time);
                            }
                        }
                    });

                    // Edit button functionality
                    editButton.setOnClickListener(v -> {
                        // For the event string stored as "Description - Time" and the date is currentSelectedDate
                        String[] parts = eventDescription.split(" - ");
                        if (parts.length != 2) return; // Safety check
                        String currentDesc = parts[0];
                        String currentTime = parts[1];
                        // current date comes from currentSelectedDate
                        String currentDate = currentSelectedDate;

                        // Inflate the custom dialog view
                        LayoutInflater inflater = LayoutInflater.from(EventTimelineActivity.this);
                        View dialogView = inflater.inflate(R.layout.dialog_edit_event, null);

                        EditText descriptionEditText = dialogView.findViewById(R.id.editDescription);
                        EditText dateEditText = dialogView.findViewById(R.id.editDate);
                        EditText timeEditText = dialogView.findViewById(R.id.editTime);

                        // Prepopulate with current event details
                        descriptionEditText.setText(currentDesc);
                        dateEditText.setText(currentDate);
                        timeEditText.setText(currentTime);

                        // Set up the DatePicker for dateEditText
                        dateEditText.setOnClickListener(v1 -> {
                            // Use current date as default for picker; you can parse the current value if needed.
                            Calendar calendar = Calendar.getInstance();
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                Date date = sdf.parse(dateEditText.getText().toString());
                                if (date != null) calendar.setTime(date);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            DatePickerDialog datePickerDialog = new DatePickerDialog(EventTimelineActivity.this,
                                    (view1, year, month, dayOfMonth) -> {
                                        String newDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                                        dateEditText.setText(newDate);
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                            );
                            datePickerDialog.show();
                        });

                        // Set up the TimePicker for timeEditText
                        timeEditText.setOnClickListener(v12 -> {
                            Calendar calendar = Calendar.getInstance();
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                                Date time = sdf.parse(timeEditText.getText().toString());
                                if (time != null) calendar.setTime(time);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            TimePickerDialog timePickerDialog = new TimePickerDialog(EventTimelineActivity.this,
                                    (view12, hourOfDay, minute) -> {
                                        String newTime = String.format(Locale.getDefault(), "%02d:%02d %s",
                                                hourOfDay > 12 ? hourOfDay - 12 : hourOfDay,
                                                minute,
                                                hourOfDay >= 12 ? "PM" : "AM");
                                        timeEditText.setText(newTime);
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    false
                            );
                            timePickerDialog.show();
                        });

                        AlertDialog.Builder builder = new AlertDialog.Builder(EventTimelineActivity.this);
                        builder.setTitle("Edit Event");
                        builder.setView(dialogView);
                        builder.setPositiveButton("Save", (dialog, which) -> {
                            String newDesc = descriptionEditText.getText().toString().trim();
                            String newDate = dateEditText.getText().toString().trim();
                            String newTime = timeEditText.getText().toString().trim();
                            if (!newDesc.isEmpty() && !newDate.isEmpty() && !newTime.isEmpty()) {
                                // Update Firestore with new details
                                editEventInFirestore(currentDesc, currentDate, currentTime, newDesc, newDate, newTime);
                                // Update local eventsMap: remove from old date key and add under the new date key
                                List<String> eventsForOldDate = eventsMap.get(currentDate);
                                if (eventsForOldDate != null) {
                                    eventsForOldDate.remove(eventDescription);
                                    eventsMap.put(currentDate, eventsForOldDate);
                                }
                                List<String> eventsForNewDate = eventsMap.getOrDefault(newDate, new ArrayList<>());
                                eventsForNewDate.add(newDesc + " - " + newTime);
                                eventsMap.put(newDate, eventsForNewDate);
                                // Update currentSelectedDate if changed
                                currentSelectedDate = newDate;
                                loadEventsForDate(currentSelectedDate);
                                updateCalendarEvents();
                            }
                        });
                        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                        builder.show();
                    });
                }
                return view;
            }
        };
        eventsListView.setAdapter(eventsAdapter);

        // Set the current date based on the calendar view's date
        Calendar calendar = Calendar.getInstance();
        CalendarDay today = CalendarDay.from(calendar);
        currentSelectedDate = formatDate(today);

        // Load saved events from Firestore
        loadEventsFromFirestore();

        // Listen for date changes on the calendar view
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            currentSelectedDate = formatDate(date);
            loadEventsForDate(currentSelectedDate);
        });

        // Handle adding new events
        Button showAddEventButton = findViewById(R.id.showAddEventButton); // Your existing Add Event trigger

        showAddEventButton.setOnClickListener(v -> {
            LayoutInflater inflater = LayoutInflater.from(EventTimelineActivity.this);
            View dialogView = inflater.inflate(R.layout.dialog_add_event, null);

            EditText inputDescription = dialogView.findViewById(R.id.inputDescription);
            EditText inputTime = dialogView.findViewById(R.id.inputTime);

            inputTime.setOnClickListener(timeView -> {
                Calendar timePickerCalendar = Calendar.getInstance();
                int hour = timePickerCalendar.get(Calendar.HOUR_OF_DAY);
                int minute = timePickerCalendar.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        EventTimelineActivity.this,
                        (view, hourOfDay, minute1) -> {
                            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s",
                                    hourOfDay > 12 ? hourOfDay - 12 : hourOfDay,
                                    minute1,
                                    hourOfDay >= 12 ? "PM" : "AM");
                            inputTime.setText(formattedTime);
                        },
                        hour, minute, false
                );
                timePickerDialog.show();
            });

            AlertDialog.Builder builder = new AlertDialog.Builder(EventTimelineActivity.this);
            builder.setTitle("Add New Event");
            builder.setView(dialogView);
            builder.setPositiveButton("Save", (dialog, which) -> {
                String desc = inputDescription.getText().toString().trim();
                String time = inputTime.getText().toString().trim();

                if (!desc.isEmpty() && !time.isEmpty()) {
                    Event event = new Event(currentSelectedDate, desc, time);
                    String userId = firebaseAuth.getCurrentUser().getUid();

                    firestore.collection("users")
                            .document(userId)
                            .collection("events")
                            .add(event)
                            .addOnSuccessListener(docRef -> {
                                Toast.makeText(EventTimelineActivity.this, "Event added", Toast.LENGTH_SHORT).show();

                                List<String> events = eventsMap.getOrDefault(currentSelectedDate, new ArrayList<>());
                                events.add(desc + " - " + time);
                                eventsMap.put(currentSelectedDate, events);
                                loadEventsForDate(currentSelectedDate);
                                updateCalendarEvents();
                                scheduleNotification(desc, currentSelectedDate, time);
                            })
                            .addOnFailureListener(e -> Toast.makeText(EventTimelineActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } else {
                    Toast.makeText(EventTimelineActivity.this, "Please enter both fields", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.show();
        });


        // Update the navigation drawer header with couples data
        fetchCouplesData();
    }

    // Format CalendarDay to dd/MM/yyyy
    private String formatDate(CalendarDay date) {
        return String.format(Locale.getDefault(), "%02d/%02d/%04d", date.getDay(), date.getMonth() + 1, date.getYear());
    }

    // Load events for a given date from the local eventsMap and update the ListView
    private void loadEventsForDate(String date) {
        List<String> events = eventsMap.get(date);
        if (events == null || events.isEmpty()) {
            events = new ArrayList<>();
            events.add("No events for this date");
        } else {
            // Sort events by time (assuming each event string is "Description - hh:mm a")
            Collections.sort(events, new Comparator<String>() {
                @Override
                public int compare(String event1, String event2) {
                    // If either event is the placeholder, leave them as-is
                    if (event1.equals("No events for this date") || event2.equals("No events for this date")) {
                        return 0;
                    }
                    String[] parts1 = event1.split(" - ");
                    String[] parts2 = event2.split(" - ");
                    if (parts1.length < 2 || parts2.length < 2) {
                        return 0;
                    }
                    String time1 = parts1[1];
                    String time2 = parts2[1];
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    try {
                        Date d1 = sdf.parse(time1);
                        Date d2 = sdf.parse(time2);
                        return d1.compareTo(d2); // Ascending order: earliest first
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    return 0;
                }
            });
        }
        eventsAdapter.clear();
        eventsAdapter.addAll(events);
        eventsAdapter.notifyDataSetChanged();
    }

    // Load all saved events from Firestore for the current user and update the local eventsMap
    private void loadEventsFromFirestore() {
        String userId = firebaseAuth.getCurrentUser().getUid();
        firestore.collection("users")
                .document(userId)
                .collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Event event = document.toObject(Event.class);
                        if (event != null) {
                            String date = event.getDate();
                            String description = event.getDescription();
                            String time = event.getTime();

                            List<String> events = eventsMap.getOrDefault(date, new ArrayList<>());
                            events.add(description + " - " + time);
                            eventsMap.put(date, events);
                            Log.d(TAG, "Loaded event for " + date + ": " + description + " at " + time);
                        }
                    }
                    loadEventsForDate(currentSelectedDate);
                    updateCalendarEvents();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load events: " + e.getMessage());
                    Toast.makeText(EventTimelineActivity.this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Update the calendar view by decorating dates that have events
    private void updateCalendarEvents() {
        calendarView.removeDecorators();
        Set<CalendarDay> eventDays = new HashSet<>();
        for (String dateStr : eventsMap.keySet()) {
            try {
                String[] parts = dateStr.split("/");
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]) - 1;
                int year = Integer.parseInt(parts[2]);
                CalendarDay dayObj = CalendarDay.from(year, month, day);
                List<String> events = eventsMap.get(dateStr);
                if (events != null && !events.isEmpty() && !events.get(0).equals("No events for this date")) {
                    eventDays.add(dayObj);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing date: " + dateStr, e);
            }
        }
        if (!eventDays.isEmpty()) {
            calendarView.addDecorator(new EventDecorator(eventDays, this));
        }
        calendarView.invalidateDecorators();
    }

    // Custom decorator class to mark dates with events using a drawable indicator
    public class EventDecorator implements DayViewDecorator {
        private final Set<CalendarDay> dates;
        private final Drawable drawable;

        public EventDecorator(Set<CalendarDay> dates, EventTimelineActivity context) {
            this.dates = dates;
            // Use ContextCompat to retrieve drawable
            this.drawable = ContextCompat.getDrawable(context, R.drawable.event_dot);
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.setBackgroundDrawable(drawable);
        }
    }

    // Update the event in Firestore with new description, date, and time
    private void editEventInFirestore(String oldDesc, String oldDate, String oldTime, String newDesc, String newDate, String newTime) {
        String userId = firebaseAuth.getCurrentUser().getUid();
        firestore.collection("users")
                .document(userId)
                .collection("events")
                .whereEqualTo("description", oldDesc)
                .whereEqualTo("date", oldDate)
                .whereEqualTo("time", oldTime)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        document.getReference().update("description", newDesc, "date", newDate, "time", newTime)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(EventTimelineActivity.this, "Event updated!", Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(EventTimelineActivity.this, "Failed to update event: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(EventTimelineActivity.this, "Failed to find event for update: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // Delete an event from Firestore
    private void deleteEventFromFirestore(String description, String time) {
        String userId = firebaseAuth.getCurrentUser().getUid();
        firestore.collection("users")
                .document(userId)
                .collection("events")
                .whereEqualTo("description", description)
                .whereEqualTo("time", time)
                .whereEqualTo("date", currentSelectedDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(EventTimelineActivity.this, "Event deleted!", Toast.LENGTH_SHORT).show();
                                    updateCalendarEvents();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(EventTimelineActivity.this, "Failed to delete event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EventTimelineActivity.this, "Failed to find event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Schedule a notification for the event
    private void scheduleNotification(String eventDescription, String eventDate, String eventTime) {
        long triggerTime = getTriggerTime(eventDate, eventTime);
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("eventDescription", eventDescription);
        intent.putExtra("eventDate", eventDate);
        intent.putExtra("eventTime", eventTime);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    // Convert event date and time to a timestamp
    private long getTriggerTime(String eventDate, String eventTime) {
        try {
            String dateTimeString = eventDate + " " + eventTime;
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
            Date date = dateFormat.parse(dateTimeString);
            return date != null ? date.getTime() : 0;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // Fetch couples data from Firestore and update the navigation drawer header
    private void fetchCouplesData() {
        String userId = firebaseAuth.getCurrentUser().getUid();
        firestore.collection("users")
                .document(userId)
                .collection("couples")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String partner1 = doc.getString("partner1");
                        String partner2 = doc.getString("partner2");
                        String coupleNames = partner1 + " & " + partner2;
                        String email = firebaseAuth.getCurrentUser().getEmail();
                        String imageUrl = doc.getString("profileImageUrl");

                        User user = new User(coupleNames, email, imageUrl);
                        Log.d(TAG, "Updating drawer header with imageUrl: " + imageUrl);

                        View headerView = navigationView.getHeaderView(0);
                        if (headerView instanceof DrawerHeaderView) {
                            DrawerHeaderView drawerHeaderView = (DrawerHeaderView) headerView;
                            drawerHeaderView.setUser(user);
                        } else {
                            Log.w(TAG, "Header view is not an instance of DrawerHeaderView.");
                        }
                    } else {
                        Toast.makeText(EventTimelineActivity.this, "No couples data found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EventTimelineActivity.this, "Failed to fetch data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Handle navigation drawer item selections
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.navBudget) {
            startActivity(new Intent(this, BudgetActivity.class));
        } else if (id == R.id.navCategories) {
            startActivity(new Intent(this, CategoriesActivity.class));
        } else if (id == R.id.navHome) {
            startActivity(new Intent(this, dashboard.class));
        } else if (id == R.id.navTimeline) {
            // Already in timeline; no action needed.
        } else if (id == R.id.navGuestList) {
            startActivity(new Intent(this, GuestsActivity.class));
        } else if (id == R.id.navLogout) {
            firebaseAuth.signOut();
            startActivity(new Intent(this, signin_page.class));
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // Override back button press to close the drawer if open
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
        }
    }

    // Handle notification permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
