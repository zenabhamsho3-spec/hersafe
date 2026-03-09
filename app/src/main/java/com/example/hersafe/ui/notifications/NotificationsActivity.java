package com.example.hersafe.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.hersafe.R;
import com.example.hersafe.service.LiveLocationService;
import com.example.hersafe.service.VideoRecordingService;
import com.google.android.material.button.MaterialButton;

public class NotificationsActivity extends AppCompatActivity {

    private TextView tvVideoStatus, tvLocationStatus, tvAllStoppedMsg;
    private MaterialButton btnForceStop;
    private ImageView imgVideoIcon, imgLocationIcon;

    private final Handler monitorHandler = new Handler();
    private final Runnable monitorRunnable = new Runnable() {
        @Override
        public void run() {
            updateStatuses();
            monitorHandler.postDelayed(this, 1500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        tvVideoStatus    = findViewById(R.id.tvVideoStatus);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        tvAllStoppedMsg  = findViewById(R.id.tvAllStoppedMsg);
        btnForceStop     = findViewById(R.id.btnForceStop);
        imgVideoIcon     = findViewById(R.id.imgVideoIcon);
        imgLocationIcon  = findViewById(R.id.imgLocationIcon);

        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Force stop
        if (btnForceStop != null) {
            btnForceStop.setOnClickListener(v -> {
                // Stop VideoRecordingService
                Intent stopVideo = new Intent(this, VideoRecordingService.class);
                stopVideo.setAction("STOP");
                startService(stopVideo);

                // Stop LiveLocationService
                stopService(new Intent(this, LiveLocationService.class));

                updateStatuses();
                Toast.makeText(this, "✅ تم إيقاف جميع الخدمات", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        monitorHandler.post(monitorRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        monitorHandler.removeCallbacks(monitorRunnable);
    }

    private void updateStatuses() {
        boolean videoOn    = VideoRecordingService.isServiceRunning;
        boolean locationOn = LiveLocationService.isRunning;

        updateBadge(tvVideoStatus, imgVideoIcon, videoOn);
        updateBadge(tvLocationStatus, imgLocationIcon, locationOn);

        boolean anyActive = videoOn || locationOn;

        if (tvAllStoppedMsg != null)
            tvAllStoppedMsg.setVisibility(anyActive ? View.GONE : View.VISIBLE);
        if (btnForceStop != null)
            btnForceStop.setVisibility(anyActive ? View.VISIBLE : View.GONE);
    }

    private void updateBadge(TextView badge, ImageView icon, boolean active) {
        if (badge == null) return;
        if (active) {
            badge.setText("● نشط");
            badge.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
            badge.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_status_active));
            if (icon != null) icon.setImageTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#C2185B")));
        } else {
            badge.setText("● متوقف");
            badge.setTextColor(android.graphics.Color.parseColor("#757575"));
            badge.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_status_stopped));
            if (icon != null) icon.setImageTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#9E9E9E")));
        }
    }
}
