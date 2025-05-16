package com.example.torrent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TorrentSearchActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private EditText etSearchTerm;
    private Button btnSearch;
    private RadioGroup radioGroupSort;
    private RecyclerView rvResults;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String userId;
    private TorrentAdapter adapter;
    private List<Torrent> torrentList;
    
    private static final int RESULTS_LIMIT = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_torrent_search);
        

        toolbar = findViewById(R.id.toolbar);
        etSearchTerm = findViewById(R.id.etSearchTerm);
        btnSearch = findViewById(R.id.btnSearch);
        radioGroupSort = findViewById(R.id.radioGroupSort);
        rvResults = findViewById(R.id.rvResults);
        

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Torrent keresés");
        toolbar.setNavigationOnClickListener(v -> finish());
        

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }
        
        userId = auth.getCurrentUser().getUid();
        

        torrentList = new ArrayList<>();
        adapter = new TorrentAdapter(torrentList);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);
        

        setupRecyclerViewClickListener();
        

        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(
                this, R.anim.layout_animation_fall_down);
        rvResults.setLayoutAnimation(animation);
        

        btnSearch.setOnClickListener(v -> performSearch());
    }
    
    private void setupRecyclerViewClickListener() {

        adapter.setOnItemClickListener((position, torrent) -> {
            openTorrentDetails(torrent);
        });
        

        ItemClickSupport.addTo(rvResults).setOnItemClickListener(
                (recyclerView, position, v) -> {
                    if (position >= 0 && position < torrentList.size()) {
                        Torrent torrent = torrentList.get(position);
                        openTorrentDetails(torrent);
                    }
                });
    }
    
    private void openTorrentDetails(Torrent torrent) {
        Intent intent = new Intent(this, TorrentDetailsActivity.class);
        intent.putExtra("torrentId", torrent.getId());
        intent.putExtra("torrentName", torrent.getName());
        intent.putExtra("torrentCode", torrent.getCode());
        intent.putExtra("torrentProgress", torrent.getProgress());
        startActivity(intent);
    }
    
    private void performSearch() {
        String searchTerm = etSearchTerm.getText().toString().trim();
        

        Query query = db.collection("users").document(userId).collection("torrents");
        

        if (!searchTerm.isEmpty()) {
            query = query.whereGreaterThanOrEqualTo("name", searchTerm)
                    .whereLessThanOrEqualTo("name", searchTerm + "\uf8ff");
        }
        

        int checkedRadioButtonId = radioGroupSort.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.radioName) {
            query = query.orderBy("name", Query.Direction.ASCENDING);
        } else if (checkedRadioButtonId == R.id.radioProgress) {
            query = query.orderBy("progress", Query.Direction.DESCENDING);
        }
        

        query = query.limit(RESULTS_LIMIT);
        

        query.get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                torrentList.clear();
                
                if (queryDocumentSnapshots.isEmpty()) {
                    Toast.makeText(this, "Nincs találat", Toast.LENGTH_SHORT).show();
                } else {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Torrent torrent = document.toObject(Torrent.class);
                        torrent.setId(document.getId());
                        torrentList.add(torrent);
                    }
                }
                
                adapter.updateTorrentList(torrentList);
                rvResults.scheduleLayoutAnimation();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Hiba a keresés közben: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
} 