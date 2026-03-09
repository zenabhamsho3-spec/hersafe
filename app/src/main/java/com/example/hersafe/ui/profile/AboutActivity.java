package com.example.hersafe.ui.profile;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.hersafe.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
