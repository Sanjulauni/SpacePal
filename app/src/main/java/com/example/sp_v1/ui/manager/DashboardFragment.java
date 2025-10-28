package com.example.sp_v1.ui.manager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sp_v1.R;
import com.example.sp_v1.adapters.ReservationManagerAdapter;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.models.Reservation;
import com.example.sp_v1.models.User;
import com.example.sp_v1.ui.VerificationActivity;
import com.example.sp_v1.utils.DatabaseHelper;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {

    // --- Views ---
    private View verifiedDashboardContent, unverifiedView, pendingView;
    private ProgressBar dashboardProgressBar;
    private Button btnStartVerification;
    private ExtendedFloatingActionButton scanFab;
    // --- REMOVED earningsText ---
    private TextView welcomeText, occupiedSpotsText, emptyView;
    private RecyclerView reservationsRecyclerView;

    // --- Firebase & Data ---
    private ReservationManagerAdapter adapter;
    private DatabaseHelper dbHelper;
    private final List<Reservation> reservationList = new ArrayList<>();
    private volatile Map<String, ParkingSpot> spotMap = new HashMap<>();
    private final Map<String, User> userMap = new HashMap<>();
    private DataSnapshot allReservationsSnapshot;
    private ValueEventListener spotsListener, reservationsListener;
    private Query spotsQuery, reservationsQuery;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new DatabaseHelper();

        // --- Initialize Views ---
        verifiedDashboardContent = view.findViewById(R.id.verified_dashboard_content);
        unverifiedView = view.findViewById(R.id.unverified_view);
        pendingView = view.findViewById(R.id.pending_view);
        dashboardProgressBar = view.findViewById(R.id.dashboard_progress_bar);
        btnStartVerification = view.findViewById(R.id.btn_start_verification);
        scanFab = view.findViewById(R.id.scan_fab);

        // Views inside the verified dashboard
        welcomeText = view.findViewById(R.id.dashboard_welcome_text);
        // --- REMOVED findViewById for earnings_text ---
        occupiedSpotsText = view.findViewById(R.id.occupied_spots_text);
        reservationsRecyclerView = view.findViewById(R.id.reservations_recycler_view);
        emptyView = view.findViewById(R.id.empty_view);

        setupRecyclerView();

        scanFab.setOnClickListener(v -> {
            IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
            integrator.setPrompt("Scan a driver's reservation QR code");
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });
    }

    // ... (onActivityResult, handleScannedReservation, showCheckInConfirmationDialog, showCheckOutConfirmationDialog remain the same) ...
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(getContext(), "Scan cancelled", Toast.LENGTH_LONG).show();
            } else {
                String reservationId = result.getContents();
                handleScannedReservation(reservationId);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleScannedReservation(String reservationId) {

        if (spotMap.isEmpty()) {
            Toast.makeText(getContext(), "Spot data is not loaded or you have no spots.", Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseReference reservationRef = FirebaseDatabase.getInstance().getReference("reservations").child(reservationId);
        reservationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(getContext(), "Error: Invalid reservation code.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Reservation reservation = snapshot.getValue(Reservation.class);
                if (reservation == null) {
                    Toast.makeText(getContext(), "Error: Could not read reservation data.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, ParkingSpot> currentSpotMap = spotMap;
                if (!currentSpotMap.containsKey(reservation.getSpotId())) {
                    Toast.makeText(getContext(), "Error: This reservation is not for one of your spots.", Toast.LENGTH_LONG).show();
                    return;
                }

                String status = reservation.getStatus();
                reservation.setId(snapshot.getKey());

                if ("active".equals(status)) {
                    showCheckInConfirmationDialog(reservation, currentSpotMap);
                } else if ("in-progress".equals(status)) {
                    showCheckOutConfirmationDialog(reservation, currentSpotMap);
                } else {
                    Toast.makeText(getContext(), "This reservation is already " + status, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCheckInConfirmationDialog(Reservation reservation, Map<String, ParkingSpot> currentSpotMap) {
        dbHelper.getSpecificUsers(List.of(reservation.getUserId()), userMap -> {
            User user = userMap.get(reservation.getUserId());
            String userName = (user != null && user.getName() != null) ? user.getName() : "this user";
            String spotName = (currentSpotMap.containsKey(reservation.getSpotId()))
                    ? currentSpotMap.get(reservation.getSpotId()).getName()
                    : "an unknown spot";

            new AlertDialog.Builder(getContext())
                    .setTitle("Confirm Check-in")
                    .setMessage("Do you want to check in " + userName + " for spot '" + spotName + "'?")
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        dbHelper.checkInUser(reservation, task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getContext(), "User checked in successfully!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Check-in failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void showCheckOutConfirmationDialog(Reservation reservation, Map<String, ParkingSpot> currentSpotMap) {
        dbHelper.getSpecificUsers(List.of(reservation.getUserId()), userMap -> {
            User user = userMap.get(reservation.getUserId());
            String userName = (user != null && user.getName() != null) ? user.getName() : "this user";
            String spotName = (currentSpotMap.containsKey(reservation.getSpotId()))
                    ? currentSpotMap.get(reservation.getSpotId()).getName()
                    : "an unknown spot";

            new AlertDialog.Builder(getContext())
                    .setTitle("Confirm Check-out")
                    .setMessage("Do you want to check out " + userName + " from spot '" + spotName + "'?")
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        dbHelper.checkOutUser(reservation, task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getContext(), "User checked out successfully!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Check-out failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }


    @Override
    public void onStart() {
        super.onStart();
        checkUserVerificationStatus();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (spotsQuery != null && spotsListener != null) spotsQuery.removeEventListener(spotsListener);
        if (reservationsQuery != null && reservationsListener != null) reservationsQuery.removeEventListener(reservationsListener);
        if (scanFab != null) {
            scanFab.setVisibility(View.GONE);
        }
    }

    private void checkUserVerificationStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        dashboardProgressBar.setVisibility(View.VISIBLE);
        verifiedDashboardContent.setVisibility(View.GONE);
        unverifiedView.setVisibility(View.GONE);
        pendingView.setVisibility(View.GONE);
        scanFab.setVisibility(View.GONE);
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dashboardProgressBar.setVisibility(View.GONE);
                User user = snapshot.getValue(User.class);
                if (user == null || user.getPartnerStatus() == null) {
                    showUnverifiedView();
                    return;
                }
                switch (user.getPartnerStatus()) {
                    case User.STATUS_ID_VERIFIED:
                        showVerifiedDashboard();
                        break;
                    case User.STATUS_PENDING_ID:
                        showPendingView();
                        break;

                    case User.STATUS_UNVERIFIED:
                    default:
                        showUnverifiedView();
                        break;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                dashboardProgressBar.setVisibility(View.GONE);
            }
        });
    }

    private void showVerifiedDashboard() {
        verifiedDashboardContent.setVisibility(View.VISIBLE);
        unverifiedView.setVisibility(View.GONE);
        pendingView.setVisibility(View.GONE);
        loadVerifiedDashboardData();
    }

    private void showPendingView() {
        pendingView.setVisibility(View.VISIBLE);
        unverifiedView.setVisibility(View.GONE);
        verifiedDashboardContent.setVisibility(View.GONE);
        scanFab.setVisibility(View.GONE);
    }

    private void showUnverifiedView() {
        unverifiedView.setVisibility(View.VISIBLE);
        pendingView.setVisibility(View.GONE);
        verifiedDashboardContent.setVisibility(View.GONE);
        scanFab.setVisibility(View.GONE);
        btnStartVerification.setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), VerificationActivity.class));
            }
        });
    }

    private void loadVerifiedDashboardData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        welcomeText.setText("Today's Summary");
        attachSpotsListener(currentUser.getUid());
        attachReservationsListener();
    }

    private void setupRecyclerView() {
        reservationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReservationManagerAdapter(getContext(), reservationList, spotMap, userMap);
        reservationsRecyclerView.setAdapter(adapter);
    }

    private void attachSpotsListener(String managerId) {
        spotsQuery = dbHelper.getSpotsForManagerQuery(managerId);
        spotsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, ParkingSpot> newSpotMap = new HashMap<>();
                int totalSlots = 0;
                int availableSpaces = 0;
                for (DataSnapshot spotSnapshot : snapshot.getChildren()) {
                    ParkingSpot spot = spotSnapshot.getValue(ParkingSpot.class);
                    if (spot != null) {
                        spot.setId(spotSnapshot.getKey());
                        newSpotMap.put(spot.getId(), spot);
                        totalSlots += spot.getTotalSlots();
                        availableSpaces += spot.getAvailableSpaces();
                    }
                }

                spotMap = newSpotMap;

                if (scanFab != null) {
                    if (spotMap.isEmpty()) {
                        scanFab.setVisibility(View.GONE);
                        Log.w("DashboardFragment", "No spots found for this manager. Scan button will be hidden.");
                    } else {
                        scanFab.setVisibility(View.VISIBLE);
                    }
                }

                updateSummaryCards(totalSlots, availableSpaces);
                processAndDisplayReservations();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DashboardFragment", "Spots listener cancelled: " + error.getMessage());
                if (scanFab != null) {
                    scanFab.setVisibility(View.GONE); // Hide fab on error
                }
            }
        };
        spotsQuery.addValueEventListener(spotsListener);
    }

    private void attachReservationsListener() {
        reservationsQuery = dbHelper.getReservationsQuery();
        reservationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allReservationsSnapshot = snapshot;
                processAndDisplayReservations();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DashboardFragment", "Reservations listener cancelled: " + error.getMessage());
            }
        };
        reservationsQuery.addValueEventListener(reservationsListener);
    }

    private void processAndDisplayReservations() {
        if (allReservationsSnapshot == null) {
            return;
        }

        final Map<String, ParkingSpot> currentSpotMap = spotMap;
        if (currentSpotMap.isEmpty()) {
            updateUIForEmptyState();
            return;
        }

        reservationList.clear();
        List<String> userIdsToFetch = new ArrayList<>();
        for (DataSnapshot resSnapshot : allReservationsSnapshot.getChildren()) {
            Reservation reservation = resSnapshot.getValue(Reservation.class);
            if (reservation != null && currentSpotMap.containsKey(reservation.getSpotId())) {
                String status = reservation.getStatus();
                if ("active".equals(status) || "in-progress".equals(status)) {
                    reservation.setId(resSnapshot.getKey());
                    reservationList.add(reservation);
                    if (!userIdsToFetch.contains(reservation.getUserId())) {
                        userIdsToFetch.add(reservation.getUserId());
                    }
                }
            }
        }
        if (!reservationList.isEmpty()) {
            fetchUsers(userIdsToFetch);
        } else {
            updateUIForEmptyState();
        }
    }

    private void fetchUsers(List<String> userIds) {
        dbHelper.getSpecificUsers(userIds, fetchedUserMap -> {
            userMap.clear();
            userMap.putAll(fetchedUserMap);
            updateUIAfterLoad();
        });
    }

    private void updateSummaryCards(int totalSlots, int availableSpaces) {
        int occupied = totalSlots - availableSpaces;
        occupiedSpotsText.setText(String.format("%d / %d", occupied, totalSlots));

        // --- REMOVED setting earningsText ---
        // earningsText.setText("$0");
    }

    private void updateUIForEmptyState() {
        emptyView.setVisibility(View.VISIBLE);
        reservationsRecyclerView.setVisibility(View.GONE);
        reservationList.clear();
        if(adapter != null) adapter.notifyDataSetChanged();
    }

    private void updateUIAfterLoad() {
        emptyView.setVisibility(View.GONE);
        reservationsRecyclerView.setVisibility(View.VISIBLE);
        if(adapter != null) adapter.notifyDataSetChanged();
    }
}