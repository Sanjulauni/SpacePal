package com.example.sp_v1.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager; // Keep this

import com.example.sp_v1.R;
import com.example.sp_v1.auth.LoginActivity;
import com.google.android.libraries.places.api.Places;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNav;

    // --- ADD THIS LINE ---
    private boolean isNavigatingProgrammatically = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initializePlacesSDK();

        bottomNav = findViewById(R.id.bottom_navigation_view);
        bottomNav.setOnItemSelectedListener(item -> {

            // --- ADD THIS 2-LINE BLOCK ---
            if (isNavigatingProgrammatically) {
                return true; // We are handling navigation, so listener should do nothing
            }

            int itemId = item.getItemId();
            Fragment targetFragment = null;

            // Determine target fragment based on ID
            if (itemId == R.id.nav_dashboard) {
                targetFragment = new UserDashboardFragment();
            } else if (itemId == R.id.nav_map) {
                targetFragment = new MapFragment(); // Simple map, no args
            } else if (itemId == R.id.nav_my_reservations) {
                targetFragment = new MyReservationsFragment();
            } else if (itemId == R.id.nav_account) {
                targetFragment = new AccountFragment();
            }

            // Perform transaction ONLY if target is valid AND different from current
            if (targetFragment != null && !isFragmentAlreadyDisplayed(targetFragment.getClass())) {
                replaceFragment(targetFragment); // Use the simple replace method
                return true; // Event handled
            } else if (targetFragment != null) {
                // Already displaying this fragment type, do nothing but consume event
                return true;
            }

            return false; // Event not handled
        });

        // Initial fragment load
        if (getIntent().hasExtra("SPOT_ID_TO_FOCUS")) {
            // If started with intent, navigate programmatically
            navigateToMapWithFocus(getIntent().getStringExtra("SPOT_ID_TO_FOCUS"));
        } else if (savedInstanceState == null) {
            // Default to dashboard - directly replace
            replaceFragment(new UserDashboardFragment());
            // Ensure the correct item is visually selected
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        }
    }

    // Helper to check currently displayed fragment
    private boolean isFragmentAlreadyDisplayed(Class<? extends Fragment> fragmentClass) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        return currentFragment != null && currentFragment.getClass().equals(fragmentClass);
    }


    private void initializePlacesSDK() {
        // ... (this method remains the same) ...
        String apiKey = "";
        try {
            ApplicationInfo app = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = app.metaData;
            if (metaData != null) {
                apiKey = metaData.getString("com.google.android.geo.API_KEY");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("MainActivity", "Failed to load Places API key", e);
            Toast.makeText(this, "Error loading API Key. Search may not work.", Toast.LENGTH_LONG).show();
            return;
        }

        if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("${MAPS_API_KEY}")) {
            if (!Places.isInitialized()) {
                Places.initialize(getApplicationContext(), apiKey);
                Log.i("MainActivity", "Places SDK Initialized successfully.");
            }
        } else {
            Log.e("MainActivity", "Places API key is missing, invalid, or still a placeholder. Search will not work.");
            Toast.makeText(this, "Map Search configuration error.", Toast.LENGTH_LONG).show();
        }
    }

    // --- Navigation methods for programmatic calls ---
    // These methods ALWAYS replace the fragment to pass new arguments

    public void navigateToDashboard() {
        replaceFragment(new UserDashboardFragment());
        if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_dashboard) {
            isNavigatingProgrammatically = true; // Set flag
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
            isNavigatingProgrammatically = false; // Unset flag
        }
    }

    public void navigateToReservations() {
        replaceFragment(new MyReservationsFragment());
        if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_my_reservations) {
            isNavigatingProgrammatically = true; // Set flag
            bottomNav.setSelectedItemId(R.id.nav_my_reservations);
            isNavigatingProgrammatically = false; // Unset flag
        }
    }

    // Call this from Dashboard Quick Access etc.
    public void navigateToMapWithFocus(String spotId) {
        MapFragment mapFragment = new MapFragment();
        if (spotId != null) {
            Bundle args = new Bundle();
            args.putString("SPOT_ID_TO_FOCUS", spotId);
            mapFragment.setArguments(args);
        }

        replaceFragment(mapFragment);

        // Manually select map item
        if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_map) {
            // --- ADD THIS WRAPPER ---
            isNavigatingProgrammatically = true;
            bottomNav.setSelectedItemId(R.id.nav_map);
            isNavigatingProgrammatically = false;
        }
    }

    // Call this from Dashboard Event Banner etc.
    public void navigateToMapWithEventFilter(String managerId) {
        MapFragment mapFragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString("FILTER_BY_MANAGER_ID", managerId);
        mapFragment.setArguments(args);

        replaceFragment(mapFragment);

        // Manually select map item
        if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_map) {
            // --- ADD THIS WRAPPER ---
            isNavigatingProgrammatically = true;
            bottomNav.setSelectedItemId(R.id.nav_map);
            isNavigatingProgrammatically = false;
        }
    }

    public void navigateToAccount() {
        replaceFragment(new AccountFragment());
        if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_account) {
            // --- ADD THIS WRAPPER ---
            isNavigatingProgrammatically = true;
            bottomNav.setSelectedItemId(R.id.nav_account);
            isNavigatingProgrammatically = false;
        }
    }

    // Basic helper method for fragment transactions
    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss(); // Use allowingStateLoss for safety
    }

    public void logoutUser() {
        // ... (logout logic remains the same) ...
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
        if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_dashboard) {
            // If not on Dashboard, switch tab to Dashboard (listener will handle fragment change)
            isNavigatingProgrammatically = false; // Ensure flag is reset
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        } else {
            // If already on Dashboard, exit the app
            super.onBackPressed();
        }
    }
}