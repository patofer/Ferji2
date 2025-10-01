package com.ferji.inspecciones

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferji.inspecciones.viewmodels.NuevaInspeccionViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaNuevaInspeccion(viewModel: NuevaInspeccionViewModel, modifier: Modifier = Modifier,isLoading: Boolean ) {
    Log.d("PantallaNuevaInsp_UI", "--- PantallaNuevaInspeccion RECOMPONIENDO --- rutInsp: ${viewModel.rutInspector}, isValid: ${viewModel.isRutInspectorValid}")
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        // Si el contenido es más corto que la pantalla, esto lo centrará.
        // Si es más largo, el scroll tomará precedencia.
    ) {

        // RUT Principal
        OutlinedTextField(
            value = viewModel.rut,
            onValueChange = { viewModel.onRutChange(it) },
            label = { Text("RUT *") },
            modifier = Modifier
                .fillMaxWidth() // <--- AÑADIDO PARA QUE OCUPE TODO EL ANCHO
                .padding(bottom = 16.dp), // Espacio inferior
            isError = viewModel.rut.isNotBlank() && !viewModel.isRutValid,
            supportingText = {
                if (viewModel.rut.isNotBlank() && !viewModel.isRutValid) {
                    Text("RUT chileno inválido")
                }
            },
            singleLine = true // Buena práctica para campos como RUT
        )


        // ✅ CAMPO SINIESTRO
        OutlinedTextField(
            value = viewModel.siniestro,
            onValueChange = { newValue -> // ✅ Llama a la función pública del ViewModel
                viewModel.onSiniestroChange(newValue)
            },


            label = { Text("Siniestro *") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // ✅ CAMPO DIRECCIÓN
        OutlinedTextField(
            value = viewModel.direccion,
            onValueChange =  { newValue -> // ✅ Llama a la función pública del ViewModel
                viewModel.onDireccionChange(newValue)
            },
            label = { Text("Dirección *") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // ✅ CAMPO RUT INSPECTOR
        // ✅ CAMPO RUT INSPECTOR
        OutlinedTextField(
            value = viewModel.rutInspector,
            onValueChange = { newValue ->
                viewModel.onRutInspectorChange(newValue)
            },
            label = { Text("RUT Inspector *") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            isError = run { // Usamos 'run' para poder añadir un log y devolver el booleano
                val rutInsp = viewModel.rutInspector
                val isNotBlank = rutInsp.isNotBlank()
                val isValid = viewModel.isRutInspectorValid // Lee el estado del ViewModel
                val showError = isNotBlank && !isValid

                Log.d(
                    "PantallaNuevaInsp_UI",
                    "RUT_INSP isError Check: value='${rutInsp}', isNotBlank=${isNotBlank}, isRutInspectorValid=${isValid}, showError=${showError}"
                )
                showError // Devuelve el resultado para 'isError'
            },
            supportingText = {
                if (viewModel.rutInspector.isNotBlank() && !viewModel.isRutInspectorValid) {
                    Log.d("PantallaNuevaInsp_UI", "RUT_INSP SupportingText: Mostrando mensaje de error.")
                    Text("RUT chileno inválido")
                } else {
                    Log.d("PantallaNuevaInsp_UI", "RUT_INSP SupportingText: NO mostrando mensaje de error.")
                }
            }
        )


        // ✅ CAMPO MAIL
        OutlinedTextField(
            value = viewModel.mail,
            onValueChange = { viewModel.onMailChange(it) }, // Llama a la función del ViewModel
            label = { Text("Mail Contacto *") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            // El error se muestra si el campo no está vacío Y no es un email válido
            isError = viewModel.mail.isNotBlank() && !viewModel.isMailValid,
            supportingText = {
                if (viewModel.mail.isNotBlank() && !viewModel.isMailValid) {
                    Text("Formato de email inválido")
                }
            }
        )

        // ✅ MENSAJE DE RESULTADO
        uiState.mensajeGlobalUi?.let { mensajeDelEstado -> // Accede a la propiedad de uiState
            if (mensajeDelEstado.isNotBlank()) {
                Text(
                    text = mensajeDelEstado,
                    color = uiState.colorMensajeGlobalUi ?: if (mensajeDelEstado.contains("✅")) Color.Green else Color.Red, // Accede a la propiedad de uiState
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }

        Log.d("NUEVA_INSPECCION_UI", "Navegando a NuevaHabitacion con ID ")
        // ✅ BOTÓN GUARDAR
        Button(
            onClick = { viewModel.guardarInspeccion() },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            enabled = viewModel.todosCamposLlenos, // ✅ Lee del ViewModel
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "GUARDAR INSPECCIÓN",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

