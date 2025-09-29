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
import com.example.lectormanga.model.Manga;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MangaAdapter mangaAdapter;
    private List<Manga> mangaList;
    private EditText searchInput;
    private Button searchButton, readMangasButton;
    private TextView statusText;

    private MangaDexApi mangaDexApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mangaDexApi = new MangaDexApi();

        initViews();
        setupRecyclerView();
        setupButtons();
        loadPopularMangas();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        searchInput = findViewById(R.id.et_buscador);
        searchButton = findViewById(R.id.btn_buscar);
        readMangasButton = findViewById(R.id.btn_mis_mangas);
        statusText = findViewById(R.id.tv_estado);
    }

    private void setupRecyclerView() {
        mangaList = new ArrayList<>();
        mangaAdapter = new MangaAdapter(mangaList, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mangaAdapter);
    }

    private void setupButtons() {
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

        readMangasButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ReadMangasActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadPopularMangas() {
        statusText.setText("🔄 Cargando mangas...");

        mangaDexApi.getPopularMangas(20, new MangaDexApi.MangaCallback() {
            @Override
            public void onSuccess(List<Manga> mangas) {
                runOnUiThread(() -> {
                    mangaList.clear();
                    mangaList.addAll(mangas);
                    mangaAdapter.notifyDataSetChanged();

                    statusText.setText("✅ " + mangas.size() + " mangas cargados");
                    Toast.makeText(MainActivity.this, "Mangas cargados", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("❌ Error al cargar");
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void searchManga(String query) {
        statusText.setText("🔍 Buscando: " + query);

        mangaDexApi.searchMangas(query, 20, new MangaDexApi.MangaCallback() {
            @Override
            public void onSuccess(List<Manga> mangas) {
                runOnUiThread(() -> {
                    mangaList.clear();
                    mangaList.addAll(mangas);
                    mangaAdapter.notifyDataSetChanged();

                    if (mangas.isEmpty()) {
                        statusText.setText("❌ Sin resultados");
                    } else {
                        statusText.setText("✅ " + mangas.size() + " resultados");
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("❌ Error");
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mangaDexApi != null) {
            mangaDexApi.cleanup();
        }
    }
}