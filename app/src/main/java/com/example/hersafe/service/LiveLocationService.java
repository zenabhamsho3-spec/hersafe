package com.example.hersafe.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.hersafe.R;
import com.example.hersafe.data.preferences.SessionManager;
import com.example.hersafe.data.remote.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveLocationService extends Service {

    private static final String TAG = "LiveLocationService";
    private static final String CHANNEL_ID = "LiveLocationChannel";
    private static final int NOTIFICATION_ID = 4001;

    // Publicly accessible flag to check if service is running
    public static volatile boolean isRunning = false;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private SessionManager sessionManager;

    private String chatId;
    private int currentMessageId = -1;
    private boolean isFirstLocation = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Service starting");
        
        sessionManager = SessionManager.getInstance(this);
        chatId = sessionManager.getTelegramChatId();
        
        createNotificationChannel();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    double lat = locationResult.getLastLocation().getLatitude();
                    double lng = locationResult.getLastLocation().getLongitude();
                    Log.d(TAG, "New live location: " + lat + ", " + lng);
                    
                    shareLocationToTelegram(lat, lng);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Foreground service required for continuous tracking
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("مشاركة الموقع الحي")
                .setContentText("يتم مشاركة موقعك المباشر حالياً لضمان سلامتك...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        isRunning = true;

        if (chatId == null || chatId.isEmpty() || !chatId.matches("[-0-9]+")) {
            Log.e(TAG, "Cannot start live location: Invalid Chat ID " + chatId);
            stopSelf();
            return START_NOT_STICKY;
        }

        startLocationUpdates();
        return START_STICKY;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted for LiveLocationService");
            stopSelf();
            return;
        }

        // Update every 10 seconds (10000 ms), minimum distance change is minimal
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.d(TAG, "Started requesting location updates for Telegram Live Location.");
    }

    private void shareLocationToTelegram(double lat, double lng) {
        String botToken = "8503140381:AAGtXY0pX8OsHvEKS92t3th8cz1KPNiPPbw"; // Hardcoded for now as per previous logic

        if (isFirstLocation) {
            // Initial call to start Live Location via 'sendLocation'
            int LIVE_PERIOD = 86400; // 24 hours (Telegram max)
            
            RetrofitClient.getTelegramService(botToken).sendLocation(chatId, lat, lng, LIVE_PERIOD)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                try {
                                    String json = response.body().string();
                                    JSONObject jsonObject = new JSONObject(json);
                                    if (jsonObject.getBoolean("ok")) {
                                        JSONObject result = jsonObject.getJSONObject("result");
                                        currentMessageId = result.getInt("message_id");
                                        isFirstLocation = false;
                                        Log.d(TAG, "Started Telegram Live Location on Message ID: " + currentMessageId);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing sendLocation response", e);
                                }
                            } else {
                                Log.e(TAG, "sendLocation failed: " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.e(TAG, "sendLocation API failure", t);
                        }
                    });

        } else if (currentMessageId != -1) {
            // Subsequent calls update the existing Live Location ping
            RetrofitClient.getTelegramService(botToken).editMessageLiveLocation(chatId, currentMessageId, lat, lng)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (!response.isSuccessful()) {
                                Log.e(TAG, "editMessageLiveLocation failed: " + response.code());
                                if (response.code() == 400 || response.code() == 404) {
                                    // The message might have been deleted, or timeout expired
                                    // Reset to create a new live location message next tick
                                    isFirstLocation = true;
                                    currentMessageId = -1;
                                }
                            } else {
                                Log.d(TAG, "Successfully updated live location.");
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.e(TAG, "editMessageLiveLocation API failure", t);
                        }
                    });
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Live Location Sharing",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Shows active live location sharing status");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        Log.d(TAG, "onDestroy: LiveLocationService stopping");
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
