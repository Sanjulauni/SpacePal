package com.example.sp_v1.adapters;

public class ParkingSpot {
    private String id;
    private String name;
    private double latitude;
    private double longitude;
    private boolean available;
    private String managerId;
    private int totalSlots;
    private int occupiedSlots;

    public ParkingSpot() {
        // Default constructor required for Firebase
    }

    public ParkingSpot(String name, double latitude, double longitude, boolean available, String managerId, int totalSlots, int occupiedSlots) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.available = available;
        this.managerId = managerId;
        this.totalSlots = totalSlots;
        this.occupiedSlots = occupiedSlots;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(int totalSlots) {
        this.totalSlots = totalSlots;
    }

    public int getOccupiedSlots() {
        return occupiedSlots;
    }

    public void setOccupiedSlots(int occupiedSlots) {
        this.occupiedSlots = occupiedSlots;
    }
}