package com.example.sp_v1.models;

public class User {
    // --- Renamed Status Constants for Partner Verification ---
    public static final String STATUS_UNVERIFIED = "UNVERIFIED";
    public static final String STATUS_PENDING_ID = "PENDING_ID";
    public static final String STATUS_ID_VERIFIED = "ID_VERIFIED";

    private String name;
    private String email;
    private String role;
    private String partnerStatus; // RENAMED from verificationStatus
    private String idImageUrl; // ADDED
    private String selfieImageUrl; // ADDED

    public User() {
        // Default constructor for Firebase
    }

    public User(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
        // Default status for any new user
        if (role.equals("Manager")) { // Only managers need verification
            this.partnerStatus = STATUS_UNVERIFIED;
        } else {
            this.partnerStatus = null; // Not applicable for drivers
        }
    }

    // --- Getters and Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // RENAMED GETTER AND SETTER
    public String getPartnerStatus() { return partnerStatus; }
    public void setPartnerStatus(String partnerStatus) { this.partnerStatus = partnerStatus; }

    // ADDED GETTERS AND SETTERS
    public String getIdImageUrl() { return idImageUrl; }
    public void setIdImageUrl(String idImageUrl) { this.idImageUrl = idImageUrl; }

    public String getSelfieImageUrl() { return selfieImageUrl; }
    public void setSelfieImageUrl(String selfieImageUrl) { this.selfieImageUrl = selfieImageUrl; }
}