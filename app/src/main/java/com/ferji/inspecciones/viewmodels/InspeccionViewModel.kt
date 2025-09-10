package com.ferji.inspecciones.viewmodels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferji.inspecciones.data.model.InspeccionEntity
import com.ferji.inspecciones.data.repository.InspeccionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class InspeccionViewModel @Inject constructor(
    private val repository: InspeccionRepository
) : ViewModel() {

    fun guardarInspeccion(
        rut: String,
        siniestro: String,
        direccion: String,
        rutInspector: String,
        mail: String
    ) {
        viewModelScope.launch {  // ✅ Esto se ejecuta en background
            val inspeccion = InspeccionEntity(
                rut = rut,
                siniestro = siniestro,
                direccion = direccion,
                rutInspector = rutInspector,
                mail = mail
            )
            repository.insertInspeccion(inspeccion)
        }
    }
}