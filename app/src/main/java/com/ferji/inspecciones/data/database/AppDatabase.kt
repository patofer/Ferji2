package com.ferji.inspecciones.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ferji.inspecciones.data.dao.HabitacionDao
import com.ferji.inspecciones.data.dao.InspeccionDao
import com.ferji.inspecciones.data.dao.PartidaDao
import com.ferji.inspecciones.data.dao.PartidaPrincipalDao
import com.ferji.inspecciones.data.dao.UserDao
import com.ferji.inspecciones.data.model.DanoPartidaCrossRef
import com.ferji.inspecciones.data.model.HabitacionEntity
import com.ferji.inspecciones.data.model.InspeccionEntity
import com.ferji.inspecciones.data.model.PartidaEntity
import com.ferji.inspecciones.data.model.PartidaPrincipalEntity
import com.ferji.inspecciones.data.model.UserEntity

@Database(
    entities = [
        InspeccionEntity::class,
        HabitacionEntity::class,
        PartidaEntity::class,
        DanoPartidaCrossRef::class,
        PartidaPrincipalEntity::class,
        UserEntity::class
    ],
    version = 17,
    exportSchema = false // Recomendado mantener en false para desarrollo.
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
                            // Código que se ejecuta cuando la BD se crea por primera vez.
                            // Útil para pre-poblar datos.
                        }
                    })
                    // fallbackToDestructiveMigration() es muy útil durante el desarrollo.
                    // Borra y recrea la base de datos si aumentas la versión sin
                    // proporcionar una migración, evitando crashes.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
