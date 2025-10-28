package com.example.sp_v1.ui.manager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast; // --- IMPORT ADDED ---

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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

// --- IMPLEMENT THE NEW INTERFACE ---
public class MySpotsFragment extends Fragment implements ParkingSpotAdapter.OnSpotListedChangeListener {

    private DatabaseHelper dbHelper;
    private ParkingSpotAdapter spotAdapter;
    private final List<ParkingSpot> managedSpots = new ArrayList<>();
    private ValueEventListener spotsListener;
    private Query spotsQuery;

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

        // --- UPDATED Adapter creation to pass 'this' for the new listener ---
        spotAdapter = new ParkingSpotAdapter(getContext(), managedSpots,
                spot -> {
                    // This is the onItemClick listener
                    Intent intent = new Intent(getActivity(), EditSpotActivity.class);
                    intent.putExtra("SPOT_ID", spot.getId());
                    startActivity(intent);
                },
                this // This is the new onSpotListedChanged listener
        );
        spotsRecyclerView.setAdapter(spotAdapter);

        FloatingActionButton addSpotFab = view.findViewById(R.id.add_spot_fab);
        addSpotFab.setOnClickListener(v -> startActivity(new Intent(getActivity(), EditSpotActivity.class)));
    }

    // --- NEW METHOD to handle the switch change ---
    @Override
    public void onSpotListedChanged(ParkingSpot spot, boolean isListed) {
        if (dbHelper != null && spot.getId() != null) {
            dbHelper.updateSpotListedStatus(spot.getId(), isListed, task -> {
                if (!task.isSuccessful()) {
                    // If the database update fails, show an error and refresh the adapter
                    // This will reset the switch to its original state
                    Toast.makeText(getContext(), "Failed to update status. Please try again.", Toast.LENGTH_SHORT).show();
                    spotAdapter.notifyDataSetChanged();
                } else {
                    // Success! Update the local object's state
                    spot.setListed(isListed);
                }
            });
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        attachSpotsListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (spotsQuery != null && spotsListener != null) {
            spotsQuery.removeEventListener(spotsListener);
        }
    }

    private void attachSpotsListener() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        spotsQuery = dbHelper.getSpotsForManagerQuery(currentUser.getUid());
        spotsListener = new ValueEventListener() {
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
        };
        spotsQuery.addValueEventListener(spotsListener);
    }
}