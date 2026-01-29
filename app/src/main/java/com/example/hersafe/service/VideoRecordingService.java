package com.example.hersafe.service;

import android.Manifest;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;

import com.example.hersafe.R;
import com.google.common.util.concurrent.ListenableFuture;

import com.example.hersafe.data.local.AppDatabase;
import com.example.hersafe.data.local.dao.ContactDao;
import com.example.hersafe.data.local.entities.Contact;
import com.example.hersafe.data.local.entities.Contact;
import java.util.List;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import org.json.JSONObject;
import org.json.JSONException;

import com.example.hersafe.data.remote.RetrofitClient;
import com.example.hersafe.data.remote.TelegramApiService;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoRecordingService extends LifecycleService {

    private static final String TAG = "VideoRecordingService";
    private static final String CHANNEL_ID = "VideoRecordingChannel";
    private static final int NOTIFICATION_ID = 101;

    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private ExecutorService serviceExecutor;
    private boolean isRecording = false;
    
    public static boolean isServiceRunning = false;
    
    private ContactDao contactDao;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;
        serviceExecutor = Executors.newSingleThreadExecutor();
        createNotificationChannel();
        
        // Initialize Database
        AppDatabase db = AppDatabase.getInstance(this);
        contactDao = db.contactDao();
        
        // Initialize Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            String action = intent.getAction();
            if ("STOP".equals(action)) {
                stopRecording();
                stopSelf();
            } else {
                startForeground(NOTIFICATION_ID, createNotification());
                startCamera();
            }
        }
        return START_NOT_STICKY;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        QualitySelector qualitySelector = QualitySelector.from(Quality.HD);
        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build();
        videoCapture = VideoCapture.withOutput(recorder);

        // We don't bind a Preview use case for "stealth" mode, 
        // but CameraX often requires a surface. 
        // However, for VideoCapture only, we might get away with it or might need a dummy preview.
        // Let's try binding only VideoCapture first.
        
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, videoCapture);
            startRecording();
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            Toast.makeText(this, "Failed to bind camera use cases", Toast.LENGTH_SHORT).show();
        }
    }

    private void startRecording() {
        if (videoCapture == null) {
            Log.e(TAG, "VideoCapture is null");
            return;
        }

        // 1. Cancel previous recording logic if overlapping
        if (recording != null) {
            recording.stop();
            recording = null;
        }

        // 2. Notify UI that recording started
        isRecording = true;
        sendBroadcastState(true);

        String name = "VID_" + System.currentTimeMillis() + ".mp4";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/HerSafe");
        }

        MediaStoreOutputOptions mediaStoreOutputOptions = new MediaStoreOutputOptions.Builder(
                getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Audio permission missing in service");
            return;
        }

        recording = videoCapture.getOutput()
                .prepareRecording(this, mediaStoreOutputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), recordEvent -> {
                    if (recordEvent instanceof VideoRecordEvent.Start) {
                        Log.d(TAG, "Recording started for new segment.");
                        // Stop automatically after 60 seconds to create segment
                         handler.postDelayed(this::stopCurrentSegment, 60000); 
                         
                    } else if (recordEvent instanceof VideoRecordEvent.Finalize) {
                         VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) recordEvent;
                        if (!finalizeEvent.hasError()) {
                            Log.d(TAG, "Segment saved successfully.");
                            // Upload to Telegram
                            uploadVideoToTelegram(finalizeEvent.getOutputResults().getOutputUri());
                        } else {
                            Log.e(TAG, "Video capture ends with error: " + finalizeEvent.getError());
                        }
                        
                        // Clear current recording reference
                        recording = null;
                        
                        // LOOP LOGIC: If service is still running and we want to record, start next segment
                        if (isServiceRunning && isRecording) {
                             Log.d(TAG, "Starting next segment...");
                             startRecording();
                        }
                    }
                });
    }

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    
    // Helper to stop just the current recording to trigger finalizing
    private void stopCurrentSegment() {
        if (recording != null) {
            recording.stop();
        }
    }

    private void stopRecording() {
        // Remove callbacks
        handler.removeCallbacksAndMessages(null);
        
        isRecording = false;
        sendBroadcastState(false);
        
        if (recording != null) {
            recording.stop();
            recording = null;
        }
    }
    
    private void sendBroadcastState(boolean isRec) {
        Intent intent = new Intent("com.example.hersafe.VIDEO_STATE_CHANGED");
        intent.putExtra("isRecording", isRec);
        // Using standard broadcast as we are in different components (Activity vs Service)
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Background Recording",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("HerSafe")
                .setContentText("جاري تسجيل الفيديو والرفع للسحابة...")
                .setSmallIcon(R.drawable.ic_camera_alt)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
    
    private void uploadVideoToTelegram(Uri videoUri) {
        new Thread(() -> {
            try {
                // 1. Fetch Data for JSON Report
                // Default location (Damascus)
                double lat = 33.5138; 
                double lng = 36.2765;
                
                // Try to get real location
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                     CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
                     try {
                         android.location.Location location = com.google.android.gms.tasks.Tasks.await(
                                 fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                         );
                         if (location != null) {
                             lat = location.getLatitude();
                             lng = location.getLongitude();
                         }
                     } catch (Exception e) {
                         Log.e(TAG, "Failed to get location", e);
                     }
                }
                
                // Get Contact Info
                String contactName = "Unknown";
                String contactPhone = "Unknown";
                List<Contact> contacts = contactDao.getAllContacts();
                if (contacts != null && !contacts.isEmpty()) {
                    Contact c = contacts.get(0);
                    contactName = c.getName();
                    contactPhone = c.getPhone();
                }

                // Construct JSON
                String videoId = "VID-" + System.currentTimeMillis(); // Simple Unique ID
                String mapLink = "https://maps.google.com/?q=" + lat + "," + lng;
                
                JSONObject json = new JSONObject();
                json.put("id", videoId);
                json.put("timestamp", System.currentTimeMillis());
                
                JSONObject locObj = new JSONObject();
                locObj.put("latitude", lat);
                locObj.put("longitude", lng);
                locObj.put("google_maps", mapLink);
                json.put("location", locObj);
                
                JSONObject contactObj = new JSONObject();
                contactObj.put("name", contactName);
                contactObj.put("phone", contactPhone);
                json.put("contact", contactObj);
                
                // Pretty print JSON + Append clean link for clickability
                String caption = json.toString(4) + "\n\n📍 Location: " + mapLink;

                // 2. Create a temporary file in cache
                File cacheFile = new File(getCacheDir(), "upload_temp_" + System.currentTimeMillis() + ".mp4");
                
                // 3. Copy data from Uri to Cache File using Input/Output streams
                try (java.io.InputStream inputStream = getContentResolver().openInputStream(videoUri);
                     java.io.FileOutputStream outputStream = new java.io.FileOutputStream(cacheFile)) {
                    
                    if (inputStream == null) {
                        Log.e(TAG, "InputStream is null");
                        return;
                    }

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to copy file to cache", e);
                    return;
                }

                // 4. Upload the Cache File with Caption
                String chatId = "5244567403"; 
                Log.d(TAG, "Uploading filtered video from cache: " + cacheFile.getAbsolutePath());

                RequestBody requestFile = RequestBody.create(MediaType.parse("video/mp4"), cacheFile);
                MultipartBody.Part body = MultipartBody.Part.createFormData("video", cacheFile.getName(), requestFile);
                RequestBody chatIdBody = RequestBody.create(MediaType.parse("text/plain"), chatId);
                RequestBody captionBody = RequestBody.create(MediaType.parse("text/plain"), caption);

                RetrofitClient.getTelegramService().sendVideo(chatIdBody, captionBody, body).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Video uploaded successfully to Telegram");
                            Toast.makeText(VideoRecordingService.this, "تم رفع الفيديو بنجاح!", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Upload failed: " + response.code());
                            Toast.makeText(VideoRecordingService.this, "فشل الرفع: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                        // Cleanup
                        if (cacheFile.exists()) cacheFile.delete();
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e(TAG, "Upload error", t);
                        Toast.makeText(VideoRecordingService.this, "خطأ في الاتصال: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                         // Cleanup
                        if (cacheFile.exists()) cacheFile.delete();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error preparing upload", e);
            }
        }).start();
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        try (Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            Log.e(TAG, "getRealPathFromURI Exception", e);
        }
        return null; // Should fall back to logic if basic query fails (e.g. Scoped Storage)
    }

    
    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
        stopRecording();
        serviceExecutor.shutdown();
    }
}
