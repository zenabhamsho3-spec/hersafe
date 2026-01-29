package com.example.hersafe.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * كيان جهة الاتصال الطارئة
 * يمثل جدول emergency_contacts في قاعدة البيانات
 */
@Entity(tableName = "emergency_contacts")
public class Contact {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "phone")
    private String phone;

    @ColumnInfo(name = "relation")
    private String relation;

    @ColumnInfo(name = "is_synced")
    private boolean isSynced;

    // Constructor
    public Contact(String name, String phone, String relation) {
        this.name = name;
        this.phone = phone;
        this.relation = relation;
        this.isSynced = false;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getRelation() {
        return relation;
    }

    public boolean isSynced() {
        return isSynced;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }
}
