package com.example.sp_v1.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioGroup; // Import RadioGroup
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.sp_v1.R;
import com.example.sp_v1.models.User;
import com.example.sp_v1.ui.MainActivity;
import com.example.sp_v1.ui.ManagerDashboardActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.radiobutton.MaterialRadioButton; // Import MaterialRadioButton
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    // --- UPDATED VIEW IDs ---
    private TextInputEditText nameInput, emailInput, passwordInput, confirmPasswordInput;
    private RadioGroup roleRadioGroup; // Changed from AutoCompleteTextView to RadioGroup
    private MaterialButton registerButton;
    private TextView goToLoginText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        // --- UPDATED findViewById CALLS ---
        nameInput = findViewById(R.id.name_edit_text);
        emailInput = findViewById(R.id.email_edit_text);
        passwordInput = findViewById(R.id.password_edit_text);
        confirmPasswordInput = findViewById(R.id.confirm_password_edit_text);
        // Changed to find RadioGroup instead of AutoCompleteTextView
        roleRadioGroup = findViewById(R.id.role_radio_group);
        registerButton = findViewById(R.id.register_button);
        goToLoginText = findViewById(R.id.go_to_login_text);

        // REMOVED: ArrayAdapter setup is no longer needed for radio buttons

        registerButton.setOnClickListener(v -> registerUser());

        goToLoginText.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    private String getSelectedRole() {
        int selectedId = roleRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radio_driver) {
            return "Driver";
        } else if (selectedId == R.id.radio_manager) {
            return "Manager";
        }
        return ""; // Should not happen as one is checked by default
    }

    private void registerUser() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String role = getSelectedRole(); // Get selected role from radio group

        // --- ADDED VALIDATION LOGIC ---
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || role.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            return;
        }
        // --- END OF VALIDATION ---

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            User user = new User(name, email, role);
                            FirebaseDatabase.getInstance().getReference("users")
                                    .child(firebaseUser.getUid())
                                    .setValue(user)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            redirectToDashboard(role);
                                        } else {
                                            Toast.makeText(this, "Database error: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void redirectToDashboard(String role) {
        Intent intent;
        if ("Driver".equals(role)) {
            intent = new Intent(this, MainActivity.class);
        } else if ("Manager".equals(role)) {
            intent = new Intent(this, ManagerDashboardActivity.class);
        } else {
            // Default to Driver dashboard
            intent = new Intent(this, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
