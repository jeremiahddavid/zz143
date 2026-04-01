package com.zz143.core.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class ZZ143Database(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "zz143.db"
        const val DB_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE sessions (
                session_id       TEXT PRIMARY KEY,
                started_at_ms    INTEGER NOT NULL,
                ended_at_ms      INTEGER,
                app_version      TEXT NOT NULL,
                sdk_version      TEXT NOT NULL,
                device_model     TEXT NOT NULL,
                os_version       TEXT NOT NULL,
                is_active        INTEGER NOT NULL DEFAULT 1
            )
        """)

        db.execSQL("""
            CREATE TABLE events (
                event_id         TEXT PRIMARY KEY,
                session_id       TEXT NOT NULL,
                event_type       INTEGER NOT NULL,
                screen_id        TEXT NOT NULL,
                timestamp_ms     INTEGER NOT NULL,
                uptime_ms        INTEGER NOT NULL,
                encoded_payload  BLOB NOT NULL,
                batch_id         TEXT
            )
        """)
        db.execSQL("CREATE INDEX idx_events_session_time ON events(session_id, timestamp_ms)")
        db.execSQL("CREATE INDEX idx_events_screen ON events(screen_id, timestamp_ms)")
        db.execSQL("CREATE INDEX idx_events_type ON events(event_type, timestamp_ms)")

        db.execSQL("""
            CREATE TABLE semantic_actions (
                action_id        TEXT PRIMARY KEY,
                event_id         TEXT NOT NULL,
                session_id       TEXT NOT NULL,
                action_type      TEXT NOT NULL,
                screen_id        TEXT NOT NULL,
                timestamp_ms     INTEGER NOT NULL,
                target_element_id TEXT,
                parameters_json  TEXT,
                action_source    INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX idx_actions_type ON semantic_actions(action_type, timestamp_ms)")
        db.execSQL("CREATE INDEX idx_actions_session ON semantic_actions(session_id, timestamp_ms)")

        db.execSQL("""
            CREATE TABLE workflows (
                workflow_id      TEXT PRIMARY KEY,
                name             TEXT NOT NULL,
                description      TEXT NOT NULL,
                steps_json       TEXT NOT NULL,
                frequency_type   INTEGER NOT NULL,
                frequency_json   TEXT NOT NULL,
                confidence_score REAL NOT NULL,
                first_seen_ms    INTEGER NOT NULL,
                last_seen_ms     INTEGER NOT NULL,
                execution_count  INTEGER NOT NULL DEFAULT 0,
                automation_count INTEGER NOT NULL DEFAULT 0,
                success_rate     REAL NOT NULL DEFAULT 0.0,
                status           INTEGER NOT NULL,
                version          INTEGER NOT NULL DEFAULT 1
            )
        """)
        db.execSQL("CREATE INDEX idx_workflows_status ON workflows(status)")
        db.execSQL("CREATE INDEX idx_workflows_confidence ON workflows(confidence_score DESC)")

        db.execSQL("""
            CREATE TABLE workflow_instances (
                instance_id      TEXT PRIMARY KEY,
                workflow_id      TEXT NOT NULL,
                session_id       TEXT NOT NULL,
                started_at_ms    INTEGER NOT NULL,
                completed_at_ms  INTEGER,
                status           INTEGER NOT NULL,
                steps_completed  INTEGER NOT NULL DEFAULT 0,
                error_json       TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE suggestions (
                suggestion_id    TEXT PRIMARY KEY,
                workflow_id      TEXT NOT NULL,
                display_type     INTEGER NOT NULL,
                title            TEXT NOT NULL,
                description      TEXT NOT NULL,
                created_at_ms    INTEGER NOT NULL,
                expires_at_ms    INTEGER NOT NULL,
                shown_at_ms      INTEGER,
                responded_at_ms  INTEGER,
                response         INTEGER,
                priority         INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX idx_suggestions_workflow ON suggestions(workflow_id)")

        db.execSQL("""
            CREATE TABLE user_preferences (
                preference_key   TEXT PRIMARY KEY,
                value_text       TEXT NOT NULL,
                updated_at_ms    INTEGER NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE pattern_ngrams (
                ngram_hash       TEXT PRIMARY KEY,
                action_types     TEXT NOT NULL,
                ngram_size       INTEGER NOT NULL,
                count            INTEGER NOT NULL DEFAULT 1,
                first_seen_ms    INTEGER NOT NULL,
                last_seen_ms     INTEGER NOT NULL,
                avg_interval_ms  REAL,
                screens_json     TEXT
            )
        """)
        db.execSQL("CREATE INDEX idx_ngrams_count ON pattern_ngrams(count DESC)")
        db.execSQL("CREATE INDEX idx_ngrams_size ON pattern_ngrams(ngram_size)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future migrations
    }
}
