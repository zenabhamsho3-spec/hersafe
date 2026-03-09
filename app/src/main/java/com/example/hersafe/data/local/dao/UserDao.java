package com.example.hersafe.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hersafe.data.local.entities.User;

/**
 * DAO للمستخدم
 * يوفر عمليات على جدول users
 */
@Dao
public interface UserDao {

    /**
     * جلب المستخدم الحالي (المسجل دخوله)
     */
    @Query("SELECT * FROM users WHERE is_logged_in = 1 LIMIT 1")
    User getCurrentUser();

    /**
     * جلب مستخدم بالـ ID
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    User getUserById(int userId);

    /**
     * جلب مستخدم بالبريد الإلكتروني
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    /**
     * إضافة مستخدم جديد
     */
    @Insert
    long insert(User user);

    /**
     * تحديث بيانات المستخدم
     */
    @Update
    void update(User user);

    /**
     * حذف مستخدم
     */
    @Delete
    void delete(User user);

    /**
     * تسجيل خروج جميع المستخدمين
     */
    @Query("UPDATE users SET is_logged_in = 0")
    void logoutAll();

    /**
     * تسجيل دخول مستخدم
     */
    @Query("UPDATE users SET is_logged_in = 1, token = :token WHERE id = :userId")
    void login(int userId, String token);

    /**
     * التحقق من وجود مستخدم مسجل
     */
    @Query("SELECT COUNT(*) FROM users WHERE is_logged_in = 1")
    int isAnyUserLoggedIn();

    /**
     * تحديث بيانات البروفايل
     */
    @Query("UPDATE users SET name = :name, phone = :phone, birthdate = :birthdate, residence = :residence, father_name = :fatherName, mother_name = :motherName WHERE id = :userId")
    void updateProfile(int userId, String name, String phone, String birthdate, String residence, String fatherName, String motherName);

    /**
     * تحديث صورة البروفايل
     */
    @Query("UPDATE users SET profile_photo_uri = :photoUri WHERE id = :userId")
    void updateProfilePhoto(int userId, String photoUri);

    /**
     * إنشاء أو تحديث مستخدم (upsert by email)
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User findByEmail(String email);
}
