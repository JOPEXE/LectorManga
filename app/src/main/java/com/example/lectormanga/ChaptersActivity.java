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
    private MangaDexApi mangaDxApi;
    private MangaDAO mangaDAO;
    private boolean fromOffline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapters);

        // Inicializar API y DAO
        mangaDxApi = new MangaDexApi();
        mangaDAO = new MangaDAO(this);

        // Inicializar vistas
        initViews();

        // Obtener datos del manga seleccionado
        getMangaData();

        // Configurar RecyclerView
        setupRecyclerView();

        // Cargar capítulos (online u offline)
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
            statusText.setText("❌ Error: ID de manga no válido");
            return;
        }

        if (fromOffline) {
            loadOfflineChapters();
        } else {
            loadOnlineChapters();
        }
    }

    // ========== CARGA OFFLINE ==========
    private void loadOfflineChapters() {
        statusText.setText("💾 Cargando capítulos guardados...");

        new Thread(() -> {
            List<Chapter> offlineChapters = mangaDAO.getChaptersByMangaId(selectedManga.getId());

            runOnUiThread(() -> {
                if (offlineChapters.isEmpty()) {
                    statusText.setText("❌ No hay capítulos guardados offline\n\nBusca este manga desde la pantalla principal para descargar capítulos");
                    Toast.makeText(this, "No hay capítulos offline. Búscalo en la pantalla principal.", Toast.LENGTH_LONG).show();
                } else {
                    chapterList.clear();
                    chapterList.addAll(offlineChapters);
                    chapterAdapter.notifyDataSetChanged();

                    statusText.setText("✅ " + offlineChapters.size() + " capítulos disponibles (OFFLINE)");
                    Toast.makeText(this, "Capítulos cargados desde SQLite", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // ========== CARGA ONLINE ==========
    private void loadOnlineChapters() {
        statusText.setText("🔄 Cargando capítulos desde MangaDex...");

        mangaDxApi.getChapters(selectedManga.getId(), 50, new MangaDexApi.ChapterCallback() {
            @Override
            public void onSuccess(List<Chapter> chapters) {
                runOnUiThread(() -> {
                    chapterList.clear();
                    chapterList.addAll(chapters);
                    chapterAdapter.notifyDataSetChanged();

                    if (chapters.isEmpty()) {
                        statusText.setText("❌ No se encontraron capítulos en inglés");
                        Toast.makeText(ChaptersActivity.this, "No hay capítulos disponibles en inglés", Toast.LENGTH_SHORT).show();
                    } else {
                        statusText.setText("✅ " + chapters.size() + " capítulos disponibles");
                        Toast.makeText(ChaptersActivity.this, "Capítulos cargados desde MangaDex", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("❌ Error al cargar capítulos");
                    Toast.makeText(ChaptersActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onChapterClick(Chapter chapter) {
        Toast.makeText(this, "Abriendo capítulo " + chapter.getChapterNumber(), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(ChaptersActivity.this, ReaderActivity.class);
        intent.putExtra("chapter_id", chapter.getId());
        intent.putExtra("chapter_title", chapter.getTitle());
        intent.putExtra("chapter_number", chapter.getChapterNumber());
        intent.putExtra("manga_id", selectedManga.getId());
        intent.putExtra("manga_title", selectedManga.getTitle());
        intent.putExtra("manga_description", selectedManga.getDescription());
        intent.putExtra("manga_cover", selectedManga.getCoverUrl());
        intent.putExtra("from_offline", fromOffline); // ✅ Pasar flag offline
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mangaDxApi != null) {
            mangaDxApi.cleanup();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}