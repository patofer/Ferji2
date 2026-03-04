package com.ferji.inspecciones.di

import com.ferji.inspecciones.data.dao.HabitacionDao
import com.ferji.inspecciones.data.dao.InspeccionDao
import com.ferji.inspecciones.data.dao.PartidaDao
import com.ferji.inspecciones.data.dao.PartidaPrincipalDao
import com.ferji.inspecciones.data.database.AppDatabase
import com.ferji.inspecciones.data.remote.PartidaRemoteDataSource // <-- IMPORTAR
import com.ferji.inspecciones.data.repository.HabitacionRepository
import com.ferji.inspecciones.data.repository.InspeccionRepository
import com.ferji.inspecciones.data.repository.PartidaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideInspeccionRepository(inspeccionDao: InspeccionDao): InspeccionRepository {
        return InspeccionRepository(inspeccionDao)
    }

    @Provides
    @Singleton
    fun provideHabitacionRepository(habitacionDao: HabitacionDao): HabitacionRepository {
        return HabitacionRepository(habitacionDao)
    }

    /**
     * Provee una instancia del repositorio de Partidas.
     * Hilt ya sabe cómo crear PartidaDao, PartidaPrincipalDao y PartidaRemoteDataSource,
     * así que puede inyectarlos aquí automáticamente.
     */
    @Provides
    @Singleton
    fun providePartidaRepository(
        partidaDao: PartidaDao,
        partidaPrincipalDao: PartidaPrincipalDao,
        remoteDataSource: PartidaRemoteDataSource,
        database: AppDatabase
    ): PartidaRepository {
        // Los parámetros ahora coinciden con el nuevo constructor de la clase
        return PartidaRepository(partidaDao, partidaPrincipalDao, remoteDataSource,database)
    }
}
