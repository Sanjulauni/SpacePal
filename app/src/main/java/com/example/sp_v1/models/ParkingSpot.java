package com.example.sp_v1.models;

public class ParkingSpot {
    private String id;
    private String name;
    private double latitude;
    private double longitude;
    private int availableSpaces;
    private int totalSlots;
    private boolean available;
    private int occupiedSlots;
    private String managerId;

    // A no-argument constructor is required by Firebase
    public ParkingSpot() {}

    // NEW: The constructor your code needs to create a new spot
    public ParkingSpot(String name, double latitude, double longitude, boolean available, String managerId, int totalSlots, int availableSpaces) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.available = available;
        this.managerId = managerId;
        this.totalSlots = totalSlots;
        this.availableSpaces = availableSpaces;
        // Occupied can be calculated or set as needed
        this.occupiedSlots = totalSlots - availableSpaces;
    }


    // Getters and Setters for all fields

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
}