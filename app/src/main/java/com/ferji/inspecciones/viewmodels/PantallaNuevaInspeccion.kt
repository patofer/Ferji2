package com.ferji.inspecciones

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferji.inspecciones.ui.theme.FerjiTheme
import com.ferji.inspecciones.viewmodels.NuevaInspeccionViewModel
import com.ferji.inspecciones.utils.validarRutChileno

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaNuevaInspeccion(viewModel: NuevaInspeccionViewModel) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nueva Inspección",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // ✅ CAMPO RUT
        OutlinedTextField(
            value = viewModel.rut, // ✅ Lee el estado del ViewModel
            onValueChange = { viewModel.rut = it }, // ✅ Actualiza el estado del ViewModel
            label = { Text("RUT *") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            isError = viewModel.rut.isNotBlank() && !validarRutChileno(viewModel.rut),
            supportingText = {
                if (viewModel.rut.isNotBlank() && !validarRutChileno(viewModel.rut)) {
                    Text("RUT chileno inválido")
                }
            }
        )

        // ✅ CAMPO SINIESTRO
        OutlinedTextField(
            value = viewModel.siniestro,
            onValueChange = { viewModel.siniestro = it },
            label = { Text("Siniestro *") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        // ✅ CAMPO DIRECCIÓN
        OutlinedTextField(
            value = viewModel.direccion,
            onValueChange = { viewModel.direccion = it },
            label = { Text("Dirección *") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        // ✅ CAMPO RUT INSPECTOR
        OutlinedTextField(
            value = viewModel.rutInspector,
            onValueChange = { viewModel.rutInspector = it },
            label = { Text("RUT Inspector *") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            isError = viewModel.rutInspector.isNotBlank() && !validarRutChileno(viewModel.rutInspector),
            supportingText = {
                if (viewModel.rutInspector.isNotBlank() && !validarRutChileno(viewModel.rutInspector)) {
                    Text("RUT chileno inválido")
                }
            }
        )

        // ✅ CAMPO MAIL
        OutlinedTextField(
            value = viewModel.mail,
            onValueChange = { viewModel.mail = it },
            label = { Text("Mail *") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            isError = viewModel.mail.isNotBlank() && !viewModel.mail.contains("@"),
            supportingText = {
                if (viewModel.mail.isNotBlank() && !viewModel.mail.contains("@")) {
                    Text("Formato de email inválido")
                }
            }
        )

        // ✅ MENSAJE DE RESULTADO
        if (viewModel.mensaje.isNotBlank()) {
            Text(
                text = viewModel.mensaje,
                color = if (viewModel.mensaje.contains("✅")) Color.Green else Color.Red,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        Log.d("NUEVA_INSPECCION_UI", "Navegando a NuevaHabitacion con ID ")
        // ✅ BOTÓN GUARDAR
        Button(
            onClick = { viewModel.guardarInspeccion() },
            modifier = Modifier.fillMaxWidth().height(60.dp),
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
