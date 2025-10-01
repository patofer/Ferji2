package com.ferji.inspecciones.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import android.util.Base64


object FileUtils {
    fun guardarBitmapEnInterno(
        context: Context,
        uriDelArchivoTemporalCamara: Uri, // <--- DEBE ACEPTAR URI AQUÍ
        nombreArchivo: String
    ): String? {
        val directorioDestino = File(context.filesDir, "fotos_inspecciones")
        if (!directorioDestino.exists()) {
            directorioDestino.mkdirs()
        }
        val archivoFotoDestino = File(directorioDestino, nombreArchivo)

        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        var originalBitmap: Bitmap? = null
        var rotatedBitmap: Bitmap? = null

        try {
            // 1. Obtener InputStream para leer el bitmap y para EXIF
            inputStream = context.contentResolver.openInputStream(uriDelArchivoTemporalCamara)
            if (inputStream == null) {
                Log.e("ProcesarGuardar", "No se pudo abrir InputStream para $uriDelArchivoTemporalCamara")
                return null
            }

            // --- Obtener Orientación EXIF del archivo temporal de la cámara ---
            val exifInputStream = context.contentResolver.openInputStream(uriDelArchivoTemporalCamara)
            val exif = exifInputStream?.use { ExifInterface(it) } // ExifInterface cierra el stream
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                ?: ExifInterface.ORIENTATION_NORMAL
            Log.d("ProcesarGuardar", "Orientación EXIF del archivo temporal: $orientation")


            // --- Decodificar Bitmap (considerando inSampleSize para OOM) ---
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            // Necesitas volver a abrir el stream o asegurarte que el primero no se consumió completamente por EXIF
            // Para simplificar, abrimos de nuevo. Idealmente, manejar el stream con más cuidado.
            context.contentResolver.openInputStream(uriDelArchivoTemporalCamara)?.use { tempInStream ->
                BitmapFactory.decodeStream(tempInStream, null, options)
            }

            // Ajustar reqWidth y reqHeight según tus necesidades.
            // Si quieres guardar la máxima resolución posible rotada, puedes poner valores muy altos
            // o adaptar calculateInSampleSize para que no reduzca tanto si no es estrictamente necesario.
            // Para este ejemplo, pongamos un límite razonable.
            val reqWidth = 1920 // Por ejemplo, Full HD
            val reqHeight = 1080
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight) // Usa la misma función de PdfGenerator
            options.inJustDecodeBounds = false

            Log.d("ProcesarGuardar", "Calculado inSampleSize: ${options.inSampleSize} para ${uriDelArchivoTemporalCamara} con dimensiones originales: ${options.outWidth}x${options.outHeight}")


            originalBitmap = context.contentResolver.openInputStream(uriDelArchivoTemporalCamara)?.use { finalInStream ->
                BitmapFactory.decodeStream(finalInStream, null, options)
            }

            if (originalBitmap == null) {
                Log.e("ProcesarGuardar", "BitmapFactory.decodeStream devolvió null para $uriDelArchivoTemporalCamara")
                return null
            }
            Log.d("ProcesarGuardar", "Bitmap cargado del temporal. Dimensiones: ${originalBitmap!!.width}x${originalBitmap!!.height}")


            // --- Rotar el Bitmap ---
            val matrix = Matrix()
            var needsRotationOrFlip = true
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_NORMAL, ExifInterface.ORIENTATION_UNDEFINED -> {
                    needsRotationOrFlip = false
                }
                else -> needsRotationOrFlip = false
            }

            if (needsRotationOrFlip && !matrix.isIdentity) {
                Log.d("ProcesarGuardar", "Aplicando rotación/transformación al bitmap antes de guardar.")
                rotatedBitmap = Bitmap.createBitmap(
                    originalBitmap!!, 0, 0,
                    originalBitmap!!.width, originalBitmap!!.height,
                    matrix, true
                )
                if (rotatedBitmap != originalBitmap) { // Solo recicla el original si se creó un bitmap nuevo
                    originalBitmap?.recycle()
                    Log.d("ProcesarGuardar", "Bitmap original (del temporal) reciclado.")
                }
            } else {
                Log.d("ProcesarGuardar", "No se requiere rotación/transformación. Usando bitmap tal cual (escalado por inSampleSize).")
                rotatedBitmap = originalBitmap // Usar el bitmap original (ya escalado por inSampleSize)
            }

            if (rotatedBitmap == null) {
                Log.e("ProcesarGuardar", "El bitmap rotado es null, no se puede guardar.")
                originalBitmap?.recycle() // Asegurarse de reciclar el original si rotatedBitmap falló
                return null
            }

            // --- Guardar el Bitmap Rotado ---
            outputStream = FileOutputStream(archivoFotoDestino)
            rotatedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, outputStream) // Ajusta calidad (90)
            outputStream.flush()

            Log.d("ProcesarGuardar", "Bitmap ROTADO Y GUARDADO en: ${archivoFotoDestino.absolutePath}")
            return archivoFotoDestino.absolutePath

        } catch (e: Exception) {
            Log.e("ProcesarGuardar", "Error al procesar y guardar imagen: ${e.message}", e)
            return null
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
                // No recicles rotatedBitmap aquí si lo vas a usar después (e.g., para mostrarlo en UI)
                // Si solo es para guardar, y es diferente del original, y el original ya fue reciclado, está bien.
                // La lógica de reciclaje actual es: originalBitmap se recicla si rotatedBitmap es un nuevo objeto.
                // rotatedBitmap se usa y luego se "olvida" (el GC se encarga), o si lo pasas a otra parte, esa parte lo gestiona.
                // Si tienes una referencia a `rotatedBitmap` fuera de esta función que necesitas limpiar, hazlo allí.
                // Por seguridad, si `rotatedBitmap` no es el mismo que `originalBitmap` (que ya se recicló),
                // y `rotatedBitmap` no se devuelve o usa más allá de esta función, podría reciclarse aquí.
                // Pero como esta función devuelve el File, se asume que el bitmap ya cumplió su propósito.
                // Si lo muestras en UI, es otra historia. Para este caso de guardar-y-olvidar:
                if (rotatedBitmap != null && rotatedBitmap != originalBitmap) {
                    // rotatedBitmap?.recycle() // CUIDADO: Solo si no se usa más.
                    // Por ahora, el GC lo manejará.
                }

            } catch (e: IOException) {
                Log.e("ProcesarGuardar", "Error cerrando streams: ${e.message}", e)
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (reqWidth <= 0 || reqHeight <= 0) return 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
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