package com.ferji.inspecciones.di // O tu paquete de módulos Dagger/Hilt

import com.ferji.inspecciones.BuildConfig
import com.ferji.inspecciones.data.network.interceptors.AuthorizationInterceptor
import com.ferji.inspecciones.data.network.sendgrid.SendGridApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory // Si usas Gson

// import com.squareup.moshi.Moshi // Si usas Moshi
// import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory // Si usas Moshi
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ¡¡¡IMPORTANTE!!! NO USES LA API KEY DIRECTAMENTE AQUÍ EN PRODUCCIÓN.
    // Obténla de BuildConfig, variables de entorno de CI, o un lugar seguro.
  //  private const val SENDGRID_API_KEY = "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"
    private const val SENDGRID_BASE_URL = "https://api.sendgrid.com/"

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Para ver requests/responses detallados en Logcat
        }
    }


    @Provides
    @Singleton
    fun provideAuthorizationInterceptor(): AuthorizationInterceptor {
        // --- [2] USA BuildConfig.SENDGRID_API_KEY ---
        if (BuildConfig.SENDGRID_API_KEY.isEmpty()) {
            throw IllegalStateException("SENDGRID_API_KEY no está configurada en local.properties o BuildConfig. Verifica tu configuración.")
        }
        return AuthorizationInterceptor(BuildConfig.SENDGRID_API_KEY)
        // --- FIN DEL CAMBIO ---
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthorizationInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)       // Interceptor de autorización primero
            .addInterceptor(loggingInterceptor) // Luego el de logging
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /* // Si usas Moshi
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }
    */

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient /*, moshi: Moshi // si usas Moshi */): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SENDGRID_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()) // Si usas Gson
            // .addConverterFactory(MoshiConverterFactory.create(moshi)) // Si usas Moshi
            .build()
    }

    @Provides
    @Singleton
    fun provideSendGridApiService(retrofit: Retrofit): SendGridApiService {
        return retrofit.create(SendGridApiService::class.java)
    }
}
