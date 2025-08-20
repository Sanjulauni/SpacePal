package com.example.sp_v1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sp_v1.R;
import com.example.sp_v1.utils.DatabaseHelper;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class ParkingSpotAdapter extends RecyclerView.Adapter<ParkingSpotAdapter.ViewHolder> {

    private List<ParkingSpot> parkingSpots;
    private DatabaseHelper dbHelper;

    public ParkingSpotAdapter(List<ParkingSpot> parkingSpots) {
        this.parkingSpots = parkingSpots;
        this.dbHelper = new DatabaseHelper();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.parking_spot_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParkingSpot spot = parkingSpots.get(position);
        holder.spotName.setText(spot.getName());
        holder.spotLocation.setText(String.format("Lat: %.4f, Long: %.4f", spot.getLatitude(), spot.getLongitude()));
        holder.reserveButton.setOnClickListener(v -> {
            dbHelper.createReservation(spot.getId(), holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return parkingSpots != null ? parkingSpots.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView spotName;
        public TextView spotLocation;
        public MaterialButton reserveButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            spotName = itemView.findViewById(R.id.spot_name);
            spotLocation = itemView.findViewById(R.id.spot_location);
            reserveButton = itemView.findViewById(R.id.reserve_button);
        }
    }
}