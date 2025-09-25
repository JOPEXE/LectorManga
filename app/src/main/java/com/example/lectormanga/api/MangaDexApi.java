package com.example.lectormanga.api;

import com.example.lectormanga.model.Chapter;
import com.example.lectormanga.model.Manga;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MangaDexApi {

    private static final String BASE_URL = "https://api.mangadex.org";
    private static final String MANGA_ENDPOINT = "/manga";
    private static final String CHAPTER_ENDPOINT = "/chapter";
    private static final String AT_HOME_ENDPOINT = "/at-home/server";

    private OkHttpClient client;

    public MangaDexApi() {
        client = new OkHttpClient();
    }

    // Interfaces para callbacks
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

    // ===== MÉTODOS PARA MANGAS =====

    // Obtener mangas populares
    public void getPopularMangas(int limit, MangaCallback callback) {
        String url = BASE_URL + MANGA_ENDPOINT + "?limit=" + limit +
                "&includes[]=cover_art&order[rating]=desc&contentRating[]=safe&contentRating[]=suggestive";
        fetchMangas(url, callback);
    }

    // Buscar mangas por título
    public void searchMangas(String query, int limit, MangaCallback callback) {
        String url = BASE_URL + MANGA_ENDPOINT + "?title=" + query + "&limit=" + limit +
                "&includes[]=cover_art&contentRating[]=safe&contentRating[]=suggestive";
        fetchMangas(url, callback);
    }

    // Obtener detalles de un manga específico
    public void getMangaDetails(String mangaId, MangaCallback callback) {
        String url = BASE_URL + MANGA_ENDPOINT + "/" + mangaId + "?includes[]=cover_art";
        fetchMangas(url, callback);
    }

    // ===== MÉTODOS PARA CAPÍTULOS =====

    // Obtener capítulos de un manga
    public void getChapters(String mangaId, int limit, ChapterCallback callback) {
        String url = BASE_URL + MANGA_ENDPOINT + "/" + mangaId +
                "/feed?limit=" + limit +
                "&order[chapter]=asc&translatedLanguage[]=en";
        fetchChapters(url, callback);
    }

    // ===== MÉTODOS PARA PÁGINAS =====

    // Obtener páginas de un capítulo
    public void getChapterPages(String chapterId, PageCallback callback) {
        String url = BASE_URL + AT_HOME_ENDPOINT + "/" + chapterId;
        fetchPages(url, callback);
    }

    // ===== MÉTODOS PRIVADOS DE FETCH =====

    private void fetchMangas(String url, MangaCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        List<Manga> mangas = parseMangaResponse(responseData);
                        callback.onSuccess(mangas);
                    } catch (JSONException e) {
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new Exception("Error HTTP: " + response.code()));
                }
            }
        });
    }

    private void fetchChapters(String url, ChapterCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        List<Chapter> chapters = parseChapterResponse(responseData);
                        callback.onSuccess(chapters);
                    } catch (JSONException e) {
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new Exception("Error HTTP: " + response.code()));
                }
            }
        });
    }

    private void fetchPages(String url, PageCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        List<String> pageUrls = parsePageResponse(responseData);
                        callback.onSuccess(pageUrls);
                    } catch (JSONException e) {
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new Exception("Error HTTP: " + response.code()));
                }
            }
        });
    }

    // ===== MÉTODOS DE PARSING =====

    private List<Manga> parseMangaResponse(String jsonData) throws JSONException {
        List<Manga> mangas = new ArrayList<>();

        JSONObject jsonObject = new JSONObject(jsonData);
        JSONArray dataArray = jsonObject.getJSONArray("data");

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject mangaObj = dataArray.getJSONObject(i);
            JSONObject attributes = mangaObj.getJSONObject("attributes");

            Manga manga = new Manga();
            manga.setId(mangaObj.getString("id"));

            // Título
            JSONObject title = attributes.getJSONObject("title");
            String titleText = "";
            if (title.has("en")) {
                titleText = title.getString("en");
            } else if (title.has("ja-ro")) {
                titleText = title.getString("ja-ro");
            } else if (title.has("ja")) {
                titleText = title.getString("ja");
            } else if (title.length() > 0) {
                String firstKey = title.keys().next();
                titleText = title.getString(firstKey);
            }
            manga.setTitle(titleText.isEmpty() ? "Sin título" : titleText);

            // Descripción
            JSONObject description = attributes.optJSONObject("description");
            String descriptionText = "";
            if (description != null) {
                if (description.has("en")) {
                    descriptionText = description.getString("en");
                } else if (description.has("es")) {
                    descriptionText = description.getString("es");
                } else if (description.length() > 0) {
                    String firstKey = description.keys().next();
                    descriptionText = description.getString(firstKey);
                }
            }

            // Limpiar y truncar descripción
            if (descriptionText.length() > 200) {
                descriptionText = descriptionText.substring(0, 200) + "...";
            }
            manga.setDescription(descriptionText.isEmpty() ? "Sin descripción disponible" : descriptionText);

            // Cover Art
            JSONArray relationships = mangaObj.getJSONArray("relationships");
            String coverArtId = "";
            String coverFileName = "";

            for (int j = 0; j < relationships.length(); j++) {
                JSONObject rel = relationships.getJSONObject(j);
                if ("cover_art".equals(rel.getString("type"))) {
                    coverArtId = rel.getString("id");
                    if (rel.has("attributes")) {
                        JSONObject coverAttributes = rel.getJSONObject("attributes");
                        coverFileName = coverAttributes.optString("fileName", "");
                    }
                    break;
                }
            }

            // Construir URL del cover
            if (!coverArtId.isEmpty() && !coverFileName.isEmpty()) {
                String coverUrl = "https://uploads.mangadex.org/covers/" + manga.getId() + "/" + coverFileName + ".512.jpg";
                manga.setCoverUrl(coverUrl);
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

            // Filtrar solo capítulos con número válido
            String chapterNum = attributes.optString("chapter", "");
            if (chapterNum.isEmpty() || chapterNum.equals("null")) {
                continue;
            }

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
        JSONArray dataArray = chapter.getJSONArray("data");

        for (int i = 0; i < dataArray.length(); i++) {
            String filename = dataArray.getString(i);
            String fullUrl = baseUrl + "/data/" + hash + "/" + filename;
            pageUrls.add(fullUrl);
        }

        return pageUrls;
    }

    // Método para limpiar recursos
    public void cleanup() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }
}