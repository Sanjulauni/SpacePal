package com.example.sp_v1.ui.manager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.sp_v1.R;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.utils.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class DashboardFragment extends Fragment {

    private TextView occupiedSpotsText, availableSpotsText, totalSpotsText, welcomeText;
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new DatabaseHelper();
        occupiedSpotsText = view.findViewById(R.id.occupied_spots_text);
        availableSpotsText = view.findViewById(R.id.available_spots_text);
        totalSpotsText = view.findViewById(R.id.total_spots_text);
        welcomeText = view.findViewById(R.id.dashboard_welcome_text);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardData(); // Refresh data every time the screen is shown
    }

    private void loadDashboardData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String welcomeMessage = "Welcome, " + (currentUser.getEmail() != null ? currentUser.getEmail() : "Manager") + "!";
        welcomeText.setText(welcomeMessage);

        dbHelper.getSpotsForManager(currentUser.getUid(), new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalSpots = 0;
                int availableSpots = 0;
                for (DataSnapshot spotSnapshot : snapshot.getChildren()) {
                    ParkingSpot spot = spotSnapshot.getValue(ParkingSpot.class);
                    if (spot != null) {
                        totalSpots += spot.getTotalSlots();
                        availableSpots += spot.getAvailableSpaces();
                    }
                }
                int occupiedSpots = totalSpots - availableSpots;
                occupiedSpotsText.setText(String.valueOf(occupiedSpots));
                availableSpotsText.setText(String.valueOf(availableSpots));
                totalSpotsText.setText("Total Spots: " + totalSpots);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }
}