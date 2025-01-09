package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcomeMessage;
    private FirebaseAuth auth;
    private DatabaseReference database;

    private RecyclerView rvFeaturedPets;
    private List<Pet> featuredPetsList;
    private PetsAdapter featuredPetsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase references
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Users");

        // Initialize UI components
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);
        rvFeaturedPets = findViewById(R.id.rvFeaturedPets);
        setWelcomeMessage();
        setupFeaturedPets();

        // Retrieve the FCM token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d("FCM Token", "Token: " + token);
                    } else {
                        Log.e("FCM Token", "Failed to get token", task.getException());
                    }
                });

        // Set up Lost Pets Card
        CardView cardLostPets = findViewById(R.id.cardLostPets);
        cardLostPets.setOnClickListener(v -> openActivity(Pets.class));

        // Set up BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.homenav);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.homenav) {
                return true; // Already on Home
            } else if (item.getItemId() == R.id.nav_pets) {
                startActivity(new Intent(MainActivity.this, Pets.class));
                return true;
            } else if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(MainActivity.this, Profile.class));
                return true;
            }
            return false;
        });
    }

    private void setWelcomeMessage() {
        // Get the current user's ID
        String userId = auth.getCurrentUser().getUid();

        // Fetch the user's data from the database
        database.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    if (username != null && !username.isEmpty()) {
                        tvWelcomeMessage.setText("Welcome, " + username + "!");
                    } else {
                        tvWelcomeMessage.setText("Welcome!");
                    }
                } else {
                    tvWelcomeMessage.setText("Welcome!");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvWelcomeMessage.setText("Welcome!");
            }
        });
    }

    private void setupFeaturedPets() {
        // Initialize the RecyclerView for Featured Pets
        featuredPetsList = new ArrayList<>();
        // Pass the third boolean parameter (e.g., true for lost pets)
        featuredPetsAdapter = new PetsAdapter(this, featuredPetsList, true);
        rvFeaturedPets.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFeaturedPets.setAdapter(featuredPetsAdapter);

        // Fetch the latest pets from Firebase
        DatabaseReference petsRef = FirebaseDatabase.getInstance().getReference("LostPets");
        petsRef.orderByKey().limitToLast(5).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                featuredPetsList.clear();
                for (DataSnapshot petSnapshot : snapshot.getChildren()) {
                    Pet pet = petSnapshot.getValue(Pet.class);
                    if (pet != null) {
                        featuredPetsList.add(0, pet); // Add to the start to display the newest first
                    }
                }
                featuredPetsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("MainActivity", "Failed to fetch featured pets: " + error.getMessage());
            }
        });
    }


    private void openActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
    }
}
