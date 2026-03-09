package com.example.hersafe.data.remote.models;

import com.google.gson.annotations.SerializedName;

public class UserResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("user")
    private RemoteUser user;

    @SerializedName("error")
    private String error;

    @SerializedName("api_token")
    private String apiToken;

    public boolean isSuccess() {
        return success;
    }

    public RemoteUser getUser() {
        return user;
    }

    public String getError() {
        return error;
    }

    public String getApiToken() {
        return apiToken;
    }

    public static class RemoteUser {
        @SerializedName("id")
        private int id;
        
        @SerializedName("name")
        private String name;
        
        @SerializedName("email")
        private String email;
        
        @SerializedName("phone")
        private String phone;
        
        @SerializedName("api_token")
        private String apiToken;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getApiToken() { return apiToken; }
    }
}
