package com.ferji.inspecciones.di

import com.ferji.inspecciones.data.dao.HabitacionDao
import com.ferji.inspecciones.data.dao.InspeccionDao
import com.ferji.inspecciones.data.repository.HabitacionRepository
import com.ferji.inspecciones.data.repository.InspeccionRepository
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
}