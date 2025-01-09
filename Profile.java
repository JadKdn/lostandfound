package com.example.lostandfound;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.squareup.picasso.Picasso;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Profile extends AppCompatActivity {

    private TextView tvFullName, tvUsername, tvEmail, tvPhoneNumber, tvAddress;
    private Button btnEditProfile, btnRemovePicture, btnLogout;
    private ImageView ivProfilePicture, ivEditPicture;
    private RecyclerView rvUserPets;

    private FirebaseAuth auth;
    private DatabaseReference database, petsDatabase;
    private StorageReference storageReference;

    private Uri selectedImageUri;
    private List<Pet> userPetsList;
    private PetsAdapter petsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Users");
        petsDatabase = FirebaseDatabase.getInstance().getReference("LostPets");
        storageReference = FirebaseStorage.getInstance().getReference("ProfilePictures");

        // Initialize UI components
        tvFullName = findViewById(R.id.tvFullName);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvAddress = findViewById(R.id.tvAddress);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnRemovePicture = findViewById(R.id.btnRemovePicture);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        ivEditPicture = findViewById(R.id.ivEditPicture);
        btnLogout = findViewById(R.id.btnLogout);

        rvUserPets = findViewById(R.id.rvUserPets);
        userPetsList = new ArrayList<>();
        petsAdapter = new PetsAdapter(this, userPetsList, true);

        rvUserPets.setLayoutManager(new LinearLayoutManager(this));
        rvUserPets.setAdapter(petsAdapter);

        // Load user profile
        loadUserProfile();

        // Load user's pets
        loadUserPets();
        setupBottomNavigationView();

        // Handle Edit Profile button
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        // Handle Profile Picture Update
        ivEditPicture.setOnClickListener(v -> selectImage());

        // Handle Remove Profile Picture
        btnRemovePicture.setOnClickListener(v -> removeProfilePicture());

        // Handle Log Out button
        btnLogout.setOnClickListener(v -> {
            // Sign out the user from Firebase
            FirebaseAuth.getInstance().signOut();

            // Redirect to login screen
            Intent intent = new Intent(Profile.this, login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupBottomNavigationView() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.homenav) {
                startActivity(new Intent(Profile.this, MainActivity.class));
                return true;
            } else if (item.getItemId() == R.id.nav_pets) {
                startActivity(new Intent(Profile.this, Pets.class));
                return true;
            } else if (item.getItemId() == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }

    private void loadUserProfile() {
        String userId = auth.getCurrentUser().getUid();
        database.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Retrieve user data
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    String username = snapshot.child("username").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String phoneNumber = snapshot.child("phoneNumber").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);
                    String profilePictureUrl = snapshot.child("profilePictureUrl").getValue(String.class);

                    // Display the data
                    tvFullName.setText("Full Name: " + (fullName != null ? fullName : "N/A"));
                    tvUsername.setText("Username: " + (username != null ? username : "N/A"));
                    tvEmail.setText("Email: " + (email != null ? email : "N/A"));
                    tvPhoneNumber.setText("Phone: " + (phoneNumber != null ? phoneNumber : "N/A"));
                    tvAddress.setText("Address: " + (address != null ? address : "N/A"));

                    // Load profile picture
                    if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                        Picasso.get().load(profilePictureUrl).into(ivProfilePicture);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Profile.this, "Failed to load profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectImage() {
        imagePickerLauncher.launch("image/*");
    }

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivProfilePicture.setImageURI(selectedImageUri); // Preview selected image
                    uploadProfilePicture();
                } else {
                    Toast.makeText(this, "Image selection canceled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void uploadProfilePicture() {
        if (selectedImageUri == null) return;

        String userId = auth.getCurrentUser().getUid();
        StorageReference profilePicRef = storageReference.child(userId + ".jpg");

        profilePicRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();

                    // Update user's profile picture URL in the database
                    database.child(userId).child("profilePictureUrl").setValue(imageUrl)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(Profile.this, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(Profile.this, "Failed to update profile picture", Toast.LENGTH_SHORT).show();
                                }
                            });
                }))
                .addOnFailureListener(e -> Toast.makeText(Profile.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeProfilePicture() {
        String userId = auth.getCurrentUser().getUid();

        // Remove profile picture from database
        database.child(userId).child("profilePictureUrl").removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ivProfilePicture.setImageResource(R.drawable.placeholder_image); // Reset to placeholder
                Toast.makeText(Profile.this, "Profile picture removed successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(Profile.this, "Failed to remove profile picture", Toast.LENGTH_SHORT).show();
            }
        });

        // Optionally, delete the profile picture from storage
        storageReference.child(userId + ".jpg").delete().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(Profile.this, "Failed to delete profile picture from storage.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserPets() {
        String userId = auth.getCurrentUser().getUid();
        petsDatabase.orderByChild("ownerId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                userPetsList.clear();
                for (DataSnapshot petSnapshot : snapshot.getChildren()) {
                    Pet pet = petSnapshot.getValue(Pet.class);
                    if (pet != null) {
                        userPetsList.add(pet);
                    }
                }
                petsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Profile.this, "Failed to load pets: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditProfileDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_profile);
        dialog.setCancelable(true);

        EditText etEditUsername = dialog.findViewById(R.id.etEditUsername);
        EditText etEditPhoneNumber = dialog.findViewById(R.id.etEditPhoneNumber);
        Button btnSaveChanges = dialog.findViewById(R.id.btnSaveChanges);

        String userId = auth.getCurrentUser().getUid();

        database.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String currentUsername = snapshot.child("username").getValue(String.class);
                    String currentPhoneNumber = snapshot.child("phoneNumber").getValue(String.class);

                    etEditUsername.setText(currentUsername);
                    etEditPhoneNumber.setText(currentPhoneNumber);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });

        btnSaveChanges.setOnClickListener(v -> {
            String newUsername = etEditUsername.getText().toString().trim();
            String newPhoneNumber = etEditPhoneNumber.getText().toString().trim();

            database.child(userId).child("username").setValue(newUsername);
            database.child(userId).child("phoneNumber").setValue(newPhoneNumber).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(Profile.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    loadUserProfile();
                    dialog.dismiss();
                } else {
                    Toast.makeText(Profile.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }
}
