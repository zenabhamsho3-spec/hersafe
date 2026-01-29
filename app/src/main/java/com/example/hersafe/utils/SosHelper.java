package com.example.hersafe.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import com.example.hersafe.data.local.AppDatabase;
import com.example.hersafe.data.local.dao.ContactDao;
import com.example.hersafe.data.local.dao.IncidentDao;
import com.example.hersafe.data.local.entities.Contact;
import com.example.hersafe.data.local.entities.Incident;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SosHelper {

    private static final String TAG = "SosHelper";

    /**
     * Sends emergency alert to all contacts in background thread to avoid UI blocking (for DB ops).
     */
    public static void sendEmergencyAlert(Context context, Location currentLocation, Runnable onComplete) {
        Log.d(TAG, "========== sendEmergencyAlert STARTED ==========");
        
        // Show immediate feedback to user
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
            android.widget.Toast.makeText(context, "جاري إرسال رسائل الطوارئ...", android.widget.Toast.LENGTH_SHORT).show()
        );
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Log.d(TAG, "Getting database instance...");
                AppDatabase db = AppDatabase.getInstance(context);
                ContactDao contactDao = db.contactDao();
                IncidentDao incidentDao = db.incidentDao();

                Log.d(TAG, "Fetching contacts from database...");
                List<Contact> contacts = contactDao.getAllContacts();

                if (contacts == null || contacts.isEmpty()) {
                    Log.e(TAG, "❌ NO CONTACTS FOUND IN DATABASE!");
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                        android.widget.Toast.makeText(context, "❌ لا توجد جهات اتصال! الرجاء إضافة جهات طوارئ أولاً", android.widget.Toast.LENGTH_LONG).show()
                    );
                    saveIncident(incidentDao, currentLocation, "Failed - No Contacts");
                    if (onComplete != null) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(onComplete);
                    }
                    return;
                }

                Log.d(TAG, "✓ Found " + contacts.size() + " contacts");

                // Prepare Data
                String alertText = "سلامتك غالية ..ثقي بحدسك واحمي وجودك";
                String locationUrl = "";

                if (currentLocation != null) {
                    double lat = currentLocation.getLatitude();
                    double lng = currentLocation.getLongitude();
                    locationUrl = "Loc: http://maps.google.com/?q=" + lat + "%2C" + lng;
                    Log.d(TAG, "Location available: " + lat + ", " + lng);
                } else {
                    Log.w(TAG, "No location available");
                }

                // Get SMS Manager
                Log.d(TAG, "Getting SmsManager...");
                SmsManager smsManager;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    smsManager = context.getSystemService(SmsManager.class);
                } else {
                    smsManager = SmsManager.getDefault();
                }

                if (smsManager == null) {
                    Log.e(TAG, "❌ SmsManager is NULL!");
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                        android.widget.Toast.makeText(context, "❌ خطأ: لا يمكن الوصول لخدمة الرسائل", android.widget.Toast.LENGTH_LONG).show()
                    );
                    saveIncident(incidentDao, currentLocation, "Failed - No SmsManager");
                    if (onComplete != null) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(onComplete);
                    }
                    return;
                }
                
                Log.d(TAG, "✓ SmsManager obtained successfully");

                int sentCount = 0;
                for (Contact contact : contacts) {
                    try {
                        String originalPhone = contact.getPhone();
                        String formattedPhone = formatPhoneNumber(originalPhone);
                        
                        Log.d(TAG, "Sending to: " + contact.getName() + " | Original: " + originalPhone + " | Formatted: " + formattedPhone);
                        
                        // 1. Send Text
                        Intent sentIntentAction1 = new Intent("SMS_SENT");
                        sentIntentAction1.putExtra("phone_number", formattedPhone);
                        PendingIntent sentIntent1 = PendingIntent.getBroadcast(
                                context,
                                (int) System.currentTimeMillis() + sentCount,
                                sentIntentAction1,
                                PendingIntent.FLAG_IMMUTABLE
                        );
                        smsManager.sendTextMessage(formattedPhone, null, alertText, sentIntent1, null);
                        Log.d(TAG, "✓ Alert message sent to: " + formattedPhone);
                        sentCount++;

                        // 2. Send Location
                        if (!locationUrl.isEmpty()) {
                            Intent sentIntentAction2 = new Intent("SMS_SENT");
                            sentIntentAction2.putExtra("phone_number", formattedPhone);
                            PendingIntent sentIntent2 = PendingIntent.getBroadcast(
                                    context,
                                    (int) System.currentTimeMillis() + sentCount + 1000,
                                    sentIntentAction2,
                                    PendingIntent.FLAG_IMMUTABLE
                            );
                            smsManager.sendTextMessage(formattedPhone, null, locationUrl, sentIntent2, null);
                            Log.d(TAG, "✓ Location message sent to: " + formattedPhone);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "❌ Failed to send to " + contact.getPhone() + ": " + e.getMessage(), e);
                    }
                }

                Log.d(TAG, "========== SMS SEND COMPLETE: " + sentCount + " messages sent ==========");
                
                final int finalSentCount = sentCount;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                    android.widget.Toast.makeText(context, "✅ تم إرسال " + finalSentCount + " رسالة طوارئ", android.widget.Toast.LENGTH_LONG).show()
                );

                if (sentCount > 0) {
                    saveIncident(incidentDao, currentLocation, "Sent");
                } else {
                    saveIncident(incidentDao, currentLocation, "Failed");
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ CRITICAL ERROR in sendEmergencyAlert: " + e.getMessage(), e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                    android.widget.Toast.makeText(context, "❌ خطأ في إرسال الرسائل: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show()
                );
            } finally {
                if (onComplete != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(onComplete);
                }
            }
        });
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

    private static String formatPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) return "";
        String formatted = phone.trim().replaceAll("\\s+", "");
        if (formatted.startsWith("+963")) return formatted;
        if (formatted.startsWith("00963")) return "+" + formatted.substring(2);
        if (formatted.startsWith("0")) formatted = formatted.substring(1);
        return "+963" + formatted;
    }
}
