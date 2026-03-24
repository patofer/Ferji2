package com.ferji.inspecciones.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    exportSchema = true // Recomendado para producción: permite auditar migraciones.
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun inspeccionDao(): InspeccionDao
    abstract fun habitacionDao(): HabitacionDao
    abstract fun partidaDao(): PartidaDao
    abstract fun partidaPrincipalDao(): PartidaPrincipalDao
    abstract fun userDao(): UserDao

    // NOTA: El companion object con getDatabase() se ha eliminado porque la instancia
    // se provee exclusivamente via Hilt (DatabaseModule.provideAppDatabase).
    // Tener dos mecanismos de creación podía generar instancias duplicadas de la BD.
}
