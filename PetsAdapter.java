package com.example.lostandfound;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.util.List;

public class PetsAdapter extends RecyclerView.Adapter<PetsAdapter.PetsViewHolder> {

    private final Context context;
    private final List<Pet> petList;
    private final boolean isLostSection;

    public PetsAdapter(Context context, List<Pet> petList, boolean isLostSection) {
        this.context = context;
        this.petList = petList;
        this.isLostSection = isLostSection;
    }

    @NonNull
    @Override
    public PetsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pet, parent, false);
        return new PetsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetsViewHolder holder, int position) {
        Pet pet = petList.get(position);

        // Bind pet data to the UI components
        holder.tvPetName.setText(pet.getPetName());
        holder.tvLastSeen.setText("Last seen: " + pet.getLastSeen());
        holder.tvReward.setText(pet.getReward() != null && !pet.getReward().isEmpty() ? "Reward: " + pet.getReward() : "No reward");

        // Load image using Picasso
        if (pet.getImageUrl() != null && !pet.getImageUrl().isEmpty()) {
            Picasso.get().load(pet.getImageUrl()).fit().centerInside().into(holder.ivPetImage);
        } else {
            holder.ivPetImage.setImageResource(android.R.drawable.ic_menu_gallery); // Fallback image
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PetDetailsActivity.class);
            intent.putExtra("petId", pet.getPetId());
            intent.putExtra("isLostSection", isLostSection); // Pass whether it's from Lost or Found
            context.startActivity(intent);
        });

        // Display "Mark as Found" button only for owners in the Lost section
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (isLostSection && pet.getOwnerId() != null && pet.getOwnerId().equals(currentUserId)) {
            holder.btnMarkAsFound.setVisibility(View.VISIBLE);
            holder.btnMarkAsFound.setOnClickListener(v -> markPetAsFound(pet, holder.getAdapterPosition()));
        } else {
            holder.btnMarkAsFound.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    private void markPetAsFound(Pet pet, int position) {
        DatabaseReference foundPetsRef = FirebaseDatabase.getInstance().getReference("FoundPets");
        DatabaseReference lostPetsRef = FirebaseDatabase.getInstance().getReference("LostPets");

        foundPetsRef.child(pet.getPetId()).setValue(pet).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Remove pet from the LostPets node
                lostPetsRef.child(pet.getPetId()).removeValue();

                // Update local list
                petList.remove(position);
                notifyItemRemoved(position);

                Toast.makeText(context, "Pet marked as found successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to mark pet as found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    public static class PetsViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPetImage;
        TextView tvPetName, tvLastSeen, tvReward;
        Button btnMarkAsFound;

        public PetsViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPetImage = itemView.findViewById(R.id.ivPetImage);
            tvPetName = itemView.findViewById(R.id.tvPetName);
            tvLastSeen = itemView.findViewById(R.id.tvLastSeen);
            tvReward = itemView.findViewById(R.id.tvReward);
            btnMarkAsFound = itemView.findViewById(R.id.btnMarkAsFound);
        }
    }
}
