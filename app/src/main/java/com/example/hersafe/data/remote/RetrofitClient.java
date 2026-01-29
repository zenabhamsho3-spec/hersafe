package com.example.hersafe.data.remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static final String BASE_URL = "https://maps.googleapis.com/";

    public static DirectionsApiService getService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(DirectionsApiService.class);
    }
    
    private static Retrofit telegramRetrofit = null;
    private static final String TELEGRAM_BASE_URL = "https://api.telegram.org/bot8503140381:AAGtXY0pX8OsHvEKS92t3th8cz1KPNiPPbw/";

    public static TelegramApiService getTelegramService() {
        if (telegramRetrofit == null) {
            telegramRetrofit = new Retrofit.Builder()
                    .baseUrl(TELEGRAM_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return telegramRetrofit.create(TelegramApiService.class);
    }
}
