package com.ferji.inspecciones.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import android.util.Base64


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

    fun convertUriToBase64(context: Context, uri: Uri): String? { // <-- La función debe ser 'fun' y pública (lo es por defecto)
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            // Usar .use para asegurar que el stream se cierre automáticamente
            val bytes = inputStream?.use { it.readBytes() }
            // inputStream?.close() // Ya no es necesario con .use

            bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) } // NO_WRAP es importante
        } catch (e: IOException) {
            Log.e("FileUtils", "Error convirtiendo URI a Base64 (IOException)", e)
            null
        } catch (e: SecurityException) {
            Log.e("FileUtils", "Error de seguridad convirtiendo URI a Base64 (permisos?)", e)
            null
        } catch (e: Exception) {
            Log.e("FileUtils", "Error genérico convirtiendo URI a Base64", e)
            null
        }
    }
}