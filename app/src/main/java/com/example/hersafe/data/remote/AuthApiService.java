package com.example.hersafe.data.remote;

import com.example.hersafe.data.remote.models.LoginRequest;
import com.example.hersafe.data.remote.models.UserResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface AuthApiService {

    @POST("auth/login")
    Call<UserResponse> login(@Body LoginRequest loginRequest);

    @POST("auth/verify")
    Call<UserResponse> verifyToken(@Body Map<String, String> body);

    @POST("auth/register")
    Call<UserResponse> register(@Body Map<String, String> body);

    @POST("api/report")
    Call<Map<String, Object>> reportAlert(@Body Map<String, Object> body);

    @POST("api/telegram/confirm")
    Call<Map<String, Object>> confirmTelegramPairing(@Body Map<String, String> body);

    @PUT("auth/profile")
    Call<UserResponse> updateProfile(@Body Map<String, String> body);

    @PUT("auth/password")
    Call<UserResponse> changePassword(@Body Map<String, String> body);
}
