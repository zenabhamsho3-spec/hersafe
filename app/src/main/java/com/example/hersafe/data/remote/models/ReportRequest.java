package com.example.hersafe.data.remote.models;

import com.google.gson.annotations.SerializedName;

public class ReportRequest {
    @SerializedName("phone")
    private String phone;

    @SerializedName("lat")
    private double latitude;

    @SerializedName("lng")
    private double longitude;

    @SerializedName("video_id")
    private String videoId;

    public ReportRequest(String phone, double latitude, double longitude, String videoId) {
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.videoId = videoId;
    }

    public String getPhone() { return phone; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getVideoId() { return videoId; }
}
