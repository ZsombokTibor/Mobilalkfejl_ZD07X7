package com.example.torrent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.app.PendingIntent;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_TORRENT_DETAILS = 1001;
    private static final String TAG = "MainActivity";

    private TextView tvUserEmail;
    private RecyclerView rvTorrents;
    private Button btnLogout;
    private FloatingActionButton fabAddTorrent;
    private MaterialToolbar toolbar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TorrentAdapter torrentAdapter;
    private List<Torrent> torrentList;
    private String userId;
    private ListenerRegistration torrentListener;
    private Handler handler;
    private Random random;
    

    private Map<String, Runnable> progressRunnables = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        tvUserEmail = findViewById(R.id.tvUserEmail);
        btnLogout = findViewById(R.id.btnLogout);
        rvTorrents = findViewById(R.id.rvTorrents);
        fabAddTorrent = findViewById(R.id.fabAddTorrent);
        toolbar = findViewById(R.id.toolbar);


        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Torrent App");
        }


        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        

        handler = new Handler(Looper.getMainLooper());
        random = new Random();


        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }


        userId = currentUser.getUid();


        tvUserEmail.setText(currentUser.getEmail());


        torrentList = new ArrayList<>();
        torrentAdapter = new TorrentAdapter(torrentList);
        rvTorrents.setLayoutManager(new LinearLayoutManager(this));
        rvTorrents.setAdapter(torrentAdapter);
        

        setupRecyclerViewClickListener();
        

        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(
                this, R.anim.layout_animation_fall_down);
        rvTorrents.setLayoutAnimation(animation);


        setupTorrentListener();


        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        fabAddTorrent.setOnClickListener(v -> showAddTorrentDialog());
        
        Log.d(TAG, "MainActivity initialized successfully");
    }

    private void setupRecyclerViewClickListener() {
        Log.d(TAG, "Setting up RecyclerView click listener");

        torrentAdapter = new TorrentAdapter(torrentList);
        torrentAdapter.setOnItemClickListener((position, torrent) -> {
            openTorrentDetails(torrent);
        });
        rvTorrents.setAdapter(torrentAdapter);
        

        ItemClickSupport.addTo(rvTorrents).setOnItemClickListener(
                (recyclerView, position, v) -> {
                    if (position >= 0 && position < torrentList.size()) {
                        Torrent torrent = torrentList.get(position);
                        Log.d(TAG, "Item clicked: " + torrent.getName());
                        openTorrentDetails(torrent);
                    }
                });
    }

    private void openTorrentDetails(Torrent torrent) {
        Log.d(TAG, "Opening details for torrent: " + torrent.getName());
        Intent intent = new Intent(this, TorrentDetailsActivity.class);
        intent.putExtra("torrentId", torrent.getId());
        intent.putExtra("torrentName", torrent.getName());
        intent.putExtra("torrentCode", torrent.getCode());
        intent.putExtra("torrentProgress", torrent.getProgress());
        startActivityForResult(intent, REQUEST_TORRENT_DETAILS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_TORRENT_DETAILS && resultCode == RESULT_OK && data != null) {

            Log.d(TAG, "Returned from TorrentDetailsActivity with OK result");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "Creating options menu");
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Log.d(TAG, "Menu item selected: " + item.getTitle());

        if (id == R.id.action_search) {
            Toast.makeText(this, "Keresés megnyitása...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, TorrentSearchActivity.class));
            return true;
        } else if (id == R.id.action_statistics) {
            Toast.makeText(this, "Statisztikák megnyitása...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, StatisticsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (torrentListener != null) {
            torrentListener.remove();
        }
        

        for (Runnable runnable : progressRunnables.values()) {
            handler.removeCallbacks(runnable);
        }
        progressRunnables.clear();
    }

    private void setupTorrentListener() {
        if (torrentListener != null) {
            torrentListener.remove();
        }
        
        torrentListener = db.collection("users").document(userId).collection("torrents")
            .addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    Toast.makeText(MainActivity.this, "Hiba történt a torrentek betöltése közben: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (snapshots != null) {

                    for (Runnable runnable : progressRunnables.values()) {
                        handler.removeCallbacks(runnable);
                    }
                    progressRunnables.clear();
                    

                    List<String> existingIds = new ArrayList<>();
                    for (Torrent torrent : torrentList) {
                        existingIds.add(torrent.getId());
                    }
                    
                    torrentList.clear();
                    for (QueryDocumentSnapshot document : snapshots) {
                        Torrent torrent = document.toObject(Torrent.class);
                        torrent.setId(document.getId());
                        torrentList.add(torrent);
                        

                        if (torrent.getProgress() < 100) {
                            simulateProgress(torrentList.size() - 1, torrent.getId());
                        }
                    }
                    torrentAdapter.updateTorrentList(torrentList);
                    

                    boolean hasNewItems = false;
                    for (Torrent torrent : torrentList) {
                        if (!existingIds.contains(torrent.getId())) {
                            hasNewItems = true;
                            break;
                        }
                    }
                    
                    if (hasNewItems && !torrentList.isEmpty()) {
                        rvTorrents.scheduleLayoutAnimation();
                    }
                }
            });
    }

    private void simulateProgress(int position, String torrentId) {
        Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {

                if (position < torrentList.size()) {
                    Torrent torrent = torrentList.get(position);
                    int currentProgress = torrent.getProgress();
                    

                    if (currentProgress < 100) {

                        int increment = random.nextInt(5) + 1;
                        int newProgress = Math.min(currentProgress + increment, 100);
                        

                        torrent.setProgress(newProgress);
                        

                        torrentAdapter.updateTorrentProgress(position, newProgress);
                        

                        db.collection("users").document(userId)
                            .collection("torrents").document(torrentId)
                            .update("progress", newProgress);
                        

                        if (newProgress == 100) {
                            sendTorrentCompletedNotification(torrent);
                        }

                        else {

                            long delay = (random.nextInt(2500) + 500);
                            handler.postDelayed(this, delay);
                        }
                    }
                }
            }
        };
        

        progressRunnables.put(torrentId, progressRunnable);
        

        handler.postDelayed(progressRunnable, 1000);
    }

    private void sendTorrentCompletedNotification(Torrent torrent) {

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("torrentName", torrent.getName());
        intent.putExtra("torrentId", torrent.getId());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this, 
            torrent.getId().hashCode(), 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        

        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "Notification sending failed", e);
        }
    }

    private void showAddTorrentDialog() {

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_torrent, null);
        

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        

        EditText etTorrentName = dialogView.findViewById(R.id.etTorrentName);
        EditText etTorrentCode = dialogView.findViewById(R.id.etTorrentCode);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnAdd = dialogView.findViewById(R.id.btnAdd);
        

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnAdd.setOnClickListener(v -> {
            String name = etTorrentName.getText().toString().trim();
            String code = etTorrentCode.getText().toString().trim();
            
            if (name.isEmpty() || code.isEmpty()) {
                Toast.makeText(MainActivity.this, "Kérlek töltsd ki mindkét mezőt!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            addTorrent(name, code);
            dialog.dismiss();
        });
        
        dialog.show();
    }

    private void addTorrent(String name, String code) {

        Torrent torrent = new Torrent(name, code);
        

        db.collection("users").document(userId).collection("torrents")
            .add(torrent)
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(MainActivity.this, "Torrent sikeresen hozzáadva!", Toast.LENGTH_SHORT).show();

            })
            .addOnFailureListener(e -> {
                Toast.makeText(MainActivity.this, "Hiba történt a torrent mentése közben: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}