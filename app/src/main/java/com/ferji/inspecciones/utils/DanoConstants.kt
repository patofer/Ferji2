// Archivo: com/ferji/inspecciones/utils/Constants.kt
package com.ferji.inspecciones.utils // o el paquete que elijas

object DanoConstants {
    val opcionesDanosConClave: List<Pair<String, String>> = listOf(
        "1" to "Daño muro",
        "2" to "Fisura en cielo",
        "3" to "Fisura en muro",
        "4" to "Fisura cornisas",
        "5" to "Daño pintura",
        "6" to "Otro"
    )
    const val CLAVE_OTRO_DANO = "6"
}