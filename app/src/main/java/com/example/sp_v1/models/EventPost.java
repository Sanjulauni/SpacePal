package com.example.sp_v1.models;

public class EventPost {

    // Constants for the event's verification status
    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private String eventId;           // Firebase push key
    private String managerId;         // The organization's User ID
    private String eventName;         // e.g., "BMICH Book Fair 2025"
    private String description;
    private String bannerImageUrl;    // URL from Firebase Storage
    private String status;            // Use one of the constants above
    private long startDate;           // Event start date as a timestamp
    private long endDate;             // Event end date as a timestamp

    // Required no-argument constructor for Firebase
    public EventPost() {
    }

    // Constructor for creating a new post
    public EventPost(String managerId, String eventName, String description, String bannerImageUrl, long startDate, long endDate) {
        this.managerId = managerId;
        this.eventName = eventName;
        this.description = description;
        this.bannerImageUrl = bannerImageUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = STATUS_PENDING_APPROVAL; // Always starts as pending
    }

    // --- Getters and Setters ---

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBannerImageUrl() {
        return bannerImageUrl;
    }

    public void setBannerImageUrl(String bannerImageUrl) {
        this.bannerImageUrl = bannerImageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }
}