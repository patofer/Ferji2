package com.ferji.inspecciones.domain.model

/**
 * Wrapper genérico para resultados de operaciones.
 *
 * Reemplaza el patrón de devolver null en caso de error, proporcionando
 * información útil sobre el fallo para que la UI pueda reaccionar.
 *
 * Uso:
 * ```
 * when (val result = generarExcel()) {
 *     is AppResult.Success -> mostrarExito(result.data)
 *     is AppResult.Error -> mostrarError(result.message)
 *     is AppResult.Loading -> mostrarCargando()
 * }
 * ```
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : AppResult<Nothing>()
    data object Loading : AppResult<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error
    val isLoading get() = this is Loading

    /**
     * Ejecuta [block] solo si es Success y devuelve el resultado mapeado.
     */
    fun <R> map(block: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(block(data))
        is Error -> this
        is Loading -> this
    }

    /**
     * Obtiene el valor o lanza la excepción si es error.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception ?: IllegalStateException(message)
        is Loading -> throw IllegalStateException("Resultado aún en progreso")
    }

    /**
     * Obtiene el valor o devuelve null.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
}

