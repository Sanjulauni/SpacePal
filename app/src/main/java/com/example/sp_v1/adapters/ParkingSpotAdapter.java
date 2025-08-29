package com.example.sp_v1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sp_v1.R;
import com.example.sp_v1.models.ParkingSpot;
import java.util.List;

public class ParkingSpotAdapter extends RecyclerView.Adapter<ParkingSpotAdapter.ViewHolder> {

    // 1. Define an interface for click events
    public interface OnItemClickListener {
        void onItemClick(ParkingSpot spot);
    }

    private final List<ParkingSpot> parkingSpots;
    private final OnItemClickListener listener;

    // 2. Update the constructor to accept the listener
    public ParkingSpotAdapter(List<ParkingSpot> parkingSpots, OnItemClickListener listener) {
        this.parkingSpots = parkingSpots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parking_spot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 3. Pass the spot and listener to the ViewHolder to bind the click event
        holder.bind(parkingSpots.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return parkingSpots.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView spotName;
        TextView spotAvailability;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            spotName = itemView.findViewById(R.id.spot_name);
            spotAvailability = itemView.findViewById(R.id.spot_availability);
        }

        // Bind data and the click listener to the item view
        public void bind(final ParkingSpot spot, final OnItemClickListener listener) {
            spotName.setText(spot.getName());
            String availability = "Available: " + spot.getAvailableSpaces() + "/" + spot.getTotalSlots();
            spotAvailability.setText(availability);
            itemView.setOnClickListener(v -> listener.onItemClick(spot));
        }
    }
}