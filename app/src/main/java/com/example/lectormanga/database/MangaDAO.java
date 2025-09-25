package com.example.lectormanga.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.lectormanga.model.Manga;

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

    // Agregar manga leído con logging mejorado
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

        // Logging para debug
        Log.d(TAG, "Guardando manga:");
        Log.d(TAG, "ID: " + manga.getId());
        Log.d(TAG, "Título: " + manga.getTitle());
        Log.d(TAG, "Descripción: " + manga.getDescription());
        Log.d(TAG, "Cover URL: " + manga.getCoverUrl());
        Log.d(TAG, "Capítulo: " + lastChapter);

        long result = db.insertWithOnConflict(DatabaseHelper.TABLE_READ_MANGAS, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        Log.d(TAG, "Resultado guardado: " + result);

        db.close();
        return result;
    }

    // Obtener todos los mangas leídos con mejor manejo de nulos
    public List<Manga> getAllReadMangas() {
        List<Manga> mangaList = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String query = "SELECT " +
                DatabaseHelper.COLUMN_MANGA_ID + ", " +
                DatabaseHelper.COLUMN_TITLE + ", " +
                DatabaseHelper.COLUMN_DESCRIPTION + ", " +
                DatabaseHelper.COLUMN_COVER_URL + ", " +
                DatabaseHelper.COLUMN_LAST_CHAPTER + ", " +
                DatabaseHelper.COLUMN_STATUS + ", " +
                DatabaseHelper.COLUMN_READ_DATE +
                " FROM " + DatabaseHelper.TABLE_READ_MANGAS +
                " ORDER BY " + DatabaseHelper.COLUMN_READ_DATE + " DESC";

        Cursor cursor = db.rawQuery(query, null);

        Log.d(TAG, "Recuperando mangas de BD. Total encontrados: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                Manga manga = new Manga();

                // Manejo seguro de nulos
                String mangaId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MANGA_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TITLE));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION));
                String coverUrl = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COVER_URL));

                manga.setId(mangaId != null ? mangaId : "");
                manga.setTitle(title != null ? title : "Sin título");
                manga.setDescription(description != null ? description : "Sin descripción");
                manga.setCoverUrl(coverUrl != null ? coverUrl : "");

                // Logging para debug
                Log.d(TAG, "Manga recuperado:");
                Log.d(TAG, "ID: " + manga.getId());
                Log.d(TAG, "Título: " + manga.getTitle());
                Log.d(TAG, "Cover URL: " + manga.getCoverUrl());

                mangaList.add(manga);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        Log.d(TAG, "Total mangas devueltos: " + mangaList.size());
        return mangaList;
    }

    // Método de debug para inspeccionar la base de datos
    public void debugDatabase() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_READ_MANGAS, null);

        Log.d(TAG, "=== DEBUG BASE DE DATOS ===");
        Log.d(TAG, "Total registros en BD: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            int count = 1;
            do {
                Log.d(TAG, "--- Registro " + count + " ---");
                Log.d(TAG, "ID: " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MANGA_ID)));
                Log.d(TAG, "Título: " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TITLE)));
                Log.d(TAG, "Descripción: " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION)));
                Log.d(TAG, "Cover URL: " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COVER_URL)));
                Log.d(TAG, "Último capítulo: " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LAST_CHAPTER)));
                Log.d(TAG, "Estado: " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STATUS)));
                Log.d(TAG, "Fecha: " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_READ_DATE)));
                count++;
            } while (cursor.moveToNext());
        } else {
            Log.d(TAG, "No hay registros en la base de datos");
        }

        Log.d(TAG, "=== FIN DEBUG ===");

        cursor.close();
        db.close();
    }

    // Verificar si un manga ya está en la lista de leídos
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

    // Obtener información adicional de manga leído
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

    // Actualizar último capítulo leído
    public void updateLastChapter(String mangaId, String lastChapter) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_LAST_CHAPTER, lastChapter);
        values.put(DatabaseHelper.COLUMN_READ_DATE, dateFormat.format(new Date()));

        db.update(DatabaseHelper.TABLE_READ_MANGAS, values,
                DatabaseHelper.COLUMN_MANGA_ID + " = ?", new String[]{mangaId});
        db.close();
    }

    // Eliminar manga de la lista
    public void removeReadManga(String mangaId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_READ_MANGAS,
                DatabaseHelper.COLUMN_MANGA_ID + " = ?", new String[]{mangaId});
        db.close();
    }

    // Obtener conteo de mangas por estado
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

    // Clase auxiliar para información de lectura
    public static class ReadMangaInfo {
        public String lastChapter;
        public String status;
        public String readDate;
    }
}