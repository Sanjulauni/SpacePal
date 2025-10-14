package com.example.sp_v1.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sp_v1.R;
import com.example.sp_v1.adapters.ReservationAdapter;
import com.example.sp_v1.models.Reservation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyReservationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ReservationAdapter adapter;
    private List<Reservation> reservationList;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reservations);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAuth = FirebaseAuth.getInstance();
        recyclerView = findViewById(R.id.reservations_recycler_view);
        progressBar = findViewById(R.id.progress_bar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        reservationList = new ArrayList<>();

        // THIS IS THE CORRECTED LINE
        adapter = new ReservationAdapter(reservationList);

        recyclerView.setAdapter(adapter);

        loadUserReservations();
    }

    private void loadUserReservations() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        String userId = currentUser.getUid();
        Query reservationsQuery = FirebaseDatabase.getInstance().getReference("reservations")
                .orderByChild("userId")
                .equalTo(userId);

        reservationsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reservationList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot reservationSnapshot : snapshot.getChildren()) {
                        Reservation reservation = reservationSnapshot.getValue(Reservation.class);
                        if (reservation != null) {
                            reservationList.add(reservation);
                        }
                    }
                    // Optional: Sort reservations by start time, newest first
                    Collections.sort(reservationList, (r1, r2) -> Long.compare(r2.getStartTime(), r1.getStartTime()));
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MyReservationsActivity.this, "You have no reservations.", Toast.LENGTH_SHORT).show();
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyReservationsActivity.this, "Failed to load reservations.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}