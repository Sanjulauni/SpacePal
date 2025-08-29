package com.example.sp_v1.models;

public class Reservation {
    private String id;
    private String userId;
    private String spotId;
    private long startTime;
    private long endTime;
    private String status; // "active", "in-progress", "completed", "expired"

    // Required empty constructor for Firebase
    public Reservation() {}

    public Reservation(String id, String userId, String spotId, long startTime, long endTime, String status) {
        this.id = id;
        this.userId = userId;
        this.spotId = spotId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSpotId() { return spotId; }
    public void setSpotId(String spotId) { this.spotId = spotId; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}