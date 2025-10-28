package com.example.sp_v1.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sp_v1.R;
import com.example.sp_v1.adapters.EventBannerAdapter;
import com.example.sp_v1.adapters.FrequentSpotAdapter;
import com.example.sp_v1.auth.LoginActivity;
import com.example.sp_v1.models.EventPost;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.models.Reservation;
import com.example.sp_v1.models.User;
import com.example.sp_v1.utils.DatabaseHelper;
// Import BottomNavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserDashboardFragment extends Fragment implements EventBannerAdapter.OnEventClickListener {

    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;

    private TextView userNameText;
    private CircleImageView profileImage;
    private RecyclerView frequentSpotsRecyclerView;
    private RecyclerView eventsRecyclerView;
    private EventBannerAdapter eventBannerAdapter;
    private List<EventPost> activeEventList;
    private FrequentSpotAdapter frequentSpotAdapter;
    private final List<ParkingSpot> frequentSpotsList = new ArrayList<>();
    private final Map<String, Integer> visitCounts = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper();

        userNameText = view.findViewById(R.id.user_name_text);
        profileImage = view.findViewById(R.id.profile_image);
        frequentSpotsRecyclerView = view.findViewById(R.id.frequent_spots_recycler_view);
        eventsRecyclerView = view.findViewById(R.id.events_recycler_view);

        setupClickListeners(); // This is the updated method
        setupUserProfile();
        setupFrequentSpotsRecyclerView();
        loadFrequentSpots();
        setupActiveEventsRecyclerView();
        loadActiveEvents();
    }

    /**
     * Sets up click listeners for UI elements.
     * Profile image click now navigates to the AccountFragment.
     */
    private void setupClickListeners() {
        profileImage.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                // Navigate to the Account Fragment using the method in MainActivity
                ((MainActivity) getActivity()).navigateToAccount();

                // Manually select the 'Account' item in the bottom navigation view
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation_view);
                if (bottomNav != null) {
                    bottomNav.getMenu().findItem(R.id.nav_account).setChecked(true);
                }
            }
        });
    }

    private void setupFrequentSpotsRecyclerView() {
        if (getContext() == null) return;
        frequentSpotAdapter = new FrequentSpotAdapter(frequentSpotsList, visitCounts, spot -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToMapWithFocus(spot.getId());
            }
        });
        frequentSpotsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        frequentSpotsRecyclerView.setAdapter(frequentSpotAdapter);
    }

    private void setupActiveEventsRecyclerView() {
        if (getContext() == null) return;
        activeEventList = new ArrayList<>();
        eventBannerAdapter = new EventBannerAdapter(getContext(), activeEventList, this);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        eventsRecyclerView.setAdapter(eventBannerAdapter);
    }

    private void loadActiveEvents() {
        Query activeEventsQuery = FirebaseDatabase.getInstance().getReference("event_posts")
                .orderByChild("status")
                .equalTo(EventPost.STATUS_ACTIVE);

        activeEventsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getContext() == null) return;
                activeEventList.clear();
                long currentTime = System.currentTimeMillis();

                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    EventPost event = eventSnapshot.getValue(EventPost.class);
                    if (event != null && event.getEndDate() > currentTime) {
                        activeEventList.add(event);
                    }
                }
                Collections.reverse(activeEventList);
                if (eventBannerAdapter != null) { // Add null check
                    eventBannerAdapter.notifyDataSetChanged();
                }

                if(getView() != null) {
                    TextView promotionsTitle = getView().findViewById(R.id.promotions_title);
                    if(promotionsTitle != null && eventsRecyclerView != null) { // Add null checks
                        if(activeEventList.isEmpty()) {
                            promotionsTitle.setVisibility(View.GONE);
                            eventsRecyclerView.setVisibility(View.GONE);
                        } else {
                            promotionsTitle.setVisibility(View.VISIBLE);
                            eventsRecyclerView.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("UserDashboard", "Failed to load active events: " + error.getMessage());
            }
        });
    }

    @Override
    public void onEventClick(EventPost eventPost) {
        if (getActivity() instanceof MainActivity) {
            Log.d("UserDashboard", "Event clicked. Filtering for managerId: " + eventPost.getManagerId());
            ((MainActivity) getActivity()).navigateToMapWithEventFilter(eventPost.getManagerId());
        }
    }


    private void setupUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), LoginActivity.class));
                getActivity().finish();
            }
            return;
        }

        dbHelper.getUser(currentUser.getUid(), new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getContext() == null || userNameText == null) return; // Add null check for userNameText
                User user = snapshot.getValue(User.class);
                if (user != null && user.getName() != null && !user.getName().isEmpty()) {
                    String[] nameParts = user.getName().split(" ");
                    userNameText.setText(nameParts[0]);
                } else {
                    userNameText.setText(currentUser.getEmail() != null ? currentUser.getEmail().split("@")[0] : "User");
                }
                // TODO: Load profile image if available
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load user data.", Toast.LENGTH_SHORT).show();
                    Log.e("UserDashboard", "Firebase DB Error: " + error.getMessage());
                }
            }
        });
    }

    private void loadFrequentSpots() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || getContext() == null) return;

        Query reservationsQuery = FirebaseDatabase.getInstance().getReference("reservations")
                .orderByChild("userId")
                .equalTo(currentUser.getUid());

        reservationsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getContext() == null || !snapshot.exists()) return;

                visitCounts.clear();
                for (DataSnapshot resSnapshot : snapshot.getChildren()) {
                    Reservation reservation = resSnapshot.getValue(Reservation.class);
                    if (reservation != null && "completed".equals(reservation.getStatus()) && reservation.getSpotId() != null) {
                        visitCounts.put(reservation.getSpotId(), visitCounts.getOrDefault(reservation.getSpotId(), 0) + 1);
                    }
                }

                if (visitCounts.isEmpty()) {
                    frequentSpotsList.clear();
                    if(frequentSpotAdapter != null) frequentSpotAdapter.notifyDataSetChanged();
                    Log.d("UserDashboard", "No completed reservations found for frequent spots.");
                    return;
                }

                List<Map.Entry<String, Integer>> sortedVisits = new ArrayList<>(visitCounts.entrySet());
                Collections.sort(sortedVisits, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

                List<String> topSpotIds = new ArrayList<>();
                for (int i = 0; i < Math.min(5, sortedVisits.size()); i++) {
                    topSpotIds.add(sortedVisits.get(i).getKey());
                }

                fetchSpotDetails(topSpotIds);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("UserDashboard", "Failed to load reservations for frequent spots.", error.toException());
                if(getContext() != null) Toast.makeText(getContext(), "Error loading history.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchSpotDetails(List<String> spotIds) {
        if (spotIds.isEmpty() || getContext() == null) {
            frequentSpotsList.clear();
            if(frequentSpotAdapter != null) frequentSpotAdapter.notifyDataSetChanged();
            return;
        }

        frequentSpotsList.clear();
        AtomicInteger counter = new AtomicInteger(spotIds.size());

        for (String spotId : spotIds) {
            FirebaseDatabase.getInstance().getReference("parking_spots").child(spotId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (getContext() == null) return;

                            ParkingSpot spot = snapshot.getValue(ParkingSpot.class);
                            if (spot != null) {
                                spot.setId(snapshot.getKey());
                                frequentSpotsList.add(spot);
                            } else {
                                Log.w("UserDashboard", "Frequent spot details not found for ID: " + spotId);
                            }
                            if (counter.decrementAndGet() == 0 && frequentSpotAdapter != null) {
                                frequentSpotAdapter.notifyDataSetChanged();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (getContext() == null) return;
                            Log.e("UserDashboard", "Failed to fetch spot details for ID: " + spotId, error.toException());
                            if (counter.decrementAndGet() == 0 && frequentSpotAdapter != null) {
                                frequentSpotAdapter.notifyDataSetChanged();
                            }
                        }
                    });
        }
    }
} // End Fragment