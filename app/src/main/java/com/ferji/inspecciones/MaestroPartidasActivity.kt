package com.ferji.inspecciones.ui.actividades

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.platform.LocalContext
import com.ferji.inspecciones.PartidaDetailActivity
import com.ferji.inspecciones.ui.mantenedor.MaestroPartidasScreen
import com.ferji.inspecciones.ui.theme.FerjiTheme
import com.ferji.inspecciones.viewmodels.PartidaPrincipalViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MaestroPartidasActivity : ComponentActivity() {

    // La declaración del ViewModel aquí está bien.
    private val viewModel: PartidaPrincipalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ 1. ¡CORRECCIÓN CLAVE! super.onCreate() DEBE ser la primera línea.
        super.onCreate(savedInstanceState)

        // ✅ 2. Llama a la sincronización DESPUÉS de super.onCreate().
        viewModel.sincronizarDatos()

        // ✅ 3. Configura el contenido de la UI al final.
        setContent {
            FerjiTheme {
                val context = LocalContext.current

                // Pasa la instancia del ViewModel que ya fue inicializada de forma segura.
                MaestroPartidasScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
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
