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
import java.util.Locale; // <-- Import Locale
import java.util.Map;

public class FrequentSpotAdapter extends RecyclerView.Adapter<FrequentSpotAdapter.ViewHolder> {

    public interface OnFrequentSpotClickListener {
        void onSpotClick(ParkingSpot spot);
    }

    private final List<ParkingSpot> frequentSpots;
    private final Map<String, Integer> visitCounts;
    private final OnFrequentSpotClickListener clickListener;

    public FrequentSpotAdapter(List<ParkingSpot> frequentSpots, Map<String, Integer> visitCounts, OnFrequentSpotClickListener clickListener) {
        this.frequentSpots = frequentSpots;
        this.visitCounts = visitCounts;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // --- UPDATED LAYOUT REFERENCE ---
        // Ensure this inflates the new card layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_frequent_spot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(frequentSpots.get(position), clickListener);
    }

    @Override
    public int getItemCount() {
        // Limit the number of items shown if desired, e.g., Math.min(frequentSpots.size(), 5)
        return frequentSpots.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView spotName;
        TextView visitCount;
        // You might add an ImageView here if you included one in the layout

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            // --- UPDATED IDs ---
            spotName = itemView.findViewById(R.id.spot_name_text);     // Changed from frequent_spot_name
            visitCount = itemView.findViewById(R.id.visit_count_text); // Changed from frequent_spot_visits
            // Find ImageView if you added one:
            // spotIcon = itemView.findViewById(R.id.spot_icon_imageview);
        }

        void bind(final ParkingSpot spot, final OnFrequentSpotClickListener listener) {
            spotName.setText(spot.getName());

            Integer count = visitCounts.get(spot.getId());
            if (count != null) {
                // Use Locale for consistent formatting
                String visitText = String.format(Locale.getDefault(), "%d %s", count, (count > 1 ? "Visits" : "Visit"));
                visitCount.setText(visitText);
            } else {
                visitCount.setText("1 Visit"); // Default if count is missing (shouldn't happen with current logic)
            }

            // Set icon if you have one
            // spotIcon.setImageResource(R.drawable.ic_location_pin); // Example

            itemView.setOnClickListener(v -> listener.onSpotClick(spot));
        }
    }
}