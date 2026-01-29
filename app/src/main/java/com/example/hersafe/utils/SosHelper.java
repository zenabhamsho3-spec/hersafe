package com.example.hersafe.utils;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.example.hersafe.data.local.AppDatabase;
import com.example.hersafe.data.local.dao.ContactDao;
import com.example.hersafe.data.local.dao.IncidentDao;
import com.example.hersafe.data.local.entities.Contact;
import com.example.hersafe.data.local.entities.Incident;
import com.example.hersafe.service.VideoRecordingService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SosHelper {

    private static final String TAG = "SosHelper";

    /**
     * Unified method to trigger SOS:
     * 1. Get Location (if not provided).
     * 2. Send SMS to all contacts ("احتاج مساعدة فورية " + Location Link).
     * 3. Start Background Video Recording (which uploads to Telegram).
     */
    public static void triggerSos(Context context) {
        Log.d(TAG, "========== TRIGGER SOS STARTED ==========");
        
        // 1. Show User Feedback
        showToast(context, "⚠️ جاري إرسال الاستغاثة وتسجيل الفيديو... ⚠️");
        
        // 2. Start Video Recording Service IMMEDIATELY
        startVideoService(context);

        // 3. Process Location and SMS in Background
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Try to get location if we don't have it passed in (we'll fetch it here)
                Location location = fetchCurrentLocation(context);
                
                // Send SMS
                sendSmsToContacts(context, location);
                
            } catch (Exception e) {
                Log.e(TAG, "Error in SOS background task", e);
            }
        });
    }

    private static void startVideoService(Context context) {
        try {
            Intent videoIntent = new Intent(context, VideoRecordingService.class);
            videoIntent.setAction("START");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(videoIntent);
            } else {
                context.startService(videoIntent);
            }
            Log.d(TAG, "VideoRecordingService started.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start video service", e);
        }
    }

    private static Location fetchCurrentLocation(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission missing.");
            return null;
        }

        try {
            FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(context);
            CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
            
            // Try to get high accuracy location urgently
            Location location = com.google.android.gms.tasks.Tasks.await(
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
            );
            
            if (location != null) {
                Log.d(TAG, "Location fetched internal: " + location.getLatitude() + "," + location.getLongitude());
                return location;
            }
            
            // Fallback to last known
            location = com.google.android.gms.tasks.Tasks.await(fusedClient.getLastLocation());
            return location;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch location internal", e);
            return null;
        }
    }

    private static void sendSmsToContacts(Context context, Location currentLocation) {
        Log.d(TAG, "Preparing to send SMS...");
        
        AppDatabase db = AppDatabase.getInstance(context);
        ContactDao contactDao = db.contactDao();
        IncidentDao incidentDao = db.incidentDao();

        List<Contact> contacts = contactDao.getAllContacts();

        if (contacts == null || contacts.isEmpty()) {
            Log.e(TAG, "❌ NO CONTACTS FOUND!");
            showToast(context, "❌ لا توجد جهات اتصال! الرجاء إضافة جهات طوارئ");
            saveIncident(incidentDao, currentLocation, "Failed - No Contacts");
            return;
        }

        // Prepare Message: "احتاج مساعدة فورية "
        String baseMessage = "احتاج مساعدة فورية ";
        String locationUrl = "";

        if (currentLocation != null) {
            double lat = currentLocation.getLatitude();
            double lng = currentLocation.getLongitude();
            locationUrl = "\nموقعي: http://maps.google.com/?q=" + lat + "%2C" + lng;
        } else {
            locationUrl = "\n(الموقع غير متوفر)";
        }

        String fullMessage = baseMessage + locationUrl;

        // Get SmsManager
        SmsManager smsManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            smsManager = context.getSystemService(SmsManager.class);
        } else {
            smsManager = SmsManager.getDefault();
        }

        if (smsManager == null) {
            showToast(context, "❌ خطأ: خدمة الرسائل غير متاحة");
            return;
        }

        int sentCount = 0;
        for (Contact contact : contacts) {
            try {
                String formattedPhone = formatPhoneNumber(contact.getPhone());
                
                // Use Multipart to handle long messages properly (Arabic + URL can be long)
                java.util.ArrayList<String> parts = smsManager.divideMessage(fullMessage);
                
                java.util.ArrayList<PendingIntent> sentIntents = new java.util.ArrayList<>();
                
                for (int i = 0; i < parts.size(); i++) {
                     Intent sentIntentAction = new Intent("SMS_SENT");
                     sentIntentAction.putExtra("phone_number", formattedPhone);
                     PendingIntent sentIntent = PendingIntent.getBroadcast(
                            context,
                            (int) System.currentTimeMillis() + sentCount + i,
                            sentIntentAction,
                            PendingIntent.FLAG_IMMUTABLE
                     );
                     sentIntents.add(sentIntent);
                }

                smsManager.sendMultipartTextMessage(formattedPhone, null, parts, sentIntents, null);
                Log.d(TAG, "✓ Alert sent to: " + formattedPhone);
                sentCount++;

            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to send to " + contact.getPhone(), e);
            }
        }

        if (sentCount > 0) {
            showToast(context, "✅ تم إرسال طلب المساعدة لـ " + sentCount + " جهات");
            saveIncident(incidentDao, currentLocation, "Sent");
        } else {
            saveIncident(incidentDao, currentLocation, "Failed");
        }
    }

    private static void saveIncident(IncidentDao dao, Location location, String status) {
        if (dao == null) return;
        try {
            Incident incident = new Incident("SOS", System.currentTimeMillis(), status);
            if (location != null) {
                incident.setLatitude(location.getLatitude());
                incident.setLongitude(location.getLongitude());
            }
            dao.insert(incident);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void showToast(Context context, String message) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        );
    }

    private static String formatPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) return "";
        String formatted = phone.trim().replaceAll("\\s+", "");
        if (formatted.startsWith("+963")) return formatted;
        if (formatted.startsWith("00963")) return "+" + formatted.substring(2);
        if (formatted.startsWith("0")) formatted = formatted.substring(1);
        return "+963" + formatted;
    }
}
