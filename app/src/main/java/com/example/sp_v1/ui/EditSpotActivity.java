package com.example.sp_v1.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.sp_v1.R;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.utils.DatabaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EditSpotActivity extends AppCompatActivity {

    private TextInputEditText spotNameEditText;
    private TextInputEditText totalSpacesEditText;
    private MaterialButton saveSpotButton;
    private TextView titleTextView;
    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private String currentSpotId;
    private ParkingSpot spotToEdit;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_spot);

        dbHelper = new DatabaseHelper();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        titleTextView = findViewById(R.id.edit_spot_title);
        spotNameEditText = findViewById(R.id.spot_name_edit_text);
        totalSpacesEditText = findViewById(R.id.total_spaces_edit_text);
        saveSpotButton = findViewById(R.id.save_spot_button);

        saveSpotButton.setOnClickListener(v -> saveParkingSpot());

        if (getIntent().hasExtra("SPOT_ID")) {
            currentSpotId = getIntent().getStringExtra("SPOT_ID");
            titleTextView.setText("Edit Parking Spot");
            loadSpotData();
        } else {
            titleTextView.setText("Add New Parking Spot");
            fetchManagerLocation(); // CORRECTED: Only fetch location when adding a new spot
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

        } else { // Add Mode
            if (currentLocation == null) {
                Toast.makeText(this, "Location not yet available. Please wait or enable location services.", Toast.LENGTH_SHORT).show();
                return;
            }
            spotToSave = new ParkingSpot(spotName, currentLocation.getLatitude(), currentLocation.getLongitude(),
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

    private void fetchManagerLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLocation = location;
                } else {
                    Toast.makeText(this, "Could not get current location.", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchManagerLocation();
        } else {
            Toast.makeText(this, "Location permission is required to add a new spot.", Toast.LENGTH_SHORT).show();
        }
    }
}