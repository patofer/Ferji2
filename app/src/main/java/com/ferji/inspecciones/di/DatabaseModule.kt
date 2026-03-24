package com.ferji.inspecciones.di

import android.content.Context
import androidx.room.Room
import com.ferji.inspecciones.data.database.AppDatabase
import com.ferji.inspecciones.data.dao.HabitacionDao
import com.ferji.inspecciones.data.dao.InspeccionDao
import com.ferji.inspecciones.data.dao.PartidaDao
import com.google.firebase.firestore.FirebaseFirestore // <-- 1. IMPORTAR FIREBASE
import com.google.firebase.firestore.ktx.firestore         // <-- 2. IMPORTAR FIREBASE KTX
import com.google.firebase.ktx.Firebase                   // <-- 3. IMPORTAR FIREBASE KTX
import com.google.gson.Gson
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
            context.applicationContext,
            AppDatabase::class.java,
            "ferji_inspecciones_db"
        )
            // ⚠️ PRODUCCIÓN: Reemplazar fallbackToDestructiveMigration() por migraciones
            // explícitas antes del lanzamiento comercial. Esta estrategia BORRA TODOS
            // LOS DATOS del usuario si se incrementa la versión de la BD sin migración.
            // Ejemplo:
            //   .addMigrations(MIGRATION_17_18, MIGRATION_18_19)
            .fallbackToDestructiveMigration()
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

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore
    }

    /**
     * Provee la instancia única de Gson.
     * Esto soluciona el error de compilación [Dagger/MissingBinding] para Gson.
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}