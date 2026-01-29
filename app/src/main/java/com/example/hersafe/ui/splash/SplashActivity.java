package com.example.hersafe.ui.splash;

import android.content.Intent; // للانتقال
import android.os.Bundle;
import android.os.Handler; // للتحكم بالوقت

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hersafe.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // معالجة الحواف (تأكد أنك أضفت id="main" في ملف XML لتجنب الخطأ)
        if (findViewById(R.id.main) != null) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // --- كود الانتقال التلقائي بعد 1 ثانية ---
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // الانتقال إلى واجهة Onboarding
                Intent intent = new Intent(SplashActivity.this, com.example.hersafe.ui.onboarding.OnboardingActivity.class);
                startActivity(intent);

                // (اختياري) لإغلاق الصفحة الحالية بحيث لا يعود المستخدم إليها عند ضغط زر الرجوع
                finish();
            }
        }, 1000); // 1000 تعني 1000 ميلي ثانية = 1 ثانية
    }
}