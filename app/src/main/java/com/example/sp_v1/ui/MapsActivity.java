package com.example.sp_v1.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.sp_v1.R;
import com.example.sp_v1.auth.LoginActivity;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.models.User;
import com.example.sp_v1.utils.DatabaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private FirebaseAuth mAuth;
    private GoogleMap mMap;
    private DatabaseHelper dbHelper;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private CardView spotDetailsCard;
    private TextView spotNameText;
    private TextView spotAvailabilityText;
    private MaterialButton reserveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        spotDetailsCard = findViewById(R.id.spot_details_card);
        spotNameText = findViewById(R.id.spot_name_text);
        spotAvailabilityText = findViewById(R.id.spot_availability_text);
        reserveButton = findViewById(R.id.reserve_button);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        checkUserRole();

        FloatingActionButton logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        fetchAndDisplayParkingSpots();
        enableMyLocation();

        mMap.setOnMarkerClickListener(marker -> {
            ParkingSpot spot = (ParkingSpot) marker.getTag();
            if (spot != null) {
                showSpotDetails(spot);
            }
            return true;
        });

        mMap.setOnMapClickListener(latLng -> hideSpotDetails());
    }

    private void showSpotDetails(ParkingSpot spot) {
        spotNameText.setText(spot.getName());
        String availability = "Available Spaces: " + spot.getAvailableSpaces();
        spotAvailabilityText.setText(availability);

        if (spot.getAvailable()) {
            reserveButton.setVisibility(View.VISIBLE);
            reserveButton.setOnClickListener(v -> handleReservationClick(spot));
        } else {
            reserveButton.setVisibility(View.GONE);
        }

        spotDetailsCard.setVisibility(View.VISIBLE);
        spotDetailsCard.setAlpha(0.0f);
        spotDetailsCard.setTranslationX(-spotDetailsCard.getWidth());

        spotDetailsCard.animate()
                .translationX(0)
                .alpha(1.0f)
                .setDuration(500);
    }

    private void handleReservationClick(ParkingSpot spot) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "You must be logged in to reserve.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Check if user already has an active reservation
        dbHelper.checkForActiveReservation(userId, hasActiveReservation -> {
            if (hasActiveReservation) {
                Toast.makeText(MapsActivity.this, "You already have an active reservation.", Toast.LENGTH_SHORT).show();
            } else {
                // 2. Show confirmation dialog
                new AlertDialog.Builder(this)
                        .setTitle("Confirm Reservation")
                        .setMessage("Do you want to reserve a spot at " + spot.getName() + " for 1 hour?")
                        .setPositiveButton("Confirm", (dialog, which) -> {
                            // 3. Create the reservation
                            proceedWithReservation(spot, userId);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    private void proceedWithReservation(ParkingSpot spot, String userId) {
        dbHelper.createReservation(spot, userId, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(MapsActivity.this, "Reservation successful!", Toast.LENGTH_SHORT).show();
                hideSpotDetails();
            } else {
                Toast.makeText(MapsActivity.this, "Reservation failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void hideSpotDetails() {
        if (spotDetailsCard.getVisibility() == View.VISIBLE) {
            spotDetailsCard.animate()
                    .translationX(-spotDetailsCard.getWidth())
                    .alpha(0.0f)
                    .setDuration(500)
                    .withEndAction(() -> spotDetailsCard.setVisibility(View.INVISIBLE));
        }
    }

    private void fetchAndDisplayParkingSpots() {
        dbHelper.getParkingSpots(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mMap == null) return;
                mMap.clear();
                hideSpotDetails();

                for (DataSnapshot spotSnapshot : snapshot.getChildren()) {
                    ParkingSpot spot = spotSnapshot.getValue(ParkingSpot.class);
                    if (spot != null) {
                        spot.setId(spotSnapshot.getKey());
                        LatLng spotLocation = new LatLng(spot.getLatitude(), spot.getLongitude());
                        float markerColor = spot.getAvailable() ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED;

                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(spotLocation)
                                .title(spot.getName())
                                .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

                        if (marker != null) {
                            marker.setTag(spot);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MapsActivity", "Parking spots fetch failed: " + error.getMessage());
            }
        });
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            zoomToUserLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
                LatLng defaultLocation = new LatLng(6.9271, 79.8612);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));
            }
        }
    }

    private void zoomToUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f));
            } else {
                LatLng defaultLocation = new LatLng(6.9271, 79.8612);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));
            }
        });
    }

    private void checkUserRole() {
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MapsActivity.this, LoginActivity.class));
            finish();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        dbHelper.getUser(userId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && "manager".equals(user.getRole())) {
                    startActivity(new Intent(MapsActivity.this, ManagerDashboardActivity.class));
                    finish();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MapsActivity", "User fetch failed: " + error.getMessage());
            }
        });
    }
}