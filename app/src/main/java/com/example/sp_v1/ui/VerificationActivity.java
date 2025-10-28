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
import com.example.sp_v1.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VerificationActivity extends AppCompatActivity {

    // CHANGED: Constants updated for ID and Selfie
    private static final int PICK_ID_IMAGE_REQUEST = 1;
    private static final int PICK_SELFIE_IMAGE_REQUEST = 2;

    // CHANGED: UI variables updated
    private ImageView idImagePreview, selfieImagePreview;
    private Button btnUploadId, btnUploadSelfie, btnSubmit;
    private ProgressBar progressBar;

    // CHANGED: URI variables updated
    private Uri idImageUri;
    private Uri selfieImageUri;

    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        // Initialize Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        // The storage path can remain the same, it will just store ID documents now
        storageReference = FirebaseStorage.getInstance().getReference("verification_documents");
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // CHANGED: Initialize new Views using new IDs from the XML
        idImagePreview = findViewById(R.id.id_image_preview);
        selfieImagePreview = findViewById(R.id.selfie_image_preview);
        btnUploadId = findViewById(R.id.btn_upload_id_image);
        btnUploadSelfie = findViewById(R.id.btn_upload_selfie_image);
        btnSubmit = findViewById(R.id.btn_submit_for_review);
        progressBar = findViewById(R.id.progress_bar);

        // CHANGED: Set Click Listeners for new buttons
        btnUploadId.setOnClickListener(v -> openFileChooser(PICK_ID_IMAGE_REQUEST));
        btnUploadSelfie.setOnClickListener(v -> openFileChooser(PICK_SELFIE_IMAGE_REQUEST));
        btnSubmit.setOnClickListener(v -> submitIdVerification()); // RENAMED method call
    }

    private void openFileChooser(int requestCode) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            // CHANGED: Logic to handle new request codes and variables
            if (requestCode == PICK_ID_IMAGE_REQUEST) {
                idImageUri = data.getData();
                idImagePreview.setImageURI(idImageUri);
            } else if (requestCode == PICK_SELFIE_IMAGE_REQUEST) {
                selfieImageUri = data.getData();
                selfieImagePreview.setImageURI(selfieImageUri);
            }
        }
    }

    // RENAMED and UPDATED the submission method
    private void submitIdVerification() {
        // CHANGED: Check for new URI variables
        if (idImageUri == null || selfieImageUri == null) {
            Toast.makeText(this, "Please upload both your ID and Selfie", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        String userId = currentUser.getUid();
        // CHANGED: File references updated
        StorageReference idFileRef = storageReference.child(userId + "/id_image_" + UUID.randomUUID().toString());
        StorageReference selfieFileRef = storageReference.child(userId + "/selfie_image_" + UUID.randomUUID().toString());

        // Upload ID Image first
        idFileRef.putFile(idImageUri)
                .addOnSuccessListener(taskSnapshot -> taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(idUri -> {
                    // After ID image is uploaded, upload selfie image
                    selfieFileRef.putFile(selfieImageUri)
                            .addOnSuccessListener(taskSnapshot2 -> taskSnapshot2.getStorage().getDownloadUrl().addOnSuccessListener(selfieUri -> {
                                // Both images uploaded, now update database
                                updateUserForVerification(userId, idUri.toString(), selfieUri.toString());
                            }))
                            .addOnFailureListener(e -> handleUploadFailure(e.getMessage()));
                }))
                .addOnFailureListener(e -> handleUploadFailure(e.getMessage()));
    }

    // RENAMED and UPDATED the database update method
    private void updateUserForVerification(String userId, String idImageUrl, String selfieImageUrl) {
        Map<String, Object> updates = new HashMap<>();
        // CHANGED: Use new field names and status from User.java
        updates.put("partnerStatus", User.STATUS_PENDING_ID);
        updates.put("idImageUrl", idImageUrl);
        updates.put("selfieImageUrl", selfieImageUrl);

        databaseReference.child(userId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(VerificationActivity.this, "Documents submitted for review!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void handleUploadFailure(String message) {
        progressBar.setVisibility(View.GONE);
        btnSubmit.setEnabled(true);
        Toast.makeText(this, "Upload failed: " + message, Toast.LENGTH_SHORT).show();
    }
}