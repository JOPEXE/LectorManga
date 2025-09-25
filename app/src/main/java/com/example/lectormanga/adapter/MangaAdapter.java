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
import com.example.lectormanga.model.Manga;

import java.util.List;

public class MangaAdapter extends RecyclerView.Adapter<MangaAdapter.MangaViewHolder> {

    private List<Manga> mangaList;
    private Context context;

    // Constructor
    public MangaAdapter(List<Manga> mangaList, Context context) {
        this.mangaList = mangaList;
        this.context = context;
    }

    @NonNull
    @Override
    public MangaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manga, parent, false);
        return new MangaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MangaViewHolder holder, int position) {
        Manga manga = mangaList.get(position);
        holder.bind(manga, context);
    }

    @Override
    public int getItemCount() {
        return mangaList.size();
    }

    // ViewHolder interno
    static class MangaViewHolder extends RecyclerView.ViewHolder {
        private ImageView mangaCover;
        private TextView mangaTitle;
        private TextView mangaDescription;

        public MangaViewHolder(@NonNull View itemView) {
            super(itemView);
            // Enlazar vistas con los IDs del layout
            mangaCover = itemView.findViewById(R.id.mangaCover);
            mangaTitle = itemView.findViewById(R.id.mangaTitle);
            mangaDescription = itemView.findViewById(R.id.mangaDescription);
        }

        public void bind(Manga manga, Context context) {
            // Establecer título
            mangaTitle.setText(manga.getTitle());

            // Establecer descripción
            mangaDescription.setText(manga.getDescription());

            // Cargar imagen con Glide
            if (manga.getCoverUrl() != null && !manga.getCoverUrl().isEmpty()) {
                Glide.with(context)
                        .load(manga.getCoverUrl())
                        .placeholder(R.drawable.placeholder_manga)
                        .error(R.drawable.placeholder_manga)
                        .into(mangaCover);
            } else {
                // Si no hay URL, usar placeholder
                mangaCover.setImageResource(R.drawable.placeholder_manga);
            }

            // ✅ CLICK LISTENER ACTUALIZADO - NAVEGA A CHAPTERSACTIVITY
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "Abriendo " + manga.getTitle(), Toast.LENGTH_SHORT).show();

                    // Crear Intent para ir a ChaptersActivity
                    Intent intent = new Intent(context, ChaptersActivity.class);
                    intent.putExtra("manga_id", manga.getId());
                    intent.putExtra("manga_title", manga.getTitle());
                    intent.putExtra("manga_description", manga.getDescription());
                    intent.putExtra("manga_cover", manga.getCoverUrl());

                    context.startActivity(intent);
                }
            });
        }
    }

    // Método para actualizar la lista
    public void updateMangaList(List<Manga> newMangaList) {
        this.mangaList.clear();
        this.mangaList.addAll(newMangaList);
        notifyDataSetChanged();
    }
}