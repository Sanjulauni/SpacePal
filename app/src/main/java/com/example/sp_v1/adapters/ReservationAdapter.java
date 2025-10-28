package com.example.sp_v1.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat; // Import this
import androidx.recyclerview.widget.RecyclerView;
import com.example.sp_v1.R;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.models.Reservation;
import com.example.sp_v1.utils.DatabaseHelper;
import com.example.sp_v1.utils.DateUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ViewHolder> {

    private final List<Reservation> reservations;
    private final Map<String, String> spotNameCache = new HashMap<>();
    private final Context context;

    public ReservationAdapter(Context context, List<Reservation> reservations) {
        this.context = context;
        this.reservations = reservations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(reservations.get(position));
    }

    @Override
    public int getItemCount() {
        return reservations.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // --- Updated Views ---
        public TextView spotNameTextView, spotIdTextView, reservationDateTextView, statusTextView;
        public View statusDot; // Added
        public Button cancelButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            spotNameTextView = itemView.findViewById(R.id.spot_name_text_view);
            spotIdTextView = itemView.findViewById(R.id.spot_id_text_view);
            reservationDateTextView = itemView.findViewById(R.id.reservation_date_text_view);
            statusTextView = itemView.findViewById(R.id.status_text); // New Status TextView
            statusDot = itemView.findViewById(R.id.status_dot); // New Status Dot View
            cancelButton = itemView.findViewById(R.id.cancel_reservation_button);
        }

        void bind(Reservation reservation) {
            spotIdTextView.setText("Spot ID: " + reservation.getSpotId());
            reservationDateTextView.setText("Date: " + DateUtils.formatTimestamp(reservation.getStartTime()));

            // --- Status Logic ---
            String status = reservation.getStatus() != null ? reservation.getStatus().toLowerCase() : "unknown";
            int statusColor;
            String statusText;

            switch (status) {
                case "completed":
                    statusColor = ContextCompat.getColor(context, R.color.success_green);
                    statusText = "Completed";
                    cancelButton.setVisibility(View.GONE);
                    break;
                case "cancelled":
                case "expired": // Treat expired same as cancelled visually
                    statusColor = ContextCompat.getColor(context, R.color.error_red);
                    statusText = "Cancelled";
                    cancelButton.setVisibility(View.GONE);
                    break;
                case "active":
                    statusColor = ContextCompat.getColor(context, R.color.info_blue); // Or maybe orange?
                    statusText = "Active";
                    cancelButton.setVisibility(View.VISIBLE); // Show cancel only for active
                    break;
                case "in-progress":
                    statusColor = ContextCompat.getColor(context, R.color.primary_blue); // Different blue for checked-in
                    statusText = "Checked In";
                    cancelButton.setVisibility(View.GONE); // Cannot cancel after check-in
                    break;
                default:
                    statusColor = ContextCompat.getColor(context, R.color.black_38); // Gray for unknown
                    statusText = "Unknown";
                    cancelButton.setVisibility(View.GONE);
                    break;
            }

            // Set the status text and color
            statusTextView.setText(statusText);
            statusTextView.setTextColor(statusColor);

            // Set the status dot color
            // Using GradientDrawable allows changing color programmatically
            if (statusDot.getBackground() instanceof GradientDrawable) {
                GradientDrawable dotBackground = (GradientDrawable) statusDot.getBackground().mutate(); // Use mutate to avoid affecting other drawables
                dotBackground.setColor(statusColor);
            } else {
                // Fallback if the background is not the expected shape (e.g., set directly in XML)
                statusDot.setBackgroundTintList(ColorStateList.valueOf(statusColor));
            }

            // --- End Status Logic ---


            cancelButton.setOnClickListener(v -> new AlertDialog.Builder(context)
                    .setTitle("Cancel Reservation")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Yes", (dialog, which) -> performCancellation(reservation, getAdapterPosition()))
                    .setNegativeButton("No", null).show());

            fetchSpotName(reservation.getSpotId());
        }

        private void fetchSpotName(String spotId) {
            if (spotNameCache.containsKey(spotId)) {
                spotNameTextView.setText(spotNameCache.get(spotId));
            } else {
                spotNameTextView.setText("Loading...");
                FirebaseDatabase.getInstance().getReference("parking_spots").child(spotId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                ParkingSpot spot = snapshot.getValue(ParkingSpot.class);
                                String name = (spot != null && spot.getName() != null) ? spot.getName() : "Unknown Spot";
                                spotNameCache.put(spotId, name);
                                // Check if the ViewHolder is still valid before setting text
                                if(getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                                    spotNameTextView.setText(name);
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                if(getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                                    spotNameTextView.setText("Error Loading Name");
                                }
                            }
                        });
            }
        }
    } // End ViewHolder

    private void performCancellation(Reservation reservation, int position) {
        if (position == RecyclerView.NO_POSITION) return; // Prevent crash if item removed quickly

        DatabaseHelper dbHelper = new DatabaseHelper();
        dbHelper.cancelReservation(reservation, task -> {
            if (task != null && task.isSuccessful()) {
                // Remove the item from the list and notify the adapter *only* on success
                // This assumes your fragment's listener doesn't automatically reload the whole list
                if (position < reservations.size()) { // Double-check position validity
                    // Update status locally for immediate feedback (optional but good UX)
                    reservations.get(position).setStatus("cancelled");
                    notifyItemChanged(position);
                }
                Toast.makeText(context, "Reservation cancelled.", Toast.LENGTH_SHORT).show();

                // If your fragment *does* reload the list, remove the notifyItemChanged line above.
            } else {
                Toast.makeText(context, "Failed to cancel. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
} // End Adapter