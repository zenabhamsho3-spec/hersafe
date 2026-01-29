package com.example.hersafe.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.example.hersafe.utils.SosHelper;

public class VolumeAccessibilityService extends AccessibilityService {

    private static final String TAG = "VolumeAccessService";
    private int volumeClickCount = 0;
    private long lastClickTime = 0;
    private static final long MAX_DELAY_BETWEEN_CLICKS = 2000; // 2 seconds to complete the sequence

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "Service Interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Service Connected. Ready for 3-click SOS.");
        showToast("خدمة الطوارئ مفعلة: اضغطي خفض الصوت 3 مرات");
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();

        // ONLY process ACTION_DOWN
        if (action == KeyEvent.ACTION_DOWN) {
            
            // IGNORE repeats (Long presses)
            if (event.getRepeatCount() > 0) {
                return super.onKeyEvent(event);
            }

            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                handleVolumeDownClick();
                // Consume the event if we are in the middle of a sequence? 
                // No, let system handle volume change too to avoid suspicion, 
                // UNLESS strictly required to hide it. For now, return false.
                return false; 
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                // Determine if we should reset on Up button? Yes, strict pattern.
                if (volumeClickCount > 0) {
                    volumeClickCount = 0;
                    Log.d(TAG, "Chain broken by Volume Up");
                }
            }
        }
        
        return super.onKeyEvent(event);
    }

    private static final long MIN_DELAY_BETWEEN_CLICKS = 150; // Ignore bounces < 150ms

    private void handleVolumeDownClick() {
        long currentTime = System.currentTimeMillis();

        // 1. Debounce (Ignore too fast clicks)
        if (currentTime - lastClickTime < MIN_DELAY_BETWEEN_CLICKS) {
            return; 
        }

        // 2. Check for timeout - if too much time passed since last click, reset
        if (volumeClickCount > 0 && (currentTime - lastClickTime > MAX_DELAY_BETWEEN_CLICKS)) {
            volumeClickCount = 0;
            Log.d(TAG, "Chain reset due to timeout");
            // showToast("Timeout reset"); // Optional debug
        }

        volumeClickCount++;
        lastClickTime = currentTime;

        Log.d(TAG, "Volume Down Click #" + volumeClickCount);

        if (volumeClickCount == 3) {
            Log.d(TAG, "SOS TRIGGERED!");
            volumeClickCount = 0; // Reset
            triggerSos();
        }
    }

    private void triggerSos() {
        // Unified Logic: Delegates entirely to SosHelper
        SosHelper.triggerSos(getApplicationContext());
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> 
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show()
        );
    }
}
