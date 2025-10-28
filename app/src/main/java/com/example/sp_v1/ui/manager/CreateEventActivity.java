package com.example.sp_v1.ui.manager;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sp_v1.R;
import com.example.sp_v1.models.EventPost;
import com.example.sp_v1.utils.DatabaseHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CreateEventActivity extends AppCompatActivity {

    private ImageView eventBannerImage;
    private Button btnUploadBanner, btnStartDate, btnEndDate, btnSubmitEvent;
    private TextInputEditText etEventName, etEventDescription;
    private TextView tvStartDate, tvEndDate;
    private ProgressBar progressBar;

    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;

    private Uri bannerImageUri;
    private long startDateTimestamp = 0;
    private long endDateTimestamp = 0;

    // Activity Result Launcher for picking an image
    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    bannerImageUri = uri;
                    eventBannerImage.setImageURI(bannerImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        dbHelper = new DatabaseHelper();
        mAuth = FirebaseAuth.getInstance();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Create New Event");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Views
        eventBannerImage = findViewById(R.id.event_banner_image);
        btnUploadBanner = findViewById(R.id.btn_upload_banner);
        etEventName = findViewById(R.id.et_event_name);
        etEventDescription = findViewById(R.id.et_event_description);
        btnStartDate = findViewById(R.id.btn_start_date);
        btnEndDate = findViewById(R.id.btn_end_date);
        tvStartDate = findViewById(R.id.tv_start_date);
        tvEndDate = findViewById(R.id.tv_end_date);
        btnSubmitEvent = findViewById(R.id.btn_submit_event);
        progressBar = findViewById(R.id.progress_bar);

        // Set Click Listeners
        btnUploadBanner.setOnClickListener(v -> mGetContent.launch("image/*"));
        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));
        btnSubmitEvent.setOnClickListener(v -> validateAndSubmitEvent());
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            // Set to start of the day
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            if (isStartDate) {
                startDateTimestamp = calendar.getTimeInMillis();
                tvStartDate.setText(formatDate(startDateTimestamp));
            } else {
                // Set to end of the day
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                endDateTimestamp = calendar.getTimeInMillis();
                tvEndDate.setText(formatDate(endDateTimestamp));
            }
        };

        new DatePickerDialog(this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    // Helper method to format date (since DateUtils.java was not provided)
    private String formatDate(long timestamp) {
        if (timestamp == 0) return "Not Set";
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
        return sdf.format(timestamp);
    }

    private void validateAndSubmitEvent() {
        String eventName = etEventName.getText().toString().trim();
        String description = etEventDescription.getText().toString().trim();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bannerImageUri == null) {
            Toast.makeText(this, "Please upload a banner image.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(eventName)) {
            etEventName.setError("Event name is required.");
            return;
        }

        if (TextUtils.isEmpty(description)) {
            etEventDescription.setError("Description is required.");
            return;
        }

        if (startDateTimestamp == 0) {
            Toast.makeText(this, "Please select a start date.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endDateTimestamp == 0) {
            Toast.makeText(this, "Please select an end date.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endDateTimestamp <= startDateTimestamp) {
            Toast.makeText(this, "End date must be after the start date.", Toast.LENGTH_LONG).show();
            return;
        }

        // All valid, start upload
        setLoading(true);
        String managerId = currentUser.getUid();

        // 1. Upload Image
        dbHelper.uploadEventBanner(bannerImageUri, managerId, new DatabaseHelper.StorageUploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                // 2. Create EventPost object
                EventPost eventPost = new EventPost(
                        managerId,
                        eventName,
                        description,
                        downloadUrl,
                        startDateTimestamp,
                        endDateTimestamp
                );

                // 3. Save EventPost to Database
                dbHelper.createEventPost(eventPost, new DatabaseHelper.DatabaseCallback() {
                    @Override
                    public void onSuccess() {
                        setLoading(false);
                        Toast.makeText(CreateEventActivity.this, "Event submitted for approval!", Toast.LENGTH_LONG).show();
                        finish(); // Close activity and go back to MyEventsFragment
                    }

                    @Override
                    public void onError(String error) {
                        setLoading(false);
                        Toast.makeText(CreateEventActivity.this, "Failed to save event: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                Toast.makeText(CreateEventActivity.this, "Image upload failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSubmitEvent.setEnabled(!isLoading);
        btnUploadBanner.setEnabled(!isLoading);
        btnStartDate.setEnabled(!isLoading);
        btnEndDate.setEnabled(!isLoading);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // Go back when back arrow is pressed
        return true;
    }
}