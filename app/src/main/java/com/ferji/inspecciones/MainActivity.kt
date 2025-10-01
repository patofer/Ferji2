package com.ferji.inspecciones

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ferji.inspecciones.data.repository.UserRoles
import com.ferji.inspecciones.ui.theme.FerjiTheme
import com.ferji.inspecciones.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FerjiTheme {
                MainRouter()
            }
        }
    }

    @Composable
    private fun MainRouter() {
        val viewModel: MainViewModel = hiltViewModel()
        val sessionState by viewModel.sessionState.collectAsState()

        when (val state = sessionState) {
            MainViewModel.SessionState.LOADING -> {
                SplashScreen()
            }
            is MainViewModel.SessionState.LoggedIn -> {
                PantallaBienvenida(
                    userName = state.data.nombre ?: "Usuario",
                    userRole = state.data.role, // <-- PASAMOS EL ROL A LA UI
                    onIngresarClick = {
                        startActivity(Intent(this, MenuPrincipalActivity::class.java))
                    },
                    onCerrarSesionClick = {
                        viewModel.logout()
                    }
                )
            }
            MainViewModel.SessionState.LoggedOut -> {
                PantallaInicial(
                    onIngresarClick = {
                        loginLauncher.launch(Intent(this, LoginActivity::class.java))
                    }
                )
            }
        }
    }
}

// PANTALLA BIENVENIDA MODIFICADA
// Ahora muestra un badge de "Administrador"
@Composable
fun PantallaBienvenida(
    modifier: Modifier = Modifier,
    userName: String,
    userRole: String?, // <-- RECIBE EL ROL
    onIngresarClick: () -> Unit,
    onCerrarSesionClick: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_ferji),
                contentDescription = "Logo de Ferji",
                modifier = Modifier.size(200.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "¡Bienvenido, $userName!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            // --- NUEVO: MUESTRA EL BADGE DE ADMIN ---
            if (userRole == UserRoles.ADMIN) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ADMINISTRADOR",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            // --- FIN DEL CAMBIO ---
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onIngresarClick,
                modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "INGRESAR", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onCerrarSesionClick) {
                Text("Cambiar de usuario")
            }
        }
    }
}


// --- SIN CAMBIOS EN LAS DEMÁS PANTALLAS (SplashScreen, PantallaInicial) ---

@Composable
fun PantallaInicial(modifier: Modifier = Modifier, onIngresarClick: () -> Unit) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(id = R.drawable.logo_ferji), contentDescription = "Logo de Ferji", modifier = Modifier.size(200.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Inspecciones de Daños por Sismo", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onIngresarClick, modifier = Modifier.fillMaxWidth(0.8f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text(text = "INICIAR SESIÓN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(painter = painterResource(id = R.drawable.logo_ferji), contentDescription = "Logo de Ferji", modifier = Modifier.size(200.dp))
    }
}

@Preview(showBackground = true, name = "Admin Logueado")
@Composable
fun PantallaBienvenidaAdminPreview() {
    FerjiTheme {
        PantallaBienvenida(userName = "Jorge Ferji", userRole = UserRoles.ADMIN, onIngresarClick = {}, onCerrarSesionClick = {})
    }
}
