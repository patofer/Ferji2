package com.ferji.inspecciones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ferji.inspecciones.ui.mantenedor.PartidaDetailListScreen
import com.ferji.inspecciones.ui.mantenedor.PartidaPrincipalListScreen
import com.ferji.inspecciones.ui.theme.FerjiTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder

@AndroidEntryPoint
class MantenedorPreciosActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FerjiTheme {
                // Llama al NavGraph que define el flujo de precios.
                MantenedorPreciosNavGraph(
                    onFinish = { finish() }
                )
            }
        }
    }
}

/**
 * Gestiona la navegación para el "Mantenedor de Precios" usando tus pantallas existentes.
 */
@Composable
fun MantenedorPreciosNavGraph(onFinish: () -> Unit) {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = "partida_principal_list") {

        /**
         * Pantalla 1: Seleccionar una categoría. Usa la pantalla 'PartidaPrincipalListScreen'.
         */
        composable(route = "partida_principal_list") {
            PartidaPrincipalListScreen(
                onBack = { onFinish() }, // Volver aquí cierra la actividad.
                // IMPORTANTE: Este callback ahora debe recibir (Long, String)
                // Asegúrate de que la firma en PartidaPrincipalListScreen.kt esté actualizada.
                onPartidaPrincipalClick = { partidaPrincipalId: Long, partidaPrincipalNombre: String ->
                    // Codificamos el nombre para evitar problemas con espacios o caracteres especiales en la URL.
                    val encodedNombre = URLEncoder.encode(partidaPrincipalNombre, "UTF-8")
                    // Navegamos pasando AMBOS parámetros en la ruta.
                    navController.navigate("partida_detail_list/$partidaPrincipalId/$encodedNombre")
                }
            )
        }

        /**
         * Pantalla 2: Gestionar las partidas de detalle para la categoría seleccionada.
         */
        composable(
            // La ruta ahora espera AMBOS parámetros.
            route = "partida_detail_list/{partidaPrincipalId}/{partidaPrincipalNombre}",
            // Se definen los dos argumentos y sus tipos.
            arguments = listOf(
                navArgument("partidaPrincipalId") { type = NavType.LongType },
                navArgument("partidaPrincipalNombre") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Se extrae el ID de la partida.
            val partidaPrincipalId = backStackEntry.arguments?.getLong("partidaPrincipalId") ?: 0L
            // Se extrae el NOMBRE codificado de la partida.
            val encodedNombre = backStackEntry.arguments?.getString("partidaPrincipalNombre") ?: ""
            // Se decodifica el nombre para mostrarlo correctamente en la UI.
            val partidaPrincipalNombre = URLDecoder.decode(encodedNombre, "UTF-8")

            // Se llama a la pantalla de detalle, pasando ambos parámetros.
            PartidaDetailListScreen(
                partidaPrincipalId = partidaPrincipalId,
                partidaPrincipalNombre = partidaPrincipalNombre, // Parámetro ahora incluido.
                onBack = { navController.popBackStack() } // Volver regresa a la lista de categorías.
            )
        }
    }
}
