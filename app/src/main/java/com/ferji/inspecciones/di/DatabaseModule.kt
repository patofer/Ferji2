package com.ferji.inspecciones.di

import android.content.Context
import androidx.room.Room
import com.ferji.inspecciones.data.database.AppDatabase
import com.ferji.inspecciones.data.dao.HabitacionDao
import com.ferji.inspecciones.data.dao.InspeccionDao
import com.ferji.inspecciones.data.dao.PartidaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext, // Es buena práctica usar applicationContext
            AppDatabase::class.java,
            "ferji_inspecciones_db" // <--- USA EL NOMBRE CONSISTENTE
        )
            // .addMigrations(MIGRATION_X_Y, ...) // Si tuvieras migraciones explícitas
            .fallbackToDestructiveMigration()  // <--- AÑADE ESTO AQUÍ
            .build()
    }

    @Provides
    fun provideInspeccionDao(database: AppDatabase): InspeccionDao {
        return database.inspeccionDao()
    }

    @Provides
    fun provideHabitacionDao(database: AppDatabase): HabitacionDao {
        return database.habitacionDao()
    }

    @Provides
    @Singleton // Opcional pero recomendado para DAOs
    fun providePartidaDao(database: AppDatabase): PartidaDao {
        return database.partidaDao() // <-- Asume que tienes un método así en tu clase AppDatabase
    }

    @Provides
    @Singleton // Es buena práctica que los DAOs sean singletons
    fun providePartidaPrincipalDao(database: AppDatabase): com.ferji.inspecciones.data.dao.PartidaPrincipalDao {
        return database.partidaPrincipalDao()
    }

    @Provides
    @Singleton // Es buena práctica que los DAOs sean singletons
    fun provideUserDao(database: AppDatabase): com.ferji.inspecciones.data.dao.UserDao {
        return database.userDao()
    }
}