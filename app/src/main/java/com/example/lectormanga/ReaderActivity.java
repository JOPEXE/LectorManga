package com.example.lectormanga;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lectormanga.adapter.PageAdapter;
import com.example.lectormanga.api.MangaDexApi;
import com.example.lectormanga.database.MangaDAO;
import com.example.lectormanga.model.Chapter;
import com.example.lectormanga.model.Manga;

import java.util.ArrayList;
import java.util.List;

public class ReaderActivity extends AppCompatActivity implements PageAdapter.OnPageLoadListener {

    private RecyclerView recyclerViewPages;
    private PageAdapter pageAdapter;
    private List<String> pageUrls;

    private TextView chapterTitle, pageIndicator, currentPageText, statusText;
    private Button btnPrevious, btnNext;
    private ProgressBar progressBar;

    private String chapterId, chapterTitleText, chapterNumber, mangaTitle;
    private int currentPage = 0;
    private int totalPages = 0;
    private int loadedPages = 0;

    private MangaDexApi mangaDxApi;
    private MangaDAO mangaDAO;
    private boolean fromOffline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        // Inicializar API y DAO
        mangaDxApi = new MangaDexApi();
        mangaDAO = new MangaDAO(this);

        // Inicializar vistas
        initViews();

        // Obtener datos del intent
        getIntentData();

        // Configurar RecyclerView
        setupRecyclerView();

        // Configurar controles
        setupControls();

