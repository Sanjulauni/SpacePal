package com.example.sp_v1.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // Not strictly needed, but good to keep
import androidx.recyclerview.widget.RecyclerView;
import com.example.sp_v1.R;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.ui.SpotVerificationActivity;
// --- IMPORT CHANGED ---
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.List;

public class ParkingSpotAdapter extends RecyclerView.Adapter<ParkingSpotAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ParkingSpot spot);
    }

    // --- NEW INTERFACE FOR THE SWITCH ---
    public interface OnSpotListedChangeListener {
        void onSpotListedChanged(ParkingSpot spot, boolean isListed);
    }

    private final List<ParkingSpot> parkingSpots;
    private final OnItemClickListener listener;
    private final Context context;
    private final OnSpotListedChangeListener listedListener; // --- NEW LISTENER ADDED ---

    // --- CONSTRUCTOR UPDATED ---
    public ParkingSpotAdapter(Context context, List<ParkingSpot> parkingSpots, OnItemClickListener listener, OnSpotListedChangeListener listedListener) {
        this.context = context;
        this.parkingSpots = parkingSpots;
        this.listener = listener;
        this.listedListener = listedListener; // --- NEW ---
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
        // UPDATED: Pass the context and new listener to the bind method
        holder.bind(context, parkingSpots.get(position), listener, listedListener);
    }

    @Override
    public int getItemCount() {
        return parkingSpots.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        // --- ViewHolder fields updated ---
        TextView spotName;
        TextView spotAvailability;
        TextView spotVerificationStatus;
        Button btnVerifySpot;
        SwitchMaterial spotListedSwitch; // --- CLASS CHANGED ---

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            spotName = itemView.findViewById(R.id.spot_name);
            spotAvailability = itemView.findViewById(R.id.spot_availability);
            spotVerificationStatus = itemView.findViewById(R.id.spot_verification_status);
            btnVerifySpot = itemView.findViewById(R.id.btn_verify_spot);
            spotListedSwitch = itemView.findViewById(R.id.spot_listed_switch); // --- NEW ---
        }

        // --- The bind method now contains all the new logic ---
        public void bind(final Context context, final ParkingSpot spot, final OnItemClickListener listener, final OnSpotListedChangeListener listedListener) {
            spotName.setText(spot.getName());
            String availability = "Available: " + spot.getAvailableSpaces() + "/" + spot.getTotalSlots();
            spotAvailability.setText(availability);

            // This listener is for editing the spot (clicking anywhere on the card)
            itemView.setOnClickListener(v -> listener.onItemClick(spot));

            // --- Logic to handle verification UI ---
            String status = spot.getSpotVerificationStatus();
            if (status == null) {
                status = ParkingSpot.STATUS_SPOT_UNVERIFIED; // Default case
            }

            switch (status) {
                case ParkingSpot.STATUS_SPOT_TITLE_VERIFIED:
                    spotVerificationStatus.setVisibility(View.VISIBLE);
                    btnVerifySpot.setVisibility(View.GONE);
                    spotVerificationStatus.setText("✔ Title-Verified");
                    spotVerificationStatus.setTextColor(Color.parseColor("#008000")); // Dark Green
                    break;

                case ParkingSpot.STATUS_SPOT_PENDING_DEED:
                    spotVerificationStatus.setVisibility(View.VISIBLE);
                    btnVerifySpot.setVisibility(View.GONE);
                    spotVerificationStatus.setText("Pending Review");
                    spotVerificationStatus.setTextColor(Color.GRAY);
                    break;

                case ParkingSpot.STATUS_SPOT_UNVERIFIED:
                default:
                    spotVerificationStatus.setVisibility(View.GONE);
                    btnVerifySpot.setVisibility(View.VISIBLE);
                    btnVerifySpot.setOnClickListener(v -> {
                        Intent intent = new Intent(context, SpotVerificationActivity.class);
                        intent.putExtra(SpotVerificationActivity.EXTRA_SPOT_ID, spot.getId());
                        context.startActivity(intent);
                    });
                    break;
            }

            // --- NEW LOGIC FOR THE SWITCH ---
            // Set initial state without firing the listener
            spotListedSwitch.setOnCheckedChangeListener(null);
            spotListedSwitch.setChecked(spot.isListed());
            // Now, add the listener that talks to the fragment
            spotListedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                listedListener.onSpotListedChanged(spot, isChecked);
            });
        }
    }
}