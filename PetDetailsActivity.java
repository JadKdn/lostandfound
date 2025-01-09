package com.example.lostandfound;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class PetDetailsActivity extends AppCompatActivity {

    private ImageView ivPetImage;
    private TextView tvPetName, tvLastSeen, tvReward, tvOwnerName, tvOwnerEmail, tvOwnerPhone;
    private Button btnCallOwner, btnEmailOwner, btnMessageOwner, btnMarkAsFound;
    private FirebaseAuth auth;
    private DatabaseReference lostPetsRef, foundPetsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_details);

        // Initialize Firebase references
        auth = FirebaseAuth.getInstance();
        lostPetsRef = FirebaseDatabase.getInstance().getReference("LostPets");
        foundPetsRef = FirebaseDatabase.getInstance().getReference("FoundPets");

        // Initialize UI elements
        ivPetImage = findViewById(R.id.ivPetImage);
        tvPetName = findViewById(R.id.tvPetName);
        tvLastSeen = findViewById(R.id.tvLastSeen);
        tvReward = findViewById(R.id.tvReward);
        tvOwnerName = findViewById(R.id.tvOwnerName);
        tvOwnerEmail = findViewById(R.id.tvOwnerEmail);
        tvOwnerPhone = findViewById(R.id.tvOwnerPhone);
        btnCallOwner = findViewById(R.id.btnCallOwner);
        btnEmailOwner = findViewById(R.id.btnEmailOwner);
        btnMessageOwner = findViewById(R.id.btnMessageOwner);
        btnMarkAsFound = findViewById(R.id.btnMarkAsFound);

        // Get pet details from intent
        String petId = getIntent().getStringExtra("petId");
        boolean isLostSection = getIntent().getBooleanExtra("isLostSection", true);

        if (petId == null || petId.isEmpty()) {
            Toast.makeText(this, "Invalid pet ID.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPetDetails(petId, isLostSection);
    }

    private void loadPetDetails(String petId, boolean isLostSection) {
        DatabaseReference petsRef = isLostSection ? lostPetsRef : foundPetsRef;

        petsRef.child(petId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Pet pet = snapshot.getValue(Pet.class);
                if (pet != null) {
                    populatePetDetails(pet);

                    // Show "Mark as Found" button only for the owner and if it's in the LostPets section
                    if (isLostSection) {
                        String currentUserId = auth.getCurrentUser().getUid();
                        if (pet.getOwnerId().equals(currentUserId)) {
                            btnMarkAsFound.setVisibility(View.VISIBLE);
                            btnMarkAsFound.setOnClickListener(v -> markAsFound(pet, petId));
                        } else {
                            btnMarkAsFound.setVisibility(View.GONE);
                        }
                    } else {
                        btnMarkAsFound.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(PetDetailsActivity.this, "Pet not found.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PetDetailsActivity.this, "Failed to load pet details.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void populatePetDetails(Pet pet) {
        tvPetName.setText(pet.getPetName());
        tvLastSeen.setText("Last Seen: " + pet.getLastSeen());
        tvReward.setText(pet.getReward() != null && !pet.getReward().isEmpty() ? "Reward: " + pet.getReward() : "No Reward");
        tvOwnerName.setText("Owner: " + pet.getOwnerId()); // Change this if you need owner's name explicitly
        tvOwnerEmail.setText("Email: Loading...");
        tvOwnerPhone.setText("Phone: Loading...");

        if (pet.getImageUrl() != null && !pet.getImageUrl().isEmpty()) {
            Picasso.get().load(pet.getImageUrl()).into(ivPetImage);
        }

        // Fetch owner details
        fetchOwnerDetails(pet.getOwnerId());
    }

    private void fetchOwnerDetails(String ownerId) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users").child(ownerId);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String phone = snapshot.child("phoneNumber").getValue(String.class);

                    tvOwnerName.setText("Owner: " + (fullName != null ? fullName : "Unknown"));
                    tvOwnerEmail.setText("Email: " + (email != null ? email : "Unknown"));
                    tvOwnerPhone.setText("Phone: " + (phone != null ? phone : "Unknown"));

                    setContactActions(phone, email);
                } else {
                    tvOwnerName.setText("Owner: Unknown");
                    tvOwnerEmail.setText("Email: Unknown");
                    tvOwnerPhone.setText("Phone: Unknown");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PetDetailsActivity.this, "Failed to load owner details.", Toast.LENGTH_SHORT).show();
                Log.e("PetDetailsActivity", "Error fetching owner details: " + error.getMessage());
            }
        });
    }

    private void setContactActions(String phone, String email) {
        if (phone != null && !phone.isEmpty()) {
            btnCallOwner.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))));
            btnMessageOwner.setOnClickListener(v -> {
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phone));
                smsIntent.putExtra("sms_body", "Hi, I saw your lost pet post.");
                startActivity(smsIntent);
            });
        } else {
            btnCallOwner.setEnabled(false);
            btnMessageOwner.setEnabled(false);
        }

        if (email != null && !email.isEmpty()) {
            btnEmailOwner.setOnClickListener(v -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Regarding your lost pet");
                startActivity(emailIntent);
            });
        } else {
            btnEmailOwner.setEnabled(false);
        }
    }

    private void markAsFound(Pet pet, String petId) {
        foundPetsRef.child(petId).setValue(pet).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                lostPetsRef.child(petId).removeValue();
                Toast.makeText(PetDetailsActivity.this, "Pet marked as found!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(PetDetailsActivity.this, "Failed to mark pet as found.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
