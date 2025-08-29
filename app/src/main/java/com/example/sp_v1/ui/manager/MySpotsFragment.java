package com.example.sp_v1.ui.manager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sp_v1.R;
import com.example.sp_v1.adapters.ParkingSpotAdapter;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.ui.EditSpotActivity;
import com.example.sp_v1.utils.DatabaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class MySpotsFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private ParkingSpotAdapter spotAdapter;
    private List<ParkingSpot> managedSpots = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_spots, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new DatabaseHelper();
        RecyclerView spotsRecyclerView = view.findViewById(R.id.manager_spots_recycler_view);
        spotsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        spotAdapter = new ParkingSpotAdapter(managedSpots, spot -> {
            Intent intent = new Intent(getActivity(), EditSpotActivity.class);
            intent.putExtra("SPOT_ID", spot.getId());
            startActivity(intent);
        });
        spotsRecyclerView.setAdapter(spotAdapter);

        FloatingActionButton addSpotFab = view.findViewById(R.id.add_spot_fab);
        addSpotFab.setOnClickListener(v -> startActivity(new Intent(getActivity(), EditSpotActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadManagerSpots();
    }

    // THIS IS THE CORRECTED METHOD NAME
    private void loadManagerSpots() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        dbHelper.getSpotsForManager(currentUser.getUid(), new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                managedSpots.clear();
                for (DataSnapshot spotSnapshot : snapshot.getChildren()) {
                    ParkingSpot spot = spotSnapshot.getValue(ParkingSpot.class);
                    if (spot != null) {
                        spot.setId(spotSnapshot.getKey());
                        managedSpots.add(spot);
                    }
                }
                spotAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MySpotsFragment", "Failed to load spots.", error.toException());
            }
        });
    }
}