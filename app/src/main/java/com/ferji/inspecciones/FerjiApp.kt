//package com.ferji.inspecciones
//
//
//import android.app.Application
//import androidx.hilt.work.HiltWorkerFactory
//import androidx.work.Configuration
//import dagger.hilt.android.HiltAndroidApp
//import javax.inject.Inject
//@HiltAndroidApp
//class FerjiApp : Application(), Configuration.Provider {
//
//    @Inject
//    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory
//
//    override fun getWorkManagerConfiguration(): Configuration =
//        Configuration.Builder()
//            .setWorkerFactory(workerFactory)
//            .setMinimumLoggingLevel(android.util.Log.DEBUG) // Para logs de WorkManager
//            .build()
//}

package com.ferji.inspecciones

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FerjiApp : Application()
