package com.aihub.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AccountEntity::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "aihub.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** 第二阶段：套餐来源、API 余额与同步状态 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN planSource TEXT NOT NULL DEFAULT 'MANUAL'")
                db.execSQL("ALTER TABLE accounts ADD COLUMN planImportedAt INTEGER")
                db.execSQL("ALTER TABLE accounts ADD COLUMN apiBalance REAL")
                db.execSQL("ALTER TABLE accounts ADD COLUMN apiBalanceCurrency TEXT")
                db.execSQL("ALTER TABLE accounts ADD COLUMN apiBalanceSyncedAt INTEGER")
                db.execSQL("ALTER TABLE accounts ADD COLUMN syncError TEXT")
            }
        }
    }
}