        // Cargar páginas (online u offline)
        if (fromOffline) {
            loadOfflinePages();
        } else {
            loadRealPages();
        }
    }

    private void initViews() {
        recyclerViewPages = findViewById(R.id.recyclerViewPages);
        chapterTitle = findViewById(R.id.chapterTitle);
        pageIndicator = findViewById(R.id.pageIndicator);
        currentPageText = findViewById(R.id.currentPageText);
        statusText = findViewById(R.id.tv_estado);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        progressBar = findViewById(R.id.progressBar);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            chapterId = intent.getStringExtra("chapter_id");
            chapterTitleText = intent.getStringExtra("chapter_title");
            chapterNumber = intent.getStringExtra("chapter_number");
            mangaTitle = intent.getStringExtra("manga_title");
            fromOffline = intent.getBooleanExtra("from_offline", false);
        }

        // Valores por defecto
        if (mangaTitle == null) mangaTitle = "Manga";
        if (chapterNumber == null) chapterNumber = "1";
        if (chapterTitleText == null) chapterTitleText = "Capítulo";

        // Mostrar título
        String titleText = mangaTitle + " - Capítulo " + chapterNumber;
        if (!chapterTitleText.equals("Sin título") && !chapterTitleText.isEmpty()) {
            titleText += ": " + chapterTitleText;
        }
        chapterTitle.setText(titleText);
    }

    private void setupRecyclerView() {
        pageUrls = new ArrayList<>();
        pageAdapter = new PageAdapter(pageUrls, this, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerViewPages.setLayoutManager(layoutManager);
        recyclerViewPages.setAdapter(pageAdapter);

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerViewPages);

        recyclerViewPages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updatePageIndicators();
                }
            }
        });
    }

    private void setupControls() {
        btnPrevious.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                recyclerViewPages.smoothScrollToPosition(currentPage);
                updatePageIndicators();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                recyclerViewPages.smoothScrollToPosition(currentPage);
                updatePageIndicators();
            } else {
                Toast.makeText(ReaderActivity.this, "✅ ¡Capítulo terminado!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========== CARGA ONLINE ==========
    private void loadRealPages() {
        if (chapterId == null) {
            statusText.setVisibility(View.VISIBLE);
            statusText.setText("❌ Error: ID de capítulo no válido");
            return;
        }

        statusText.setVisibility(View.VISIBLE);
        statusText.setText("🔄 Cargando páginas desde MangaDex...");
        progressBar.setVisibility(View.VISIBLE);

        mangaDxApi.getChapterPages(chapterId, new MangaDexApi.PageCallback() {
            @Override
            public void onSuccess(List<String> urls) {
                runOnUiThread(() -> {
                    if (urls.isEmpty()) {
                        statusText.setText("❌ No se encontraron páginas");
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ReaderActivity.this, "Este capítulo no tiene páginas disponibles", Toast.LENGTH_LONG).show();
                        return;
                    }

                    totalPages = urls.size();
                    loadedPages = 0;

                    pageUrls.clear();
                    pageUrls.addAll(urls);
                    pageAdapter.notifyDataSetChanged();

                    updatePageIndicators();

                    statusText.setText("📖 " + totalPages + " páginas cargadas desde MangaDex");
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ReaderActivity.this, "✅ " + totalPages + " páginas listas para leer", Toast.LENGTH_SHORT).show();

                    // ✅ GUARDADO COMPLETO OFFLINE
                    saveCompleteMangaOffline(urls);

                    statusText.postDelayed(() -> {
                        if (statusText != null) {
                            statusText.setVisibility(View.GONE);
                        }
                    }, 3000);
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("❌ Error al cargar páginas: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ReaderActivity.this, "Error de conexión: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ========== CARGA OFFLINE ==========
    private void loadOfflinePages() {
        if (chapterId == null) {
            statusText.setVisibility(View.VISIBLE);
            statusText.setText("❌ Error: ID de capítulo no válido");
            return;
        }

        statusText.setVisibility(View.VISIBLE);
        statusText.setText("💾 Cargando páginas desde SQLite...");
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            List<String> offlineUrls = mangaDAO.getPageUrlsByChapterId(chapterId);

            runOnUiThread(() -> {
                if (offlineUrls.isEmpty()) {
                    statusText.setText("❌ No hay páginas guardadas offline");
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ReaderActivity.this, "Este capítulo no está disponible offline", Toast.LENGTH_LONG).show();
                    return;
                }

                totalPages = offlineUrls.size();
                pageUrls.clear();
                pageUrls.addAll(offlineUrls);
                pageAdapter.notifyDataSetChanged();

                updatePageIndicators();

                statusText.setText("✅ " + totalPages + " páginas cargadas (OFFLINE)");
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ReaderActivity.this, "📚 Leyendo offline: " + totalPages + " páginas", Toast.LENGTH_SHORT).show();

                statusText.postDelayed(() -> {
                    if (statusText != null) {
                        statusText.setVisibility(View.GONE);
                    }
                }, 3000);
            });
        }).start();
    }

    // ========== GUARDADO OFFLINE COMPLETO ==========
    private void saveCompleteMangaOffline(List<String> pageUrls) {
        new Thread(() -> {
            try {
                String mangaId = getIntent().getStringExtra("manga_id");
                if (mangaId == null) mangaId = chapterId;

                // 1. Guardar/Actualizar manga con imagen (solo si no existe)
                if (!mangaDAO.isMangaRead(mangaId)) {
                    Manga manga = new Manga();
                    manga.setId(mangaId);
                    manga.setTitle(mangaTitle != null ? mangaTitle : "Manga");
                    manga.setDescription(getIntent().getStringExtra("manga_description"));
                    manga.setCoverUrl(getIntent().getStringExtra("manga_cover"));

                    long mangaResult = mangaDAO.addReadMangaWithImage(manga, chapterNumber, "reading");
                    Log.d("ReaderActivity", "Manga guardado: " + mangaResult);
                } else {
                    // Solo actualizar último capítulo leído
                    mangaDAO.updateLastChapter(mangaId, chapterNumber);
                    Log.d("ReaderActivity", "Manga actualizado - último capítulo: " + chapterNumber);
                }

                // 2. Verificar si el capítulo ya está guardado
                if (mangaDAO.hasOfflinePages(chapterId)) {
                    runOnUiThread(() -> {
                        Toast.makeText(ReaderActivity.this,
                                "✅ Capítulo " + chapterNumber + " ya está guardado offline",
                                Toast.LENGTH_SHORT).show();
                    });
                    return; // Ya existe, no guardar de nuevo
                }

                // 3. Guardar capítulo NUEVO
                Chapter chapter = new Chapter();
                chapter.setId(chapterId);
                chapter.setChapterNumber(chapterNumber);
                chapter.setTitle(chapterTitleText);
                chapter.setPages(String.valueOf(pageUrls.size()));

                long chapterResult = mangaDAO.addChapter(chapter, mangaId);
                Log.d("ReaderActivity", "Capítulo guardado: " + chapterResult);

                // 4. Guardar todas las páginas con imágenes
                for (int i = 0; i < pageUrls.size(); i++) {
                    long pageResult = mangaDAO.addPageWithImage(chapterId, i + 1, pageUrls.get(i));
                    Log.d("ReaderActivity", "Página " + (i + 1) + " guardada: " + pageResult);

                    int finalI = i;
                    runOnUiThread(() -> {
                        statusText.setVisibility(View.VISIBLE);
                        statusText.setText("💾 Guardando capítulo " + chapterNumber + " - página " + (finalI + 1) + "/" + pageUrls.size());
                    });
                }

                // 5. Notificar éxito
                String finalMangaId = mangaId;
                runOnUiThread(() -> {
                    int totalChapters = mangaDAO.getChaptersByMangaId(finalMangaId).size();
                    statusText.setText("✅ Capítulo " + chapterNumber + " guardado offline");
                    Toast.makeText(ReaderActivity.this,
                            "📚 Capítulo " + chapterNumber + " guardado (" + totalChapters + " capítulos offline)",
                            Toast.LENGTH_LONG).show();

                    statusText.postDelayed(() -> {
                        if (statusText != null) {
                            statusText.setVisibility(View.GONE);
                        }
                    }, 2000);
                });

            } catch (Exception e) {
                Log.e("ReaderActivity", "Error guardando offline: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(ReaderActivity.this,
                            "⚠️ Error al guardar offline: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void updatePageIndicators() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerViewPages.getLayoutManager();
        if (layoutManager != null) {
            currentPage = layoutManager.findFirstCompletelyVisibleItemPosition();
            if (currentPage == -1) {
                currentPage = layoutManager.findFirstVisibleItemPosition();
            }
        }

        if (totalPages > 0) {
            pageIndicator.setText("Página " + (currentPage + 1) + " de " + totalPages);
            currentPageText.setText((currentPage + 1) + " / " + totalPages);

            btnPrevious.setEnabled(currentPage > 0);
            btnNext.setEnabled(currentPage < totalPages - 1);

            if (currentPage >= totalPages - 1) {
                btnNext.setText("Terminar");
            } else {
                btnNext.setText("Siguiente ➡");
            }
        }
    }

    @Override
    public void onPageLoaded(int position) {
        loadedPages++;
    }

    @Override
    public void onPageError(int position) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Error al cargar página " + (position + 1), Toast.LENGTH_SHORT).show();
        });
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