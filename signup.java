package com.example.lostandfound;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class signup extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference database;


    private TextInputEditText etFullName, etUsername, etPhoneNumber, etAddress, etEmail, etPassword, etConfirmPassword;

    private String fullName, username, phoneNumber, address, email, password, confirmPassword;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Authentication and Database
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Users");

        // Initialize UI Elements
        etFullName = findViewById(R.id.etFullName);
        etUsername = findViewById(R.id.etUsername);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etAddress = findViewById(R.id.etAddress);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        MaterialButton btnCreateAccount = findViewById(R.id.btnCreateAccount);

        // Set Click Listener for Create Account Button
        btnCreateAccount.setOnClickListener(v -> createAccount());
    }

    private void createAccount() {
       fullName = etFullName.getText().toString().trim();
       username = etUsername.getText().toString().trim();
       phoneNumber = etPhoneNumber.getText().toString().trim();
       address = etAddress.getText().toString().trim();
       email = etEmail.getText().toString().trim();
       password = etPassword.getText().toString().trim();
       confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validate Input Fields
        if (fullName.isEmpty() || username.isEmpty() || phoneNumber.isEmpty() || address.isEmpty()
                || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showPopupMessage("Error", "Please fill all fields");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showPopupMessage("Error", "Passwords do not match");
            return;
        }

        if (password.length() < 6) {
            showPopupMessage("Error", "Password must be at least 6 characters long");
            return;
        }

        // Create User with Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Save Additional User Data in Firebase Realtime Database
                        String userId = auth.getCurrentUser().getUid();
                        User user = new User(fullName, username, phoneNumber, address, email);

                        database.child(userId).setValue(user)
                                .addOnCompleteListener(databaseTask -> {
                                    if (databaseTask.isSuccessful()) {
                                        showPopupMessage("Success", "Account created successfully!", () -> {
                                            // Redirect to Main Activity
                                            Intent intent = new Intent(signup.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        });
                                    } else {
                                        showPopupMessage("Error", "Failed to save user data: " + databaseTask.getException().getMessage());
                                    }
                                });
                    } else {
                        showPopupMessage("Error", "Sign-Up failed: " + task.getException().getMessage());
                    }
                });
    }

    // Method to Show Popup Message
    private void showPopupMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    // Overloaded Method to Show Popup Message with Action
    private void showPopupMessage(String title, String message, Runnable onPositiveButtonClick) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialogInterface, i) -> onPositiveButtonClick.run());
        builder.show();
    }

    // User class to store user data in the Firebase Realtime Database
    public static class User {
        public String fullName, username, phoneNumber, address, email;

        public User(String fullName, String username, String phoneNumber, String address, String email) {
            this.fullName = fullName;
            this.username = username;
            this.phoneNumber = phoneNumber;
            this.address = address;
            this.email = email;
        }
    }
}
