
package com.ferji.inspecciones

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FerjiApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // --- INICIO DE LA CORRECCIÓN ---
    // En lugar de una función, ahora se implementa una propiedad 'val'.
    // Usamos 'override val' en lugar de 'override fun'.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    // --- FIN DE LA CORRECCIÓN ---
}