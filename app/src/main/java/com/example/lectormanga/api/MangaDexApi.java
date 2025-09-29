package com.example.lectormanga.api;

import android.util.Log;

import com.example.lectormanga.model.Chapter;
import com.example.lectormanga.model.Manga;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MangaDexApi {

    private static final String TAG = "MangaDexApi";
    private static final String BASE_URL = "https://api.mangadex.org";

    private OkHttpClient client;

    public MangaDexApi() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(10);
        dispatcher.setMaxRequestsPerHost(5);

        ConnectionPool connectionPool = new ConnectionPool(5, 5, TimeUnit.MINUTES);

        client = new OkHttpClient.Builder()
                .connectTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .callTimeout(120, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(connectionPool)
                .dispatcher(dispatcher)
                .build();

        Log.d(TAG, "✅ MangaDexApi inicializada");
    }

    // Interfaces
    public interface MangaCallback {
        void onSuccess(List<Manga> mangas);
        void onFailure(Exception e);
    }

    public interface ChapterCallback {
        void onSuccess(List<Chapter> chapters);
        void onFailure(Exception e);
    }

    public interface PageCallback {
        void onSuccess(List<String> pageUrls);
        void onFailure(Exception e);
    }

    // Métodos públicos
    public void searchMangas(String query, int limit, MangaCallback callback) {
        String url = BASE_URL + "/manga?title=" + query.replace(" ", "%20") +
                "&limit=" + limit +
                "&includes[]=cover_art" +
                "&contentRating[]=safe" +
                "&contentRating[]=suggestive" +
                "&order[relevance]=desc";

        Log.d(TAG, "🔍 Buscando: " + query);
        fetchMangas(url, callback);
    }

    public void getPopularMangas(int limit, MangaCallback callback) {
        String url = BASE_URL + "/manga?limit=" + limit +
                "&includes[]=cover_art" +
                "&order[followedCount]=desc" +
                "&contentRating[]=safe" +
                "&contentRating[]=suggestive" +
                "&hasAvailableChapters=true";

        Log.d(TAG, "📚 Cargando populares");
        fetchMangas(url, callback);
    }

    public void getChapters(String mangaId, int limit, ChapterCallback callback) {
        String url = BASE_URL + "/manga/" + mangaId +
                "/feed?limit=" + limit +
                "&order[chapter]=asc" +
                "&translatedLanguage[]=en" +
                "&includes[]=scanlation_group" +
                "&contentRating[]=safe" +
                "&contentRating[]=suggestive";

        Log.d(TAG, "📖 Cargando capítulos");
        fetchChapters(url, callback);
    }

    public void getChapterPages(String chapterId, PageCallback callback) {
        String url = BASE_URL + "/at-home/server/" + chapterId;
        Log.d(TAG, "📄 Cargando páginas");
        fetchPages(url, callback);
    }

    // Métodos de fetch
    private void fetchMangas(String url, MangaCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "LectorManga/1.0")
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ Error: " + e.getMessage());
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        List<Manga> mangas = parseMangaResponse(responseData);
                        callback.onSuccess(mangas);
                    } catch (JSONException e) {
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new Exception("HTTP " + response.code()));
                }
            }
        });
    }

    private void fetchChapters(String url, ChapterCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "LectorManga/1.0")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        List<Chapter> chapters = parseChapterResponse(responseData);
                        callback.onSuccess(chapters);
                    } catch (JSONException e) {
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new Exception("HTTP " + response.code()));
                }
            }
        });
    }

    private void fetchPages(String url, PageCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "LectorManga/1.0")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        List<String> pageUrls = parsePageResponse(responseData);
                        callback.onSuccess(pageUrls);
                    } catch (JSONException e) {
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new Exception("HTTP " + response.code()));
                }
            }
        });
    }

    // Métodos de parsing
    private List<Manga> parseMangaResponse(String jsonData) throws JSONException {
        List<Manga> mangas = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(jsonData);
        JSONArray dataArray = jsonObject.getJSONArray("data");

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject mangaObj = dataArray.getJSONObject(i);
            JSONObject attributes = mangaObj.getJSONObject("attributes");

            Manga manga = new Manga();
            manga.setId(mangaObj.getString("id"));

            JSONObject title = attributes.getJSONObject("title");
            String titleText = title.optString("en",
                    title.optString("ja-ro",
                            title.optString("ja", "Sin título")));
            manga.setTitle(titleText);

            JSONObject description = attributes.optJSONObject("description");
            String descriptionText = "";
            if (description != null) {
                descriptionText = description.optString("en",
                        description.optString("es", "Sin descripción"));
            }
            if (descriptionText.length() > 200) {
                descriptionText = descriptionText.substring(0, 200) + "...";
            }
            manga.setDescription(descriptionText.isEmpty() ? "Sin descripción" : descriptionText);

            JSONArray relationships = mangaObj.getJSONArray("relationships");
            for (int j = 0; j < relationships.length(); j++) {
                JSONObject rel = relationships.getJSONObject(j);
                if ("cover_art".equals(rel.getString("type"))) {
                    if (rel.has("attributes")) {
                        String fileName = rel.getJSONObject("attributes").optString("fileName", "");
                        if (!fileName.isEmpty()) {
                            String coverUrl = "https://uploads.mangadex.org/covers/" +
                                    manga.getId() + "/" + fileName + ".256.jpg";
                            manga.setCoverUrl(coverUrl);
                        }
                    }
                    break;
                }
            }

            mangas.add(manga);
        }

        return mangas;
    }

    private List<Chapter> parseChapterResponse(String jsonData) throws JSONException {
        List<Chapter> chapters = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(jsonData);
        JSONArray dataArray = jsonObject.getJSONArray("data");

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject chapterObj = dataArray.getJSONObject(i);
            JSONObject attributes = chapterObj.getJSONObject("attributes");

            String chapterNum = attributes.optString("chapter", "");
            if (chapterNum.isEmpty()) continue;

            Chapter chapter = new Chapter();
            chapter.setId(chapterObj.getString("id"));
            chapter.setChapterNumber(chapterNum);
            chapter.setTitle(attributes.optString("title", ""));
            chapter.setPages(String.valueOf(attributes.optInt("pages", 0)));
            chapter.setPublishedAt(attributes.optString("publishAt", ""));

            chapters.add(chapter);
        }

        return chapters;
    }

    private List<String> parsePageResponse(String jsonData) throws JSONException {
        List<String> pageUrls = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(jsonData);
        String baseUrl = jsonObject.getString("baseUrl");
        JSONObject chapter = jsonObject.getJSONObject("chapter");
        String hash = chapter.getString("hash");

        JSONArray dataArray;
        if (chapter.has("dataSaver")) {
            dataArray = chapter.getJSONArray("dataSaver");
        } else {
            dataArray = chapter.getJSONArray("data");
        }

        for (int i = 0; i < dataArray.length(); i++) {
            String filename = dataArray.getString(i);
            String endpoint = chapter.has("dataSaver") ? "/data-saver/" : "/data/";
            String fullUrl = baseUrl + endpoint + hash + "/" + filename;
            pageUrls.add(fullUrl);
        }

        return pageUrls;
    }

    public void cleanup() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }
}