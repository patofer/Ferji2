package com.ferji.inspecciones.utils

// The function is now a top-level function in the 'utils' package.
// It can be called from anywhere that imports this package.
fun validarRutChileno(rut: String): Boolean {
    if (rut.isBlank()) return false

    val rutLimpio = rut.replace("[.\\-\\s]".toRegex(), "").uppercase()
    if (rutLimpio.length < 2) return false

    val cuerpo = rutLimpio.substring(0, rutLimpio.length - 1)
    val dv = rutLimpio.substring(rutLimpio.length - 1)

    if (!cuerpo.matches(Regex("\\d+"))) return false

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

    return dv == dvEsperado
}