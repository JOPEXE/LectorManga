package com.example.lectormanga.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.lectormanga.R;

import java.util.List;

public class PageAdapter extends RecyclerView.Adapter<PageAdapter.PageViewHolder> {

    private List<String> pageUrls;
    private Context context;

    public interface OnPageLoadListener {
        void onPageLoaded(int position);
        void onPageError(int position);
    }

    private OnPageLoadListener listener;

    public PageAdapter(List<String> pageUrls, Context context) {
        this.pageUrls = pageUrls;
        this.context = context;
    }

    public PageAdapter(List<String> pageUrls, Context context, OnPageLoadListener listener) {
        this.pageUrls = pageUrls;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        String pageUrl = pageUrls.get(position);
        holder.bind(pageUrl, position + 1, context, listener);
    }

    @Override
    public int getItemCount() {
        return pageUrls.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        private ImageView pageImage;
        private ProgressBar pageProgressBar;
        private TextView pageNumberText;

        public PageViewHolder(@NonNull View itemView) {
            super(itemView);
            pageImage = itemView.findViewById(R.id.pageImage);
            pageProgressBar = itemView.findViewById(R.id.pageProgressBar);
            pageNumberText = itemView.findViewById(R.id.pageNumberText);
        }

        public void bind(String pageUrl, int pageNumber, Context context, OnPageLoadListener listener) {
            pageNumberText.setText(String.valueOf(pageNumber));
            pageProgressBar.setVisibility(View.VISIBLE);

            if (pageUrl != null && !pageUrl.isEmpty()) {
                Glide.with(context)
                        .load(pageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.placeholder_manga)
                        .error(R.drawable.placeholder_manga)
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                pageProgressBar.setVisibility(View.GONE);
                                if (listener != null) {
                                    listener.onPageError(getAdapterPosition());
                                }
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                pageProgressBar.setVisibility(View.GONE);
                                if (listener != null) {
                                    listener.onPageLoaded(getAdapterPosition());
                                }
                                return false;
                            }
                        })
                        .into(pageImage);
            } else {
                // Sin URL, mostrar placeholder
                pageProgressBar.setVisibility(View.GONE);
                pageImage.setImageResource(R.drawable.placeholder_manga);
            }
        }
    }

    public void updatePages(List<String> newPages) {
        this.pageUrls.clear();
        this.pageUrls.addAll(newPages);
        notifyDataSetChanged();
    }
}