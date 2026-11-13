package com.example.sportik.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Club::class, Coach::class, Fighter::class, Tournament::class,
        Fight::class, FightResult::class, AuditLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sorev.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(InitCallback(context.applicationContext))
                    .build()
                    .also { instance = it }
            }
    }
}

/**
 * Миграция 1 → 2: в таблицу Fight добавляются два участника (fighter1Id, fighter2Id).
 * Данные сохраняются; если у старого поединка есть результат, победитель становится первым участником.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Fight ADD COLUMN fighter1Id INTEGER REFERENCES Fighter(id) ON UPDATE NO ACTION ON DELETE RESTRICT")
        db.execSQL("ALTER TABLE Fight ADD COLUMN fighter2Id INTEGER REFERENCES Fighter(id) ON UPDATE NO ACTION ON DELETE RESTRICT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_Fight_fighter1Id ON Fight (fighter1Id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_Fight_fighter2Id ON Fight (fighter2Id)")
        db.execSQL(
            "UPDATE Fight SET fighter1Id = (SELECT r.fighterId FROM Result r WHERE r.fightId = Fight.id) " +
                "WHERE EXISTS (SELECT 1 FROM Result r WHERE r.fightId = Fight.id)"
        )
    }
}

/**
 * Обратные вызовы БД: при создании файла заполняет таблицы начальными данными (assets/db/seed.sql),
 * при каждом открытии проверяет наличие триггеров (assets/db/triggers.sql).
 * Триггеры создаются после заполнения, чтобы журнал изменений начинался пустым.
 */
private class InitCallback(private val context: Context) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        runScript(db, "db/seed.sql")
    }

    // Триггеры создаются командами CREATE TRIGGER IF NOT EXISTS: при первом запуске они появляются
    // сразу после заполнения таблиц, а при обновлении приложения недостающие триггеры добавляются.
    override fun onOpen(db: SupportSQLiteDatabase) {
        runScript(db, "db/triggers.sql")
    }

    private fun runScript(db: SupportSQLiteDatabase, assetPath: String) {
        val text = context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
        text.split("#####")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { db.execSQL(it) }
    }
}
