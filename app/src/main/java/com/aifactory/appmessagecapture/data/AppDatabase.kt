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
    entities = [NotificationEntity::class, BillEntity::class, BirthdayEntity::class, RecurringBillEntity::class, PlatformAccountEntity::class],
    version = 12,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao
    abstract fun billDao(): BillDao
    abstract fun birthdayDao(): BirthdayDao
    abstract fun recurringBillDao(): RecurringBillDao
    abstract fun platformAccountDao(): PlatformAccountDao

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
         * Migrate from v9 to v10:
         * Rename legacy short category names to current full names.
         * E.g. "餐饮" → "餐饮美食", "工资" → "工资薪金".
         */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                BirthdayLog.i("[DB Migration] Executing MIGRATION_9_10: migrating category names")
                migrateCategoryNames(db)
                BirthdayLog.i("[DB Migration] MIGRATION_9_10 completed")
            }
        }

        /**
         * Shared helper: executes UPDATE statements to rename old short category
         * names to the current full category names defined in Categories.kt.
         */
        private fun migrateCategoryNames(db: SupportSQLiteDatabase) {
            val updates = arrayOf(
                // Expense categories
                "UPDATE bills SET category = '餐饮美食' WHERE category = '餐饮'",
                "UPDATE bills SET category = '交通出行' WHERE category = '交通'",
                "UPDATE bills SET category = '购物消费' WHERE category = '购物'",
                "UPDATE bills SET category = '休闲娱乐' WHERE category = '娱乐'",
                "UPDATE bills SET category = '居家生活' WHERE category = '生活缴费'",
                "UPDATE bills SET category = '医疗健康' WHERE category = '医疗'",
                "UPDATE bills SET category = '其他支出' WHERE category = '其他'",
                // Income categories
                "UPDATE bills SET category = '工资薪金' WHERE category = '工资'",
                "UPDATE bills SET category = '退款返现' WHERE category = '退款'",
                "UPDATE bills SET category = '红包转账' WHERE category = '红包'",
                "UPDATE bills SET category = '投资理财' WHERE category = '理财收益'",
                "UPDATE bills SET category = '其他收入' WHERE category = '转账'",
            )
            for (sql in updates) {
                db.execSQL(sql)
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

        /**
         * Migrate from v10 to v11:
         * Create `recurring_bills` table for recurring/periodic bill templates.
         */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                BirthdayLog.i("[DB Migration] Executing MIGRATION_10_11: creating recurring_bills table")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recurring_bills (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        amount REAL NOT NULL,
                        category TEXT NOT NULL DEFAULT '未分类',
                        isIncome INTEGER NOT NULL DEFAULT 0,
                        frequency TEXT NOT NULL DEFAULT 'MONTHLY',
                        startDate INTEGER NOT NULL,
                        nextDueDate INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                BirthdayLog.i("[DB Migration] MIGRATION_10_11 completed")
            }
        }

        /**
         * Migrate from v11 to v12:
         * 1. Create `platform_accounts` table for per-platform balance tracking.
         * 2. Add `platformAccountId` column to `bills` (nullable; null = 待对账).
         */
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                BirthdayLog.i("[DB Migration] Executing MIGRATION_11_12: creating platform_accounts table + bills.platformAccountId")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS platform_accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        balance REAL NOT NULL,
                        icon TEXT,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE bills ADD COLUMN platformAccountId INTEGER")
                BirthdayLog.i("[DB Migration] MIGRATION_11_12 completed")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notification_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .build()
                INSTANCE = instance
                BirthdayLog.i("AppDatabase initialized. Version=12")
                instance
            }
        }
    }
}
