package com.ferji.inspecciones

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels // Necesario para by viewModels()
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Ya no necesitas AppDatabase ni InspeccionRepository aquí directamente
// import com.ferji.inspecciones.data.database.AppDatabase
// import com.ferji.inspecciones.data.repository.InspeccionRepository
import com.ferji.inspecciones.ui.theme.FerjiTheme
import com.ferji.inspecciones.viewmodels.NuevaInspeccionViewModel // IMPORTA TU VIEWMODEL
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest // Para SharedFlow

// Asegúrate de que tu Activity esté anotada
@AndroidEntryPoint
class NuevaInspeccionActivity : ComponentActivity() {

    // Obtén la instancia del ViewModel inyectada por Hilt
    private val viewModel: NuevaInspeccionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ya no inicializas database ni repository aquí
        // Hilt se encarga de proveer el repository al viewModel

        setContent {
            FerjiTheme {
                // Pasamos el ViewModel al Composable
                PantallaNuevaInspeccionView(viewModel = viewModel)
            }
        }
    }
}

//
@OptIn(ExperimentalMaterial3Api::class)
@Composable
// La firma del Composable ahora toma el ViewModel
fun PantallaNuevaInspeccionView(viewModel: NuevaInspeccionViewModel) {
    // Los estados (rut, siniestro, etc.) se leen y actualizan a través del viewModel
    // var rut by remember { mutableStateOf("") } // Ya no se necesita
    // ... y así para los otros estados locales de los campos

    // El mensaje de estado ahora se maneja a través de UiEvents y Snackbar
    // var mensaje by remember { mutableStateOf("") } // Ya no se necesita

    val context = LocalContext.current
    // val coroutineScope = rememberCoroutineScope() // El viewModel tiene su propio viewModelScope
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Observa los UiEvents del ViewModel para navegación y mensajes
    LaunchedEffect(key1 = Unit) { // key1 = Unit para que se ejecute solo una vez al componer
        viewModel.uiEvents.collectLatest { event -> // Usamos collectLatest
            when (event) {
                is NuevaInspeccionViewModel.UiEvent.NavigateToNewRoom -> {
                    Log.d("NUEVA_INSPECCION_UI", "Navegando a NuevaHabitacion con ID: ${event.inspeccionId}")
                    val intent = Intent(context, NuevaHabitacionActivity::class.java)
                    intent.putExtra("INSPECCION_ID", event.inspeccionId)
                    context.startActivity(intent)
                }
                is NuevaInspeccionViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Usamos Scaffold para poder integrar el SnackbarHost
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Aplica el padding del Scaffold
                .padding(24.dp) // Tu padding original adicional
                .verticalScroll(scrollState),
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
                value = viewModel.rut, // Lee del ViewModel
                onValueChange = { viewModel.rut = it }, // Actualiza la propiedad en el ViewModel
                label = { Text("RUT *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // ✅ CAMPO DIRECCIÓN
            OutlinedTextField(
                value = viewModel.direccion,
                onValueChange = { viewModel.direccion = it },
                label = { Text("Dirección *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // ✅ CAMPO RUT INSPECTOR
            OutlinedTextField(
                value = viewModel.rutInspector,
                onValueChange = { viewModel.rutInspector = it },
                label = { Text("RUT Inspector *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                isError = viewModel.mail.isNotBlank() && !viewModel.mail.contains("@"), // Asumiendo que el VM no hace esta validación específica
                supportingText = {
                    if (viewModel.mail.isNotBlank() && !viewModel.mail.contains("@")) {
                        Text("Formato de email inválido")
                    }
                }
            )

            // Ya no se muestra el 'mensaje' local aquí, se usa el Snackbar

            // ✅ BOTÓN GUARDAR
            Button(
                onClick = {
                    // La validación principal de campos llenos está en el ViewModel
                    // Las validaciones de formato (RUT, mail) aún pueden estar aquí
                    // o también podrían moverse al ViewModel.
                    if (viewModel.todosCamposLlenos && // Usamos la propiedad del VM
                        validarRutChileno(viewModel.rut) &&
                        validarRutChileno(viewModel.rutInspector) &&
                        viewModel.mail.contains("@") // Validación de formato de email
                    ) {
                        viewModel.guardarInspeccion() // Llama al método del ViewModel
                    } else {
                        // El ViewModel ya emite un Snackbar si los campos están vacíos.
                        // Si estas validaciones de formato fallan, podrías querer un mensaje más específico.
                        // Opcional: podrías llamar a una función en el VM que emita el error de formato.
                        // Por ahora, si no pasan, el `guardarInspeccion` del VM gestionará el error de "campos vacíos".
                        // Si quieres ser más específico, podrías tener:
                        if (!viewModel.todosCamposLlenos) {
                            viewModel.guardarInspeccion() // Dejar que el VM maneje el "campos vacíos"
                        } else if (!validarRutChileno(viewModel.rut) || !validarRutChileno(viewModel.rutInspector)) {
                            // snackbarHostState.showSnackbar("Formato de RUT inválido.") // Ejemplo de mensaje local
                            // O, mejor, que el VM tenga una función para esto
                        } else if (!viewModel.mail.contains("@")) {
                            // snackbarHostState.showSnackbar("Formato de email inválido.") // Ejemplo
                        }
                        // La forma más simple es dejar que el VM maneje el mensaje de "campos vacíos"
                        // si `todosCamposLlenos` es false.
                        // Si las otras validaciones fallan, el `guardarInspeccion()` no se llamará.
                        // Para mejorar, el ViewModel podría tener validaciones más granulares.
                        // Por ahora, el ViewModel se encarga de la validación general de campos llenos.
                        // Si las otras fallan, el botón simplemente no hará nada o podrías mostrar un Toast/Snackbar local.
                        if (!viewModel.todosCamposLlenos) {
                            viewModel.guardarInspeccion() // Para que emita el evento de "Por favor complete..."
                        } else {
                            // Si todos los campos están llenos pero el formato es incorrecto,
                            // el viewModel.guardarInspeccion() no se llamará y no habrá feedback
                            // a menos que lo añadas aquí o en el VM.
                            // Por simplicidad, asumimos que el usuario ve los errores en los TextFields.
                            // Idealmente, el VM debería manejar todas las validaciones.
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                enabled = viewModel.todosCamposLlenos, // Habilita el botón basado en la lógica del ViewModel
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
}

// Función para validar RUT chileno (asegúrate de que esté definida y accesible)
// fun validarRutChileno(rut: String): Boolean { ... }


// Preview para diseño (necesitará un ViewModel mock o no usar el ViewModel)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PantallaNuevaInspeccionPreview() {
    FerjiTheme {
        // Para el preview, como el ViewModel real necesita Hilt,
        // puedes crear una versión simplificada del Composable que no dependa del ViewModel
        // o usar una instancia mock del ViewModel si tienes una factory para ello.
        // Por ahora, mostraré un preview básico de la UI sin lógica de ViewModel.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Nueva Inspección",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            OutlinedTextField(value = "12.345.678-9", onValueChange = {}, label = { Text("RUT *") }, modifier = Modifier.fillMaxWidth().padding(bottom=16.dp))
            OutlinedTextField(value = "SINS-123", onValueChange = {}, label = { Text("Siniestro *") }, modifier = Modifier.fillMaxWidth().padding(bottom=16.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                Text("GUARDAR INSPECCIÓN")
            }
        }
    }
}
