package io.github.gopalsuryak.galaxywatch.mobile

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.github.gopalsuryak.galaxywatch.shared.HealthSample
import io.github.gopalsuryak.galaxywatch.shared.SampleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Everything stays on the phone in a local SQLite file. No account, no cloud.
 *
 * Re-sent batches are harmless: a sample is unique by (type, start, end), so a duplicate
 * just overwrites itself.
 */
class SampleStore private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "samples.db", null, 1) {

    private val _version = MutableStateFlow(0L)

    /** Bumps every time new samples land, so the UI knows to reload. */
    val version: StateFlow<Long> = _version.asStateFlow()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE samples (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                value REAL NOT NULL,
                start_ms INTEGER NOT NULL,
                end_ms INTEGER NOT NULL,
                UNIQUE(type, start_ms, end_ms) ON CONFLICT REPLACE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_samples_type_end ON samples(type, end_ms)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun insert(samples: List<HealthSample>) {
        if (samples.isEmpty()) return
        writableDatabase.run {
            beginTransaction()
            try {
                for (s in samples) {
                    insert("samples", null, ContentValues().apply {
                        put("type", s.type.name)
                        put("value", s.value)
                        put("start_ms", s.startMs)
                        put("end_ms", s.endMs)
                    })
                }
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
        _version.value += 1
    }

    fun latest(type: SampleType): HealthSample? = query(
        "SELECT type, value, start_ms, end_ms FROM samples WHERE type = ? ORDER BY end_ms DESC LIMIT 1",
        arrayOf(type.name),
    ).firstOrNull()

    fun recent(limit: Int): List<HealthSample> = query(
        "SELECT type, value, start_ms, end_ms FROM samples ORDER BY end_ms DESC LIMIT ?",
        arrayOf(limit.toString()),
    )

    fun all(): List<HealthSample> = query(
        "SELECT type, value, start_ms, end_ms FROM samples ORDER BY end_ms ASC",
        emptyArray(),
    )

    private fun query(sql: String, args: Array<String>): List<HealthSample> =
        readableDatabase.rawQuery(sql, args).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val type = SampleType.entries.firstOrNull { it.name == c.getString(0) } ?: continue
                    add(HealthSample(type, c.getDouble(1), c.getLong(2), c.getLong(3)))
                }
            }
        }

    companion object {
        @Volatile private var instance: SampleStore? = null

        fun get(context: Context): SampleStore =
            instance ?: synchronized(this) { instance ?: SampleStore(context).also { instance = it } }
    }
}
