package com.example.sp_v1.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sp_v1.R;
import com.example.sp_v1.models.ParkingSpot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpotVerificationActivity extends AppCompatActivity {

    public static final String EXTRA_SPOT_ID = "com.example.sp_v1.ui.SPOT_ID";
    private static final int PICK_DEED_IMAGE_REQUEST = 1;

    private ImageView deedImagePreview;
    private Button btnUploadDeed, btnSubmit;
    private ProgressBar progressBar;

    private Uri deedImageUri;
    private String spotId;
    private String userId;

    private StorageReference storageReference;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spot_verification);

        spotId = getIntent().getStringExtra(EXTRA_SPOT_ID);
        if (spotId == null || spotId.isEmpty()) {
            Toast.makeText(this, "Error: Parking Spot ID is missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Set up Firebase references
        storageReference = FirebaseStorage.getInstance().getReference("deed_documents");
        databaseReference = FirebaseDatabase.getInstance().getReference("parking_spots");

        // Initialize Views
        deedImagePreview = findViewById(R.id.deed_image_preview);
        btnUploadDeed = findViewById(R.id.btn_upload_deed_image);
        btnSubmit = findViewById(R.id.btn_submit_deed_for_review);
        progressBar = findViewById(R.id.spot_verification_progress_bar);

        // Set Click Listeners
        btnUploadDeed.setOnClickListener(v -> openFileChooser());
        btnSubmit.setOnClickListener(v -> submitDeedForVerification());
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_DEED_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_DEED_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            deedImageUri = data.getData();
            deedImagePreview.setImageURI(deedImageUri);
        }
    }

    private void submitDeedForVerification() {
        if (deedImageUri == null) {
            Toast.makeText(this, "Please select a document image to upload.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        // Create a unique path for the document using the user and spot IDs
        final StorageReference fileReference = storageReference.child(userId + "/" + spotId + "/" + UUID.randomUUID().toString());

        fileReference.putFile(deedImageUri)
                .addOnSuccessListener(taskSnapshot -> taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    updateSpotVerificationStatus(downloadUrl);
                }))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(SpotVerificationActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateSpotVerificationStatus(String downloadUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("spotVerificationStatus", ParkingSpot.STATUS_SPOT_PENDING_DEED);
        updates.put("deedImageUrl", downloadUrl);

        databaseReference.child(spotId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SpotVerificationActivity.this, "Document submitted for review!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(SpotVerificationActivity.this, "Failed to update spot status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}