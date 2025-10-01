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


class MenuPrincipalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            FerjiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PantallaMenuPrincipal()
                }
            }
        }
    }
}

@Composable
fun PantallaMenuPrincipal() {
    val context = LocalContext.current // ✅ Obtener el contexto

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

        // Botón para nueva inspección
        Button(
            onClick = {
                val intent = Intent(context, NuevaInspeccionActivity::class.java)
                startActivity(context, intent, null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Nueva Inspección",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para inspecciones pendientes
        Button(
            onClick = {
                val intent = Intent(context, ListaInspeccionesActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Inspecciones Pendientes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- INICIO DEL NUEVO BOTÓN ---
        // Botón para el Maestro de Partidas (GESTIONAR partidas)
        Button(
            onClick = {
                // 3. CAMBIAMOS LA ACCIÓN PARA ABRIR LA ACTIVIDAD CORRECTA
                val intent = Intent(context, MaestroPartidasActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7D5260), // Un color ligeramente diferente para distinguirlo
                contentColor = Color.White
            )
        ) {
            // 4. USAMOS UN ÍCONO DIFERENTE
            Icon(Icons.Default.Blinds, contentDescription = "Maestro de Partidas")
            Spacer(modifier = Modifier.width(8.dp))
            // 5. CAMBIAMOS EL TEXTO
            Text(
                text = "Maestro de Partidas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        // --- FIN DEL NUEVO BOTÓN ---

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para el mantenedor de partidas
        Button(
            onClick = {
                val intent = Intent(context, MantenedorPreciosActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF625b71), // Un color morado similar al de Material 3
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Build, contentDescription = "Mantenedor")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Mantenedor precios",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))



        // Botón para historial (sin acción por ahora)
        Button(
            onClick = { /* Aquí irá la acción para ver historial */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Historial de Inspecciones",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PantallaMenuPrincipalPreview() {
    FerjiTheme {
        PantallaMenuPrincipal()
    }
}
