package com.example.sp_v1.utils;

import androidx.annotation.NonNull;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.models.Reservation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
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

    // =================================================================================
    // THIS IS THE NEW METHOD THAT WAS MISSING
    // =================================================================================
    public void createReservation(ParkingSpot spot, String userId, OnCompleteListener<Void> listener) {
        DatabaseReference spotRef = dbRootRef.child("parking_spots").child(spot.getId());

        spotRef.runTransaction(new Transaction.Handler() {
            @Override
            @NonNull
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                ParkingSpot currentSpot = mutableData.getValue(ParkingSpot.class);
                if (currentSpot == null) {
                    return Transaction.success(mutableData);
                }

                if (currentSpot.getAvailableSpaces() > 0) {
                    currentSpot.setAvailableSpaces(currentSpot.getAvailableSpaces() - 1);
                    if (currentSpot.getAvailableSpaces() == 0) {
                        currentSpot.setAvailable(false);
                    }
                    mutableData.setValue(currentSpot);
                    return Transaction.success(mutableData);
                } else {
                    return Transaction.abort();
                }
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                if (committed) {
                    DatabaseReference reservationsRef = dbRootRef.child("reservations");
                    String reservationId = reservationsRef.push().getKey();

                    Map<String, Object> reservationValues = new HashMap<>();
                    reservationValues.put("id", reservationId);
                    reservationValues.put("userId", userId);
                    reservationValues.put("spotId", spot.getId());
                    reservationValues.put("startTime", ServerValue.TIMESTAMP);
                    reservationValues.put("endTime", 0L);
                    reservationValues.put("status", "active");

                    if (reservationId != null) {
                        reservationsRef.child(reservationId).setValue(reservationValues).addOnCompleteListener(listener);
                    }
                } else {
                    // Transaction failed (e.g., no spots were available), so we call the listener with a null task.
                    listener.onComplete(null);
                }
            }
        });
    }

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
            // This case should be rare, but it's good practice to handle it.
            if (listener != null) {
                listener.onComplete(null);
            }
        }
    }

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