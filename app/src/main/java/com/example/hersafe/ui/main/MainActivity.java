package com.example.hersafe.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback; // استيراد مهم
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.hersafe.R;
import com.example.hersafe.service.VideoRecordingService;
import com.google.android.material.navigation.NavigationView;
import com.example.hersafe.ui.features.sos.SosAlertActivity;
import com.example.hersafe.ui.features.journey.SafeJourneyActivity;
import com.example.hersafe.ui.features.contacts.ContactsActivity;
import com.example.hersafe.ui.auth.LoginActivity;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int VIDEO_RECORD_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 1. معالجة الحواف
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // 2. تعريف العناصر
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        // 3. برمجة زر الرجوع (الكود الجديد بدلاً من onBackPressed)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // 4. برمجة زر القائمة
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        // 5. برمجة القائمة الجانبية
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    // نحن في الرئيسية
                } else if (id == R.id.nav_profile) {
                    Intent intent = new Intent(MainActivity.this, com.example.hersafe.ui.profile.ProfileActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_notification) {
                    Toast.makeText(MainActivity.this, "لا توجد إشعارات", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_logout) {
                    performLogout();
                }

                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });
        }

        setupDashboardButtons();
    }

    private void setupDashboardButtons() {
        View btnSOS = findViewById(R.id.btnSOS);
        if (btnSOS != null) {
            btnSOS.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SosAlertActivity.class);
                startActivity(intent);
            });
        }

        View btnLocation = findViewById(R.id.btnLocation);
        if (btnLocation != null) {
            btnLocation.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SafeJourneyActivity.class);
                startActivity(intent);
            });
        }

        View btnHelpline = findViewById(R.id.btnHelpline);
        if (btnHelpline != null) {
            btnHelpline.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:911"));
                startActivity(intent);
            });
        }

        View btnCamera = findViewById(R.id.btnCamera);
        if (btnCamera != null) {
            btnCamera.setOnClickListener(v -> {
                if (checkPermissions()) {
                    toggleVideoRecording();
                } else {
                    requestPermissions();
                }
            });
        }

        View btnParents = findViewById(R.id.btnParents);
        if (btnParents != null) {
            btnParents.setOnClickListener(v -> {
                // استخدمت الاسم الجديد ContactsActivity
                Intent intent = new Intent(MainActivity.this, ContactsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void openCamera() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "حدث خطأ أثناء فتح الكاميرا", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                    Manifest.permission.CAMERA, 
                    Manifest.permission.RECORD_AUDIO, 
                    Manifest.permission.SEND_SMS, 
                    Manifest.permission.ACCESS_FINE_LOCATION
                },
                VIDEO_RECORD_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == VIDEO_RECORD_PERMISSION_CODE) {
            if (grantResults.length > 0 && checkPermissions()) {
                toggleVideoRecording();
            } else {
                Toast.makeText(this, "يجب السماح بجميع الأذونات (كاميرا، صوت، رسائل، موقع) لاستخدام ميزات الطوارئ", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void toggleVideoRecording() {
        if (VideoRecordingService.isServiceRunning) {
            stopVideoService();
        } else {
            startVideoService();
        }
    }

    private void startVideoService() {
        Intent serviceIntent = new Intent(this, VideoRecordingService.class);
        serviceIntent.setAction("START");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopVideoService() {
        Intent serviceIntent = new Intent(this, VideoRecordingService.class);
        serviceIntent.setAction("STOP");
        startService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 1. Check if Accessibility Service is enabled (Critical for SOS)
        if (!isAccessibilityServiceEnabled()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("تفعيل زر الطوارئ")
                .setMessage("يجب تفعيل خدمة 'HerSafe SOS Trigger' من إعدادات إمكانية الوصول لكي يعمل زر خفض الصوت للطوارئ.")
                .setPositiveButton("الذهاب للإعدادات", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("إلغاء", null)
                .show();
        }

        // 2. Register receiver for video state updates
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            registerReceiver(videoStateReceiver, new android.content.IntentFilter("com.example.hersafe.VIDEO_STATE_CHANGED"), RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(videoStateReceiver, new android.content.IntentFilter("com.example.hersafe.VIDEO_STATE_CHANGED"));
        }
        
        // 3. Start Volume Service (Foreground) - Keep generic service for location/others if needed, 
        // even if SOS trigger is disabled there.
        Intent volumeService = new Intent(this, com.example.hersafe.service.VolumeButtonService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(volumeService);
        } else {
            startService(volumeService);
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        android.content.ComponentName componentName = new android.content.ComponentName(this, com.example.hersafe.service.VolumeAccessibilityService.class);
        String enabledServices = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServices == null) return false;
        
        android.text.TextUtils.SimpleStringSplitter colonSplitter = new android.text.TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServices);
        
        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            if (componentNameString != null && componentNameString.equalsIgnoreCase(componentName.flattenToString())) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(videoStateReceiver);
        } catch (Exception e) {}
    }

    private final android.content.BroadcastReceiver videoStateReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            boolean isRecording = intent.getBooleanExtra("isRecording", false);
            updateCameraIcon(isRecording);
        }
    };
    
    private void updateCameraIcon(boolean isRecording) {
        ImageView icon = findViewById(R.id.imgCameraIcon);
        if (icon != null) {
            icon.setImageDrawable(ContextCompat.getDrawable(this, 
                isRecording ? R.drawable.ic_stop : R.drawable.ic_camera_alt));
        }
    }

    private void performLogout() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}