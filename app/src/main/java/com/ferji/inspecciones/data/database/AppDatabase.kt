package com.ferji.inspecciones.data.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ferji.inspecciones.data.dao.HabitacionDao
import com.ferji.inspecciones.data.dao.InspeccionDao
import com.ferji.inspecciones.data.model.HabitacionEntity // Asegúrate de importar tu entidad Habitación
import com.ferji.inspecciones.data.model.InspeccionEntity

//val MIGRATION_1_2 = object : Migration(1, 2) {
//    override fun migrate(database: SupportSQLiteDatabase) {
//        // Aquí defines los cambios manualmente si son complejos
//        // database.execSQL("ALTER TABLE inspeccion ADD COLUMN nueva_columna TEXT")
//    }
//}

@Database(
    entities = [InspeccionEntity::class, HabitacionEntity::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun inspeccionDao(): InspeccionDao
    abstract fun habitacionDao(): HabitacionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ferji_inspecciones_db"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Código que se ejecuta cuando se crea la BD por primera vez
                        }
                    })
                    // Si acabas de añadir la entidad y la app ya se ha ejecutado antes
                    // con la versión 1 sin la tabla 'habitaciones', necesitarás
                    // incrementar la versión y posiblemente añadir una migración,
                    // o usar fallbackToDestructiveMigration() durante el desarrollo.
                    .fallbackToDestructiveMigration() // Opción para desarrollo
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
