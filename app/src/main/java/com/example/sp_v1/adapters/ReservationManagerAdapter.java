package com.example.sp_v1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sp_v1.R;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.models.Reservation;
import com.example.sp_v1.models.User;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReservationManagerAdapter extends RecyclerView.Adapter<ReservationManagerAdapter.ViewHolder> {

    public interface OnReservationActionListener {
        void onActionClick(Reservation reservation);
    }

    private List<Reservation> reservations;
    private Map<String, ParkingSpot> spotMap;
    private Map<String, User> userMap;
    private final OnReservationActionListener listener;
    private int expandedPosition = -1;

    public ReservationManagerAdapter(List<Reservation> reservations, Map<String, ParkingSpot> spotMap, Map<String, User> userMap, OnReservationActionListener listener) {
        this.reservations = reservations;
        this.spotMap = spotMap;
        this.userMap = userMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manager_reservation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(reservations.get(position), position == expandedPosition, listener);
    }

    @Override
    public int getItemCount() {
        return reservations.size();
    }

    // Method to safely update the data and refresh the UI
    public void updateData(List<Reservation> newReservations, Map<String, ParkingSpot> newSpotMap, Map<String, User> newUserMap) {
        this.reservations = newReservations;
        this.spotMap = newSpotMap;
        this.userMap = newUserMap;
        expandedPosition = -1; // Reset expanded state on new data
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView spotNameText, userEmailText, reservationTimeText;
        LinearLayout expandableLayout;
        MaterialButton actionButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            spotNameText = itemView.findViewById(R.id.spot_name_text);
            userEmailText = itemView.findViewById(R.id.user_email_text);
            reservationTimeText = itemView.findViewById(R.id.reservation_time_text);
            expandableLayout = itemView.findViewById(R.id.expandable_layout);
            actionButton = itemView.findViewById(R.id.action_button);
        }

        void bind(final Reservation reservation, final boolean isExpanded, final OnReservationActionListener listener) {
            ParkingSpot spot = spotMap.get(reservation.getSpotId());
            User user = userMap.get(reservation.getUserId());

            spotNameText.setText(spot != null ? spot.getName() : "Loading...");
            userEmailText.setText(user != null ? user.getEmail() : "Loading...");

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String startTime = sdf.format(new Date(reservation.getStartTime()));
            String endTime = sdf.format(new Date(reservation.getEndTime()));
            reservationTimeText.setText("Time: " + startTime + " - " + endTime);

            // Correctly set button text for both states
            if ("active".equals(reservation.getStatus())) {
                actionButton.setText("Check-In");
            } else if ("in-progress".equals(reservation.getStatus())) {
                actionButton.setText("Check-Out");
            }

            expandableLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> {
                int previousExpandedPosition = expandedPosition;
                expandedPosition = isExpanded ? -1 : getAdapterPosition();
                if (previousExpandedPosition != -1) {
                    notifyItemChanged(previousExpandedPosition);
                }
                if (expandedPosition != -1) {
                    notifyItemChanged(expandedPosition);
                }
            });

            actionButton.setOnClickListener(v -> listener.onActionClick(reservation));
        }
    }
}