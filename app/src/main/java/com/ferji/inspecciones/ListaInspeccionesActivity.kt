package com.ferji.inspecciones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferji.inspecciones.data.database.AppDatabase
import com.ferji.inspecciones.data.repository.InspeccionRepository
import com.ferji.inspecciones.ui.theme.FerjiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*



class ListaInspeccionesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        val repository = InspeccionRepository(database.inspeccionDao())

        setContent {
            FerjiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PantallaListaInspecciones(repository, onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaInspecciones(
    repository: InspeccionRepository,
    onBack: () -> Unit = {}
) {
    var inspecciones by remember { mutableStateOf(emptyList<com.ferji.inspecciones.data.model.InspeccionEntity>()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Cargar datos de la base de datos REAL
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            try {
                repository.getAllInspecciones().collect { lista ->
                    inspecciones = lista
                    isLoading = false
                }
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("📋 Inspecciones Guardadas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("❌ Error: $error", color = Color.Red)
                }
            } else if (inspecciones.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭 No hay inspecciones guardadas", fontSize = 18.sp)
                        Text("Crea una nueva inspección primero", color = Color.Gray)
                    }
                }
            } else {
                Text(
                    text = "Total: ${inspecciones.size} inspecciones",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn {
                    items(inspecciones) { inspeccion ->
                        TarjetaInspeccion(inspeccion = inspeccion)
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaInspeccion(inspeccion: com.ferji.inspecciones.data.model.InspeccionEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🆔 ID: ${inspeccion.id}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("👤 RUT: ${inspeccion.rut}")
            Text("📋 Siniestro: ${inspeccion.siniestro}")
            Text("🏠 Dirección: ${inspeccion.direccion}")
            Text("🔍 Inspector: ${inspeccion.rutInspector}")
            Text("📧 Mail: ${inspeccion.mail}")
            Text(
                text = "📅 Fecha: ${formatearFecha(inspeccion.fechaCreacion)}",
                color = Color.Gray,
                fontSize = 12.sp
            )
            Text(
                text = "✅ Estado: ${inspeccion.estado}",
                color = when (inspeccion.estado) {
                    "PENDIENTE" -> Color.Yellow
                    "COMPLETADA" -> Color.Green
                    else -> Color.Gray
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Función para formatear la fecha
fun formatearFecha(date: Date): String {
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return format.format(date)
}

// ✅ Preview SIMPLIFICADO sin mock
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PantallaListaInspeccionesPreview() {
    FerjiTheme {
        // Preview básico sin datos reales
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "📋 Inspecciones Guardadas",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Vista previa - Los datos reales se cargan desde la BD",
                color = Color.Gray
            )
        }
    }
}

@Preview
@Composable
fun TarjetaInspeccionPreview() {
    FerjiTheme {
        TarjetaInspeccion(
            inspeccion = com.ferji.inspecciones.data.model.InspeccionEntity(
                id = 1,
                rut = "12.345.678-9",
                siniestro = "SIS-2024-001",
                direccion = "Av. Principal 123",
                rutInspector = "98.765.432-1",
                mail = "inspector@ferji.cl",
                fechaCreacion = Date(),
                estado = "PENDIENTE"
            )
        )
    }
}