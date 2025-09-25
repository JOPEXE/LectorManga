package com.example.lectormanga.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lectormanga.R;
import com.example.lectormanga.model.Chapter;

import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {

    private List<Chapter> chapterList;
    private Context context;

    // Interface para manejar clicks (para próximos pasos)
    public interface OnChapterClickListener {
        void onChapterClick(Chapter chapter);
    }

    private OnChapterClickListener listener;

    // Constructor
    public ChapterAdapter(List<Chapter> chapterList, Context context) {
        this.chapterList = chapterList;
        this.context = context;
    }

    // Constructor con listener
    public ChapterAdapter(List<Chapter> chapterList, Context context, OnChapterClickListener listener) {
        this.chapterList = chapterList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        Chapter chapter = chapterList.get(position);
        holder.bind(chapter, context, listener);
    }

    @Override
    public int getItemCount() {
        return chapterList.size();
    }

    // ViewHolder
    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        private TextView chapterNumber;
        private TextView chapterTitle;
        private TextView chapterPages;

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            chapterNumber = itemView.findViewById(R.id.chapterNumber);
            chapterTitle = itemView.findViewById(R.id.chapterTitle);
            chapterPages = itemView.findViewById(R.id.chapterPages);
        }

        public void bind(Chapter chapter, Context context, OnChapterClickListener listener) {
            // Número del capítulo
            chapterNumber.setText("#" + chapter.getChapterNumber());

            // Título del capítulo
            String title = chapter.getTitle();
            if (title == null || title.isEmpty() || title.equals("Sin título")) {
                chapterTitle.setText("Capítulo " + chapter.getChapterNumber());
            } else {
                chapterTitle.setText(title);
            }

            // Número de páginas
            chapterPages.setText(chapter.getPages() + " páginas");

            // Click listener
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onChapterClick(chapter);
                    } else {
                        // Toast temporal
                        Toast.makeText(context,
                                "Capítulo " + chapter.getChapterNumber() + "\n(Próximamente: lector)",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // Método para actualizar la lista
    public void updateChapterList(List<Chapter> newChapterList) {
        this.chapterList.clear();
        this.chapterList.addAll(newChapterList);
        notifyDataSetChanged();
    }
}