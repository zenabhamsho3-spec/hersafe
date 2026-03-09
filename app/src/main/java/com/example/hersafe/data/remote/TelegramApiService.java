package com.example.hersafe.data.remote;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface TelegramApiService {
    @Multipart
    @POST("sendVideo")
    Call<ResponseBody> sendVideo(
            @Part("chat_id") RequestBody chatId,
            @Part("caption") RequestBody caption,
            @Part MultipartBody.Part video
    );

    @retrofit2.http.GET("getMe")
    Call<ResponseBody> getMe();

    @retrofit2.http.GET("getUpdates")
    Call<ResponseBody> getUpdates();

    @retrofit2.http.FormUrlEncoded
    @POST("sendLocation")
    Call<ResponseBody> sendLocation(
            @retrofit2.http.Field("chat_id") String chatId,
            @retrofit2.http.Field("latitude") double latitude,
            @retrofit2.http.Field("longitude") double longitude,
            @retrofit2.http.Field("live_period") int livePeriod // seconds, e.g. 3600 for 1h
    );

    @retrofit2.http.FormUrlEncoded
    @POST("editMessageLiveLocation")
    Call<ResponseBody> editMessageLiveLocation(
            @retrofit2.http.Field("chat_id") String chatId,
            @retrofit2.http.Field("message_id") int messageId,
            @retrofit2.http.Field("latitude") double latitude,
            @retrofit2.http.Field("longitude") double longitude
    );
}
