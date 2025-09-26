package com.example.lectormanga.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.lectormanga.ChaptersActivity;
import com.example.lectormanga.R;
import com.example.lectormanga.database.MangaDAO;
import com.example.lectormanga.model.Manga;

import java.util.List;

public class ReadMangaAdapter extends RecyclerView.Adapter<ReadMangaAdapter.ReadMangaViewHolder> {

    private List<Manga> mangaList;
    private Context context;
    private MangaDAO mangaDAO;

    public ReadMangaAdapter(List<Manga> mangaList, Context context, MangaDAO mangaDAO) {
        this.mangaList = mangaList;
        this.context = context;
        this.mangaDAO = mangaDAO;
    }

    @NonNull
    @Override
    public ReadMangaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_read_manga, parent, false);
        return new ReadMangaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReadMangaViewHolder holder, int position) {
        Manga manga = mangaList.get(position);
        holder.bind(manga, context, mangaDAO);
    }

    @Override
    public int getItemCount() {
        return mangaList.size();
    }

    static class ReadMangaViewHolder extends RecyclerView.ViewHolder {
        private ImageView mangaCover;
        private TextView mangaTitle, mangaDescription, statusInfo, lastChapterInfo;

        public ReadMangaViewHolder(@NonNull View itemView) {
            super(itemView);
            mangaCover = itemView.findViewById(R.id.mangaCover);
            mangaTitle = itemView.findViewById(R.id.mangaTitle);
            mangaDescription = itemView.findViewById(R.id.mangaDescription);
            statusInfo = itemView.findViewById(R.id.statusInfo);
            lastChapterInfo = itemView.findViewById(R.id.lastChapterInfo);
        }

        public void bind(Manga manga, Context context, MangaDAO mangaDAO) {
            mangaTitle.setText(manga.getTitle());
            mangaDescription.setText(manga.getDescription());

            // ✅ Obtener información adicional de la base de datos
            MangaDAO.ReadMangaInfo readInfo = mangaDAO.getMangaReadInfo(manga.getId());
            int chaptersCount = mangaDAO.getChaptersByMangaId(manga.getId()).size();

            if (readInfo != null) {
                statusInfo.setText("Estado: " + getStatusText(readInfo.status));
                lastChapterInfo.setText("Último capítulo: " + readInfo.lastChapter +
                        " • " + chaptersCount + " capítulos guardados");
            } else {
                lastChapterInfo.setText(chaptersCount + " capítulos guardados offline");
            }

            // Cargar imagen
            if (manga.getCoverUrl() != null && !manga.getCoverUrl().isEmpty()) {
                Glide.with(context)
                        .load(manga.getCoverUrl())
                        .placeholder(R.drawable.placeholder_manga)
                        .error(R.drawable.placeholder_manga)
                        .into(mangaCover);
            } else {
                mangaCover.setImageResource(R.drawable.placeholder_manga);
            }

            // ✅ Click listener - ir a capítulos offline
            itemView.setOnClickListener(v -> {
                Toast.makeText(context,
                        "Abriendo " + manga.getTitle() + " (" + chaptersCount + " capítulos offline)",
                        Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(context, ChaptersActivity.class);
                intent.putExtra("manga_id", manga.getId());
                intent.putExtra("manga_title", manga.getTitle());
                intent.putExtra("manga_description", manga.getDescription());
                intent.putExtra("manga_cover", manga.getCoverUrl());
                intent.putExtra("from_offline", true);
                context.startActivity(intent);
            });
        }

        private String getStatusText(String status) {
            switch (status) {
                case "reading": return "📖 Leyendo";
                case "completed": return "✅ Completado";
                case "paused": return "⏸️ Pausado";
                default: return "❓ " + status;
            }
        }
    }

    public void updateMangaList(List<Manga> newMangaList) {
        this.mangaList.clear();
        this.mangaList.addAll(newMangaList);
        notifyDataSetChanged();
    }
}