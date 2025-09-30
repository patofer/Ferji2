package com.ferji.inspecciones.utils

import android.util.Log

// The function is now a top-level function in the 'utils' package.
// It can be called from anywhere that imports this package.
// En tu ViewModel o en ValidationUtils.kt
fun validarRutChileno(rut: String): Boolean {
    val rutLimpio = rut.replace("[.\\-\\s]".toRegex(), "").uppercase()

    // Log para ver qué entra y qué se limpia
    Log.d("validarRutChileno", "Input: '$rut', Limpio: '$rutLimpio'")

    if (rutLimpio.isBlank()) {
        Log.d("validarRutChileno", "Resultado: false (limpio está en blanco)")
        return false // Un RUT válido para guardar no puede estar en blanco
    }

    if (rutLimpio.length < 2) {
        Log.d("validarRutChileno", "Resultado: false (longitud < 2)")
        return false // Necesita al menos cuerpo y DV
    }

    val cuerpo = rutLimpio.substring(0, rutLimpio.length - 1)
    val dv = rutLimpio.substring(rutLimpio.length - 1)

    Log.d("validarRutChileno", "Cuerpo: '$cuerpo', DV: '$dv'")

    if (!cuerpo.matches(Regex("\\d+"))) {
        Log.d("validarRutChileno", "Resultado: false (cuerpo no son solo dígitos)")
        return false // El cuerpo deben ser solo dígitos
    }

    // Si el cuerpo es vacío después de quitar el DV (ej. solo se ingresó 'K'), no es válido.
    if (cuerpo.isEmpty()) {
        Log.d("validarRutChileno", "Resultado: false (cuerpo vacío después de extraer DV)")
        return false
    }

    var suma = 0
    var multiplicador = 2
    for (i in cuerpo.length - 1 downTo 0) {
        suma += cuerpo[i].toString().toInt() * multiplicador
        multiplicador = if (multiplicador == 7) 2 else multiplicador + 1
    }

    val dvEsperado = when (val resto = suma % 11) {
        0 -> "0"
        1 -> "K"
        else -> (11 - resto).toString()
    }

    Log.d("validarRutChileno", "DV Esperado: $dvEsperado, DV Calculado: $dv")
    val esValido = dv == dvEsperado
    Log.d("validarRutChileno", "Resultado: $esValido")
    return esValido
}
