package com.example.sp_v1.ui.manager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sp_v1.R;
// --- REMOVED ManagerDashboardActivity import ---
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private SwitchMaterial reservationNotificationsSwitch;
    private SwitchMaterial spotStatusNotificationsSwitch;
    private SwitchMaterial emailNotificationsSwitch;
    private SwitchMaterial darkModeSwitch;
    private SwitchMaterial autoRefreshSwitch;
    private TextView appVersionText;

    private SharedPreferences preferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_settings, container, false);

        // Initialize views
        reservationNotificationsSwitch = view.findViewById(R.id.reservation_notifications_switch);
        spotStatusNotificationsSwitch = view.findViewById(R.id.spot_status_notifications_switch);
        emailNotificationsSwitch = view.findViewById(R.id.email_notifications_switch);
        darkModeSwitch = view.findViewById(R.id.dark_mode_switch);
        autoRefreshSwitch = view.findViewById(R.id.auto_refresh_switch);
        appVersionText = view.findViewById(R.id.app_version_text);

        // Initialize SharedPreferences
        if (getActivity() != null) { // Add null check for getActivity()
            preferences = getActivity().getSharedPreferences("manager_settings", 0);
        }

        // --- REMOVED Click listeners for change_password_layout and edit_profile_layout ---
        // These actions are now handled in ProfileFragment

        view.findViewById(R.id.privacy_policy_layout).setOnClickListener(v -> openPrivacyPolicy());
        view.findViewById(R.id.terms_of_service_layout).setOnClickListener(v -> openTermsOfService());

        // Set up switch listeners
        setupSwitchListeners();

        // Load saved preferences
        loadPreferences();

        // Set app version (replace with dynamic version later if needed)
        appVersionText.setText("1.0.0"); // Example version

        return view;
    }

    private void setupSwitchListeners() {
        // Add null checks for safety
        if (reservationNotificationsSwitch != null) {
            reservationNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                savePreference("reservation_notifications", isChecked);
                if (isChecked) Toast.makeText(getContext(), "Reservation notifications enabled", Toast.LENGTH_SHORT).show();
                else Toast.makeText(getContext(), "Reservation notifications disabled", Toast.LENGTH_SHORT).show();
            });
        }

        if (spotStatusNotificationsSwitch != null) {
            spotStatusNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                savePreference("spot_status_notifications", isChecked);
                if (isChecked) Toast.makeText(getContext(), "Spot status notifications enabled", Toast.LENGTH_SHORT).show();
                else Toast.makeText(getContext(), "Spot status notifications disabled", Toast.LENGTH_SHORT).show();
            });
        }

        if (emailNotificationsSwitch != null) {
            emailNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                savePreference("email_notifications", isChecked);
                if (isChecked) Toast.makeText(getContext(), "Email notifications enabled", Toast.LENGTH_SHORT).show();
                else Toast.makeText(getContext(), "Email notifications disabled", Toast.LENGTH_SHORT).show();
            });
        }

        if (darkModeSwitch != null) {
            darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                savePreference("dark_mode", isChecked);
                // Note: Applying dark mode usually requires recreating the Activity
                if (isChecked) Toast.makeText(getContext(), "Dark mode enabled. Restart app to apply.", Toast.LENGTH_LONG).show();
                else Toast.makeText(getContext(), "Dark mode disabled. Restart app to apply.", Toast.LENGTH_LONG).show();
            });
        }

        if (autoRefreshSwitch != null) {
            autoRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                savePreference("auto_refresh", isChecked);
                if (isChecked) Toast.makeText(getContext(), "Auto-refresh enabled", Toast.LENGTH_SHORT).show();
                else Toast.makeText(getContext(), "Auto-refresh disabled", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void savePreference(String key, boolean value) {
        if (preferences != null) { // Add null check
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(key, value);
            editor.apply();
        }
    }

    private void loadPreferences() {
        if (preferences != null) { // Add null check
            if (reservationNotificationsSwitch != null) reservationNotificationsSwitch.setChecked(preferences.getBoolean("reservation_notifications", true));
            if (spotStatusNotificationsSwitch != null) spotStatusNotificationsSwitch.setChecked(preferences.getBoolean("spot_status_notifications", true));
            if (emailNotificationsSwitch != null) emailNotificationsSwitch.setChecked(preferences.getBoolean("email_notifications", false));
            if (darkModeSwitch != null) darkModeSwitch.setChecked(preferences.getBoolean("dark_mode", false));
            if (autoRefreshSwitch != null) autoRefreshSwitch.setChecked(preferences.getBoolean("auto_refresh", true));
        }
    }

    private void openPrivacyPolicy() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/privacy-policy")); // Use a real URL
            startActivity(intent);
        } catch (Exception e) {
            if (getContext() != null) Toast.makeText(getContext(), "Unable to open privacy policy", Toast.LENGTH_SHORT).show();
        }
    }

    private void openTermsOfService() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/terms-of-service")); // Use a real URL
            startActivity(intent);
        } catch (Exception e) {
            if (getContext() != null) Toast.makeText(getContext(), "Unable to open terms of service", Toast.LENGTH_SHORT).show();
        }
    }
}