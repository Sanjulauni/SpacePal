package com.example.sp_v1.models;

public class ParkingSpot {
    private String name;
    private double latitude;
    private double longitude;
    private boolean available; // Changed from isAvailable
    private String managerId;
    private int capacity;
    private int availableSpots;

    public ParkingSpot() {} // Default constructor for Firebase

    public ParkingSpot(String name, double latitude, double longitude, boolean available, String managerId, int capacity, int availableSpots) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.available = available; // Changed from isAvailable
        this.managerId = managerId;
        this.capacity = capacity;
        this.availableSpots = availableSpots;
    }

    // Getters and setters
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean isAvailable() { return available; } // Getter
    public String getManagerId() { return managerId; }
    public int getCapacity() { return capacity; }
    public int getAvailableSpots() { return availableSpots; }

    public void setName(String name) { this.name = name; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setAvailable(boolean available) { this.available = available; } // Setter
    public void setManagerId(String managerId) { this.managerId = managerId; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setAvailableSpots(int availableSpots) { this.availableSpots = availableSpots; }
}