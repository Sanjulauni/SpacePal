package com.example.sp_v1.utils;

import android.util.Log;
import androidx.annotation.NonNull;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.models.Reservation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper {
    private final DatabaseReference dbRootRef = FirebaseDatabase.getInstance().getReference();

    public interface ActiveReservationCheckListener {
        void onResult(boolean hasActiveReservation);
    }

    public void getUser(String userId, ValueEventListener listener) {
        dbRootRef.child("users").child(userId).addListenerForSingleValueEvent(listener);
    }

    public void getParkingSpots(ValueEventListener listener) {
        dbRootRef.child("parking_spots").addValueEventListener(listener);
    }

    public void checkForActiveReservation(String userId, ActiveReservationCheckListener listener) {
        dbRootRef.child("reservations").orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean hasActive = false;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Reservation res = child.getValue(Reservation.class);
                            if (res != null && "active".equals(res.getStatus())) {
                                hasActive = true;
                                break;
                            }
                        }
                        listener.onResult(hasActive);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onResult(false);
                    }
                });
    }

    public void createReservation(ParkingSpot spot, String userId, OnCompleteListener<Void> listener) {
        String reservationId = dbRootRef.child("reservations").push().getKey();
        if (reservationId == null) {
            listener.onComplete(null);
            return;
        }
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (60 * 60 * 1000);
        Reservation reservation = new Reservation(reservationId, userId, spot.getId(), startTime, endTime, "active");
        Map<String, Object> updates = new HashMap<>();
        updates.put("/reservations/" + reservationId, reservation);
        updates.put("/parking_spots/" + spot.getId() + "/availableSpaces", spot.getAvailableSpaces() - 1);
        if (spot.getAvailableSpaces() - 1 == 0) {
            updates.put("/parking_spots/" + spot.getId() + "/available", false);
        }
        dbRootRef.updateChildren(updates).addOnCompleteListener(listener);
    }

    // CORRECTED: This now fetches the list once and stops listening.
    public void getSpotsForManager(String managerId, ValueEventListener listener) {
        dbRootRef.child("parking_spots").orderByChild("managerId").equalTo(managerId)
                .addListenerForSingleValueEvent(listener);
    }

    public void saveParkingSpot(ParkingSpot spot, OnCompleteListener<Void> listener) {
        String spotId = spot.getId();
        if (spotId == null) {
            spotId = dbRootRef.child("parking_spots").push().getKey();
            spot.setId(spotId);
        }
        if (spotId != null) {
            dbRootRef.child("parking_spots").child(spotId).setValue(spot).addOnCompleteListener(listener);
        } else {
            listener.onComplete(null);
        }
    }

    // CORRECTED: This now fetches the list once and stops listening.
    public void getReservationsByStatus(String status, ValueEventListener listener) {
        dbRootRef.child("reservations").orderByChild("status").equalTo(status)
                .addListenerForSingleValueEvent(listener);
    }

    public void updateReservationStatus(String reservationId, String newStatus, OnCompleteListener<Void> listener) {
        dbRootRef.child("reservations").child(reservationId).child("status").setValue(newStatus)
                .addOnCompleteListener(listener);
    }

    public void getUsersByIds(List<String> userIds, ValueEventListener listener) {
        dbRootRef.child("users").addListenerForSingleValueEvent(listener);
    }

    public void getSpotsByIds(List<String> spotIds, ValueEventListener listener) {
        dbRootRef.child("parking_spots").addListenerForSingleValueEvent(listener);
    }

    public void checkOutUser(Reservation reservation, OnCompleteListener<Void> listener) {
        dbRootRef.child("parking_spots").child(reservation.getSpotId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ParkingSpot spot = snapshot.getValue(ParkingSpot.class);
                if (spot == null) {
                    if (listener != null) listener.onComplete(null);
                    return;
                }
                Map<String, Object> updates = new HashMap<>();
                updates.put("/reservations/" + reservation.getId() + "/status", "completed");
                updates.put("/parking_spots/" + reservation.getSpotId() + "/availableSpaces", spot.getAvailableSpaces() + 1);
                if (!spot.getAvailable()) {
                    updates.put("/parking_spots/" + reservation.getSpotId() + "/available", true);
                }
                dbRootRef.updateChildren(updates).addOnCompleteListener(listener);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) listener.onComplete(null);
            }
        });
    }
}