package com.example.hersafe.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hersafe.R;
import com.example.hersafe.data.preferences.SessionManager;
import com.example.hersafe.data.remote.RetrofitClient;
import com.example.hersafe.data.remote.models.UserResponse;
import com.example.hersafe.ui.auth.LoginActivity;
import com.example.hersafe.ui.main.MainActivity;
import com.example.hersafe.ui.onboarding.OnboardingActivity;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private SessionManager sessionManager;

    @android.annotation.SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        sessionManager = SessionManager.getInstance(this);

        if (findViewById(R.id.main) != null) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::checkSession, 400);
    }

    private void checkSession() {
        // Offline-first: trust local session state
        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "Local session found, navigating to main");
            navigateToMain();
            return;
        }

        // No local session, go to onboarding or login
        handleNoSession();
    }

    private void handleNoSession() {
        if (sessionManager.isFirstTime()) {
            startActivity(new Intent(this, OnboardingActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}