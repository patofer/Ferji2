package com.ferji.inspecciones.viewmodels

/**
 * Estado unificado del formulario de nueva inspección.
 *
 * Consolidar todos los campos en un solo data class resuelve múltiples problemas:
 * - Evita la mezcla de mutableStateOf y StateFlow (inconsistencia de APIs).
 * - Hace que el estado sea Immutable → los Composables pueden hacer comparación
 *   por referencia para evitar recomposiciones innecesarias.
 * - Facilita el testing: un solo objeto para verificar estado.
 * - Aplica el principio de "Single Source of Truth" para todo el formulario.
 *
 * NOTA: Esta clase es @Immutable porque todos sus campos son val y tipos primitivos/String.
 * Compose puede hacer skip de recomposiciones cuando el estado no ha cambiado.
 */
data class NuevaInspeccionFormState(
    val rut: String = "",
    val siniestro: String = "",
    val direccion: String = "",
    val rutInspector: String = "",
    val mail: String = "",
    val isMailValid: Boolean = true,
    val isRutValid: Boolean = true,
    val isRutInspectorValid: Boolean = true
) {
    /**
     * Calcula si todos los campos están completos y válidos.
     * Al ser una propiedad computada del data class, evita estados desincronizados
     * y elimina la necesidad del método actualizarTodosCamposLlenos().
     */
    val todosCamposLlenos: Boolean
        get() = rut.isNotBlank() && isRutValid &&
                siniestro.isNotBlank() &&
                direccion.isNotBlank() &&
                rutInspector.isNotBlank() && isRutInspectorValid &&
                mail.isNotBlank() && isMailValid
}

