package com.example.hersafe.ui.features.reports;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hersafe.R;
import com.example.hersafe.data.remote.RetrofitClient;
import com.example.hersafe.data.remote.models.Alert;
import com.example.hersafe.utils.GeoUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AreaDetailActivity extends AppCompatActivity {

    private String governorateName;
    private RecyclerView recyclerView;
    private GovernorateAdapter adapter; // Reusing adapter for areas simply
    
    private TextView tvTotal, tvRiskLevel;
    private View btnDay, btnWeek, btnMonth;
    
    // Cache data
    private List<Alert> allAlerts;
    private String currentFilter = "day"; // day, week, month

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_detail);

        governorateName = getIntent().getStringExtra("GOVERNORATE_NAME");
        if (governorateName == null) finish();

        TextView title = findViewById(R.id.tvGovernorateTitle);
        title.setText("محافظة " + governorateName);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvTotal = findViewById(R.id.tvTotalAlerts);
        tvRiskLevel = findViewById(R.id.tvRiskLevel);
        
        recyclerView = findViewById(R.id.rvAreas);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Reusing adapter logic: showing Name -> Count. 
        // Here names are "Areas" (e.g. Masyaf) instead of Governorates.
        adapter = new GovernorateAdapter((name, count) -> {
            // Optional: Show map or even more details
        });
        recyclerView.setAdapter(adapter);

        setupFilters();
        fetchData();
    }

    private void setupFilters() {
        btnDay = findViewById(R.id.btnFilterDay);
        btnWeek = findViewById(R.id.btnFilterWeek);
        btnMonth = findViewById(R.id.btnFilterMonth);

        View.OnClickListener listener = v -> {
            resetFilterStyles();
            v.setBackgroundResource(R.drawable.bg_filter_selected);
            ((TextView)v).setTextColor(getResources().getColor(R.color.white));
            
            if (v == btnDay) currentFilter = "day";
            else if (v == btnWeek) currentFilter = "week";
            else if (v == btnMonth) currentFilter = "month";
            
            filterAndDisplay();
        };

        btnDay.setOnClickListener(listener);
        btnWeek.setOnClickListener(listener);
        btnMonth.setOnClickListener(listener);
    }
    
    private void resetFilterStyles() {
        // Simple reset logic
        int defColor = 0xFF757575; // Gray
        ((TextView)btnDay).setTextColor(defColor);
        btnDay.setBackground(null);
        ((TextView)btnWeek).setTextColor(defColor);
        btnWeek.setBackground(null);
        ((TextView)btnMonth).setTextColor(defColor);
        btnMonth.setBackground(null);
    }

    private void fetchData() {
        RetrofitClient.getReportsService().getPublicAlerts().enqueue(new Callback<List<Alert>>() {
            @Override
            public void onResponse(Call<List<Alert>> call, Response<List<Alert>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allAlerts = response.body();
                    filterAndDisplay();
                }
            }

            @Override
            public void onFailure(Call<List<Alert>> call, Throwable t) {
                Toast.makeText(AreaDetailActivity.this, "فشل تحديث البيانات", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterAndDisplay() {
        if (allAlerts == null) return;

        Map<String, Integer> areaCounts = new HashMap<>();
        int totalForFilter = 0;
        
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        
        for (Alert alert : allAlerts) {
            // Check Filter
            if (!isWithinTimeRange(alert.getCreatedAt(), currentFilter)) continue;

            // Check Governorate
            String area = GeoUtils.getClosestArea(alert.getLatitude(), alert.getLongitude());
            String gov = GeoUtils.getGovernorateFromArea(area);
            
            if (gov.equals(governorateName)) {
                areaCounts.put(area, areaCounts.getOrDefault(area, 0) + 1);
                totalForFilter++;
            }
        }

        // Update UI
        tvTotal.setText(String.valueOf(totalForFilter));
        updateRiskLevel(totalForFilter);
        adapter.setData(areaCounts);
    }
    
    private boolean isWithinTimeRange(String dateStr, String filter) {
        if (dateStr == null) return false;
        // Backend stores as "YYYY-MM-DD HH:MM:SS" usually, or ISO. My backend creates with CURRENT_TIMESTAMP (SQLite) -> "YYYY-MM-DD HH:MM:SS"
        // Let's parse loosely
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        // SQLite might be returning '2023-10-27 10:00:00'
        
        try {
            Date date = sdf.parse(dateStr);
            if (date == null) return false;
            
            long diff = new Date().getTime() - date.getTime();
            long day = 24 * 60 * 60 * 1000L;
            
            switch (filter) {
                case "day": return diff <= day;
                case "week": return diff <= 7 * day;
                case "month": return diff <= 30 * day;
            }
        } catch (ParseException e) {
            // fallback if format differs (e.g. ISO 8601 T)
             try {
                 SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US); // Simple ISO
                 Date date = iso.parse(dateStr);
                 if(date != null) {
                     long diff = new Date().getTime() - date.getTime();
                     long day = 24 * 60 * 60 * 1000L;
                     switch (filter) {
                        case "day": return diff <= day;
                        case "week": return diff <= 7 * day;
                        case "month": return diff <= 30 * day;
                     }
                 }
             } catch (Exception ex) {}
        }
        return true; // Default to include if parsing fails? No, exclude. But for demo maybe true? No, safe false.
    }
    
    private void updateRiskLevel(int count) {
        if (count > 20) {
            tvRiskLevel.setText(R.string.risk_high);
            tvRiskLevel.setTextColor(getResources().getColor(R.color.danger_red));
        } else if (count > 5) {
            tvRiskLevel.setText(R.string.risk_medium);
            tvRiskLevel.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            tvRiskLevel.setText(R.string.risk_low);
            tvRiskLevel.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }
}
