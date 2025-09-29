package com.example.lectormanga;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.lectormanga.adapter.ChapterAdapter;
import com.example.lectormanga.api.MangaDexApi;
import com.example.lectormanga.database.MangaDAO;
import com.example.lectormanga.model.Chapter;
import com.example.lectormanga.model.Manga;

import java.util.ArrayList;
import java.util.List;

public class ChaptersActivity extends AppCompatActivity implements ChapterAdapter.OnChapterClickListener {

    private RecyclerView recyclerViewChapters;
    private ChapterAdapter chapterAdapter;
    private List<Chapter> chapterList;
    private ImageView mangaCover;
    private TextView mangaTitle, mangaDescription, statusText;
    private Manga selectedManga;
    private MangaDexApi mangaDexApi;
    private MangaDAO mangaDAO;
    private boolean fromOffline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapters);

        mangaDexApi = new MangaDexApi();
        mangaDAO = new MangaDAO(this);

        initViews();
        getMangaData();
        setupRecyclerView();
        loadRealChapters();
    }

    private void initViews() {
        recyclerViewChapters = findViewById(R.id.recyclerViewChapters);
        mangaCover = findViewById(R.id.mangaCover);
        mangaTitle = findViewById(R.id.mangaTitle);
        mangaDescription = findViewById(R.id.mangaDescription);
        statusText = findViewById(R.id.tv_estado);
    }

    private void getMangaData() {
        Intent intent = getIntent();
        if (intent != null) {
            selectedManga = new Manga();
            selectedManga.setId(intent.getStringExtra("manga_id"));
            selectedManga.setTitle(intent.getStringExtra("manga_title"));
            selectedManga.setDescription(intent.getStringExtra("manga_description"));
            selectedManga.setCoverUrl(intent.getStringExtra("manga_cover"));
            fromOffline = intent.getBooleanExtra("from_offline", false);

            displayMangaInfo();
        } else {
            Toast.makeText(this, "Error: No hay datos del manga", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayMangaInfo() {
        if (selectedManga != null) {
            mangaTitle.setText(selectedManga.getTitle());
            mangaDescription.setText(selectedManga.getDescription());

            if (selectedManga.getCoverUrl() != null && !selectedManga.getCoverUrl().isEmpty()) {
                Glide.with(this)
                        .load(selectedManga.getCoverUrl())
                        .placeholder(R.drawable.placeholder_manga)
                        .error(R.drawable.placeholder_manga)
                        .into(mangaCover);
            } else {
                mangaCover.setImageResource(R.drawable.placeholder_manga);
            }
        }
    }

    private void setupRecyclerView() {
        chapterList = new ArrayList<>();
        chapterAdapter = new ChapterAdapter(chapterList, this, this);

        recyclerViewChapters.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChapters.setAdapter(chapterAdapter);
    }

    private void loadRealChapters() {
        if (selectedManga == null || selectedManga.getId() == null) {
            statusText.setText("❌ Error: ID no válido");
            return;
        }

        if (fromOffline) {
            loadOfflineChapters();
        } else {
            loadOnlineChapters();
        }
    }

    private void loadOfflineChapters() {
        statusText.setText("💾 Cargando offline...");

        new Thread(() -> {
            List<Chapter> offlineChapters = mangaDAO.getChaptersByMangaId(selectedManga.getId());

            runOnUiThread(() -> {
                if (offlineChapters.isEmpty()) {
                    statusText.setText("❌ No hay capítulos offline");
                } else {
                    chapterList.clear();
                    chapterList.addAll(offlineChapters);
                    chapterAdapter.notifyDataSetChanged();
                    statusText.setText("✅ " + offlineChapters.size() + " capítulos (OFFLINE)");
                }
            });
        }).start();
    }

    private void loadOnlineChapters() {
        statusText.setText("🔄 Cargando capítulos...");

        mangaDexApi.getChapters(selectedManga.getId(), 100, new MangaDexApi.ChapterCallback() {
            @Override
            public void onSuccess(List<Chapter> chapters) {
                runOnUiThread(() -> {
                    chapterList.clear();
                    chapterList.addAll(chapters);
                    chapterAdapter.notifyDataSetChanged();

                    if (chapters.isEmpty()) {
                        statusText.setText("❌ Sin capítulos");
                    } else {
                        statusText.setText("✅ " + chapters.size() + " capítulos");
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("❌ Error");
                    Toast.makeText(ChaptersActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onChapterClick(Chapter chapter) {
        Intent intent = new Intent(ChaptersActivity.this, ReaderActivity.class);
        intent.putExtra("chapter_id", chapter.getId());
        intent.putExtra("chapter_title", chapter.getTitle());
        intent.putExtra("chapter_number", chapter.getChapterNumber());
        intent.putExtra("manga_id", selectedManga.getId());
        intent.putExtra("manga_title", selectedManga.getTitle());
        intent.putExtra("manga_description", selectedManga.getDescription());
        intent.putExtra("manga_cover", selectedManga.getCoverUrl());
        intent.putExtra("from_offline", fromOffline);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mangaDexApi != null) {
            mangaDexApi.cleanup();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}