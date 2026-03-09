package com.example.hersafe.data.remote;

import com.example.hersafe.data.remote.models.Alert;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ReportsApiService {
    @GET("api/public/alerts")
    Call<List<Alert>> getPublicAlerts();

    @POST("api/report")
    Call<ResponseBody> reportAlert(@retrofit2.http.Body com.example.hersafe.data.remote.models.ReportRequest request);
}
