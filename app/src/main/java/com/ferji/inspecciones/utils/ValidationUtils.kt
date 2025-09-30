package com.ferji.inspecciones.utils

import android.util.Log
import android.util.Patterns // Importante para la validación de email

fun esEmailValido(email: String): Boolean {
    if (email.isBlank()) {
        // Un email en blanco no es "inválido" en formato, pero puede ser "no lleno"
        // para la lógica de habilitar el botón.
        // Para la propiedad isMailValid que indica el formato, esto está bien.
        return true
    }
    val isValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    Log.d("ValidationUtils", "Email: '$email', esValido: $isValid") // <-- Log para depurar
    return isValid
}

