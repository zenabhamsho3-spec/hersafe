package com.example.hersafe.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * كيان المستخدم
 * يمثل بيانات المستخدم المحلية للربط المستقبلي مع API
 */
@Entity(tableName = "users")
public class User {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "phone")
    private String phone;

    @ColumnInfo(name = "token")
    private String token;

    @ColumnInfo(name = "profile_photo_uri")
    private String profilePhotoUri;

    @ColumnInfo(name = "birthdate")
    private String birthdate;

    @ColumnInfo(name = "residence")
    private String residence;

    @ColumnInfo(name = "father_name")
    private String fatherName;

    @ColumnInfo(name = "mother_name")
    private String motherName;

    @ColumnInfo(name = "is_logged_in")
    private boolean isLoggedIn;

    // Constructor
    public User(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.isLoggedIn = false;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getToken() {
        return token;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public String getProfilePhotoUri() {
        return profilePhotoUri;
    }

    public void setProfilePhotoUri(String profilePhotoUri) {
        this.profilePhotoUri = profilePhotoUri;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getResidence() {
        return residence;
    }

    public void setResidence(String residence) {
        this.residence = residence;
    }

    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getMotherName() {
        return motherName;
    }

    public void setMotherName(String motherName) {
        this.motherName = motherName;
    }
}
