package com.example.hersafe.ui.profile;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hersafe.R;
import com.example.hersafe.data.local.AppDatabase;
import com.example.hersafe.data.local.entities.User;
import com.example.hersafe.data.preferences.SessionManager;
import com.example.hersafe.data.remote.RetrofitClient;
import com.example.hersafe.data.remote.models.UserResponse;
import com.example.hersafe.ui.auth.LoginActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private AppDatabase db;
    private User currentUser;
    private SessionManager sessionManager;

    private TextView tvUserName, tvUserPhone;
    private ImageView imgProfile;

    // Photo picker launcher
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    saveProfilePhoto(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = AppDatabase.getInstance(this);
        sessionManager = SessionManager.getInstance(this);

        tvUserName = findViewById(R.id.tvUserName);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        imgProfile = findViewById(R.id.imgProfile);

        loadUserData();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }

    private void loadUserData() {
        currentUser = db.userDao().getCurrentUser();

        if (currentUser != null) {
            // Load all data from local Room DB
            tvUserName.setText(currentUser.getName() != null && !currentUser.getName().isEmpty()
                    ? currentUser.getName() : "مستخدم");
            tvUserPhone.setText(currentUser.getPhone() != null && !currentUser.getPhone().isEmpty()
                    ? currentUser.getPhone() : "---");

            // Load profile photo
            if (currentUser.getProfilePhotoUri() != null && !currentUser.getProfilePhotoUri().isEmpty()) {
                File photoFile = new File(currentUser.getProfilePhotoUri());
                if (photoFile.exists()) {
                    imgProfile.setImageURI(null);
                    imgProfile.setImageURI(Uri.fromFile(photoFile));
                }
            }
        } else {
            // Fallback to SessionManager if no Room user exists
            String name = sessionManager.getUserName();
            String phone = sessionManager.getUserPhone();
            tvUserName.setText(name != null && !name.isEmpty() ? name : "مستخدم");
            tvUserPhone.setText(phone != null && !phone.isEmpty() ? phone : "---");
        }
    }

    private void setupClickListeners() {
        // تعديل البيانات الشخصية
        findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditProfileDialog());

        // تغيير كلمة المرور
        findViewById(R.id.btnChangePassword).setOnClickListener(v -> showChangePasswordDialog());

        // تعديل صورة البروفايل
        findViewById(R.id.fabEditPhoto).setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // ربط التلجرام
        findViewById(R.id.btnTelegramPairing).setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.hersafe.ui.TelegramPairingActivity.class));
        });

        // تسجيل الخروج
        findViewById(R.id.btnLogout).setOnClickListener(v -> performLogout());
    }

    // ============================
    // تعديل البيانات الشخصية
    // ============================
    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        EditText etName = dialogView.findViewById(R.id.etEditName);
        EditText etPhone = dialogView.findViewById(R.id.etEditPhone);
        EditText etBirthdate = dialogView.findViewById(R.id.etEditBirthdate);
        EditText etResidence = dialogView.findViewById(R.id.etEditResidence);
        EditText etFatherName = dialogView.findViewById(R.id.etEditFatherName);
        EditText etMotherName = dialogView.findViewById(R.id.etEditMotherName);

        // Pre-fill ALL fields from local Room DB
        if (currentUser != null) {
            etName.setText(currentUser.getName() != null ? currentUser.getName() : "");
            etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
            etBirthdate.setText(currentUser.getBirthdate() != null ? currentUser.getBirthdate() : "");
            etResidence.setText(currentUser.getResidence() != null ? currentUser.getResidence() : "");
            etFatherName.setText(currentUser.getFatherName() != null ? currentUser.getFatherName() : "");
            etMotherName.setText(currentUser.getMotherName() != null ? currentUser.getMotherName() : "");
        } else {
            // Fallback to SessionManager
            etName.setText(sessionManager.getUserName());
            etPhone.setText(sessionManager.getUserPhone());
        }

        // Date picker for birthdate
        etBirthdate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR) - 20;
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, y, m, d) -> {
                        String date = String.format(Locale.US, "%d-%02d-%02d", y, m + 1, d);
                        etBirthdate.setText(date);
                    }, year, month, day);
            datePickerDialog.show();
        });

        new AlertDialog.Builder(this)
                .setTitle("تعديل البيانات الشخصية")
                .setView(dialogView)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newPhone = etPhone.getText().toString().trim();
                    String newBirthdate = etBirthdate.getText().toString().trim();
                    String newResidence = etResidence.getText().toString().trim();
                    String newFatherName = etFatherName.getText().toString().trim();
                    String newMotherName = etMotherName.getText().toString().trim();

                    if (newName.isEmpty()) {
                        Toast.makeText(this, "الاسم مطلوب", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateProfile(newName, newPhone, newBirthdate, newResidence, newFatherName, newMotherName);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void updateProfile(String newName, String newPhone, String birthdate, String residence, String fatherName, String motherName) {
        String token = sessionManager.getAuthToken();

        // 1. Update backend (global DB)
        Map<String, String> body = new HashMap<>();
        body.put("api_token", token);
        body.put("name", newName);
        body.put("phone", newPhone);
        body.put("birthdate", birthdate);
        body.put("residence", residence);
        body.put("father_name", fatherName);
        body.put("mother_name", motherName);

        RetrofitClient.getAuthService().updateProfile(body).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "Profile updated on backend");
                } else {
                    Log.w(TAG, "Backend update failed, but local update continues");
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.w(TAG, "Backend unreachable, local update continues: " + t.getMessage());
            }
        });

        // 2. Update SessionManager
        sessionManager.createLoginSession(
                sessionManager.getUserId(),
                newName,
                sessionManager.getUserEmail(),
                newPhone,
                token
        );

        // 3. Update local Room DB
        if (currentUser != null) {
            db.userDao().updateProfile(currentUser.getId(), newName, newPhone, birthdate, residence, fatherName, motherName);
            // Refresh local user object
            currentUser = db.userDao().getCurrentUser();
        }

        // 4. Refresh UI
        tvUserName.setText(newName);
        tvUserPhone.setText(newPhone.isEmpty() ? "---" : newPhone);

        Toast.makeText(this, "تم تحديث البيانات بنجاح", Toast.LENGTH_SHORT).show();
    }

    // ============================
    // تغيير كلمة المرور
    // ============================
    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        EditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);

        new AlertDialog.Builder(this)
                .setTitle("تغيير كلمة المرور")
                .setView(dialogView)
                .setPositiveButton("تغيير", (dialog, which) -> {
                    String currentPassword = etCurrentPassword.getText().toString().trim();
                    String newPassword = etNewPassword.getText().toString().trim();
                    String confirmPassword = etConfirmPassword.getText().toString().trim();

                    if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                        Toast.makeText(this, "جميع الحقول مطلوبة", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (newPassword.length() < 6) {
                        Toast.makeText(this, "كلمة المرور الجديدة يجب أن تكون 6 أحرف على الأقل", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(this, "كلمة المرور الجديدة غير متطابقة", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    changePassword(currentPassword, newPassword);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void changePassword(String currentPassword, String newPassword) {
        String token = sessionManager.getAuthToken();

        Map<String, String> body = new HashMap<>();
        body.put("api_token", token);
        body.put("current_password", currentPassword);
        body.put("new_password", newPassword);

        RetrofitClient.getAuthService().changePassword(body).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ProfileActivity.this, "تم تغيير كلمة المرور بنجاح", Toast.LENGTH_SHORT).show();
                } else {
                    String error = "فشل تغيير كلمة المرور";
                    if (response.body() != null && response.body().getError() != null) {
                        error = response.body().getError();
                    }
                    Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "خطأ في الاتصال: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ============================
    // صورة البروفايل
    // ============================
    private void saveProfilePhoto(Uri sourceUri) {
        try {
            File profileDir = new File(getFilesDir(), "profile");
            if (!profileDir.exists()) profileDir.mkdirs();

            File destFile = new File(profileDir, "profile_photo.jpg");

            InputStream in = getContentResolver().openInputStream(sourceUri);
            FileOutputStream out = new FileOutputStream(destFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            in.close();
            out.close();

            String savedPath = destFile.getAbsolutePath();

            if (currentUser != null) {
                db.userDao().updateProfilePhoto(currentUser.getId(), savedPath);
            }

            imgProfile.setImageURI(null);
            imgProfile.setImageURI(Uri.fromFile(destFile));

            Toast.makeText(this, "تم تحديث الصورة الشخصية", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Profile photo saved to: " + savedPath);

        } catch (Exception e) {
            Log.e(TAG, "Failed to save profile photo", e);
            Toast.makeText(this, "فشل في حفظ الصورة", Toast.LENGTH_SHORT).show();
        }
    }

    // ============================
    // تسجيل الخروج
    // ============================
    private void performLogout() {
        db.userDao().logoutAll();
        sessionManager.logout();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}