package com.ferji.inspecciones

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi // <-- 1. AÑADE ESTE IMPORT
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Blinds
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.ferji.inspecciones.ui.actividades.MaestroPartidasActivity
import com.ferji.inspecciones.ui.theme.FerjiTheme
import androidx.activity.viewModels // <-- AÑADE ESTE IMPORT
import androidx.compose.runtime.collectAsState // <-- AÑADE ESTE IMPORT
import androidx.compose.runtime.getValue // <-- AÑADE ESTE IMPORT
import com.ferji.inspecciones.viewmodels.MenuPrincipalViewModel // <-- AÑADE ESTE IMPORT
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint // <-- 1. AÑADE ESTA ANOTACIÓN
class MenuPrincipalActivity : ComponentActivity() {

    // 2. Inyecta tu ViewModel usando Hilt
    private val viewModel: MenuPrincipalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FerjiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 3. Obtiene el estado del ViewModel y pásalo al Composable
                    val esAdmin by viewModel.esAdministrador.collectAsState()
                    PantallaMenuPrincipal(esAdministrador = esAdmin)
                }
            }
        }
    }
}

@Composable
// 4. El Composable ahora recibe el estado booleano
fun PantallaMenuPrincipal(esAdministrador: Boolean) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Menú Principal",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- MENÚS VISIBLES PARA TODOS LOS ROLES ---

        Button(
            onClick = {
                val intent = Intent(context, NuevaInspeccionActivity::class.java)
                startActivity(context, intent, null)
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Nueva Inspección", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val intent = Intent(context, ListaInspeccionesActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Inspecciones Pendientes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 5. LÓGICA CONDICIONAL PARA MOSTRAR BOTONES DE ADMINISTRADOR ---
        if (esAdministrador) {
            // Botón para el Maestro de Partidas
            Button(
                onClick = {
                    val intent = Intent(context, MaestroPartidasActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7D5260))
            ) {
                Icon(Icons.Default.Blinds, contentDescription = "Maestro de Partidas")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Maestro de Partidas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para el mantenedor de precios
            Button(
                onClick = {
                    val intent = Intent(context, MantenedorPreciosActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF625b71))
            ) {
                Icon(Icons.Default.Build, contentDescription = "Mantenedor")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mantenedor precios", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- CONTINUACIÓN DEL MENÚ PARA TODOS ---
        Button(
            onClick = { /* Acción para ver historial */ },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Text("Historial de Inspecciones", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}


// --- 6. ACTUALIZA LAS PREVIEWS PARA PROBAR AMBOS CASOS ---

@Preview(showBackground = true, name = "Vista como Administrador")
@Composable
fun PantallaMenuAdminPreview() {
    FerjiTheme {
        // Le pasamos 'true' para simular la vista de admin
        PantallaMenuPrincipal(esAdministrador = true)
    }
}

@Preview(showBackground = true, name = "Vista como Usuario Normal")
@Composable
fun PantallaMenuUsuarioPreview() {
    FerjiTheme {
        // Le pasamos 'false' para simular la vista de un usuario no-admin
        PantallaMenuPrincipal(esAdministrador = false)
    }
}
