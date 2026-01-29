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
}
