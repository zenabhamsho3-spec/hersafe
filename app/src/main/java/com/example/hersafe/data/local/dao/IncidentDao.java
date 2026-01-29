package com.example.hersafe.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hersafe.data.local.entities.Incident;

import java.util.List;

/**
 * DAO للحوادث والتنبيهات
 * يوفر عمليات على جدول incidents_history
 */
@Dao
public interface IncidentDao {

    /**
     * جلب جميع الحوادث مرتبة من الأحدث للأقدم
     */
    @Query("SELECT * FROM incidents_history ORDER BY timestamp DESC")
    List<Incident> getAllIncidents();

    /**
     * جلب حادث بالـ ID
     */
    @Query("SELECT * FROM incidents_history WHERE id = :incidentId")
    Incident getIncidentById(int incidentId);

    /**
     * جلب الحوادث حسب النوع (SOS أو Journey)
     */
    @Query("SELECT * FROM incidents_history WHERE type = :type ORDER BY timestamp DESC")
    List<Incident> getIncidentsByType(String type);

    /**
     * جلب الحوادث حسب الحالة (Sent أو Failed)
     */
    @Query("SELECT * FROM incidents_history WHERE status = :status ORDER BY timestamp DESC")
    List<Incident> getIncidentsByStatus(String status);

    /**
     * جلب آخر N حوادث
     */
    @Query("SELECT * FROM incidents_history ORDER BY timestamp DESC LIMIT :limit")
    List<Incident> getRecentIncidents(int limit);

    /**
     * جلب الحوادث في فترة زمنية محددة
     */
    @Query("SELECT * FROM incidents_history WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<Incident> getIncidentsBetween(long startTime, long endTime);

    /**
     * إضافة حادث جديد
     */
    @Insert
    long insert(Incident incident);

    /**
     * تحديث حادث
     */
    @Update
    void update(Incident incident);

    /**
     * حذف حادث
     */
    @Delete
    void delete(Incident incident);

    /**
     * حذف جميع الحوادث
     */
    @Query("DELETE FROM incidents_history")
    void deleteAll();

    /**
     * عدد الحوادث
     */
    @Query("SELECT COUNT(*) FROM incidents_history")
    int getIncidentsCount();

    /**
     * عدد حوادث SOS
     */
    @Query("SELECT COUNT(*) FROM incidents_history WHERE type = 'SOS'")
    int getSosCount();

    /**
     * تحديث حالة الحادث
     */
    @Query("UPDATE incidents_history SET status = :status WHERE id = :incidentId")
    void updateStatus(int incidentId, String status);
}
