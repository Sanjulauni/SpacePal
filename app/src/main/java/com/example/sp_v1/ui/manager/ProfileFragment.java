package com.example.sp_v1.ui.manager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // <-- Import Button
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sp_v1.R;
import com.example.sp_v1.models.User;
import com.example.sp_v1.ui.ManagerDashboardActivity;
import com.example.sp_v1.utils.DatabaseHelper;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView profileName, profileEmail, profileRole, profileCreatedDate, profileTotalSpots;
    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;
    private User currentUserData;
    // --- ADD Logout Button ---
    private Button btnLogout, btnSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // --- UPDATED Layout reference ---
        View view = inflater.inflate(R.layout.fragment_manager_profile, container, false);

        // Initialize views
        profileName = view.findViewById(R.id.profile_name);
        profileEmail = view.findViewById(R.id.profile_email);
        profileRole = view.findViewById(R.id.profile_role);
        profileCreatedDate = view.findViewById(R.id.profile_created_date);
        profileTotalSpots = view.findViewById(R.id.profile_total_spots);
        btnLogout = view.findViewById(R.id.logout_button); // <-- Initialize Logout Button
        btnSettings = view.findViewById(R.id.view_settings_button); // <-- Re-initialize Settings Button

        // Initialize Firebase and DatabaseHelper
        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper();

        // Set up click listeners
        view.findViewById(R.id.edit_profile_button).setOnClickListener(v -> showEditProfileDialog());
        view.findViewById(R.id.change_password_button).setOnClickListener(v -> showChangePasswordDialog());

        // --- UPDATED Settings button to call method in Activity ---
        btnSettings.setOnClickListener(v -> {
            if (getActivity() instanceof ManagerDashboardActivity) {
                ((ManagerDashboardActivity) getActivity()).navigateToSettings();
            }
        });

        // --- ADDED Logout button listener ---
        btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof ManagerDashboardActivity) {
                ((ManagerDashboardActivity) getActivity()).logoutUser();
            }
        });
        // --- END ADDED ---

        // Load user data
        loadUserData();

        return view;
    }

    // ... (loadUserData, loadTotalSpotsCount, showEditProfileDialog, updateUserProfile remain the same) ...
    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser(); // Get current user
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String userEmail = currentUser.getEmail();

            // Set email
            if (userEmail != null) {
                profileEmail.setText(userEmail);
            }

            // Load user data from database
            dbHelper.getUser(userId, new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (getContext() == null) return; // Check if fragment is attached
                    currentUserData = snapshot.getValue(User.class); // Store user data
                    if (currentUserData != null) {
                        profileName.setText(currentUserData.getName() != null ? currentUserData.getName() : "Manager");
                        profileRole.setText(currentUserData.getRole() != null ? currentUserData.getRole() : "Manager");

                        // Try to get creation date from Firebase Auth if available
                        if (currentUser.getMetadata() != null) {
                            long creationTimestamp = currentUser.getMetadata().getCreationTimestamp();
                            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                            profileCreatedDate.setText("Member since " + sdf.format(new Date(creationTimestamp)));
                        } else {
                            profileCreatedDate.setText("Date unavailable");
                        }
                    } else {
                        profileName.setText("Manager");
                        profileRole.setText("Manager");
                        profileCreatedDate.setText("Date unavailable");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (getContext() != null) { // Check context
                        Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                        Log.e("ProfileFragment", "Firebase DB Error: " + error.getMessage());
                    }
                }
            });

            // Load total spots count
            loadTotalSpotsCount(userId);
        } else {
            // Handle case where user is somehow null (e.g., redirect to login)
            Log.e("ProfileFragment", "Current user is null, cannot load data.");
            if (getContext() != null) Toast.makeText(getContext(), "Error: Not logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTotalSpotsCount(String userId) {
        dbHelper.getManagerSpots(userId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getContext() == null) return;
                long count = snapshot.getChildrenCount();
                profileTotalSpots.setText(String.valueOf(count));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() == null) return;
                profileTotalSpots.setText("0");
                Log.e("ProfileFragment", "Error loading spot count: " + error.getMessage());
            }
        });
    }

    private void showEditProfileDialog() {
        // Check if data is loaded and context is available
        if (getContext() == null || currentUserData == null || mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "User data not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext()); // Use requireContext()
        builder.setTitle("Edit Profile");

        // Inflate layout using requireActivity()
        View dialogView = requireActivity().getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        EditText nameInput = dialogView.findViewById(R.id.name_input);
        EditText emailInput = dialogView.findViewById(R.id.email_input);

        // Pre-fill current values
        nameInput.setText(currentUserData.getName());
        emailInput.setText(mAuth.getCurrentUser().getEmail());
        emailInput.setEnabled(false); // Email cannot be changed

        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = nameInput.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateUserProfile(newName);
            } else {
                Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateUserProfile(String newName) {
        FirebaseUser currentUserAuth = mAuth.getCurrentUser();
        // Check everything is available
        if (currentUserAuth != null && currentUserData != null && getContext() != null) {
            String userId = currentUserAuth.getUid();

            // Update the name in the locally stored User object
            currentUserData.setName(newName);

            dbHelper.updateUserProfile(userId, currentUserData, new DatabaseHelper.DatabaseCallback() {
                @Override
                public void onSuccess() {
                    // Check context again inside async callback
                    if (getContext() != null) {
                        profileName.setText(newName); // Update UI
                        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String error) {
                    if (getContext() != null) { // Check context
                        Toast.makeText(getContext(), "Error updating profile: " + error, Toast.LENGTH_SHORT).show();
                        Log.e("ProfileFragment", "Update Error: " + error);
                    }
                }
            });

        } else {
            Log.e("ProfileFragment", "Cannot update profile: User or data is null.");
            if (getContext() != null) Toast.makeText(getContext(), "Error updating profile.", Toast.LENGTH_SHORT).show();
        }
    }


    // ... (showChangePasswordDialog, validatePasswordChange, reauthenticateAndChangePassword remain the same) ...
    private void showChangePasswordDialog() {
        if (getContext() == null || mAuth.getCurrentUser() == null || mAuth.getCurrentUser().getEmail() == null) {
            Toast.makeText(getContext(), "Cannot change password now.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change Password");

        // Inflate layout using requireActivity()
        View dialogView = requireActivity().getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText currentPasswordInput = dialogView.findViewById(R.id.current_password_input);
        EditText newPasswordInput = dialogView.findViewById(R.id.new_password_input);
        EditText confirmPasswordInput = dialogView.findViewById(R.id.confirm_password_input);

        builder.setView(dialogView);

        // Set null listener initially to prevent closing on error
        builder.setPositiveButton("Change", null);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String currentPassword = currentPasswordInput.getText().toString();
                String newPassword = newPasswordInput.getText().toString();
                String confirmPassword = confirmPasswordInput.getText().toString();

                if (validatePasswordChange(currentPassword, newPassword, confirmPassword)) {
                    // Call the re-authentication and change method
                    reauthenticateAndChangePassword(currentPassword, newPassword, dialog);
                }
            });
        });

        dialog.show();
    }

    private boolean validatePasswordChange(String currentPassword, String newPassword, String confirmPassword) {
        if (getContext() == null) return false; // Need context for Toasts

        if (TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(getContext(), "Current password is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(getContext(), "New password is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (newPassword.length() < 6) {
            Toast.makeText(getContext(), "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getContext(), "New passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void reauthenticateAndChangePassword(String currentPassword, String newPassword, AlertDialog dialog) {
        FirebaseUser user = mAuth.getCurrentUser();
        // Check context and user again
        if (user == null || user.getEmail() == null || getContext() == null) {
            Toast.makeText(getContext(), "Error: User session invalid.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Verifying current password...", Toast.LENGTH_SHORT).show();

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    // Check context again in callback
                    if (getContext() == null) return;

                    if (reauthTask.isSuccessful()) {
                        // Re-authentication successful, now change the password
                        user.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (getContext() == null) return; // Check context again

                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(getContext(), "Password changed successfully", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss(); // Close dialog on success
                                    } else {
                                        String errorMsg = updateTask.getException() != null ? updateTask.getException().getMessage() : "Unknown error";
                                        Toast.makeText(getContext(), "Error changing password: " + errorMsg, Toast.LENGTH_LONG).show();
                                        Log.e("ProfileFragment", "Password Update Failed: " + errorMsg);
                                    }
                                });
                    } else {
                        // Re-authentication failed
                        String errorMsg = reauthTask.getException() != null ? reauthTask.getException().getMessage() : "Incorrect password";
                        Toast.makeText(getContext(), "Authentication failed: " + errorMsg, Toast.LENGTH_LONG).show();
                        Log.e("ProfileFragment", "Re-authentication Failed: " + errorMsg);
                    }
                });
    }

} // End Fragment