package com.example.sp_v1.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.sp_v1.R;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.utils.DatabaseHelper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Locale;

public class EditSpotActivity extends AppCompatActivity {

    private TextInputEditText spotNameEditText;
    private TextInputEditText totalSpacesEditText;
    private MaterialButton saveSpotButton;
    private MaterialButton pickLocationButton;
    private TextView titleTextView;
    private TextView locationTextView;

    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;

    private String currentSpotId;
    private ParkingSpot spotToEdit;
    private LatLng selectedLocation;

    private static final int MAP_PICKER_REQUEST_CODE = 999;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_spot);

        dbHelper = new DatabaseHelper();
        mAuth = FirebaseAuth.getInstance();

        titleTextView = findViewById(R.id.edit_spot_title);
        spotNameEditText = findViewById(R.id.spot_name_edit_text);
        totalSpacesEditText = findViewById(R.id.total_spaces_edit_text);
        saveSpotButton = findViewById(R.id.save_spot_button);
        locationTextView = findViewById(R.id.location_text_view);
        pickLocationButton = findViewById(R.id.pick_location_button);

        saveSpotButton.setOnClickListener(v -> saveParkingSpot());

        pickLocationButton.setOnClickListener(v -> {
            Intent intent = new Intent(EditSpotActivity.this, MapPickerActivity.class);
            startActivityForResult(intent, MAP_PICKER_REQUEST_CODE);
        });

        if (getIntent().hasExtra("SPOT_ID")) {
            currentSpotId = getIntent().getStringExtra("SPOT_ID");
            titleTextView.setText("Edit Parking Spot");
            loadSpotData();
        } else {
            titleTextView.setText("Add New Parking Spot");
        }
    }

    private void loadSpotData() {
        FirebaseDatabase.getInstance().getReference("parking_spots").child(currentSpotId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        spotToEdit = snapshot.getValue(ParkingSpot.class);
                        if (spotToEdit != null) {
                            spotToEdit.setId(snapshot.getKey());
                            spotNameEditText.setText(spotToEdit.getName());
                            totalSpacesEditText.setText(String.valueOf(spotToEdit.getTotalSlots()));

                            selectedLocation = new LatLng(spotToEdit.getLatitude(), spotToEdit.getLongitude());
                            locationTextView.setText(String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f",
                                    selectedLocation.latitude, selectedLocation.longitude));
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(EditSpotActivity.this, "Failed to load spot data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveParkingSpot() {
        String spotName = spotNameEditText.getText().toString().trim();
        String totalSpacesStr = totalSpacesEditText.getText().toString().trim();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (TextUtils.isEmpty(spotName) || TextUtils.isEmpty(totalSpacesStr) || currentUser == null) {
            Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalSpaces = Integer.parseInt(totalSpacesStr);
        ParkingSpot spotToSave;

        if (currentSpotId != null && spotToEdit != null) { // Edit Mode
            spotToSave = spotToEdit;
            spotToSave.setName(spotName);
            int occupied = spotToSave.getTotalSlots() - spotToSave.getAvailableSpaces();
            spotToSave.setTotalSlots(totalSpaces);
            spotToSave.setAvailableSpaces(Math.max(0, totalSpaces - occupied));

            if (selectedLocation != null) {
                spotToSave.setLatitude(selectedLocation.latitude);
                spotToSave.setLongitude(selectedLocation.longitude);
            }

        } else { // Add Mode - This part is correct
            if (selectedLocation == null) {
                Toast.makeText(this, "Please pick a location on the map.", Toast.LENGTH_SHORT).show();
                return;
            }
            spotToSave = new ParkingSpot(spotName, selectedLocation.latitude, selectedLocation.longitude,
                    true, currentUser.getUid(), totalSpaces, totalSpaces);
        }

        dbHelper.saveParkingSpot(spotToSave, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(EditSpotActivity.this, "Parking spot saved!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(EditSpotActivity.this, "Failed to save spot.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MAP_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                double latitude = data.getDoubleExtra("picked_latitude", 0);
                double longitude = data.getDoubleExtra("picked_longitude", 0);
                selectedLocation = new LatLng(latitude, longitude);

                locationTextView.setText(String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f", latitude, longitude));
            }
        }
    }
}