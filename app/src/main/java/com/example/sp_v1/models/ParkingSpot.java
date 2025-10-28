package com.example.sp_v1.models;

public class ParkingSpot {

    // ADDED: Constants for Spot Verification Status
    public static final String STATUS_SPOT_UNVERIFIED = "UNVERIFIED";
    public static final String STATUS_SPOT_PENDING_DEED = "PENDING_DEED";
    public static final String STATUS_SPOT_TITLE_VERIFIED = "TITLE_VERIFIED";

    private String id;
    private String name;
    private double latitude;
    private double longitude;
    private int availableSpaces;
    private int totalSlots;
    private boolean available;
    private int occupiedSlots;
    private String managerId;

    // ADDED: New fields for Tier 2 Verification
    private String spotVerificationStatus;
    private String deedImageUrl;

    // --- NEW FIELD ---
    private boolean isListed; // For the visibility switch

    // A no-argument constructor is required by Firebase
    public ParkingSpot() {}

    // UPDATED: The constructor now sets a default verification status and listed status
    public ParkingSpot(String name, double latitude, double longitude, boolean available, String managerId, int totalSlots, int availableSpaces) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.available = available;
        this.managerId = managerId;
        this.totalSlots = totalSlots;
        this.availableSpaces = availableSpaces;
        this.occupiedSlots = totalSlots - availableSpaces;
        this.spotVerificationStatus = STATUS_SPOT_UNVERIFIED;
        this.isListed = true; // --- ADDED: Default to true
    }


    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getAvailableSpaces() { return availableSpaces; }
    public void setAvailableSpaces(int availableSpaces) { this.availableSpaces = availableSpaces; }

    public int getTotalSlots() { return totalSlots; }
    public void setTotalSlots(int totalSlots) { this.totalSlots = totalSlots; }

    public boolean getAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public int getOccupiedSlots() { return occupiedSlots; }
    public void setOccupiedSlots(int occupiedSlots) { this.occupiedSlots = occupiedSlots; }

    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }

    // ADDED: Getters and Setters for the new fields
    public String getSpotVerificationStatus() { return spotVerificationStatus; }
    public void setSpotVerificationStatus(String spotVerificationStatus) { this.spotVerificationStatus = spotVerificationStatus; }

    public String getDeedImageUrl() { return deedImageUrl; }
    public void setDeedImageUrl(String deedImageUrl) { this.deedImageUrl = deedImageUrl; }

    // --- NEW GETTER/SETTER ---
    public boolean isListed() { return isListed; }
    public void setListed(boolean listed) { isListed = listed; }
}