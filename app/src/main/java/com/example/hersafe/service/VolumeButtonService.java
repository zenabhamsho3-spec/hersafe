package com.example.hersafe.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;
import android.location.Location;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.hersafe.R;
import com.example.hersafe.utils.SosHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class VolumeButtonService extends Service {

    private static final String TAG = "VolumeButtonService";
    private static final String CHANNEL_ID = "VolumeServiceChannel";
    
    private int volumeClickCount = 0;
    private long lastClickTime = 0;
    
    // Thresholds
    private static final long RESET_THRESHOLD = 3000; // Reset count if no click for 3s
    private static final long HOLD_DEBOUNCE_TIME = 200; // Ignore rapid fires

    private VolumeReceiver volumeReceiver;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "VolumeButtonService Created");
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Register Volume Receiver dynamically
        volumeReceiver = new VolumeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(volumeReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("HerSafe Active")
                .setContentText("Emergency triggers active")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
                
        startForeground(1, notification);
        return START_STICKY;
    }

    private class VolumeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) {
                handleVolumeChange();
            }
        }
    }

    private void handleVolumeChange() {
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - lastClickTime;

        if (diff < HOLD_DEBOUNCE_TIME) return;

        if (diff > RESET_THRESHOLD) {
            volumeClickCount = 1;
        } else {
            volumeClickCount++;
        }
        
        lastClickTime = currentTime;
        
        // vibrate(100); 
        
        if (volumeClickCount >= 3) {
            Log.d(TAG, "SOS Condition met in VolumeButtonService, BUT DISABLED in favor of AccessibilityService.");
            volumeClickCount = 0;
            // triggerSos(); // DISABLED: Using VolumeAccessibilityService for strict 3-click detection
        }
    }

    private void triggerSos() {
        Log.d(TAG, "Triggering SOS via Helper...");
        vibrate(1000); // 1 second vibration
        
        // Unified Logic via Helper
        SosHelper.triggerSos(this);
    }
    
    private void vibrate(long duration) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(duration);
        }
    }
    

    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Volume Button Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (volumeReceiver != null) {
            unregisterReceiver(volumeReceiver);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; 
    }
}
