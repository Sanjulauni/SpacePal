package com.example.sp_v1.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.sp_v1.adapters.ParkingSpot;
import com.example.sp_v1.adapters.Reservation;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper {
    private DatabaseReference db = FirebaseDatabase.getInstance().getReference();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public void addSampleParkingSpot(String spotId, String managerId) {
        ParkingSpot spot = new ParkingSpot("Downtown Lot A", 6.9271, 79.8612, true, managerId, 50, 30);
        spot.setId(spotId);
        db.child("parking_spots").child(spotId).setValue(spot)
                .addOnSuccessListener(aVoid -> Log.d("DatabaseHelper", "Sample spot added: " + spotId))
                .addOnFailureListener(e -> Log.e("DatabaseHelper", "Error: " + e.getMessage()));
    }

    public void addSampleReservation(String resId, String userId, String spotId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        String startTime = sdf.format(new Date());
        String endTime = sdf.format(new Date(System.currentTimeMillis() + 7200000)); // 2 hours
        Reservation res = new Reservation(userId, spotId, startTime, endTime, "active");
        db.child("reservations").child(resId).setValue(res)
                .addOnSuccessListener(aVoid -> Log.d("DatabaseHelper", "Sample reservation added: " + resId))
                .addOnFailureListener(e -> Log.e("DatabaseHelper", "Error: " + e.getMessage()));
    }

    public void createReservation(String spotId, int position) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "test_driver";
        String resId = "res_" + System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        String startTime = sdf.format(new Date());
        String endTime = sdf.format(new Date(System.currentTimeMillis() + 7200000)); // 2 hours
        Reservation res = new Reservation(userId, spotId, startTime, endTime, "active");
        db.child("reservations").child(resId).setValue(res)
                .addOnSuccessListener(aVoid -> {
                    Log.d("DatabaseHelper", "Reservation created: " + resId);
                })
                .addOnFailureListener(e -> Log.e("DatabaseHelper", "Error creating reservation: " + e.getMessage()));
    }

    public void getAvailableParkingSpots(ValueEventListener listener) {
        db.child("parking_spots").orderByChild("available").equalTo(true)
                .addListenerForSingleValueEvent(listener);
    }

    public void getUserReservations(String userId, ValueEventListener listener) {
        db.child("reservations").orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(listener);
    }

    public void checkAndExpireReservations(String userId) {
        db.child("reservations").orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                String currentTime = sdf.format(new Date());
                for (DataSnapshot child : snapshot.getChildren()) {
                    Reservation res = child.getValue(Reservation.class);
                    if (res != null && res.getStatus().equals("active")) {
                        try {
                            Date endTime = sdf.parse(res.getEndTime());
                            Date now = sdf.parse(currentTime);
                            if (endTime != null && now != null && now.after(endTime)) {
                                res.setStatus("expired");
                                db.child("reservations").child(child.getKey()).setValue(res)
                                        .addOnSuccessListener(aVoid -> Log.d("DatabaseHelper", "Reservation expired: " + child.getKey()))
                                        .addOnFailureListener(e -> Log.e("DatabaseHelper", "Error expiring reservation: " + e.getMessage()));
                            }
                        } catch (Exception e) {
                            Log.e("DatabaseHelper", "Date parsing error: " + e.getMessage());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("DatabaseHelper", "Expiration check failed: " + error.getMessage());
            }
        });
    }

    public void getUser(String userId, ValueEventListener listener) {
        db.child("users").child(userId).addListenerForSingleValueEvent(listener);
    }
}