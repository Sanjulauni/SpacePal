package com.example.sp_v1.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.sp_v1.R;
import com.example.sp_v1.models.User;
import com.example.sp_v1.utils.DatabaseHelper;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class AccountFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;
    private FirebaseUser currentUser;
    private User currentUserData;

    // UI Elements
    private CircleImageView profileImage;
    private TextView userNameText, userEmailText;
    private Button btnEditProfile, btnChangePassword, btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Should not happen if MainActivity checks this, but good practice
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logoutUser(); // Use existing logout
            }
            return;
        }

        // Initialize Views
        profileImage = view.findViewById(R.id.profile_image_account);
        userNameText = view.findViewById(R.id.user_name_account);
        userEmailText = view.findViewById(R.id.user_email_account);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnChangePassword = view.findViewById(R.id.btn_change_password);
        btnLogout = view.findViewById(R.id.btn_logout);

        loadUserProfile();

        // Set Click Listeners
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logoutUser(); // Call the logout method in MainActivity
            }
        });
    }

    private void loadUserProfile() {
        if (currentUser == null || getContext() == null) return;

        // Set email immediately (from Auth)
        userEmailText.setText(currentUser.getEmail());

        // Fetch user details (like name) from Database
        dbHelper.getUser(currentUser.getUid(), new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getContext() == null) return; // Check if fragment is still attached

                currentUserData = snapshot.getValue(User.class);
                if (currentUserData != null) {
                    userNameText.setText(currentUserData.getName());
                    // TODO: Load profile image using Glide/Picasso if you add image URLs later
                    // Glide.with(AccountFragment.this).load(currentUserData.getProfileImageUrl()).into(profileImage);
                } else {
                    // Handle case where user data might be missing in DB
                    userNameText.setText("Name not set");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load profile details.", Toast.LENGTH_SHORT).show();
                }
                Log.e("AccountFragment", "Firebase DB Error: " + error.getMessage());
            }
        });
    }

    private void showEditProfileDialog() {
        if (getContext() == null || currentUserData == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_profile, null);

        final EditText nameInput = dialogView.findViewById(R.id.name_input);
        final EditText emailInput = dialogView.findViewById(R.id.email_input); // Keep this for display/potential update

        // Pre-fill with current data
        nameInput.setText(currentUserData.getName());
        emailInput.setText(currentUser.getEmail());
        emailInput.setEnabled(false); // Disallow email editing for now for simplicity

        builder.setView(dialogView)
                .setTitle("Edit Profile")
                .setPositiveButton("Save", (dialog, id) -> {
                    String newName = nameInput.getText().toString().trim();
                    // Basic validation
                    if (TextUtils.isEmpty(newName)) {
                        Toast.makeText(getContext(), "Name cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateUserProfile(newName);
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());
        builder.create().show();
    }

    private void updateUserProfile(String newName) {
        if (currentUser == null || currentUserData == null || getContext() == null) return;

        // Update name in User object
        currentUserData.setName(newName);

        // Save updated User object back to Firebase Database
        dbHelper.updateUserProfile(currentUser.getUid(), currentUserData, new DatabaseHelper.DatabaseCallback() {
            @Override
            public void onSuccess() {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Profile updated successfully.", Toast.LENGTH_SHORT).show();
                    userNameText.setText(newName); // Update UI immediately
                }
            }

            @Override
            public void onError(String error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Profile update failed: " + error, Toast.LENGTH_LONG).show();
                }
                // Optionally revert UI change or reload data
            }
        });
    }


    private void showChangePasswordDialog() {
        if (getContext() == null || currentUser == null || currentUser.getEmail() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);

        final EditText currentPasswordInput = dialogView.findViewById(R.id.current_password_input);
        final EditText newPasswordInput = dialogView.findViewById(R.id.new_password_input);
        final EditText confirmPasswordInput = dialogView.findViewById(R.id.confirm_password_input);

        builder.setView(dialogView)
                .setTitle("Change Password")
                .setPositiveButton("Change", null) // Set null listener initially
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String currentPassword = currentPasswordInput.getText().toString();
                String newPassword = newPasswordInput.getText().toString();
                String confirmPassword = confirmPasswordInput.getText().toString();

                if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
                    Toast.makeText(getContext(), "All fields are required.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newPassword.length() < 6) {
                    Toast.makeText(getContext(), "New password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newPassword.equals(confirmPassword)) {
                    Toast.makeText(getContext(), "New passwords do not match.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Proceed with Firebase password change
                reauthenticateAndChangePassword(currentPassword, newPassword, dialog);
            });
        });

        dialog.show();
    }

    private void reauthenticateAndChangePassword(String currentPassword, String newPassword, AlertDialog dialog) {
        if (currentUser == null || currentUser.getEmail() == null || getContext() == null) return;

        // Show progress indicator if you have one
        Toast.makeText(getContext(), "Verifying current password...", Toast.LENGTH_SHORT).show();

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);

        currentUser.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        // Re-authentication successful, now change the password
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (getContext() == null) return; // Check context again
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(getContext(), "Password updated successfully.", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss(); // Close the dialog on success
                                    } else {
                                        Toast.makeText(getContext(), "Failed to update password: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        // Re-authentication failed
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Authentication failed: Incorrect current password.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}