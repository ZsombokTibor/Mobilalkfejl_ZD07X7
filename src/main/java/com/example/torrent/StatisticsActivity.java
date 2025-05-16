package com.example.torrent;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity implements LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1002;
    
    private MaterialToolbar toolbar;
    private TextView tvTotalTorrents;
    private TextView tvCompletedTorrents;
    private TextView tvAverageProgress;
    private TextView tvLocation;
    private Button btnExportStats;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String userId;
    
    private LocationManager locationManager;
    private String currentLocation = "Ismeretlen helyszín";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        

        toolbar = findViewById(R.id.toolbar);
        tvTotalTorrents = findViewById(R.id.tvTotalTorrents);
        tvCompletedTorrents = findViewById(R.id.tvCompletedTorrents);
        tvAverageProgress = findViewById(R.id.tvAverageProgress);
        tvLocation = findViewById(R.id.tvLocation);
        btnExportStats = findViewById(R.id.btnExportStats);
        

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Torrent statisztikák");
        toolbar.setNavigationOnClickListener(v -> finish());
        

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }
        
        userId = auth.getCurrentUser().getUid();
        

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        tvTotalTorrents.startAnimation(fadeIn);
        tvCompletedTorrents.startAnimation(fadeIn);
        tvAverageProgress.startAnimation(fadeIn);
        tvLocation.startAnimation(fadeIn);
        

        loadStatistics();
        

        initLocation();
        

        btnExportStats.setOnClickListener(v -> exportStatistics());
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        

        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }
    
    private void loadStatistics() {
        db.collection("users").document(userId).collection("torrents")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int totalTorrents = queryDocumentSnapshots.size();
                int completedTorrents = 0;
                int totalProgress = 0;
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Torrent torrent = document.toObject(Torrent.class);
                    totalProgress += torrent.getProgress();
                    
                    if (torrent.getProgress() == 100) {
                        completedTorrents++;
                    }
                }
                
                float averageProgress = totalTorrents > 0 ? (float) totalProgress / totalTorrents : 0;
                

                tvTotalTorrents.setText("Összes torrent: " + totalTorrents);
                tvCompletedTorrents.setText("Befejezett torrentek: " + completedTorrents);
                tvAverageProgress.setText("Átlagos haladás: " + String.format("%.1f%%", averageProgress));
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Hiba a statisztikák betöltése közben: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void initLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        
        try {

            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            
            if (gpsEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            } else if (networkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            } else {
                tvLocation.setText("Helymeghatározás nem elérhető");
            }
        } catch (Exception e) {
            Log.e("StatisticsActivity", "Error initializing location", e);
            tvLocation.setText("Helymeghatározás hiba: " + e.getMessage());
        }
    }
    
    private void exportStatistics() {

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
            return;
        }
        

        db.collection("users").document(userId).collection("torrents")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int totalTorrents = queryDocumentSnapshots.size();
                int completedTorrents = 0;
                int totalProgress = 0;
                StringBuilder torrentList = new StringBuilder();
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Torrent torrent = document.toObject(Torrent.class);
                    totalProgress += torrent.getProgress();
                    
                    if (torrent.getProgress() == 100) {
                        completedTorrents++;
                    }
                    
                    torrentList.append("- ").append(torrent.getName())
                            .append(" (").append(torrent.getProgress()).append("%)\n");
                }
                
                float averageProgress = totalTorrents > 0 ? (float) totalProgress / totalTorrents : 0;
                

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                StringBuilder report = new StringBuilder();
                report.append("Torrent Statisztikák - ").append(timeStamp).append("\n\n");
                report.append("Helyszín: ").append(currentLocation).append("\n\n");
                report.append("Összes torrent: ").append(totalTorrents).append("\n");
                report.append("Befejezett torrentek: ").append(completedTorrents).append("\n");
                report.append("Átlagos haladás: ").append(String.format("%.1f%%", averageProgress)).append("\n\n");
                report.append("Torrentek listája:\n").append(torrentList);
                

                try {
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File file = new File(downloadsDir, "TorrentStats_" + timeStamp + ".txt");
                    
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(report.toString().getBytes());
                    fos.close();
                    
                    Toast.makeText(this, "Statisztikák elmentve: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    Toast.makeText(this, "Hiba a fájl mentése közben: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Hiba a statisztikák exportálása közben: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initLocation();
            } else {
                tvLocation.setText("Helymeghatározás engedély megtagadva");
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportStatistics();
            } else {
                Toast.makeText(this, "Tárhely írás engedély megtagadva", Toast.LENGTH_SHORT).show();
            }
        }
    }
    

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            currentLocation = "Szélesség: " + location.getLatitude() + ", Hosszúság: " + location.getLongitude();
            tvLocation.setText("Helyszín: " + currentLocation);
            

            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
} 