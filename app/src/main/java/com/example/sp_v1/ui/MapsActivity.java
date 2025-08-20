package com.example.sp_v1.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sp_v1.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.example.sp_v1.R;
import com.example.sp_v1.adapters.ParkingSpotAdapter;
import com.example.sp_v1.adapters.ReservationAdapter;
import com.example.sp_v1.auth.LoginActivity;
import com.example.sp_v1.adapters.ParkingSpot;
import com.example.sp_v1.adapters.Reservation;
import com.example.sp_v1.utils.DatabaseHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private RecyclerView parkingSpotsRecyclerView;
    private RecyclerView reservationsRecyclerView;
    private ParkingSpotAdapter parkingSpotAdapter;
    private ReservationAdapter reservationAdapter;
    private List<ParkingSpot> parkingSpots = new ArrayList<>();
    private List<Reservation> reservations = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private Handler handler = new Handler();
    private Runnable expirationCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkReservationsForExpiration();
            handler.postDelayed(this, 60000); // Check every minute
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper();

        // Check role after login
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "test_driver";
        dbHelper.getUser(userId, new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && "manager".equals(user.getRole())) {
                    startActivity(new Intent(MapsActivity.this, ManagerDashboardActivity.class));
                    finish();
                }
                // Proceed with Maps setup for drivers
                setupMaps();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("MapsActivity", "User fetch failed: " + error.getMessage());
                setupMaps(); // Fallback to Maps setup if fetch fails
            }
        });

        MaterialButton logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void setupMaps() {
        // Set up Parking Spots RecyclerView
        parkingSpotsRecyclerView = findViewById(R.id.parking_spots_recycler_view);
        parkingSpotsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        parkingSpotAdapter = new ParkingSpotAdapter(parkingSpots);
        parkingSpotsRecyclerView.setAdapter(parkingSpotAdapter);

        // Set up Reservations RecyclerView
        reservationsRecyclerView = findViewById(R.id.reservations_recycler_view);
        reservationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reservationAdapter = new ReservationAdapter(reservations);
        reservationsRecyclerView.setAdapter(reservationAdapter);

        // Fetch and display parking spots
        dbHelper.getAvailableParkingSpots(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                parkingSpots.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    ParkingSpot spot = child.getValue(ParkingSpot.class);
                    if (spot != null) {
                        spot.setId(child.getKey());
                        parkingSpots.add(spot);
                    }
                }
                parkingSpotAdapter.notifyDataSetChanged();
                Log.d("MapsActivity", "Parking spots loaded: " + parkingSpots.size());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("MapsActivity", "Parking spots fetch failed: " + error.getMessage());
            }
        });

        // Fetch and display reservations for the current user
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "test_driver";
        dbHelper.getUserReservations(userId, new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                reservations.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Reservation res = child.getValue(Reservation.class);
                    if (res != null && res.getUserId().equals(userId)) {
                        reservations.add(res);
                    }
                }
                reservationAdapter.notifyDataSetChanged();
                Log.d("MapsActivity", "Reservations loaded: " + reservations.size());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("MapsActivity", "Reservation fetch failed: " + error.getMessage());
            }
        });

        // Start expiration check
        handler.post(expirationCheckRunnable);
    }

    private void checkReservationsForExpiration() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "test_driver";
        dbHelper.checkAndExpireReservations(userId);
        dbHelper.getUserReservations(userId, new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                reservations.clear();
                boolean expiredShown = false;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Reservation res = child.getValue(Reservation.class);
                    if (res != null && res.getUserId().equals(userId)) {
                        reservations.add(res);
                        if (res.getStatus().equals("expired") && !expiredShown) {
                            Log.d("MapsActivity", "Expired spotId: " + res.getSpotId()); // Debug log
                            Toast.makeText(MapsActivity.this, "Reservation for " + res.getSpotId() + " has expired!", Toast.LENGTH_SHORT).show();
                            expiredShown = true;
                        }
                    }
                }
                reservationAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("MapsActivity", "Expiration update failed: " + error.getMessage());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(expirationCheckRunnable); // Stop timer when activity is destroyed
    }
}