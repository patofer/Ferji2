package com.ferji.inspecciones.utils

import android.util.Log



import android.util.Patterns // Asegúrate de tener este import

/**
 * Función de extensión para validar el formato de un email.
 * Se usa así: "mi.correo@dominio.com".esEmailValido()
 */
fun String.esEmailValido(): Boolean {
    // isBlank() comprueba que no sea nulo, vacío o solo espacios en blanco.
    if (this.isBlank()) return false
    // Patterns.EMAIL_ADDRESS es el validador oficial de Android.
    return Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

/**
 * Función de extensión para validar un RUT chileno.
 * Se usa así: "12345678-9".validarRutChileno()
 */
fun String.validarRutChileno(): Boolean {
    // Limpiamos el RUT de puntos y guiones, y lo convertimos a mayúsculas
    val rutLimpio = this.replace(Regex("[.-]"), "").uppercase()

    // El RUT debe tener entre 8 y 9 caracteres después de limpiarlo
    if (rutLimpio.length !in 8..9) return false

    try {
        // Separamos el cuerpo del dígito verificador
        val cuerpo = rutLimpio.substring(0, rutLimpio.length - 1).toInt()
        val dv = rutLimpio.last()

        // --- Algoritmo de cálculo del Dígito Verificador ---
        var suma = 0
        var multiplo = 2
        var cuerpoRut = cuerpo
        while (cuerpoRut > 0) {
            suma += (cuerpoRut % 10) * multiplo
            cuerpoRut /= 10
            multiplo++
            if (multiplo > 7) {
                multiplo = 2
            }
        }

        // Calculamos el dígito verificador esperado
        val dvEsperado = when (val resto = 11 - (suma % 11)) {
            11 -> '0'
            10 -> 'K'
            else -> resto.toString().first()
        }
        // --- Fin del Algoritmo ---

        // Comparamos el dígito verificador ingresado con el calculado
        return dv == dvEsperado
    } catch (e: Exception) {
        // Si algo falla (ej. al convertir 'cuerpo' a Int), el RUT es inválido
        return false
    }
}
