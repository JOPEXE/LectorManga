package com.example.lectormanga;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lectormanga.adapter.ReadMangaAdapter;
import com.example.lectormanga.database.MangaDAO;
import com.example.lectormanga.model.Manga;

import java.util.ArrayList;
import java.util.List;

public class ReadMangasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ReadMangaAdapter readMangaAdapter;
    private List<Manga> readMangasList;
    private TextView statusText, statsText;
    private MangaDAO mangaDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_mangas);

        // Inicializar base de datos
        mangaDAO = new MangaDAO(this);

        // Inicializar vistas
        initViews();

        // Configurar RecyclerView
        setupRecyclerView();

        // Cargar mangas leídos
        loadReadMangas();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        statusText = findViewById(R.id.statusText);
        statsText = findViewById(R.id.statsText);
    }

    private void setupRecyclerView() {
        readMangasList = new ArrayList<>();
        readMangaAdapter = new ReadMangaAdapter(readMangasList, this, mangaDAO);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(readMangaAdapter);
    }

    private void loadReadMangas() {
        mangaDAO.debugDatabase();
        List<Manga> readMangas = mangaDAO.getAllReadMangas();

        if (readMangas.isEmpty()) {
            statusText.setText("No has guardado ningún manga aún\n\nCuando leas un manga, aparecerá aquí para acceso offline");
            statsText.setText("📊 0 mangas guardados");
            Toast.makeText(this, "Guarda mangas desde el lector para verlos aquí", Toast.LENGTH_LONG).show();
        } else {
            readMangasList.clear();
            readMangasList.addAll(readMangas);
            readMangaAdapter.notifyDataSetChanged();

            // Mostrar estadísticas
            int reading = mangaDAO.getCountByStatus("reading");
            int completed = mangaDAO.getCountByStatus("completed");
            int paused = mangaDAO.getCountByStatus("paused");

            statusText.setText("📚 " + readMangas.size() + " mangas guardados offline");
            statsText.setText("📊 Leyendo: " + reading + " • Completados: " + completed + " • Pausados: " + paused);

            Toast.makeText(this, "Cargados " + readMangas.size() + " mangas desde SQLite", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar cuando volvemos a esta activity
        loadReadMangas();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}