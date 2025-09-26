package com.example.lectormanga.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "manga_reader.db";
    private static final int DATABASE_VERSION = 3; // ✅ INCREMENTADO

    // ========== TABLA DE MANGAS LEÍDOS ==========
    public static final String TABLE_READ_MANGAS = "read_mangas";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_MANGA_ID = "manga_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_COVER_URL = "cover_url";
    public static final String COLUMN_COVER_IMAGE = "cover_image"; // ✅ NUEVO - Imagen en Base64
    public static final String COLUMN_READ_DATE = "read_date";
    public static final String COLUMN_LAST_CHAPTER = "last_chapter";
    public static final String COLUMN_STATUS = "status";

    // ========== TABLA DE CAPÍTULOS ==========
    public static final String TABLE_CHAPTERS = "chapters";
    public static final String CHAPTER_ID = "chapter_id";
    public static final String CHAPTER_MANGA_ID = "manga_id"; // FK a read_mangas
    public static final String CHAPTER_NUMBER = "chapter_number";
    public static final String CHAPTER_TITLE = "chapter_title";
    public static final String CHAPTER_PAGES_COUNT = "pages_count";
    public static final String CHAPTER_PUBLISHED_AT = "published_at";

    // ========== TABLA DE PÁGINAS/IMÁGENES ==========
    public static final String TABLE_PAGES = "pages";
    public static final String PAGE_ID = "page_id";
    public static final String PAGE_CHAPTER_ID = "chapter_id"; // FK a chapters
    public static final String PAGE_NUMBER = "page_number";
    public static final String PAGE_IMAGE_URL = "image_url";
    public static final String PAGE_IMAGE_DATA = "image_data"; // ✅ Imagen en Base64

    // ========== SCRIPTS DE CREACIÓN ==========
    private static final String CREATE_TABLE_READ_MANGAS =
            "CREATE TABLE " + TABLE_READ_MANGAS + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_MANGA_ID + " TEXT UNIQUE, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_COVER_URL + " TEXT, " +
                    COLUMN_COVER_IMAGE + " TEXT, " + // ✅ Base64
                    COLUMN_READ_DATE + " TEXT, " +
                    COLUMN_LAST_CHAPTER + " TEXT, " +
                    COLUMN_STATUS + " TEXT" +
                    ")";

    private static final String CREATE_TABLE_CHAPTERS =
            "CREATE TABLE " + TABLE_CHAPTERS + "(" +
                    CHAPTER_ID + " TEXT PRIMARY KEY, " + // ID del capítulo de MangaDex
                    CHAPTER_MANGA_ID + " TEXT, " +
                    CHAPTER_NUMBER + " TEXT, " +
                    CHAPTER_TITLE + " TEXT, " +
                    CHAPTER_PAGES_COUNT + " INTEGER, " +
                    CHAPTER_PUBLISHED_AT + " TEXT, " +
                    "FOREIGN KEY(" + CHAPTER_MANGA_ID + ") REFERENCES " +
                    TABLE_READ_MANGAS + "(" + COLUMN_MANGA_ID + ") ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_TABLE_PAGES =
            "CREATE TABLE " + TABLE_PAGES + "(" +
                    PAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    PAGE_CHAPTER_ID + " TEXT, " +
                    PAGE_NUMBER + " INTEGER, " +
                    PAGE_IMAGE_URL + " TEXT, " +
                    PAGE_IMAGE_DATA + " TEXT, " + // ✅ Base64
                    "FOREIGN KEY(" + PAGE_CHAPTER_ID + ") REFERENCES " +
                    TABLE_CHAPTERS + "(" + CHAPTER_ID + ") ON DELETE CASCADE" +
                    ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_READ_MANGAS);
        db.execSQL(CREATE_TABLE_CHAPTERS);
        db.execSQL(CREATE_TABLE_PAGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAPTERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_READ_MANGAS);
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
}