package com.example.lostandfound;

public class Pet {
    private String petId;
    private String petName;
    private String petType;
    private String lastSeen;
    private String reward;
    private String imageUrl;
    private String ownerId;

    // Empty constructor for Firebase
    public Pet() {}

    // Constructor for the parameters used in your code
    public Pet(String petId, String petName, String petType, String lastSeen, String reward, String imageUrl, String ownerId) {
        this.petId = petId;
        this.petName = petName;
        this.petType = petType;
        this.lastSeen = lastSeen;
        this.reward = reward;
        this.imageUrl = imageUrl;
        this.ownerId = ownerId;
    }

    // Getters and Setters
    public String getPetId() {
        return petId;
    }

    public void setPetId(String petId) {
        this.petId = petId;
    }

    public String getPetName() {
        return petName;
    }

    public void setPetName(String petName) {
        this.petName = petName;
    }

    public String getPetType() {
        return petType;
    }

    public void setPetType(String petType) {
        this.petType = petType;
    }

    public String getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(String lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getReward() {
        return reward;
    }

    public void setReward(String reward) {
        this.reward = reward;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
}
