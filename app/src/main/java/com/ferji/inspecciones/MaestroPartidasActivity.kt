package com.ferji.inspecciones.ui.actividades
// O el paquete donde la tengas, ej: com.ferji.inspecciones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext // <-- Añadir esta importación
import com.ferji.inspecciones.PartidaDetailActivity // <-- Añadir esta importación
import com.ferji.inspecciones.ui.mantenedor.MaestroPartidasScreen // <- Usa la pantalla de gestión CRUD
import com.ferji.inspecciones.ui.theme.FerjiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MaestroPartidasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FerjiTheme {
                // Obtenemos el contexto actual para poder lanzar la nueva actividad
                val context = LocalContext.current

                // Llama directamente a la pantalla de gestión. No se necesita NavGraph aquí.
                MaestroPartidasScreen(
                    onBack = { finish() } , // El botón de volver simplemente cierra la actividad.

                    // --- ¡AQUÍ ESTÁ LA SOLUCIÓN! ---
                    // Ahora, al hacer clic, creamos y lanzamos el Intent.
                    onPartidaClick = { partidaId, partidaNombre ->
                        val intent = PartidaDetailActivity.newIntent(
                            context = context,
                            partidaId = partidaId,
                            partidaNombre = partidaNombre
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}
