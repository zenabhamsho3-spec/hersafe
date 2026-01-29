package com.example.hersafe.ui.features.journey;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hersafe.R;
import com.example.hersafe.data.local.AppDatabase;
import com.example.hersafe.data.local.dao.ContactDao;
import com.example.hersafe.data.local.entities.Contact;
import com.example.hersafe.data.remote.DirectionsApiService;
import com.example.hersafe.data.remote.DirectionsResponse;
import com.example.hersafe.data.remote.RetrofitClient;
import com.example.hersafe.utils.MapService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SafeJourneyActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private EditText etDestination;
    private Spinner spinnerContacts;
    private View groupSetup, groupActiveTrip;
    private android.widget.TextView tvEta;

    private ContactDao contactDao;
    private FusedLocationProviderClient fusedLocationClient;
    private List<Contact> contactList = new ArrayList<>();
    private Contact selectedContact;
    private Location startLocation;
    
    private GoogleMap mMap;
    private android.widget.RadioGroup rgModes;
    private android.widget.TextView tvDuration;
    private android.widget.TextView tvLiveEta;
    private android.widget.TextView tvStatus; // The "Tracking your path" text
    private android.widget.TextView tvTrackingStatus;
    private View cardActiveTrip;
    private LocationCallback locationCallback;
    private LatLng destinationLatLng;
    private View fabRestore;
    private int initialApiDurationSeconds = 0; // Store API duration

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_safe_journey);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initData();
        checkPermissions();
        
        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    
    

    private void initViews() {
        etDestination = findViewById(R.id.etDestination);
        spinnerContacts = findViewById(R.id.spinnerContacts);
        groupSetup = findViewById(R.id.groupSetup);
        groupActiveTrip = findViewById(R.id.groupActiveTrip);
        
        // Aliases for toggle logic
        cardActiveTrip = findViewById(R.id.cardActiveTrip);
        tvEta = findViewById(R.id.tvHeaderSub); // The subtitle "Journey in progress..."
        
        rgModes = findViewById(R.id.rgModes);
        tvDuration = findViewById(R.id.tvDuration);
        tvLiveEta = findViewById(R.id.tvLiveEta);
        tvTrackingStatus = findViewById(R.id.tvTrackingStatus); 
        fabRestore = findViewById(R.id.fabRestore);
        
        // 1. Click on "Tracking your path" text -> HIDE the card, SHOW the FAB
        tvTrackingStatus.setOnClickListener(v -> {
            cardActiveTrip.setVisibility(View.GONE);
            fabRestore.setVisibility(View.VISIBLE);
        });
        
        // 2. Click on FAB -> SHOW the card, HIDE the FAB
        fabRestore.setOnClickListener(v -> {
            fabRestore.setVisibility(View.GONE);
            cardActiveTrip.setVisibility(View.VISIBLE);
        });

        // 3. Keep Header Subtitle as alternative toggle
        tvEta.setOnClickListener(v -> {
            if (cardActiveTrip.getVisibility() == View.VISIBLE) {
                cardActiveTrip.setVisibility(View.GONE);
                fabRestore.setVisibility(View.VISIBLE);
            } else {
                cardActiveTrip.setVisibility(View.VISIBLE);
                fabRestore.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.btnStartTrip).setOnClickListener(v -> startJourney());
        findViewById(R.id.btnArrivedSafe).setOnClickListener(v -> endJourney(true));
        findViewById(R.id.btnDanger).setOnClickListener(v -> endJourney(false));

        // Search Button Logic
        findViewById(R.id.btnSearch).setOnClickListener(v -> {
            String query = etDestination.getText().toString().trim();
            if (!query.isEmpty()) {
                geocodeAndDrawRoute(null, query); // null start means just search dist
            }
        });

        // Mode Change Listener
        rgModes.setOnCheckedChangeListener((group, checkedId) -> {
             String query = etDestination.getText().toString().trim();
             if (!query.isEmpty()) {
                 // Trigger route update if destination is set
                 // We need to know start location. If we have it (from previous search), use it.
                 // For now, if we don't have startLocation, we just search destination again.
                 if (startLocation != null) {
                     geocodeAndDrawRoute(startLocation, query);
                 } else {
                     geocodeAndDrawRoute(null, query);
                 }
             }
        });

        // Search on Enter Key
        etDestination.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String query = etDestination.getText().toString().trim();
                if (!query.isEmpty()) {
                    geocodeAndDrawRoute(null, query);
                }
                return true;
            }
            return false;
        });
    }

    private void initData() {
        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize Location Callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (destinationLatLng != null) {
                        calculateAndShowEta(location, destinationLatLng);
                    }
                }
            }
        };

        // Initialize Database
        AppDatabase db = AppDatabase.getInstance(this);
        contactDao = db.contactDao();

        // Load Contacts into Spinner
        loadContacts();
    }

    private void loadContacts() {
        new Thread(() -> {
            contactList = contactDao.getAllContacts();
            runOnUiThread(() -> {
                List<String> contactNames = new ArrayList<>();
                for (Contact c : contactList) {
                    contactNames.add(c.getName());
                }
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, contactNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerContacts.setAdapter(adapter);

                if (contactList.isEmpty()) {
                     Toast.makeText(SafeJourneyActivity.this, "يرجى إضافة جهات اتصال للطوارئ أولاً", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.SEND_SMS
            }, PERMISSION_REQUEST_CODE);
        } else {
            // Permissions already granted
            enableMyLocation();
        }
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
             if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        this.startLocation = location;
                        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 15));
                    }
                });
             }
        }
    }

    private boolean isJourneyActive = false;

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();

        // 1. Map Click Listener for Destination
        mMap.setOnMapClickListener(latLng -> {
            if (isJourneyActive) return; // Prevent changing destination during active journey

            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title("الوجهة المحددة"));
            
            // Reverse Geocode to show address in EditText
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String address = addresses.get(0).getAddressLine(0);
                    etDestination.setText(address);
                } else {
                    etDestination.setText(String.format(Locale.US, "%.5f, %.5f", latLng.latitude, latLng.longitude));
                }
            } catch (IOException e) {
                etDestination.setText(String.format(Locale.US, "%.5f, %.5f", latLng.latitude, latLng.longitude));
            }
        });
    }

    // ... (enableMyLocation remains same)

    @SuppressLint("MissingPermission")
    private void startJourney() {
        if (isJourneyActive) return; // Prevent double start
        isJourneyActive = true;

        String destinationStr = etDestination.getText().toString().trim();
        if (destinationStr.isEmpty()) {
            etDestination.setError("يرجى تحديد وجهة الوصول");
            isJourneyActive = false; // Reset if failed
            return;
        }

        if (contactList.isEmpty()) {
            Toast.makeText(this, "لا توجد جهات اتصال مختارة", Toast.LENGTH_SHORT).show();
            isJourneyActive = false;
            return;
        }

        selectedContact = contactList.get(spinnerContacts.getSelectedItemPosition());

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            this.startLocation = location;
            if (location == null) {
                Toast.makeText(this, "تعذر تحديد موقعك الحالي", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // We need to find the destination LatLng again to draw route from HERE to THERE
            // If user clicked map, we could store that LatLng, but geocoding string is safer for now
            geocodeAndDrawRoute(location, destinationStr);
            
            sendStartSms(destinationStr, location);
            showActiveUI();
            
            // Set initial ETA if we have start location and destination
            if (destinationLatLng != null) {
                // Initialize Dynamic Tracking Variables for Sync
                float[] results = new float[1];
                Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                        destinationLatLng.latitude, destinationLatLng.longitude, results);
                this.initialStraightDistance = results[0];
                this.initialApiDuration = this.initialApiDurationSeconds;

                calculateAndShowEta(location, destinationLatLng);
            }
            
            startLocationUpdates();
        });
    }

    private void geocodeAndDrawRoute(Location startLocation, String destinationStr) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(destinationStr, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address destAddress = addresses.get(0);
                LatLng endLatLng = new LatLng(destAddress.getLatitude(), destAddress.getLongitude());
                this.destinationLatLng = endLatLng; // Store for live updates
                
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(endLatLng).title("الوجهة"));
                
                if (startLocation != null) {
                    LatLng startLatLng = new LatLng(startLocation.getLatitude(), startLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(startLatLng).title("أنا"));
                    fetchRoute(startLatLng, endLatLng);
                } else {
                    // Just searching, move camera
                     mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(endLatLng, 15));
                }

            } else {
                Toast.makeText(this, "لم يتم العثور على الوجهة", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "خطأ في الاتصال بالخرائط", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void fetchRoute(LatLng start, LatLng end) {
        String origin = start.latitude + "," + start.longitude;
        String dest = end.latitude + "," + end.longitude;
        String apiKey = getApiKey();
        
        if (apiKey == null) return;

        String mode = getSelectedMode();

        // Request alternatives=true to get multiple routes
        RetrofitClient.getService().getDirections(origin, dest, apiKey, true, mode).enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().routes.isEmpty()) {
                    
                    // Find the shortest route
                    List<DirectionsResponse.Route> routes = response.body().routes;
                    DirectionsResponse.Route bestRoute = null;
                    int minDistance = Integer.MAX_VALUE;
                    int bestDuration = 0;

                    for (DirectionsResponse.Route route : routes) {
                        int currentDistance = 0;
                        int currentDuration = 0;
                        
                        if (route.legs != null) {
                            for (DirectionsResponse.Leg leg : route.legs) {
                                if (leg.distance != null) currentDistance += leg.distance.value;
                                if (leg.duration != null) currentDuration += leg.duration.value;
                            }
                        }
                        
                        // Pick shortest distance. 
                        // Note: For "transit", Google usually returns the best route first, 
                        // but logic to pick shortest distance is fine for now.
                        if (bestRoute == null || currentDistance < minDistance) {
                            minDistance = currentDistance;
                            bestDuration = currentDuration;
                            bestRoute = route;
                        }
                    }
                    
                    // Store for Active Trip consistency
                    initialApiDurationSeconds = bestDuration;

                    if (bestRoute != null && bestRoute.overviewPolyline != null) {
                        String encodedPath = bestRoute.overviewPolyline.points;
                        List<LatLng> path = MapService.decodePolyline(encodedPath);
                        
                        mMap.addPolyline(new PolylineOptions().addAll(path).color(ContextCompat.getColor(SafeJourneyActivity.this, R.color.route_dark_pink)).width(10));
                        
                        // Zoom to fit
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(start);
                        builder.include(end);
                        for(LatLng p : path) builder.include(p);
                        try {
                             mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
                        } catch (Exception e) {
                             mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(start, 15));
                        }
                        
                        // Update UI with Duration
                        updateDurationUI(bestDuration, mode);
                    }
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Log.e("SafeJourney", "Route fetch failed", t);
                Toast.makeText(SafeJourneyActivity.this, "فشل تحميل المسار", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getSelectedMode() {
        int id = rgModes.getCheckedRadioButtonId();
        if (id == R.id.rbWalking) return "walking";
        if (id == R.id.rbTransit) return "transit";
        return "driving";
    }

    private void updateDurationUI(int seconds, String mode) {
        int minutes = seconds / 60;
        String modeName = "سيارة";
        if (mode.equals("walking")) modeName = "مشي";
        else if (mode.equals("transit")) modeName = "مواصلات";

        String durationText = "المدة المتوقعة: " + minutes + " دقيقة (" + modeName + ")";
        tvDuration.setText(durationText);
        tvDuration.setVisibility(View.VISIBLE);
    }

    private String getApiKey() {
        try {
            ApplicationInfo app = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = app.metaData;
            return bundle.getString("com.google.android.geo.API_KEY");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendStartSms(String destination, Location location) {
        String locLink = (location != null) ? 
            "http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude() : 
            "Unknown";
        
        String message = "أنا أبدأ رحلتي الآمنة الآن متوجهة إلى: " + destination + 
                         "\nموقعي الحالي: " + locLink;
        
        sendSms(selectedContact.getPhone(), message);
    }

    private void endJourney(boolean arrivedSafe) {
        if (arrivedSafe) {
            sendSms(selectedContact.getPhone(), "لقد وصلت بسلام لطمأنتك. شكراً لك!");
            Toast.makeText(this, "تم إرسال رسالة الوصول بسلام", Toast.LENGTH_SHORT).show();
        } else {
            // Danger
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                String locLink = (location != null) ? 
                    "http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude() : 
                    "Unknown";
                sendSms(selectedContact.getPhone(), "خطر!! أنا في حالة طارئة في رحلتي. موقعي: " + locLink);
            });
            Toast.makeText(this, "تم إرسال تنبيه خطر!", Toast.LENGTH_SHORT).show();
        }
        showSetupUI();
        if (mMap != null) mMap.clear();
        isJourneyActive = false; // Allow map changes again
    }

    private void sendSms(String phone, String message) {
        try {
            SmsManager smsManager = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ?
                    getSystemService(SmsManager.class) : SmsManager.getDefault();
            smsManager.sendTextMessage(formatPhoneNumber(phone), null, message, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "فشل في إرسال الرسالة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String formatPhoneNumber(String phone) {
        if (phone.startsWith("0")) phone = phone.substring(1);
        if (!phone.startsWith("+963")) phone = "+963" + phone;
        return phone;
    }

    private void showActiveUI() {
        groupSetup.setVisibility(View.GONE);
        groupActiveTrip.setVisibility(View.VISIBLE);
        tvEta.setText("الرحلة جارية... يتم مشاركة موقعك");
    }

    private void showSetupUI() {
        groupSetup.setVisibility(View.VISIBLE);
        groupActiveTrip.setVisibility(View.GONE);
        tvEta.setText(R.string.journey_subtitle);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               enableMyLocation();
            } else {
                Toast.makeText(this, "يجب منح الأذونات للعمل بشكل صحيح", Toast.LENGTH_SHORT).show();
            }
        }
    
    }

    // Variables for Dynamic ETA Calculation
    private float initialStraightDistance = -1;
    private int initialApiDuration = -1;

    private void calculateAndShowEta(Location current, LatLng dest) {
        float[] results = new float[1];
        Location.distanceBetween(current.getLatitude(), current.getLongitude(),
                dest.latitude, dest.longitude, results);
        float currentStraightDistance = results[0];

        int seconds;

        // If we have initial data from API, use linear interpolation for smoother/matching updates
        if (initialStraightDistance > 0 && initialApiDuration > 0) {
             // Ratio of distance remaining
             float fraction = currentStraightDistance / initialStraightDistance;
             if (fraction > 1.0f) fraction = 1.0f; // Should not happen usually unless moving away
             
             seconds = (int) (fraction * initialApiDuration);
        } else {
            // Fallback to simple physics calculation if API failed or not ready
            float speed = 11.1f; // Car ~40km/h
            String mode = getSelectedMode();
            if (mode.equals("walking")) speed = 1.4f; // ~5km/h
            else if (mode.equals("transit")) speed = 5.5f; // ~20km/h
            
            seconds = (int) (currentStraightDistance / speed);
        }
        
        // Prevent negative or zero if very close
        if (seconds < 60 && currentStraightDistance > 50) seconds = 60; // Min 1 min if not "arrived"

        int minutes = seconds / 60;
        
        String modeName = "سيارة";
        String mode = getSelectedMode();
        if (mode.equals("walking")) modeName = "مشي";
        else if (mode.equals("transit")) modeName = "مواصلات";
        
        // If minutes > 60, show hours
        String timeStr;
        if (minutes >= 60) {
            int hours = minutes / 60;
            int mins = minutes % 60;
             timeStr = hours + " ساعة و " + mins + " دقيقة";
        } else {
            timeStr = minutes + " دقيقة";
        }

        tvLiveEta.setText("الوصول المتوقع: " + timeStr + " (" + modeName + ")");
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .build();
        
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, android.os.Looper.getMainLooper());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}