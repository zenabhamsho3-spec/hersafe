package com.example.hersafe.ui.profile;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hersafe.R;

public class ProfileActivity extends AppCompatActivity {


    private com.example.hersafe.data.local.AppDatabase db;
    private com.example.hersafe.data.local.entities.User currentUser;

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

        // Initialize Database
        db = com.example.hersafe.data.local.AppDatabase.getInstance(this);

        // Load specific UI elements
        loadUserData();
        setupClickListeners();
    }

    private void loadUserData() {
        // Run on background thread ideally, but Main Thread allowed for now via AppDatabase config
        currentUser = db.userDao().getCurrentUser();

        android.widget.TextView tvUserName = findViewById(R.id.tvUserName);
        android.widget.TextView tvUserPhone = findViewById(R.id.tvUserPhone);
        
        if (currentUser != null) {
            tvUserName.setText(currentUser.getName() != null ? currentUser.getName() : "مستخدم");
            tvUserPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "---");
        } else {
            tvUserName.setText("زائر");
            tvUserPhone.setText("---");
        }
    }

    private void setupClickListeners() {
        // Back Button (using the custom header logic if applicable, or system back)
        // Note: The layout uses standard Linear/Constraint layouts. 
        // We'll trust the system back behavior or add explicit logic if a back button exists in XML.
        
        // Logout
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            if (currentUser != null) {
                // Logout logic
                db.userDao().logoutAll();
                
                // Navigate to Login
                android.content.Intent intent = new android.content.Intent(ProfileActivity.this, com.example.hersafe.ui.auth.LoginActivity.class);
                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                android.widget.Toast.makeText(this, "أنت غير مسجل دخول بالفعل", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        // Placeholder actions for other buttons
        android.view.View.OnClickListener placeholderListener = v -> 
            android.widget.Toast.makeText(this, "هذه الميزة قادمة قريباً", android.widget.Toast.LENGTH_SHORT).show();

        findViewById(R.id.btnEditProfile).setOnClickListener(placeholderListener);
        findViewById(R.id.btnChangePassword).setOnClickListener(placeholderListener);
        findViewById(R.id.fabEditPhoto).setOnClickListener(placeholderListener);
    }
}