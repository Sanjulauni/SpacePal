package com.example.sp_v1.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem; // Keep this import
import androidx.annotation.NonNull; // Keep this import
import androidx.appcompat.app.AlertDialog; // <-- Import AlertDialog
import androidx.appcompat.app.AppCompatActivity;
// import androidx.appcompat.widget.Toolbar; // --- REMOVED ---
import androidx.fragment.app.Fragment;
import com.example.sp_v1.R;
import com.example.sp_v1.auth.LoginActivity;
import com.example.sp_v1.ui.manager.DashboardFragment;
import com.example.sp_v1.ui.manager.MyEventsFragment;
import com.example.sp_v1.ui.manager.MySpotsFragment;
import com.example.sp_v1.ui.manager.ProfileFragment;
import com.example.sp_v1.ui.manager.SettingsFragment; // Keep for navigation target
// --- Import BottomNavigationView ---
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class ManagerDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        mAuth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // --- REMOVED Toolbar setup ---
        // Toolbar toolbar = findViewById(R.id.toolbar);
        // setSupportActionBar(toolbar);
        // --- END REMOVAL ---

        // Setup Bottom Navigation
        bottomNav = findViewById(R.id.bottom_navigation_view_manager);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;
            // String title = ""; // Title no longer needed

            if (itemId == R.id.nav_dashboard) {
                selectedFragment = new DashboardFragment();
                // title = "Dashboard";
            } else if (itemId == R.id.nav_my_spots) {
                selectedFragment = new MySpotsFragment();
                // title = "My Spots";
            } else if (itemId == R.id.nav_my_events) {
                selectedFragment = new MyEventsFragment();
                // title = "My Events";
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
                // title = "Profile";
            }

            if (selectedFragment != null) {
                // --- Pass only the fragment ---
                replaceFragment(selectedFragment);
                return true; // Indicate event was handled
            }
            return false; // Indicate event was not handled
        });

        // Load the default fragment
        if (savedInstanceState == null) {
            navigateToDashboard(); // Load dashboard by default
        }
    }

    public void logoutUser() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (bottomNav.getSelectedItemId() != R.id.nav_dashboard) {
            // If not on Dashboard, go to Dashboard
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
            // The listener above will handle the fragment replacement
        } else {
            // If on Dashboard, exit the app
            super.onBackPressed();
        }
    }

    // --- Navigation Methods (Simplified) ---
    public void navigateToDashboard() {
        replaceFragment(new DashboardFragment());
    }

    public void navigateToMySpots() {
        replaceFragment(new MySpotsFragment());
    }

    public void navigateToMyEvents() {
        replaceFragment(new MyEventsFragment());
    }

    public void navigateToProfile() {
        replaceFragment(new ProfileFragment());
        if(bottomNav.getSelectedItemId() != R.id.nav_profile) {
            bottomNav.getMenu().findItem(R.id.nav_profile).setChecked(true);
        }
    }

    public void navigateToSettings() {
        replaceFragment(new SettingsFragment());
    }

    // Helper method for fragment transactions (no title needed)
    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        // --- REMOVED Title setting ---
        // if (getSupportActionBar() != null) {
        //     getSupportActionBar().setTitle(title);
        // }
    }
}