package com.example.hersafe.ui.features.reports;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hersafe.R;
import com.example.hersafe.data.remote.RetrofitClient;
import com.example.hersafe.data.remote.models.Alert;
import com.example.hersafe.utils.GeoUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HighRiskAreasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GovernorateAdapter adapter;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_high_risk_areas);

        // Header
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.rvGovernorates);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new GovernorateAdapter((name, count) -> {
            Intent intent = new Intent(this, AreaDetailActivity.class);
            intent.putExtra("GOVERNORATE_NAME", name);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        fetchData();
    }

    private void fetchData() {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getReportsService().getPublicAlerts().enqueue(new Callback<List<Alert>>() {
            @Override
            public void onResponse(Call<List<Alert>> call, Response<List<Alert>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    processAlerts(response.body());
                } else {
                    Toast.makeText(HighRiskAreasActivity.this, "فشل تحميل البيانات", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Alert>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(HighRiskAreasActivity.this, "خطأ في الشبكة", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processAlerts(List<Alert> alerts) {
        Map<String, Integer> govCounts = new HashMap<>();

        for (Alert alert : alerts) {
            // 1. Get Area from Lat/Lng
            String area = GeoUtils.getClosestArea(alert.getLatitude(), alert.getLongitude());
            // 2. Get Governorate from Area
            String gov = GeoUtils.getGovernorateFromArea(area);
            
            govCounts.put(gov, govCounts.getOrDefault(gov, 0) + 1);
        }

        // Save raw alerts for detail view if needed (optimally pass ID or cache, but for now re-fetch or static cache)
        // For simplicity in this demo, we assume detail view re-fetches or we filter locally. 
        // Ideally we'd use a ViewModel or Repository.
        // Let's rely on simple re-fetch in detail view for MVP to avoid complex VM setup not present in other parts.
        
        adapter.setData(govCounts);
    }
}
