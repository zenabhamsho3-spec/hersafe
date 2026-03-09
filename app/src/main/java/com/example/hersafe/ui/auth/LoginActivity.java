package com.example.hersafe.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView; // استيراد TextView

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hersafe.data.preferences.SessionManager;
import com.example.hersafe.data.remote.RetrofitClient;
import com.example.hersafe.data.remote.models.LoginRequest;
import com.example.hersafe.data.remote.models.UserResponse;
import com.example.hersafe.ui.main.MainActivity;
import com.example.hersafe.ui.auth.SignupActivity;
import com.example.hersafe.R;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // معالجة الحواف
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. برمجة زر تسجيل الدخول (للانتقال للقائمة الرئيسية)
        Button btnLogin = findViewById(R.id.btSignin);
        TextView tvEmail = findViewById(R.id.etEmail);
        TextView tvPassword = findViewById(R.id.etPassword);

        btnLogin.setOnClickListener(v -> {
            String email = tvEmail.getText().toString();
            String password = tvPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                tvEmail.setError("يرجى إدخال البيانات");
                return;
            }

            // طلب تسجيل الدخول من الباك إند
            LoginRequest loginRequest = new LoginRequest(email, password);
            RetrofitClient.getAuthService().login(loginRequest).enqueue(new Callback<UserResponse>() {
                @Override
                public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        UserResponse.RemoteUser user = response.body().getUser();
                        // حفظ الجلسة
                        SessionManager.getInstance(LoginActivity.this).createLoginSession(
                                user.getId(),
                                user.getName(),
                                user.getEmail(),
                                user.getPhone(),
                                user.getApiToken()
                        );

                        // حفظ المستخدم في قاعدة البيانات المحلية
                        com.example.hersafe.data.local.AppDatabase db = com.example.hersafe.data.local.AppDatabase.getInstance(LoginActivity.this);
                        db.userDao().logoutAll();
                        com.example.hersafe.data.local.entities.User localUser = db.userDao().getUserByEmail(user.getEmail());
                        if (localUser == null) {
                            localUser = new com.example.hersafe.data.local.entities.User(user.getName(), user.getEmail(), user.getPhone());
                            localUser.setToken(user.getApiToken());
                            localUser.setLoggedIn(true);
                            db.userDao().insert(localUser);
                        } else {
                            localUser.setName(user.getName());
                            localUser.setPhone(user.getPhone());
                            localUser.setToken(user.getApiToken());
                            localUser.setLoggedIn(true);
                            db.userDao().update(localUser);
                        }
                        
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        tvEmail.setError("بيانات الدخول غير صحيحة");
                    }
                }

                @Override
                public void onFailure(Call<UserResponse> call, Throwable t) {
                    tvEmail.setError("خطأ في الاتصال: " + t.getMessage());
                }
            });
        });

        // 2. برمجة نص "إنشاء حساب" (للانتقال لصفحة التسجيل)
        // لقد قمنا بتفعيل الكود هنا وتوجيهه للصفحة الصحيحة
        TextView tvSignUp = findViewById(R.id.tvSignUp);
        tvSignUp.setOnClickListener(v -> {
            // الانتقال من Login إلى Signup
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }
}