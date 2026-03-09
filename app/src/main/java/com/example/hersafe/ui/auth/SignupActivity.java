package com.example.hersafe.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.app.DatePickerDialog;
import java.util.Calendar;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hersafe.R;
import com.example.hersafe.data.preferences.SessionManager;
import com.example.hersafe.data.remote.RetrofitClient;
import com.example.hersafe.data.remote.models.UserResponse;
import com.example.hersafe.ui.main.MainActivity;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etFirstName = findViewById(R.id.etFirstName);
        EditText etLastName = findViewById(R.id.etLastName);
        EditText etPhone = findViewById(R.id.etPhone);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etBirthDate = findViewById(R.id.etBirthDate);
        EditText etResidence = findViewById(R.id.etResidence);
        EditText etFatherName = findViewById(R.id.etFatherName);
        EditText etMotherName = findViewById(R.id.etMotherName);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnSignup = findViewById(R.id.btnSignup);

        // Date Picker Logic
        etBirthDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year1, month1, dayOfMonth) -> {
                        String date = String.format(Locale.US, "%d-%02d-%02d", year1, month1 + 1, dayOfMonth);
                        etBirthDate.setText(date);
                    }, year, month, day);
            datePickerDialog.show();
        });

        btnSignup.setOnClickListener(v -> {
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String birthdate = etBirthDate.getText().toString().trim();
            String residence = etResidence.getText().toString().trim();
            String fatherName = etFatherName.getText().toString().trim();
            String motherName = etMotherName.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (firstName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "يرجى ملء الحقول الأساسية", Toast.LENGTH_SHORT).show();
                return;
            }

            // تجهيز البيانات للباك إند
            Map<String, String> body = new HashMap<>();
            body.put("name", firstName + " " + lastName);
            body.put("email", email);
            body.put("phone", phone);
            body.put("password", password);
            // الحقول الإضافية
            body.put("birthdate", birthdate);
            body.put("residence", residence);
            body.put("father_name", fatherName);
            body.put("mother_name", motherName);

            RetrofitClient.getAuthService().register(body).enqueue(new Callback<UserResponse>() {
                @Override
                public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        UserResponse.RemoteUser remoteUser = response.body().getUser();
                        String token = response.body().getApiToken();
                        int userId = remoteUser != null ? remoteUser.getId() : 0;
                        String fullName = remoteUser != null ? remoteUser.getName() : (firstName + " " + lastName);

                        // حفظ الجلسة
                        SessionManager.getInstance(SignupActivity.this).createLoginSession(
                                userId,
                                fullName,
                                email,
                                phone,
                                token
                        );

                        // حفظ المستخدم في قاعدة البيانات المحلية
                        com.example.hersafe.data.local.AppDatabase db = com.example.hersafe.data.local.AppDatabase.getInstance(SignupActivity.this);
                        db.userDao().logoutAll();
                        
                        com.example.hersafe.data.local.entities.User localUser = new com.example.hersafe.data.local.entities.User(fullName, email, phone);
                        localUser.setToken(token);
                        localUser.setLoggedIn(true);
                        // حفظ الحقول الإضافية محلياً
                        localUser.setBirthdate(birthdate);
                        localUser.setResidence(residence);
                        localUser.setFatherName(fatherName);
                        localUser.setMotherName(motherName);
                        
                        db.userDao().insert(localUser);
                        
                        Toast.makeText(SignupActivity.this, "تم إنشاء الحساب بنجاح", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignupActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(SignupActivity.this, "فشل إنشاء الحساب: " + (response.body() != null ? response.body().getError() : "خطأ غير معروف"), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<UserResponse> call, Throwable t) {
                    Toast.makeText(SignupActivity.this, "خطأ في الاتصال: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}