package com.example.hersafe.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.hersafe.data.local.dao.ContactDao;
import com.example.hersafe.data.local.dao.IncidentDao;
import com.example.hersafe.data.local.dao.UserDao;
import com.example.hersafe.data.local.entities.Contact;
import com.example.hersafe.data.local.entities.Incident;
import com.example.hersafe.data.local.entities.User;

/**
 * قاعدة بيانات Room الرئيسية للتطبيق
 * Singleton pattern لضمان نسخة واحدة فقط
 */
@Database(
    entities = {Contact.class, User.class, Incident.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    // اسم قاعدة البيانات
    private static final String DATABASE_NAME = "hersafe_database";

    // Singleton instance
    private static volatile AppDatabase INSTANCE;

    // DAOs المتاحة
    public abstract ContactDao contactDao();
    public abstract UserDao userDao();
    public abstract IncidentDao incidentDao();

    /**
     * الحصول على نسخة قاعدة البيانات (Singleton)
     * @param context سياق التطبيق
     * @return نسخة قاعدة البيانات
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    // السماح بالعمليات على الـ Main Thread (للتبسيط فقط)
                    // في الإنتاج يفضل استخدام AsyncTask أو Coroutines
                    .allowMainThreadQueries()
                    // إعادة بناء قاعدة البيانات عند تغيير الـ schema
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * إغلاق قاعدة البيانات (للاختبارات)
     */
    public static void destroyInstance() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
        }
        INSTANCE = null;
    }
}
