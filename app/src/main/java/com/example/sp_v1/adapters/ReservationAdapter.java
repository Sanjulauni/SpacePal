package com.example.sp_v1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sp_v1.R;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.models.Reservation;
import com.example.sp_v1.utils.DateUtils; // We will create this new utility class
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ViewHolder> {

    private List<Reservation> reservations;
    // Simple cache to store spot names so we don't re-fetch them for the same spot
    private Map<String, String> spotNameCache = new HashMap<>();

    public ReservationAdapter(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Make sure this layout name matches the one you created: item_reservation.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reservation reservation = reservations.get(position);
        holder.bind(reservation);
    }

    @Override
    public int getItemCount() {
        return reservations != null ? reservations.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // These IDs must match the IDs in your item_reservation.xml
        public TextView spotNameTextView;
        public TextView spotIdTextView;
        public TextView reservationDateTextView;
        public TextView reservationStatusTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            spotNameTextView = itemView.findViewById(R.id.spot_name_text_view);
            spotIdTextView = itemView.findViewById(R.id.spot_id_text_view);
            reservationDateTextView = itemView.findViewById(R.id.reservation_date_text_view);
            reservationStatusTextView = itemView.findViewById(R.id.reservation_status_text_view);
        }

        void bind(Reservation reservation) {
            spotIdTextView.setText("Spot ID: " + reservation.getSpotId());
            reservationStatusTextView.setText("Status: " + reservation.getStatus());

            // Format the timestamp into a readable date string
            reservationDateTextView.setText("Date: " + DateUtils.formatTimestamp(reservation.getStartTime()));

            // Now, fetch the spot name
            String spotId = reservation.getSpotId();
            if (spotNameCache.containsKey(spotId)) {
                // If we already fetched this name, use the cached version
                spotNameTextView.setText(spotNameCache.get(spotId));
            } else {
                // Otherwise, fetch it from Firebase
                spotNameTextView.setText("Loading spot name...");
                FirebaseDatabase.getInstance().getReference("parking_spots").child(spotId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                ParkingSpot spot = snapshot.getValue(ParkingSpot.class);
                                if (spot != null) {
                                    String spotName = spot.getName();
                                    spotNameCache.put(spotId, spotName); // Save to cache
                                    spotNameTextView.setText(spotName);
                                } else {
                                    spotNameTextView.setText("Unknown Spot");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                spotNameTextView.setText("Error loading name");
                            }
                        });
            }
        }
    }
}