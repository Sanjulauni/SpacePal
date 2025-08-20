package com.example.sp_v1.adapters;

import java.io.Serializable;

public class Reservation implements Serializable {
    private String userId;
    private String spotId;
    private String startTime;
    private String endTime;
    private String status;

    public Reservation() {
        // Default constructor required for Firebase
    }

    public Reservation(String userId, String spotId, String startTime, String endTime, String status) {
        this.userId = userId;
        this.spotId = spotId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(String spotId) {
        this.spotId = spotId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}