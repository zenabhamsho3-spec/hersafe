package com.example.hersafe.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hersafe.R;
import com.example.hersafe.data.preferences.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.google.zxing.BarcodeFormat;
import com.example.hersafe.data.remote.RetrofitClient;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TelegramPairingActivity extends AppCompatActivity {

    private ImageView ivQrCode;
    private MaterialButton btnScanBotQr;
    private TextView tvStatus;
    private SessionManager sessionManager;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null) {
            Toast.makeText(this, "تم إلغاء المسح", Toast.LENGTH_SHORT).show();
        } else {
            String scannedData = result.getContents().trim();
            // Basic extraction: if the bot prefixes it, extract the digits or just use it raw.
            // For now, assume scannedData is the Chat ID directly or contains the chat id.
            String chatId = scannedData.replaceAll("[^0-9-]", ""); 
            
            if (!chatId.isEmpty()) {
                sessionManager.setTelegramChatId(chatId);
                tvStatus.setText("جاري إرسال رسالة التأكيد...");
                tvStatus.setTextColor(getResources().getColor(R.color.success_green, getTheme()));
                tvStatus.setVisibility(View.VISIBLE);
                
                Map<String, String> body = new HashMap<>();
                body.put("chat_id", chatId);
                RetrofitClient.getAuthService().confirmTelegramPairing(body).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> res) {
                        if (res.isSuccessful()) {
                            tvStatus.setText("تم الربط بنجاح! معرف المحادثة: " + chatId);
                            Toast.makeText(TelegramPairingActivity.this, "تم تفعيل تتبع التلجرام وإرسال التأكيد", Toast.LENGTH_SHORT).show();
                        } else {
                            tvStatus.setText("تم حفظ المعرف لكن فشل إرسال رسالة التأكيد");
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        tvStatus.setText("تم حفظ المعرف لكن فشل الاتصال بالسيرفر");
                    }
                });

            } else {
                Toast.makeText(this, "رمز QR غير صالح لمعرف المحادثة", Toast.LENGTH_LONG).show();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telegram_pairing);

        sessionManager = SessionManager.getInstance(this);

        ivQrCode = findViewById(R.id.ivQrCode);
        btnScanBotQr = findViewById(R.id.btnScanBotQr);
        tvStatus = findViewById(R.id.tvStatus);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        if (sessionManager.hasTelegramChatId()) {
            tvStatus.setText("تم الربط مسبقاً! معرف المحادثة: " + sessionManager.getTelegramChatId());
            tvStatus.setTextColor(getResources().getColor(R.color.success_green, getTheme()));
            tvStatus.setVisibility(View.VISIBLE);
        }

        generateDeepLinkQr();

        btnScanBotQr.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("قم بتوجيه الكاميرا نحو رمز الاستجابة السريعة المعروض في التلجرام");
            options.setCameraId(0);  // Use a specific camera of the device
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(true);
            barcodeLauncher.launch(options);
        });
    }

    private void generateDeepLinkQr() {
        int userId = sessionManager.getUserId();
        String identifier = userId != -1 ? String.valueOf(userId) : sessionManager.getUserPhone();
        if (identifier == null || identifier.isEmpty()) {
            identifier = "user";
        }
        
        // Deep link structure: https://t.me/hersafe_bot?start=<userId>
        // Use your actual bot username if hersafe_bot is just a placeholder.
        String deepLink = "https://t.me/hersafe_bot?start=" + identifier;

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(deepLink, BarcodeFormat.QR_CODE, 600, 600);
            ivQrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "فشل في توليد رمز QR", Toast.LENGTH_SHORT).show();
        }
    }
}
