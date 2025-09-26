package com.example.lectormanga.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.example.lectormanga.model.Chapter;
import com.example.lectormanga.model.Manga;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MangaDAO {

    private DatabaseHelper databaseHelper;
    private SimpleDateFormat dateFormat;
    private static final String TAG = "MangaDAO";

    public MangaDAO(Context context) {
        databaseHelper = new DatabaseHelper(context);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    // ==================== MÉTODOS PARA MANGAS ====================

    public long addReadManga(Manga manga, String lastChapter, String status) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(DatabaseHelper.COLUMN_MANGA_ID, manga.getId());
        values.put(DatabaseHelper.COLUMN_TITLE, manga.getTitle());
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, manga.getDescription());
        values.put(DatabaseHelper.COLUMN_COVER_URL, manga.getCoverUrl());
        values.put(DatabaseHelper.COLUMN_READ_DATE, dateFormat.format(new Date()));
        values.put(DatabaseHelper.COLUMN_LAST_CHAPTER, lastChapter != null ? lastChapter : "1");
        values.put(DatabaseHelper.COLUMN_STATUS, status != null ? status : "reading");

        Log.d(TAG, "Guardando manga: " + manga.getTitle());
        long result = db.insertWithOnConflict(DatabaseHelper.TABLE_READ_MANGAS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return result;
    }

    // ✅ NUEVO - Guardar manga con imagen descargada
    public long addReadMangaWithImage(Manga manga, String lastChapter, String status) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(DatabaseHelper.COLUMN_MANGA_ID, manga.getId());
        values.put(DatabaseHelper.COLUMN_TITLE, manga.getTitle());
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, manga.getDescription());
        values.put(DatabaseHelper.COLUMN_COVER_URL, manga.getCoverUrl());

        // Descargar y guardar imagen
        String base64Image = downloadAndEncodeImage(manga.getCoverUrl());
        if (base64Image != null) {
            values.put(DatabaseHelper.COLUMN_COVER_IMAGE, base64Image);
            Log.d(TAG, "Imagen de portada guardada en Base64");
        }

        values.put(DatabaseHelper.COLUMN_READ_DATE, dateFormat.format(new Date()));
        values.put(DatabaseHelper.COLUMN_LAST_CHAPTER, lastChapter != null ? lastChapter : "1");
        values.put(DatabaseHelper.COLUMN_STATUS, status != null ? status : "reading");

        Log.d(TAG, "Guardando manga completo: " + manga.getTitle());
        long result = db.insertWithOnConflict(DatabaseHelper.TABLE_READ_MANGAS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return result;
    }

    public List<Manga> getAllReadMangas() {
        List<Manga> mangaList = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String query = "SELECT * FROM " + DatabaseHelper.TABLE_READ_MANGAS +
                " ORDER BY " + DatabaseHelper.COLUMN_READ_DATE + " DESC";

        Cursor cursor = db.rawQuery(query, null);
        Log.d(TAG, "Recuperando mangas. Total: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                Manga manga = new Manga();
                manga.setId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MANGA_ID)));
                manga.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TITLE)));
                manga.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION)));
                manga.setCoverUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COVER_URL)));
                mangaList.add(manga);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return mangaList;
    }

    // ✅ NUEVO - Obtener imagen de portada guardada
    public Bitmap getMangaCoverImage(String mangaId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String query = "SELECT " + DatabaseHelper.COLUMN_COVER_IMAGE +
                " FROM " + DatabaseHelper.TABLE_READ_MANGAS +
                " WHERE " + DatabaseHelper.COLUMN_MANGA_ID + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{mangaId});
        Bitmap bitmap = null;

        if (cursor.moveToFirst()) {
            String base64Image = cursor.getString(0);
            if (base64Image != null && !base64Image.isEmpty()) {
                bitmap = decodeBase64ToBitmap(base64Image);
            }
        }

        cursor.close();
        db.close();
        return bitmap;
    }

    // ==================== MÉTODOS PARA CAPÍTULOS ====================

    // ✅ NUEVO - Guardar capítulo
    public long addChapter(Chapter chapter, String mangaId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(DatabaseHelper.CHAPTER_ID, chapter.getId());
        values.put(DatabaseHelper.CHAPTER_MANGA_ID, mangaId);
        values.put(DatabaseHelper.CHAPTER_NUMBER, chapter.getChapterNumber());
        values.put(DatabaseHelper.CHAPTER_TITLE, chapter.getTitle());
        values.put(DatabaseHelper.CHAPTER_PAGES_COUNT, chapter.getPages());
        values.put(DatabaseHelper.CHAPTER_PUBLISHED_AT, chapter.getPublishedAt());

        Log.d(TAG, "Guardando capítulo: " + chapter.getChapterNumber());
        long result = db.insertWithOnConflict(DatabaseHelper.TABLE_CHAPTERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return result;
    }

    // ✅ NUEVO - Obtener capítulos de un manga
    public List<Chapter> getChaptersByMangaId(String mangaId) {
        List<Chapter> chapters = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String query = "SELECT * FROM " + DatabaseHelper.TABLE_CHAPTERS +
                " WHERE " + DatabaseHelper.CHAPTER_MANGA_ID + " = ?" +
                " ORDER BY CAST(" + DatabaseHelper.CHAPTER_NUMBER + " AS REAL) ASC";

        Cursor cursor = db.rawQuery(query, new String[]{mangaId});
        Log.d(TAG, "Capítulos encontrados: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                Chapter chapter = new Chapter();
                chapter.setId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.CHAPTER_ID)));
                chapter.setChapterNumber(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.CHAPTER_NUMBER)));
                chapter.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.CHAPTER_TITLE)));
                chapter.setPages(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.CHAPTER_PAGES_COUNT)));
                chapter.setPublishedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.CHAPTER_PUBLISHED_AT)));
                chapters.add(chapter);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return chapters;
    }

    // ==================== MÉTODOS PARA PÁGINAS/IMÁGENES ====================

    // ✅ NUEVO - Guardar página con imagen
    public long addPageWithImage(String chapterId, int pageNumber, String imageUrl) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(DatabaseHelper.PAGE_CHAPTER_ID, chapterId);
        values.put(DatabaseHelper.PAGE_NUMBER, pageNumber);
        values.put(DatabaseHelper.PAGE_IMAGE_URL, imageUrl);

        // Descargar y guardar imagen
        String base64Image = downloadAndEncodeImage(imageUrl);
        if (base64Image != null) {
            values.put(DatabaseHelper.PAGE_IMAGE_DATA, base64Image);
            Log.d(TAG, "Página " + pageNumber + " guardada");
        }

        long result = db.insertWithOnConflict(DatabaseHelper.TABLE_PAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return result;
    }

    // ✅ NUEVO - Obtener páginas de un capítulo (como Bitmap)
    public List<Bitmap> getPageImagesByChapterId(String chapterId) {
        List<Bitmap> images = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String query = "SELECT " + DatabaseHelper.PAGE_IMAGE_DATA +
                " FROM " + DatabaseHelper.TABLE_PAGES +
                " WHERE " + DatabaseHelper.PAGE_CHAPTER_ID + " = ?" +
                " ORDER BY " + DatabaseHelper.PAGE_NUMBER + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{chapterId});
        Log.d(TAG, "Páginas encontradas: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                String base64Image = cursor.getString(0);
                if (base64Image != null && !base64Image.isEmpty()) {
                    Bitmap bitmap = decodeBase64ToBitmap(base64Image);
                    if (bitmap != null) {
                        images.add(bitmap);
                    }
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return images;
    }

    // ✅ NUEVO - Obtener URLs de páginas (para usar con adaptador existente)
    public List<String> getPageUrlsByChapterId(String chapterId) {
        List<String> urls = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String query = "SELECT " + DatabaseHelper.PAGE_IMAGE_URL +
                " FROM " + DatabaseHelper.TABLE_PAGES +
                " WHERE " + DatabaseHelper.PAGE_CHAPTER_ID + " = ?" +
                " ORDER BY " + DatabaseHelper.PAGE_NUMBER + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{chapterId});

        if (cursor.moveToFirst()) {
            do {
                urls.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return urls;
    }

    // ==================== MÉTODOS AUXILIARES ====================

    // ✅ Descargar imagen y convertir a Base64
    private String downloadAndEncodeImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return null;

        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();

            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();

            if (bitmap != null) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream); // 70% calidad
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                return Base64.encodeToString(byteArray, Base64.DEFAULT);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error descargando imagen: " + e.getMessage());
        }
        return null;
    }

    // ✅ Decodificar Base64 a Bitmap
    private Bitmap decodeBase64ToBitmap(String base64) {
        try {
            byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error decodificando imagen: " + e.getMessage());
            return null;
        }
    }

    // ✅ Verificar si un capítulo tiene páginas guardadas
    public boolean hasOfflinePages(String chapterId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_PAGES +
                " WHERE " + DatabaseHelper.PAGE_CHAPTER_ID + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{chapterId});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count > 0;
    }

    // ==================== MÉTODOS EXISTENTES ====================

    public boolean isMangaRead(String mangaId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String query = "SELECT 1 FROM " + DatabaseHelper.TABLE_READ_MANGAS +
                " WHERE " + DatabaseHelper.COLUMN_MANGA_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{mangaId});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    public ReadMangaInfo getMangaReadInfo(String mangaId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_READ_MANGAS +
                " WHERE " + DatabaseHelper.COLUMN_MANGA_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{mangaId});
        ReadMangaInfo info = null;

        if (cursor.moveToFirst()) {
            info = new ReadMangaInfo();
            info.lastChapter = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LAST_CHAPTER));
            info.status = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STATUS));
            info.readDate = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_READ_DATE));
        }

        cursor.close();
        db.close();
        return info;
    }

    public void updateLastChapter(String mangaId, String lastChapter) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_LAST_CHAPTER, lastChapter);
        values.put(DatabaseHelper.COLUMN_READ_DATE, dateFormat.format(new Date()));
        db.update(DatabaseHelper.TABLE_READ_MANGAS, values,
                DatabaseHelper.COLUMN_MANGA_ID + " = ?", new String[]{mangaId});
        db.close();
    }

    public void removeReadManga(String mangaId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_READ_MANGAS,
                DatabaseHelper.COLUMN_MANGA_ID + " = ?", new String[]{mangaId});
        db.close();
    }

    public int getCountByStatus(String status) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_READ_MANGAS +
                " WHERE " + DatabaseHelper.COLUMN_STATUS + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{status});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public void debugDatabase() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_READ_MANGAS, null);
        Log.d(TAG, "=== DEBUG BASE DE DATOS ===");
        Log.d(TAG, "Total mangas: " + cursor.getCount());
        cursor.close();
        db.close();
    }

    public static class ReadMangaInfo {
        public String lastChapter;
        public String status;
        public String readDate;
    }
}