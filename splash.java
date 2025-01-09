package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.TimeUnit;

public class splash extends AppCompatActivity {

    private static final String PREF_FIRST_TIME = "isFirstTime";
    private static final String PREF_LAST_ACTIVE = "lastActiveTime";
    private static final long FIFTEEN_DAYS_MS = TimeUnit.DAYS.toMillis(15);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize buttons
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnSignUp = findViewById(R.id.btnSignUp);

        if (isInactiveFor15Days()) {
            // Logout the user if inactive for 15 days
            FirebaseAuth.getInstance().signOut();
        }

        // Set click listeners for login and sign-up buttons
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(splash.this, login.class);
            startActivity(intent);
            finish();
        });

        btnSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(splash.this, signup.class);
            startActivity(intent);
            finish();
        });
    }

    private boolean isInactiveFor15Days() {
        // Get the last active time from SharedPreferences
        long lastActiveTime = PreferenceManager.getDefaultSharedPreferences(this)
                .getLong(PREF_LAST_ACTIVE, 0);

        long currentTime = System.currentTimeMillis();

        // Check if 15 days have passed since the last active time
        if ((currentTime - lastActiveTime) > FIFTEEN_DAYS_MS) {
            return true;
        } else {
            // Update the last active time to the current time
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putLong(PREF_LAST_ACTIVE, currentTime)
                    .apply();
            return false;
        }
    }
}
