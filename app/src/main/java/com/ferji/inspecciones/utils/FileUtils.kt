package com.ferji.inspecciones.utils

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object FileUtils {
    fun guardarBitmapEnInterno(context: Context, bitmap: Bitmap, nombreArchivo: String): String {
        val directorio = File(context.filesDir, "fotos_inspecciones")
        if (!directorio.exists()) directorio.mkdirs()

        val archivo = File(directorio, nombreArchivo)
        try {
            FileOutputStream(archivo).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
            return archivo.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }
    }

    fun eliminarArchivo(rutaArchivo: String): Boolean {
        return try {
            File(rutaArchivo).delete()
        } catch (e: Exception) {
            false
        }
    }
}