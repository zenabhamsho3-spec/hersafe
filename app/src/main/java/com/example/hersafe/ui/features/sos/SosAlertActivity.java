package com.example.hersafe.ui.features.sos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.telephony.SmsManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.hersafe.R;
import com.example.hersafe.data.local.AppDatabase;
import com.example.hersafe.data.local.dao.ContactDao;
import com.example.hersafe.data.local.dao.IncidentDao;
import com.example.hersafe.data.local.entities.Contact;
import com.example.hersafe.data.local.entities.Incident;
import com.example.hersafe.data.preferences.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;

import android.util.Log;

/**
 * شاشة تنبيه الطوارئ SOS
 * تبدأ عداداً تنازلياً ثم ترسل رسائل SMS لجميع جهات الاتصال الطارئة مع الموقع
 */
public class SosAlertActivity extends AppCompatActivity {

    private static final String TAG = "SOS_ALERT";

    private static final int SMS_PERMISSION_REQUEST = 1;
    private static final int LOCATION_PERMISSION_REQUEST = 2;

    private TextView tvCountDown;
    private CountDownTimer timer;
    private boolean isCancelled = false;

    private ContactDao contactDao;
    private IncidentDao incidentDao;
    private SessionManager sessionManager;

    // للحصول على الموقع
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    // تعريف BroadcastReceiver لمراقبة حالة إرسال الرسالة
    private final android.content.BroadcastReceiver smsSentReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            String phoneNumber = intent.getStringExtra("phone_number");
            switch (getResultCode()) {
                case android.app.Activity.RESULT_OK:
                    Log.d(TAG, "SMS delivered successfully to: " + phoneNumber);
                    Toast.makeText(context, "تم إرسال الرسالة بنجاح", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Log.e(TAG, "SMS Generic Failure for: " + phoneNumber + " (Code: " + getResultCode() + ")");
                    Toast.makeText(context, "فشل عام في إرسال الرسالة", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    Log.e(TAG, "SMS No Service for: " + phoneNumber);
                    Toast.makeText(context, "لا توجد خدمة شبكة", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    Log.e(TAG, "SMS Null PDU for: " + phoneNumber);
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    Log.e(TAG, "SMS Radio Off for: " + phoneNumber);
                    Toast.makeText(context, "وضع الطيران مفعل أو الشبكة مغلقة", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Log.e(TAG, "SMS Unknown Error for: " + phoneNumber + " Code: " + getResultCode());
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // تسجيل المستقبل عند فتح النشاط
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            registerReceiver(smsSentReceiver, new android.content.IntentFilter("SMS_SENT"), android.content.Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(smsSentReceiver, new android.content.IntentFilter("SMS_SENT"));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // إلغاء التسجيل لتجنب تسريب الذاكرة
        try {
            unregisterReceiver(smsSentReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_alert);

        // تهيئة قاعدة البيانات
        try {
            AppDatabase db = AppDatabase.getInstance(this);
            contactDao = db.contactDao();
            incidentDao = db.incidentDao();
            sessionManager = SessionManager.getInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "خطأ في تهيئة قاعدة البيانات", Toast.LENGTH_LONG).show();
        }

        // تهيئة مزود الموقع
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tvCountDown = findViewById(R.id.tvCountDown);

        // الحصول على الموقع أولاً
        requestLocationUpdate();

        // زر الإلغاء (أنا بخير)
        findViewById(R.id.btnCancel).setOnClickListener(v -> {
            isCancelled = true;
            if (timer != null) timer.cancel();
            Toast.makeText(this, "تم إلغاء التنبيه", Toast.LENGTH_SHORT).show();
            finish();
        });

        // زر الإرسال فوراً
        findViewById(R.id.btnSendNow).setOnClickListener(v -> {
            if (timer != null) timer.cancel();
            sendEmergencyAlert();
        });
        
        // بدء العداد التنازلي
        startEmergencyCountdown();
    }

    /**
     * طلب تحديث الموقع
     */
    @SuppressLint("MissingPermission")
    private void requestLocationUpdate() {
        // التحقق من الإذن
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        // طلب الموقع الحالي
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500)
                .setMaxUpdates(1)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                currentLocation = locationResult.getLastLocation();
            }
        }, Looper.getMainLooper());

        // محاولة الحصول على آخر موقع معروف
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLocation = location;
            }
        });
    }

    /**
     * بدء العداد التنازلي
     */
    private void startEmergencyCountdown() {
        int countdownSeconds = 5; // افتراضي 5 ثواني
        if (sessionManager != null) {
            countdownSeconds = sessionManager.getSosCountdown();
        }
        long countdownMs = countdownSeconds * 1000L;

        timer = new CountDownTimer(countdownMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000) + 1;
                tvCountDown.setText(String.valueOf(secondsLeft));
            }

            @Override
            public void onFinish() {
                if (!isCancelled) {
                    tvCountDown.setText("0");
                    sendEmergencyAlert();
                }
            }
        }.start();
    }

    /**
     * إرسال تنبيه الطوارئ لجميع جهات الاتصال
     */
    /**
     * إرسال تنبيه الطوارئ لجميع جهات الاتصال
     */
    private void sendEmergencyAlert() {
        Log.d(TAG, "sendEmergencyAlert() called");
        
        // التحقق من إذن الرسائل
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST);
            return;
        }

        // Unified Logic via Helper
        // Helper handles: SMS, Location, Video, Telegram
        com.example.hersafe.utils.SosHelper.triggerSos(this);

        finish();
    }

    /**
     * حفظ الحادث في سجل الحوادث
     */
    private void saveIncident(String status) {
        try {
            if (incidentDao != null) {
                Incident incident = new Incident("SOS", System.currentTimeMillis(), status);
                if (currentLocation != null) {
                    incident.setLatitude(currentLocation.getLatitude());
                    incident.setLongitude(currentLocation.getLongitude());
                }
                incidentDao.insert(incident);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendEmergencyAlert();
            } else {
                Toast.makeText(this,
                        "يجب منح إذن الرسائل لإرسال تنبيه الطوارئ",
                        Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdate();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }

    /**
     * تنسيق رقم الهاتف: إزالة الصفر بادئ وإضافة +963
     */
    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) return "";

        // إزالة الفراغات
        String formatted = phone.trim().replaceAll("\\s+", "");

        // إذا كان يبدأ بـ +963، نتركه كما هو
        if (formatted.startsWith("+963")) {
            return formatted;
        }

        // إذا كان يبدأ بـ 00963، نستبدلها بـ +963
        if (formatted.startsWith("00963")) {
            return "+" + formatted.substring(2);
        }

        // إذا كان يبدأ بـ 0، نحذفها ونضيف +963
        if (formatted.startsWith("0")) {
            formatted = formatted.substring(1);
        }

        return "+963" + formatted;
    }
}