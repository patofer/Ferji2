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
import com.ferji.inspecciones.data.dao.PartidaDao
import com.ferji.inspecciones.data.dao.PartidaPrincipalDao
import com.ferji.inspecciones.data.dao.UserDao
import com.ferji.inspecciones.data.model.DanoPartidaCrossRef
import com.ferji.inspecciones.data.model.PartidaEntity
import com.ferji.inspecciones.data.model.HabitacionEntity // Asegúrate de importar tu entidad Habitación
import com.ferji.inspecciones.data.model.InspeccionEntity
import com.ferji.inspecciones.data.model.PartidaPrincipalEntity
import com.ferji.inspecciones.data.model.UserEntity



@Database(
    entities = [
        InspeccionEntity::class,
        HabitacionEntity::class,
        PartidaEntity::class,       // <-- ENTIDAD AÑADIDA
        DanoPartidaCrossRef::class ,
        PartidaPrincipalEntity::class,
        UserEntity::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun inspeccionDao(): InspeccionDao
    abstract fun habitacionDao(): HabitacionDao
    abstract fun partidaDao(): PartidaDao
    abstract fun partidaPrincipalDao(): PartidaPrincipalDao
    abstract fun userDao(): UserDao

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
