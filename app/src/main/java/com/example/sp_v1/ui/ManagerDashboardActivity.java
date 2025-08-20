package com.example.sp_v1.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.example.sp_v1.R;
import com.example.sp_v1.adapters.ParkingSpotAdapter;
import com.example.sp_v1.adapters.ParkingSpot;
import com.example.sp_v1.auth.LoginActivity;
import com.example.sp_v1.utils.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ManagerDashboardActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private RecyclerView spotsRecyclerView;
    private ParkingSpotAdapter spotAdapter;
    private List<ParkingSpot> managedSpots = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);
        Log.d("ManagerDashboard", "Layout set to activity_manager_dashboard");

        dbHelper = new DatabaseHelper();

        // Initialize RecyclerView
        spotsRecyclerView = findViewById(R.id.manager_spots_recycler_view);
        if (spotsRecyclerView != null) {
            spotsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            spotAdapter = new ParkingSpotAdapter(managedSpots);
            spotsRecyclerView.setAdapter(spotAdapter);
            Log.d("ManagerDashboard", "RecyclerView initialized");
        } else {
            Log.e("ManagerDashboard", "manager_spots_recycler_view is null");
            Toast.makeText(this, "Error: RecyclerView not found", Toast.LENGTH_SHORT).show();
        }

        // Initialize Update Button
        MaterialButton updateButton = findViewById(R.id.update_availability_button);
        if (updateButton != null) {
            updateButton.setOnClickListener(v -> {
                Toast.makeText(this, "Update availability clicked", Toast.LENGTH_SHORT).show();
                Log.d("ManagerDashboard", "Update button clicked");
            });
            Log.d("ManagerDashboard", "Button initialized");
        } else {
            Log.e("ManagerDashboard", "update_availability_button is null");
            Toast.makeText(this, "Error: Button not found", Toast.LENGTH_SHORT).show();
        }

        // Initialize Logout Button
        MaterialButton logoutButton = findViewById(R.id.logout_button);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                dbHelper = new DatabaseHelper(); // Ensure dbHelper is initialized
                logout();
            });
            Log.d("ManagerDashboard", "Logout button initialized");
        } else {
            Log.e("ManagerDashboard", "logout_button is null");
            Toast.makeText(this, "Error: Logout button not found", Toast.LENGTH_SHORT).show();
        }

        // Placeholder data
        loadSampleSpots();
    }

    private void loadSampleSpots() {
        managedSpots.add(new ParkingSpot("Spot 1", 6.9271, 79.8612, true, "manager1", 50, 30));
        managedSpots.add(new ParkingSpot("Spot 2", 6.9272, 79.8613, false, "manager1", 40, 20));
        if (spotAdapter != null) {
            spotAdapter.notifyDataSetChanged();
            Log.d("ManagerDashboard", "Sample spots loaded: " + managedSpots.size());
        } else {
            Log.e("ManagerDashboard", "spotAdapter is null");
        }
    }

    private void logout() {
        try {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } catch (Exception e) {
            Log.e("ManagerDashboard", "Logout failed: " + e.getMessage());
            Toast.makeText(this, "Logout failed", Toast.LENGTH_SHORT).show();
        }
    }
}