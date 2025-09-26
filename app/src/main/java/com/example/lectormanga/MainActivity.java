package com.example.lectormanga;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lectormanga.adapter.MangaAdapter;
import com.example.lectormanga.api.MangaDexApi;
import com.example.lectormanga.api.MangaDexApi;
import com.example.lectormanga.model.Manga;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MangaAdapter mangaAdapter;
    private List<Manga> mangaList;
    private EditText searchInput;
    private Button searchButton, readMangasButton; // ✅ AGREGAR readMangasButton
    private TextView statusText;

    private MangaDexApi mangaDxApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        com.example.lectormanga.utils.NetworkHelper.logNetworkStatus(this);

        String connectionType = com.example.lectormanga.utils.NetworkHelper.getConnectionType(this);
        Toast.makeText(this, "📡 Conexión: " + connectionType, Toast.LENGTH_LONG).show();

        if (com.example.lectormanga.utils.NetworkHelper.isMobileDataConnected(this)) {
            Toast.makeText(this, "⚠️ Usando datos móviles - Puede haber problemas con la API", Toast.LENGTH_LONG).show();
        }


        // Inicializar API
        mangaDxApi = new MangaDexApi();

        // Inicializar vistas
        initViews();

        // Configurar RecyclerView
        setupRecyclerView();

        // Configurar botones (actualizado)
        setupButtons();

        // Cargar mangas populares al inicio
        loadPopularMangas();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        searchInput = findViewById(R.id.et_buscador);
        searchButton = findViewById(R.id.btn_buscar);
        readMangasButton = findViewById(R.id.btn_mis_mangas); // ✅ AGREGAR ESTA LÍNEA
        statusText = findViewById(R.id.tv_estado);
    }

    private void setupRecyclerView() {
        mangaList = new ArrayList<>();
        mangaAdapter = new MangaAdapter(mangaList, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mangaAdapter);
    }

    // ✅ MÉTODO ACTUALIZADO - Cambiar setupSearchButton por setupButtons
    private void setupButtons() {
        // Botón de búsqueda
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchManga(query);
                } else {
                    Toast.makeText(MainActivity.this, "Ingresa un término de búsqueda", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ✅ NUEVO - Botón de mangas leídos
        readMangasButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Abriendo biblioteca personal", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, ReadMangasActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadPopularMangas() {
        statusText.setText("🔄 Cargando mangas populares...");

        mangaDxApi.getPopularMangas(20, new MangaDexApi.MangaCallback() {
            @Override
            public void onSuccess(List<Manga> mangas) {
                runOnUiThread(() -> {
                    mangaList.clear();
                    mangaList.addAll(mangas);
                    mangaAdapter.notifyDataSetChanged();

                    statusText.setText("✅ " + mangas.size() + " mangas populares cargados");
                    Toast.makeText(MainActivity.this, "Mangas cargados desde MangaDx", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("❌ Error al cargar mangas");
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void searchManga(String query) {
        statusText.setText("🔍 Buscando: " + query + "...");

        mangaDxApi.searchMangas(query, 20, new MangaDexApi.MangaCallback() {
            @Override
            public void onSuccess(List<Manga> mangas) {
                runOnUiThread(() -> {
                    mangaList.clear();
                    mangaList.addAll(mangas);
                    mangaAdapter.notifyDataSetChanged();

                    if (mangas.isEmpty()) {
                        statusText.setText("❌ No se encontraron mangas para: " + query);
                        Toast.makeText(MainActivity.this, "No se encontraron resultados", Toast.LENGTH_SHORT).show();
                    } else {
                        statusText.setText("✅ " + mangas.size() + " resultados para: " + query);
                        Toast.makeText(MainActivity.this, "Encontrados " + mangas.size() + " mangas", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("❌ Error en la búsqueda");
                    Toast.makeText(MainActivity.this, "Error en búsqueda: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mangaDxApi != null) {
            mangaDxApi.cleanup();
        }
    }
}