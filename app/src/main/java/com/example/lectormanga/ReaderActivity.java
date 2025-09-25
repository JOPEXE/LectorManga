package com.example.lectormanga;

import android.content.Intent;
import android.os.Bundle;
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
    private MangaDAO mangaDAO; // ✅ AGREGADO

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        // Inicializar API
        mangaDxApi = new MangaDexApi();

        // ✅ AGREGADO - Inicializar base de datos
        mangaDAO = new MangaDAO(this);

        // Inicializar vistas
        initViews();

        // Obtener datos del intent
        getIntentData();

        // Configurar RecyclerView
        setupRecyclerView();

        // Configurar controles
        setupControls();

        // Cargar páginas reales de la API
        loadRealPages();
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
        }

        // Valores por defecto si no hay datos
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

        // Snap helper para páginas
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerViewPages);

        // Listener para detectar cambio de página
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
        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage > 0) {
                    currentPage--;
                    recyclerViewPages.smoothScrollToPosition(currentPage);
                    updatePageIndicators();
                }
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    recyclerViewPages.smoothScrollToPosition(currentPage);
                    updatePageIndicators();
                } else {
                    Toast.makeText(ReaderActivity.this, "✅ ¡Capítulo terminado!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadRealPages() {
        if (chapterId == null) {
            statusText.setVisibility(View.VISIBLE);
            statusText.setText("❌ Error: ID de capítulo no válido");
            return;
        }

        // Mostrar estado de carga
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("🔄 Cargando páginas desde MangaDX...");
        progressBar.setVisibility(View.VISIBLE);

        mangaDxApi.getChapterPages(chapterId, new MangaDexApi.PageCallback() {
            @Override
            public void onSuccess(List<String> urls) {
                runOnUiThread(() -> {
                    if (urls.isEmpty()) {
                        statusText.setText("❌ No se encontraron páginas para este capítulo");
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ReaderActivity.this, "Este capítulo no tiene páginas disponibles", Toast.LENGTH_LONG).show();
                        return;
                    }

                    totalPages = urls.size();
                    loadedPages = 0;

                    // Actualizar adapter con URLs reales
                    pageUrls.clear();
                    pageUrls.addAll(urls);
                    pageAdapter.notifyDataSetChanged();

                    // Actualizar indicadores
                    updatePageIndicators();

                    statusText.setText("📖 " + totalPages + " páginas cargadas desde MangaDX");
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ReaderActivity.this, "✅ " + totalPages + " páginas listas para leer", Toast.LENGTH_SHORT).show();

                    // ✅ GUARDADO AUTOMÁTICO - Después de que las páginas se cargan exitosamente
                    saveMangaToDatabase();

                    // Ocultar status después de 3 segundos
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

    // ✅ NUEVO MÉTODO - Guardado automático en SQLite
    private void saveMangaToDatabase() {
        if (mangaTitle == null || chapterId == null) return;

        // Obtener ID del manga desde el intent (si está disponible)
        String mangaId = getIntent().getStringExtra("manga_id");
        if (mangaId == null) mangaId = chapterId; // Usar chapterId como fallback

        // Crear objeto manga con la información disponible
        Manga manga = new Manga();
        manga.setId(mangaId);
        manga.setTitle(mangaTitle);
        manga.setDescription(getIntent().getStringExtra("manga_description") != null ?
                getIntent().getStringExtra("manga_description") :
                "Manga leído desde el lector");
        manga.setCoverUrl(getIntent().getStringExtra("manga_cover") != null ?
                getIntent().getStringExtra("manga_cover") : "");

        // Guardar en base de datos
        long result = mangaDAO.addReadManga(manga, chapterNumber, "reading");

        if (result > 0) {
            runOnUiThread(() -> {
                Toast.makeText(ReaderActivity.this, "📚 Manga guardado en biblioteca personal", Toast.LENGTH_SHORT).show();
            });
        }
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
            // Actualizar textos
            pageIndicator.setText("Página " + (currentPage + 1) + " de " + totalPages);
            currentPageText.setText((currentPage + 1) + " / " + totalPages);

            // Actualizar botones
            btnPrevious.setEnabled(currentPage > 0);
            btnNext.setEnabled(currentPage < totalPages - 1);

            // Cambiar texto del botón Next en la última página
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
        // Opcional: mostrar progreso de carga
        if (loadedPages == 1 && statusText.getVisibility() == View.VISIBLE) {
            runOnUiThread(() -> {
                statusText.setText("📖 Cargando imágenes... (" + loadedPages + "/" + totalPages + ")");
            });
        }
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
        // Suprimir warning de deprecación
        //noinspection deprecation
        super.onBackPressed();
        finish();
    }
}