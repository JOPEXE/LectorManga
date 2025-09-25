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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapters);

        // Inicializar API
        mangaDxApi = new MangaDexApi();

        // Inicializar vistas
        initViews();

        // Obtener datos del manga seleccionado
        getMangaData();

        // Configurar RecyclerView
        setupRecyclerView();

        // Cargar capítulos reales
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

            // Cargar imagen del manga
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
        if (selectedManga == null) {
            Toast.makeText(this, "Error: No hay datos del manga", Toast.LENGTH_SHORT).show();
            finish();
            return;
    }

            if (selectedManga == null || selectedManga.getId() == null) {
                statusText.setText("❌ Error: ID de manga no válido");
                return;
            }

            // Verificar si viene de contenido offline
            boolean fromOffline = getIntent().getBooleanExtra("from_offline", false);

            if (fromOffline) {
                // Mostrar mensaje especial para contenido offline
                statusText.setText("📚 Manga guardado localmente\n\nEste manga está en tu biblioteca personal. Para ver capítulos, búscalo desde la pantalla principal.");
                Toast.makeText(this, "Manga guardado offline. Búscalo en la pantalla principal para ver capítulos.", Toast.LENGTH_LONG).show();
                return;
            }

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
        intent.putExtra("manga_title", selectedManga.getTitle());
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