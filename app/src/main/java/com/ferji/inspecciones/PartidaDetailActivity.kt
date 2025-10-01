package com.ferji.inspecciones

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ferji.inspecciones.ui.mantenedor.PartidaDetailListScreen
import com.ferji.inspecciones.ui.theme.FerjiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PartidaDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recuperar los argumentos pasados desde la actividad anterior
        val partidaId = intent.getLongExtra(EXTRA_PARTIDA_ID, -1L)
        val partidaNombre = intent.getStringExtra(EXTRA_PARTIDA_NOMBRE) ?: "Detalles"

        // Si el ID no es válido, cerramos la actividad para evitar errores
        if (partidaId == -1L) {
            finish()
            return
        }

        setContent {
            FerjiTheme {
                PartidaDetailListScreen(
                    partidaPrincipalId = partidaId,
                    partidaPrincipalNombre = partidaNombre,
                    onBack = { finish() } // El botón de volver cierra esta actividad
                )
            }
        }
    }

    // Companion object para crear un Intent de forma limpia y segura
    companion object {
        private const val EXTRA_PARTIDA_ID = "partida_id"
        private const val EXTRA_PARTIDA_NOMBRE = "partida_nombre"

        fun newIntent(context: Context, partidaId: Long, partidaNombre: String): Intent {
            return Intent(context, PartidaDetailActivity::class.java).apply {
                putExtra(EXTRA_PARTIDA_ID, partidaId)
                putExtra(EXTRA_PARTIDA_NOMBRE, partidaNombre)
            }
        }
    }
}
