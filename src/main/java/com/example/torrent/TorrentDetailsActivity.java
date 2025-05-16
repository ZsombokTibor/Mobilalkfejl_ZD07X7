package com.example.torrent;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class TorrentDetailsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private EditText etTorrentName;
    private EditText etTorrentCode;
    private ProgressBar progressBar;
    private TextView tvProgress;
    private Button btnSave;
    private Button btnDelete;
    private Button btnSchedule;

    private String torrentId;
    private String userId;
    private FirebaseFirestore db;
    private FirebaseAuth auth;


    private static final String CHANNEL_ID = "torrent_notification_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_torrent_details);


        toolbar = findViewById(R.id.toolbar);
        etTorrentName = findViewById(R.id.etTorrentName);
        etTorrentCode = findViewById(R.id.etTorrentCode);
        progressBar = findViewById(R.id.progressBar);
        tvProgress = findViewById(R.id.tvProgress);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnSchedule = findViewById(R.id.btnSchedule);


        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Torrent részletek");
        toolbar.setNavigationOnClickListener(v -> finish());


        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        userId = auth.getCurrentUser().getUid();


        createNotificationChannel();


        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("torrentId")) {
            torrentId = intent.getStringExtra("torrentId");
            String name = intent.getStringExtra("torrentName");
            String code = intent.getStringExtra("torrentCode");
            int progress = intent.getIntExtra("torrentProgress", 0);


            etTorrentName.setText(name);
            etTorrentCode.setText(code);
            progressBar.setProgress(progress);
            tvProgress.setText(progress + "%");
            

            btnSave.setVisibility(View.GONE);
            

            btnSchedule.setVisibility(View.GONE);
            

            Animation buttonAnimation = AnimationUtils.loadAnimation(this, R.anim.button_scale);
            btnDelete.startAnimation(buttonAnimation);
        } else {

            Toast.makeText(this, "Hiba történt a torrent betöltése közben", Toast.LENGTH_SHORT).show();
            finish();
        }


        btnDelete.setOnClickListener(v -> deleteTorrent());
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private void deleteTorrent() {

        db.collection("users").document(userId)
            .collection("torrents").document(torrentId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(TorrentDetailsActivity.this, "Torrent sikeresen törölve", Toast.LENGTH_SHORT).show();

            })
            .addOnFailureListener(e -> {
                Toast.makeText(TorrentDetailsActivity.this, "Hiba történt a törlés közben: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Torrent értesítések";
            String description = "Értesítések a torrentekkel kapcsolatban";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
} 