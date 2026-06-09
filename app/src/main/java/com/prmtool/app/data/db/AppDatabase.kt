package com.prmtool.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v2 → v3: add the email, source-details and company-enrichment columns to `contacts`.
 * Done as an additive ALTER so existing drafts, events and sources are preserved on upgrade.
 */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE contacts ADD COLUMN email TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE contacts ADD COLUMN sourceDetails TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE contacts ADD COLUMN companyLinkedinUrl TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE contacts ADD COLUMN companyEmployees INTEGER")
        db.execSQL("ALTER TABLE contacts ADD COLUMN companyAddress TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE contacts ADD COLUMN companyEnrichedJson TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [ContactEntity::class, EventEntity::class, SourceEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun eventDao(): EventDao
    abstract fun sourceDao(): SourceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prm.db"
                )
                    // v2→v3 preserves data via MIGRATION_2_3; destructive fallback covers any
                    // older/unknown schema only.
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            // Seed default sources. "Event" is auto-added when an event is selected.
                            listOf("Event", "Referral", "Cold outreach", "Online").forEach {
                                db.execSQL("INSERT INTO sources (name) VALUES ('$it')")
                            }
                        }
                    }).build().also { INSTANCE = it }
            }
    }
}
