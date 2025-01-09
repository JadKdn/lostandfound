// Pets.java
package com.example.lostandfound;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class Pets extends AppCompatActivity {

    private RecyclerView rvPets;
    private Button btnAddPet, btnViewLostPets, btnViewFoundPets;
    private DatabaseReference lostPetsRef, foundPetsRef;
    private StorageReference storageReference;
    private FirebaseAuth auth;
    private List<Pet> petList;
    private PetsAdapter petsAdapter;
    private Uri petImageUri;

    private boolean isLostSection = true; // Flag to determine which section is being viewed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pets);

        // Initialize Firebase references
        lostPetsRef = FirebaseDatabase.getInstance().getReference("LostPets");
        foundPetsRef = FirebaseDatabase.getInstance().getReference("FoundPets");
        storageReference = FirebaseStorage.getInstance().getReference("PetImages");
        auth = FirebaseAuth.getInstance();

        // Initialize UI components
        rvPets = findViewById(R.id.rvPets);
        btnAddPet = findViewById(R.id.btnAddPet);
        btnViewLostPets = findViewById(R.id.btnViewLostPets);
        btnViewFoundPets = findViewById(R.id.btnViewFoundPets);

        // Set up RecyclerView with adapter
        petList = new ArrayList<>();
        updateRecyclerViewAdapter();

        rvPets.setLayoutManager(new LinearLayoutManager(this));
        rvPets.setAdapter(petsAdapter);

        // Load lost pets by default
        loadLostPets();

        // Set click listener for adding a new pet
        btnAddPet.setOnClickListener(v -> openAddPetDialog());

        // Set click listeners for Lost and Found Pets
        btnViewLostPets.setOnClickListener(v -> {
            isLostSection = true;
            updateRecyclerViewAdapter();
            loadLostPets();
        });

        btnViewFoundPets.setOnClickListener(v -> {
            isLostSection = false;
            updateRecyclerViewAdapter();
            loadFoundPets();
        });

        // Set up BottomNavigationView
        setupBottomNavigationView();
    }

    private void updateRecyclerViewAdapter() {
        petsAdapter = new PetsAdapter(this, petList, isLostSection);
        rvPets.setAdapter(petsAdapter);
    }

    private void loadLostPets() {
        lostPetsRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                petList.clear();
                for (DataSnapshot petSnapshot : snapshot.getChildren()) {
                    Pet pet = petSnapshot.getValue(Pet.class);
                    if (pet != null) {
                        petList.add(pet);
                    }
                }
                petsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Pets.this, "Failed to load lost pets: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFoundPets() {
        foundPetsRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                petList.clear();
                for (DataSnapshot petSnapshot : snapshot.getChildren()) {
                    Pet pet = petSnapshot.getValue(Pet.class);
                    if (pet != null) {
                        petList.add(pet);
                    }
                }
                petsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Pets.this, "Failed to load found pets: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openAddPetDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_pet);
        dialog.setCancelable(true);

        // Initialize Dialog UI components
        EditText etPetName = dialog.findViewById(R.id.etPetName);
        EditText etPetType = dialog.findViewById(R.id.etPetType);
        EditText etLastSeen = dialog.findViewById(R.id.etLastSeen);
        EditText etReward = dialog.findViewById(R.id.etReward);
        Button btnUploadImage = dialog.findViewById(R.id.btnUploadImage);
        Button btnSubmitPet = dialog.findViewById(R.id.btnSubmitPet);

        btnUploadImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnSubmitPet.setOnClickListener(v -> {
            String petName = etPetName.getText().toString().trim();
            String petType = etPetType.getText().toString().trim();
            String lastSeen = etLastSeen.getText().toString().trim();
            String reward = etReward.getText().toString().trim();

            if (petName.isEmpty() || petType.isEmpty() || lastSeen.isEmpty() || petImageUri == null) {
                Toast.makeText(Pets.this, "Please fill all fields and upload an image", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadPetImage(dialog, petName, petType, lastSeen, reward);
        });

        dialog.show();
    }

    private void uploadPetImage(Dialog dialog, String petName, String petType, String lastSeen, String reward) {
        StorageReference imageRef = storageReference.child(System.currentTimeMillis() + ".jpeg");

        imageRef.putFile(petImageUri).addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            String imageUrl = uri.toString();
            savePetDetailsToDatabase(petName, petType, lastSeen, reward, imageUrl, dialog);
        }).addOnFailureListener(e -> Toast.makeText(Pets.this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
    }

    private void savePetDetailsToDatabase(String petName, String petType, String lastSeen, String reward, String imageUrl, Dialog dialog) {
        String petId = lostPetsRef.push().getKey();
        String ownerId = auth.getCurrentUser().getUid();

        if (petId == null) {
            Toast.makeText(this, "Failed to generate unique pet ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Pet pet = new Pet(petId, petName, petType, lastSeen, reward, imageUrl, ownerId);

        lostPetsRef.child(petId).setValue(pet).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(Pets.this, "Pet added successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(Pets.this, "Failed to save pet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNavigationView() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_pets);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.homenav) {
                startActivity(new Intent(Pets.this, MainActivity.class));
                return true;
            } else if (item.getItemId() == R.id.nav_pets) {
                return true; // Already in Pets activity
            } else if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(Pets.this, Profile.class));
                return true;
            }
            return false;
        });
    }

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri != null) {
            petImageUri = uri;
            Toast.makeText(this, "Image selected successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Image selection failed!", Toast.LENGTH_SHORT).show();
        }
    });
}
