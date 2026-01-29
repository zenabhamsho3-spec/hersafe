package com.example.hersafe.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * مدير الجلسات - يدير حالة تسجيل الدخول والإعدادات البسيطة
 * يستخدم SharedPreferences للتخزين السريع
 */
public class SessionManager {

    private static final String PREF_NAME = "HerSafePrefs";

    // Keys
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_PHONE = "userPhone";
    private static final String KEY_AUTH_TOKEN = "authToken";
    private static final String KEY_IS_FIRST_TIME = "isFirstTime";
    private static final String KEY_SOS_COUNTDOWN = "sosCountdown";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    // Singleton
    private static volatile SessionManager INSTANCE;

    private SessionManager(Context context) {
        pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * الحصول على نسخة SessionManager (Singleton)
     */
    public static SessionManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SessionManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SessionManager(context);
                }
            }
        }
        return INSTANCE;
    }

    // ==================== Login/Logout ====================

    /**
     * تسجيل الدخول وحفظ بيانات المستخدم
     */
    public void createLoginSession(int userId, String name, String email, String phone, String token) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_PHONE, phone);
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.apply();
    }

    /**
     * تسجيل الخروج ومسح كل البيانات
     */
    public void logout() {
        editor.clear();
        editor.apply();
    }

    /**
     * التحقق من حالة تسجيل الدخول
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // ==================== User Data ====================

    public int getUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "");
    }

    public String getUserPhone() {
        return pref.getString(KEY_USER_PHONE, "");
    }

    public String getAuthToken() {
        return pref.getString(KEY_AUTH_TOKEN, "");
    }

    // ==================== First Time / Onboarding ====================

    /**
     * التحقق إذا كانت أول مرة يفتح المستخدم التطبيق
     */
    public boolean isFirstTime() {
        return pref.getBoolean(KEY_IS_FIRST_TIME, true);
    }

    /**
     * تعيين أن المستخدم شاهد شاشة الترحيب
     */
    public void setFirstTimeDone() {
        editor.putBoolean(KEY_IS_FIRST_TIME, false);
        editor.apply();
    }

    // ==================== App Settings ====================

    /**
     * الحصول على مدة العد التنازلي لـ SOS (بالثواني)
     */
    public int getSosCountdown() {
        return pref.getInt(KEY_SOS_COUNTDOWN, 5); // افتراضي 5 ثواني
    }

    /**
     * تعيين مدة العد التنازلي لـ SOS
     */
    public void setSosCountdown(int seconds) {
        editor.putInt(KEY_SOS_COUNTDOWN, seconds);
        editor.apply();
    }
}
