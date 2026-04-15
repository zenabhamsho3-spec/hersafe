package com.example.hersafe.data.remote;

import retrofit2.Retrofit;

import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "https://hersafe-backend.zenabhamsho3.workers.dev/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static AuthApiService getAuthService() {
        return getClient().create(AuthApiService.class);
    }

    public static TelegramApiService getTelegramService(String token) {
        // Explicitly trim token to avoid 404 errors due to spaces
        String cleanToken = token.trim();
        if (cleanToken.isEmpty()) {
            android.util.Log.e("RetrofitClient", "Empty token provided!");
        }
        
        // Remove 'bot' prefix if user pasted it by mistake
        if (cleanToken.toLowerCase().startsWith("bot")) {
             // Ensure it's not the part of the ID itself (unlikely, but check if followed by digit)
             // Typically bot tokens start with digits.
             // If input is "bot12345...", we want "12345..."
             // A very simple check:
             if (cleanToken.length() > 3 && Character.isDigit(cleanToken.charAt(3))) {
                 cleanToken = cleanToken.substring(3);
             }
        }
        
        // Final Formatting Check
        if (!cleanToken.matches("^\\d{8,}:[A-Za-z0-9_-]+$")) {
            android.util.Log.e("RetrofitClient", "Invalid Token Format: " + cleanToken);
            // We can't proceed well with a bad token
        }

        // Add Logging Interceptor
        okhttp3.logging.HttpLoggingInterceptor logging = new okhttp3.logging.HttpLoggingInterceptor();
        logging.setLevel(okhttp3.logging.HttpLoggingInterceptor.Level.BODY);

        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        String baseUrl = "https://api.telegram.org/bot" + cleanToken + "/";
        
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TelegramApiService.class);
    }

    public static DirectionsApiService getService() {
        okhttp3.logging.HttpLoggingInterceptor logging = new okhttp3.logging.HttpLoggingInterceptor();
        logging.setLevel(okhttp3.logging.HttpLoggingInterceptor.Level.BODY);

        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        return new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DirectionsApiService.class);
    }
    public static ReportsApiService getReportsService() {
        return getClient().create(ReportsApiService.class);
    }
}
