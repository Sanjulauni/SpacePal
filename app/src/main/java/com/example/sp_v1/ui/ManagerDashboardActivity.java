package com.example.sp_v1.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.example.sp_v1.R;
import com.example.sp_v1.auth.LoginActivity;
import com.example.sp_v1.ui.manager.DashboardFragment;
import com.example.sp_v1.ui.manager.MySpotsFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class ManagerDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Load the default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new DashboardFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_dashboard);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Dashboard");
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        String title = "";
        int itemId = item.getItemId();

        if (itemId == R.id.nav_dashboard) {
            selectedFragment = new DashboardFragment();
            title = "Dashboard";
        } else if (itemId == R.id.nav_my_spots) {
            selectedFragment = new MySpotsFragment();
            title = "My Spots";
        } else if (itemId == R.id.nav_logout) {
            logout();
            return true; // Logout action handled, no fragment to load
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(ManagerDashboardActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}