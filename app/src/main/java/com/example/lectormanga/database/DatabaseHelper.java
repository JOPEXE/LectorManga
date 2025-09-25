package com.example.lectormanga.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "manga_reader.db";
    private static final int DATABASE_VERSION = 1;

    // Tabla de mangas leídos
    public static final String TABLE_READ_MANGAS = "read_mangas";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_MANGA_ID = "manga_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_COVER_URL = "cover_url";
    public static final String COLUMN_READ_DATE = "read_date";
    public static final String COLUMN_LAST_CHAPTER = "last_chapter";
    public static final String COLUMN_STATUS = "status"; // reading, completed, paused

    // Script de creación de tabla
    private static final String CREATE_TABLE_READ_MANGAS =
            "CREATE TABLE " + TABLE_READ_MANGAS + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_MANGA_ID + " TEXT UNIQUE, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_COVER_URL + " TEXT, " +
                    COLUMN_READ_DATE + " TEXT, " +
                    COLUMN_LAST_CHAPTER + " TEXT, " +
                    COLUMN_STATUS + " TEXT" +
                    ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_READ_MANGAS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_READ_MANGAS);
        onCreate(db);
    }
}