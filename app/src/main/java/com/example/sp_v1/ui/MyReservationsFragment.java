package com.example.sp_v1.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
// Import MaterialCardView explicitly
import com.google.android.material.card.MaterialCardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sp_v1.R;
import com.example.sp_v1.adapters.ReservationAdapter;
import com.example.sp_v1.models.Reservation;
import com.example.sp_v1.utils.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MyReservationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ReservationAdapter adapter;
    private List<Reservation> reservationHistoryList;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;

    // --- Updated View Variables ---
    private MaterialCardView activeReservationCard; // Changed from CardView
    private TextView activeSpotName, activeTimer, noReservationsText, historyLabel;
    private Button btnCancel; // Still Button, but styled differently in XML
    private CountDownTimer countDownTimer;
    private ImageView qrCodeImageView;

    // --- Same constant ---
    private static final long RESERVATION_EXPIRATION_MS = 2 * 60 * 1000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the *redesigned* layout
        return inflater.inflate(R.layout.fragment_my_reservations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper();

        // --- Updated findViewById Calls ---
        progressBar = view.findViewById(R.id.progress_bar);
        noReservationsText = view.findViewById(R.id.no_reservations_text);
        historyLabel = view.findViewById(R.id.history_label);

        activeReservationCard = view.findViewById(R.id.active_reservation_card);
        activeSpotName = view.findViewById(R.id.active_spot_name);
        activeTimer = view.findViewById(R.id.active_timer);
        btnCancel = view.findViewById(R.id.btn_cancel_reservation);
        qrCodeImageView = view.findViewById(R.id.qr_code_image);

        recyclerView = view.findViewById(R.id.reservations_recycler_view);
        // --- End of Updates ---

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        reservationHistoryList = new ArrayList<>();
        adapter = new ReservationAdapter(getContext(), reservationHistoryList);
        recyclerView.setAdapter(adapter);

        loadUserReservations();
    }

    private void displayActiveReservation(Reservation reservation) {
        activeReservationCard.setVisibility(View.VISIBLE);
        historyLabel.setVisibility(View.GONE); // Hide history label when active card is shown
        recyclerView.setVisibility(View.GONE); // Hide history recycler when active card is shown
        noReservationsText.setVisibility(View.GONE); // Hide "no reservations" text


        if (reservation == null || reservation.getId() == null || reservation.getId().isEmpty()) {
            Log.e("MyReservationsFragment", "Cannot generate QR code: Reservation ID is null or empty.");
            qrCodeImageView.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Error: Could not display QR Code.", Toast.LENGTH_SHORT).show();
        } else {
            try {
                qrCodeImageView.setVisibility(View.VISIBLE);
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                // Adjust size slightly if needed for the new card layout
                Bitmap bitmap = barcodeEncoder.encodeBitmap(reservation.getId(), BarcodeFormat.QR_CODE, 350, 350);
                qrCodeImageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.e("MyReservationsFragment", "Error generating QR code", e);
                Toast.makeText(getContext(), "Could not generate QR Code", Toast.LENGTH_SHORT).show();
                qrCodeImageView.setVisibility(View.GONE);
            }
        }

        FirebaseDatabase.getInstance().getReference("parking_spots").child(reservation.getSpotId()).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String spotName = snapshot.getValue(String.class);
                        // Removed "Spot: " prefix for cleaner look in the new design
                        activeSpotName.setText(spotName != null ? spotName : "N/A");
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        activeSpotName.setText("N/A");
                    }
                });

        long elapsedTime = System.currentTimeMillis() - reservation.getStartTime();
        long timeLeft = RESERVATION_EXPIRATION_MS - elapsedTime;

        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                String time = String.format(Locale.getDefault(), "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)));
                // Removed "Time left: " prefix
                activeTimer.setText(time);
            }
            @Override
            public void onFinish() {
                Toast.makeText(getContext(), "Reservation expired and was automatically cancelled.", Toast.LENGTH_LONG).show();
                dbHelper.cancelReservation(reservation, task -> {
                    if (task.isSuccessful()) {
                        loadUserReservations(); // Reload to show history
                    } else {
                        // If cancel fails, at least hide the card
                        activeReservationCard.setVisibility(View.GONE);
                        historyLabel.setVisibility(View.VISIBLE); // Show history label again
                        recyclerView.setVisibility(View.VISIBLE); // Show history list again
                    }
                });
            }
        }.start();

        btnCancel.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Reservation")
                .setMessage("Are you sure you want to cancel this reservation?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (countDownTimer != null) countDownTimer.cancel();
                    dbHelper.cancelReservation(reservation, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Reservation cancelled.", Toast.LENGTH_SHORT).show();
                            loadUserReservations(); // Reload to show history
                        } else {
                            Toast.makeText(getContext(), "Failed to cancel.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show());
    }


    private void loadUserReservations() {
        progressBar.setVisibility(View.VISIBLE);
        noReservationsText.setVisibility(View.GONE);
        historyLabel.setVisibility(View.GONE);
        activeReservationCard.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE); // Initially hide history list

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            progressBar.setVisibility(View.GONE);
            noReservationsText.setVisibility(View.VISIBLE); // Show no reservations if not logged in
            return;
        }
        String userId = currentUser.getUid();
        Query reservationsQuery = FirebaseDatabase.getInstance().getReference("reservations")
                .orderByChild("userId")
                .equalTo(userId);

        reservationsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reservationHistoryList.clear();
                Reservation activeReservation = null;

                if (snapshot.exists()) {
                    for (DataSnapshot reservationSnapshot : snapshot.getChildren()) {
                        Reservation reservation = reservationSnapshot.getValue(Reservation.class);
                        if (reservation != null) {
                            // Store the ID within the reservation object
                            reservation.setId(reservationSnapshot.getKey());

                            // Check for active reservation (using status and time check)
                            if ("active".equals(reservation.getStatus()) && (System.currentTimeMillis() - reservation.getStartTime()) < RESERVATION_EXPIRATION_MS) {
                                activeReservation = reservation;
                            } else {
                                // Add non-active reservations to history
                                reservationHistoryList.add(reservation);
                            }
                        }
                    }
                }

                progressBar.setVisibility(View.GONE);

                if (activeReservation != null) {
                    // If there's an active one, display it
                    displayActiveReservation(activeReservation);
                    // Don't show history or "no reservations" text yet
                } else if (!reservationHistoryList.isEmpty()) {
                    // No active reservation, but there is history
                    historyLabel.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.VISIBLE);
                    noReservationsText.setVisibility(View.GONE);
                    Collections.sort(reservationHistoryList, (r1, r2) -> Long.compare(r2.getStartTime(), r1.getStartTime()));
                    adapter.notifyDataSetChanged();
                } else {
                    // No active reservation and no history
                    noReservationsText.setVisibility(View.VISIBLE);
                    historyLabel.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load reservations.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                noReservationsText.setVisibility(View.VISIBLE); // Show error state
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}