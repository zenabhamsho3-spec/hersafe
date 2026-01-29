package com.example.hersafe.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hersafe.data.local.entities.Contact;

import java.util.List;

/**
 * DAO لجهات الاتصال الطارئة
 * يوفر جميع عمليات CRUD على جدول emergency_contacts
 */
@Dao
public interface ContactDao {

    /**
     * جلب جميع جهات الاتصال
     */
    @Query("SELECT * FROM emergency_contacts ORDER BY name ASC")
    List<Contact> getAllContacts();

    /**
     * جلب جهة اتصال بالـ ID
     */
    @Query("SELECT * FROM emergency_contacts WHERE id = :contactId")
    Contact getContactById(int contactId);

    /**
     * جلب جهات الاتصال غير المتزامنة مع السيرفر
     */
    @Query("SELECT * FROM emergency_contacts WHERE is_synced = 0")
    List<Contact> getUnsyncedContacts();

    /**
     * البحث عن جهة اتصال بالاسم
     */
    @Query("SELECT * FROM emergency_contacts WHERE name LIKE '%' || :searchQuery || '%'")
    List<Contact> searchContacts(String searchQuery);

    /**
     * إضافة جهة اتصال جديدة
     */
    @Insert
    long insert(Contact contact);

    /**
     * إضافة عدة جهات اتصال
     */
    @Insert
    void insertAll(List<Contact> contacts);

    /**
     * تحديث جهة اتصال
     */
    @Update
    void update(Contact contact);

    /**
     * حذف جهة اتصال
     */
    @Delete
    void delete(Contact contact);

    /**
     * حذف جهة اتصال بالـ ID
     */
    @Query("DELETE FROM emergency_contacts WHERE id = :contactId")
    void deleteById(int contactId);

    /**
     * حذف جميع جهات الاتصال
     */
    @Query("DELETE FROM emergency_contacts")
    void deleteAll();

    /**
     * عدد جهات الاتصال
     */
    @Query("SELECT COUNT(*) FROM emergency_contacts")
    int getContactsCount();

    /**
     * تحديث حالة المزامنة
     */
    @Query("UPDATE emergency_contacts SET is_synced = :isSynced WHERE id = :contactId")
    void updateSyncStatus(int contactId, boolean isSynced);
}
