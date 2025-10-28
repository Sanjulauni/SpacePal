package com.example.sp_v1.utils;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.sp_v1.models.EventPost;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.models.Reservation;
import com.example.sp_v1.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseHelper {
    private final DatabaseReference dbRootRef = FirebaseDatabase.getInstance().getReference();
    private final StorageReference storageRootRef = FirebaseStorage.getInstance().getReference();
    private static final long RESERVATION_EXPIRATION_MS = 2 * 60 * 1000;

    // --- Interfaces ---
    public interface ActiveReservationCheckListener { void onResult(boolean hasActiveReservation); }
    public interface DatabaseCallback { void onSuccess(); void onError(String error); }
    public interface StorageUploadCallback { void onSuccess(String downloadUrl); void onError(String error); }
    public interface UsersCallback { void onCallback(Map<String, User> userMap); }

    // --- User Methods ---
    public void getUser(String userId, ValueEventListener listener) {
        dbRootRef.child("users").child(userId).addListenerForSingleValueEvent(listener);
    }

    // --- UPDATED: Replaced updateUser with specific updateUserProfile ---
    public void updateUserProfile(String userId, User user, DatabaseCallback callback) {
        // Use a Map to update only specific fields if needed in the future,
        // but for now, we rewrite the whole user object.
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("name", user.getName());
        // Add other fields like profileImageUrl here if you implement them

        dbRootRef.child("users").child(userId).updateChildren(userUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    public void getSpecificUsers(List<String> userIds, final UsersCallback usersCallback) {
        final Map<String, User> fetchedUsers = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            usersCallback.onCallback(fetchedUsers);
            return;
        }
        final AtomicInteger counter = new AtomicInteger(userIds.size());
        for (String userId : userIds) {
            dbRootRef.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) { fetchedUsers.put(snapshot.getKey(), user); }
                    if (counter.decrementAndGet() == 0) { usersCallback.onCallback(fetchedUsers); }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (counter.decrementAndGet() == 0) { usersCallback.onCallback(fetchedUsers); }
                }
            });
        }
    }


    // --- Parking Spot Methods ---
    public void getParkingSpots(ValueEventListener listener) {
        dbRootRef.child("parking_spots").addValueEventListener(listener);
    }

    public Query getSpotsForManagerQuery(String managerId) {
        return dbRootRef.child("parking_spots").orderByChild("managerId").equalTo(managerId);
    }

    public void getManagerSpots(String managerId, ValueEventListener listener) {
        dbRootRef.child("parking_spots").orderByChild("managerId").equalTo(managerId).addListenerForSingleValueEvent(listener);
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
            if (listener != null) { listener.onComplete(null); }
        }
    }

    // --- NEW METHOD ADDED ---
    /**
     * Updates only the 'listed' status of a parking spot.
     * @param spotId The ID of the spot to update.
     * @param isListed The new listed status.
     * @param listener A listener to notify of completion.
     */
    public void updateSpotListedStatus(String spotId, boolean isListed, OnCompleteListener<Void> listener) {
        dbRootRef.child("parking_spots").child(spotId).child("listed")
                .setValue(isListed)
                .addOnCompleteListener(listener);
    }
    // --- END OF NEW METHOD ---


    // --- Reservation Methods ---
    public void checkForActiveReservation(String userId, ActiveReservationCheckListener listener) {
        dbRootRef.child("reservations").orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean hasValidActive = false;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Reservation res = child.getValue(Reservation.class);
                            if (res != null && "active".equals(res.getStatus()) && (System.currentTimeMillis() - res.getStartTime()) < RESERVATION_EXPIRATION_MS) {
                                hasValidActive = true; break;
                            }
                        }
                        listener.onResult(hasValidActive);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { listener.onResult(false); }
                });
    }

    public Query getReservationsQuery() {
        return dbRootRef.child("reservations");
    }

    public void createReservation(ParkingSpot spot, String userId, OnCompleteListener<Void> listener) {
        DatabaseReference reservationsRef = dbRootRef.child("reservations");
        String reservationId = reservationsRef.push().getKey();
        Map<String, Object> values = new HashMap<>();
        values.put("id", reservationId); values.put("userId", userId); values.put("spotId", spot.getId());
        values.put("startTime", ServerValue.TIMESTAMP); values.put("endTime", 0L); values.put("status", "active");
        if (reservationId != null) {
            reservationsRef.child(reservationId).setValue(values).addOnCompleteListener(listener);
        } else if (listener != null) {
            com.google.android.gms.tasks.TaskCompletionSource<Void> tcs = new com.google.android.gms.tasks.TaskCompletionSource<>();
            tcs.setException(new Exception("Failed key gen.")); listener.onComplete(tcs.getTask());
        }
    }

    public void checkInUser(Reservation reservation, OnCompleteListener<Void> listener) {
        DatabaseReference spotRef = dbRootRef.child("parking_spots").child(reservation.getSpotId());
        spotRef.runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                ParkingSpot spot = mutableData.getValue(ParkingSpot.class);
                if (spot == null) { return Transaction.abort(); }
                if (spot.getAvailableSpaces() > 0) {
                    spot.setAvailableSpaces(spot.getAvailableSpaces() - 1); mutableData.setValue(spot); return Transaction.success(mutableData);
                } else { return Transaction.abort(); }
            }
            @Override public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (committed) {
                    dbRootRef.child("reservations").child(reservation.getId()).child("status").setValue("in-progress").addOnCompleteListener(listener);
                } else if (listener != null) {
                    com.google.android.gms.tasks.TaskCompletionSource<Void> tcs = new com.google.android.gms.tasks.TaskCompletionSource<>();
                    tcs.setException(new Exception("Check-in fail. Spot full?")); listener.onComplete(tcs.getTask());
                }
            }
        });
    }

    public void cancelReservation(Reservation reservation, OnCompleteListener<Void> listener) {
        String spotId = reservation.getSpotId(); String reservationId = reservation.getId();
        dbRootRef.child("parking_spots").child(spotId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                ParkingSpot spot = snapshot.getValue(ParkingSpot.class);
                if (spot == null) { if (listener != null) listener.onComplete(null); return; }
                Map<String, Object> updates = new HashMap<>();
                updates.put("/reservations/" + reservationId + "/status", "cancelled");
                if ("in-progress".equals(reservation.getStatus())) {
                    updates.put("/parking_spots/" + spotId + "/availableSpaces", spot.getAvailableSpaces() + 1);
                    updates.put("/parking_spots/" + spotId + "/available", true);
                }
                dbRootRef.updateChildren(updates).addOnCompleteListener(listener);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { if (listener != null) listener.onComplete(null); }
        });
    }

    public void checkOutUser(Reservation reservation, OnCompleteListener<Void> listener) {
        String spotId = reservation.getSpotId(); String reservationId = reservation.getId();
        dbRootRef.child("parking_spots").child(spotId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                ParkingSpot spot = snapshot.getValue(ParkingSpot.class);
                if (spot == null) { if (listener != null) listener.onComplete(null); return; }
                Map<String, Object> updates = new HashMap<>();
                updates.put("/reservations/" + reservationId + "/status", "completed");
                updates.put("/reservations/" + reservationId + "/endTime", ServerValue.TIMESTAMP);
                updates.put("/parking_spots/" + spotId + "/availableSpaces", spot.getAvailableSpaces() + 1);
                if (!spot.getAvailable()) { updates.put("/parking_spots/" + spotId + "/available", true); }
                dbRootRef.updateChildren(updates).addOnCompleteListener(listener);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { if (listener != null) listener.onComplete(null); }
        });
    }

    // --- Event Post Methods ---
    public void uploadEventBanner(Uri imageUri, String managerId, StorageUploadCallback callback) {
        String fileName = UUID.randomUUID().toString();
        StorageReference fileRef = storageRootRef.child("event_banners/" + managerId + "/" + fileName);
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                        .addOnFailureListener(e -> callback.onError(e.getMessage())))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void createEventPost(EventPost eventPost, DatabaseCallback callback) {
        DatabaseReference eventRef = dbRootRef.child("event_posts");
        String eventId = eventRef.push().getKey();
        if (eventId == null) { callback.onError("Could not generate event ID."); return; }
        eventPost.setEventId(eventId);
        eventRef.child(eventId).setValue(eventPost)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) { callback.onSuccess(); }
                    else { callback.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error"); }
                });
    }
}