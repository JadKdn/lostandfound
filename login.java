package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class login extends AppCompatActivity {

    private FirebaseAuth auth;
    TextView tvSignUp, tvForgotPassword;
    MaterialButton btnLogin;
    private TextInputEditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Initialize UI Elements
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Login Button Click Listener
        btnLogin.setOnClickListener(v -> loginUser());

        // Sign Up Text Click Listener
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(login.this, signup.class);
            startActivity(intent);
        });

        // Forgot Password Click Listener
        tvForgotPassword.setOnClickListener(v -> forgotPassword());
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showPopup("Error", "Please enter both email and password");
            return;
        }

        // Authenticate with Firebase
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showPopup("Success", "Login successful!", () -> {
                            // Redirect to Main Activity
                            Intent intent = new Intent(login.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        showPopup("Error", "Login failed: " + task.getException().getMessage());
                    }
                });
    }

    private void forgotPassword() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            showPopup("Error", "Please enter your email to reset your password");
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showPopup("Success", "Password reset email sent to " + email);
                    } else {
                        showPopup("Error", "Failed to send password reset email: " + task.getException().getMessage());
                    }
                });
    }

    // Method to Show Popup Message
    private void showPopup(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // Overloaded Method to Show Popup with Action
    private void showPopup(String title, String message, Runnable onPositiveAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> onPositiveAction.run());
        builder.show();
    }
}
