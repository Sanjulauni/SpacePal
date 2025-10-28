package com.example.sp_v1.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sp_v1.R;
import com.example.sp_v1.models.Reservation;
import com.example.sp_v1.models.User;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.utils.DatabaseHelper;
import com.example.sp_v1.utils.DateUtils;
import java.util.List;
import java.util.Map;

public class ReservationManagerAdapter extends RecyclerView.Adapter<ReservationManagerAdapter.ViewHolder> {

    private final List<Reservation> reservations;
    private final Map<String, ParkingSpot> spotMap;
    private final Map<String, User> userMap;
    private final Context context;
    private final DatabaseHelper dbHelper;

    public ReservationManagerAdapter(Context context, List<Reservation> reservations, Map<String, ParkingSpot> spotMap, Map<String, User> userMap) {
        this.context = context;
        this.reservations = reservations;
        this.spotMap = spotMap;
        this.userMap = userMap;
        this.dbHelper = new DatabaseHelper();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manager_reservation, parent, false);
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

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView userNameText, spotNameText, reservationTimeText, reservationStatusText; // Added status text
        Button checkoutButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            userNameText = itemView.findViewById(R.id.user_name_text);
            spotNameText = itemView.findViewById(R.id.spot_name_text);
            reservationTimeText = itemView.findViewById(R.id.reservation_time_text);
            reservationStatusText = itemView.findViewById(R.id.reservation_status_text); // Initialize status text
            checkoutButton = itemView.findViewById(R.id.checkout_button);
        }

        void bind(final Reservation reservation) {
            ParkingSpot spot = spotMap.get(reservation.getSpotId());
            User user = userMap.get(reservation.getUserId());

            String userName = (user != null && user.getName() != null) ? user.getName() : "Unknown User";
            userNameText.setText(userName);
            spotNameText.setText("Spot: " + (spot != null ? spot.getName() : "Unknown Spot"));
            String startTime = DateUtils.formatTimestamp(reservation.getStartTime());
            reservationTimeText.setText("Reserved on: " + startTime);

            // --- THIS IS THE CORRECTED LOGIC ---
            if ("in-progress".equals(reservation.getStatus())) {
                reservationStatusText.setText("Status: In-Progress");
                // CHANGED: Using a standard, reliable blue color
                reservationStatusText.setTextColor(Color.parseColor("#007BFF")); // A standard blue
                checkoutButton.setVisibility(View.VISIBLE); // Show the button
                checkoutButton.setOnClickListener(v -> {
                    new AlertDialog.Builder(context)
                            .setTitle("Confirm Check-Out")
                            .setMessage("Are you sure you want to check out " + userName + "?")
                            .setPositiveButton("Yes", (dialog, which) -> performCheckout(reservation))
                            .setNegativeButton("No", null)
                            .show();
                });
            } else if ("active".equals(reservation.getStatus())) {
                reservationStatusText.setText("Status: Active (Arriving)");
                reservationStatusText.setTextColor(Color.GRAY);
                checkoutButton.setVisibility(View.GONE); // Hide the button
            } else {
                // For "completed", "cancelled", etc.
                reservationStatusText.setText("Status: " + reservation.getStatus());
                reservationStatusText.setTextColor(Color.GRAY);
                checkoutButton.setVisibility(View.GONE); // Hide the button
            }
        }
    }

    private void performCheckout(Reservation reservation) {
        dbHelper.checkOutUser(reservation, task -> {
            if (task != null && task.isSuccessful()) {
                // The real-time listener in DashboardFragment will automatically update the list.
                Toast.makeText(context, "User checked out successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Check-out failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}