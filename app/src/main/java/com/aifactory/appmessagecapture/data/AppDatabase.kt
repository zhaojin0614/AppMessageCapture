package com.aifactory.appmessagecapture.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aifactory.appmessagecapture.birthday.data.BirthdayDao
import com.aifactory.appmessagecapture.birthday.data.BirthdayEntity
import com.aifactory.appmessagecapture.birthday.data.Converters
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog

/**
 * Room database for locally storing captured notifications and birthday records.
 */
@Database(
    entities = [NotificationEntity::class, BillEntity::class, BirthdayEntity::class],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao
    abstract fun billDao(): BillDao
    abstract fun birthdayDao(): BirthdayDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migrate from v2 to v3:
         * Removed the `isConfirmed` column from the `bills` table.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                BirthdayLog.i("[DB Migration] Executing MIGRATION_2_3")
                db.execSQL(
                    """
                    CREATE TABLE bills_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        appName TEXT NOT NULL,
                        packageName TEXT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT '未分类',
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO bills_new (id, amount, appName, packageName, title, content, category, timestamp)
                    SELECT id, amount, appName, packageName, title, content, category, timestamp FROM bills
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE bills")
                db.execSQL("ALTER TABLE bills_new RENAME TO bills")
                BirthdayLog.i("[DB Migration] MIGRATION_2_3 completed")
            }
        }

        /**
         * Migrate from v3 to v4:
         * Added `secondaryAppName` and `secondaryPackageName` columns to `bills` table
         * for merging duplicate bills from different apps at the same time.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                BirthdayLog.i("[DB Migration] Executing MIGRATION_3_4")
                db.execSQL("ALTER TABLE bills ADD COLUMN secondaryAppName TEXT")
                db.execSQL("ALTER TABLE bills ADD COLUMN secondaryPackageName TEXT")
                BirthdayLog.i("[DB Migration] MIGRATION_3_4 completed")
            }
        }

        /**
         * Migrate from v4 to v5:
         * Added `birthdays` table for BirthdayKeeper module.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                BirthdayLog.i("[DB Migration] Executing MIGRATION_4_5: creating birthdays table")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS birthdays (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        isLunar INTEGER NOT NULL,
                        birthYear INTEGER,
                        birthMonth INTEGER NOT NULL,
                        birthDay INTEGER NOT NULL,
                        reminderType TEXT NOT NULL DEFAULT 'ON_DAY',
                        reminderTime TEXT
                    )
                    """.trimIndent()
                )
                BirthdayLog.i("[DB Migration] MIGRATION_4_5 completed. birthdays table created.")
            }
        }

        /**
         * Migrate from v5 to v6:
         * Removed the `content` column from the `bills` table.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                BirthdayLog.i("[DB Migration] Executing MIGRATION_6_7: adding isIncome column to bills")
                db.execSQL("ALTER TABLE bills ADD COLUMN isIncome INTEGER NOT NULL DEFAULT 0")
                BirthdayLog.i("[DB Migration] MIGRATION_6_7 completed")
            }
        }

        /**
         * Migrate from v7 to v8:
         * Removed the `isRead` column from the `notifications` table.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                BirthdayLog.i("[DB Migration] Executing MIGRATION_7_8: removing isRead column from notifications")
                db.execSQL(
                    """
                    CREATE TABLE notifications_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        appName TEXT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO notifications_new (id, packageName, appName, title, content, timestamp)
                    SELECT id, packageName, appName, title, content, timestamp FROM notifications
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE notifications")
                db.execSQL("ALTER TABLE notifications_new RENAME TO notifications")
                BirthdayLog.i("[DB Migration] MIGRATION_7_8 completed")
            }
        }

        /**
         * Migrate from v8 to v9:
         * No schema changes needed — categories are stored as strings.
         * Existing data retains original categories; users can update manually.
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                BirthdayLog.i("[DB Migration] Executing MIGRATION_8_9: no schema change needed")
            }
        }

        /**
         * Migrate from v5 to v6:
         * Removed the `content` column from the `bills` table.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                BirthdayLog.i("[DB Migration] Executing MIGRATION_5_6: removing content column from bills")
                db.execSQL(
                    """
                    CREATE TABLE bills_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        appName TEXT NOT NULL,
                        packageName TEXT NOT NULL,
                        secondaryAppName TEXT,
                        secondaryPackageName TEXT,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT '未分类',
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO bills_new (id, amount, appName, packageName, secondaryAppName, secondaryPackageName, title, category, timestamp)
                    SELECT id, amount, appName, packageName, secondaryAppName, secondaryPackageName, title, category, timestamp FROM bills
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE bills")
                db.execSQL("ALTER TABLE bills_new RENAME TO bills")
                BirthdayLog.i("[DB Migration] MIGRATION_5_6 completed")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notification_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                INSTANCE = instance
                BirthdayLog.i("AppDatabase initialized. Version=9")
                instance
            }
        }
    }
}
