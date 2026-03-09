package com.example.hersafe.data.remote.models;

import com.google.gson.annotations.SerializedName;

public class Alert {
    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("created_at")
    private String createdAt;

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
