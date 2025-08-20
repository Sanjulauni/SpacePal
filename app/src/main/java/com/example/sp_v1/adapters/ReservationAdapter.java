package com.example.sp_v1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sp_v1.R;
import java.util.List;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ViewHolder> {

    private List<Reservation> reservations;

    public ReservationAdapter(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.reservation_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reservation res = reservations.get(position);
        holder.reservationSpot.setText("Spot: " + res.getSpotId());
        holder.reservationTime.setText("Time: " + res.getStartTime() + " - " + res.getEndTime());
        holder.reservationStatus.setText("Status: " + res.getStatus());
    }

    @Override
    public int getItemCount() {
        return reservations != null ? reservations.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView reservationSpot;
        public TextView reservationTime;
        public TextView reservationStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            reservationSpot = itemView.findViewById(R.id.reservation_spot);
            reservationTime = itemView.findViewById(R.id.reservation_time);
            reservationStatus = itemView.findViewById(R.id.reservation_status);
        }
    }
}