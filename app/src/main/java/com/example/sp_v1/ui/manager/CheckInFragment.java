package com.example.sp_v1.ui.manager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sp_v1.R;
import com.example.sp_v1.adapters.ReservationManagerAdapter;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.models.Reservation;
import com.example.sp_v1.models.User;
import com.example.sp_v1.utils.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CheckInFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private ReservationManagerAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_check_in, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new DatabaseHelper();
        RecyclerView recyclerView = view.findViewById(R.id.check_in_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ReservationManagerAdapter(new ArrayList<>(), new HashMap<>(), new HashMap<>(), reservation -> {
            dbHelper.updateReservationStatus(reservation.getId(), "in-progress", task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "User checked in!", Toast.LENGTH_SHORT).show();
                    loadAndFilterReservations();
                } else {
                    Toast.makeText(getContext(), "Check-in failed.", Toast.LENGTH_SHORT).show();
                }
            });
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAndFilterReservations();
    }

    private void loadAndFilterReservations() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        dbHelper.getSpotsForManager(currentUser.getUid(), new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot managerSpotsSnapshot) {
                Set<String> managerSpotIds = new HashSet<>();
                for (DataSnapshot spotSnapshot : managerSpotsSnapshot.getChildren()) {
                    managerSpotIds.add(spotSnapshot.getKey());
                }

                if (managerSpotIds.isEmpty()) {
                    adapter.updateData(new ArrayList<>(), new HashMap<>(), new HashMap<>());
                    return;
                }

                dbHelper.getReservationsByStatus("active", new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot reservationsSnapshot) {
                        List<Reservation> filteredReservations = new ArrayList<>();
                        Set<String> spotIds = new HashSet<>();
                        Set<String> userIds = new HashSet<>();

                        for (DataSnapshot resSnapshot : reservationsSnapshot.getChildren()) {
                            Reservation res = resSnapshot.getValue(Reservation.class);
                            if (res != null && managerSpotIds.contains(res.getSpotId())) {
                                filteredReservations.add(res);
                                spotIds.add(res.getSpotId());
                                userIds.add(res.getUserId());
                            }
                        }

                        if (!filteredReservations.isEmpty()) {
                            fetchDetails(filteredReservations, new ArrayList<>(spotIds), new ArrayList<>(userIds));
                        } else {
                            adapter.updateData(new ArrayList<>(), new HashMap<>(), new HashMap<>());
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchDetails(List<Reservation> reservations, List<String> spotIds, List<String> userIds) {
        dbHelper.getSpotsByIds(spotIds, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot spotsSnapshot) {
                Map<String, ParkingSpot> spotMap = new HashMap<>();
                for (DataSnapshot spotSnapshot : spotsSnapshot.getChildren()) {
                    spotMap.put(spotSnapshot.getKey(), spotSnapshot.getValue(ParkingSpot.class));
                }

                dbHelper.getUsersByIds(userIds, new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                        Map<String, User> userMap = new HashMap<>();
                        for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                            userMap.put(userSnapshot.getKey(), userSnapshot.getValue(User.class));
                        }
                        adapter.updateData(reservations, spotMap, userMap);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}