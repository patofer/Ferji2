package com.ferji.inspecciones

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ferji.inspecciones.ui.components.FerjiDotLogo
import com.ferji.inspecciones.ui.components.FerjiGradientDivider
import com.ferji.inspecciones.ui.theme.*
import com.ferji.inspecciones.utils.esEmailValido
import com.ferji.inspecciones.utils.validarRutChileno
import com.ferji.inspecciones.viewmodels.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.loginSuccessEvent.collectLatest {
                setResult(RESULT_OK)
                finish()
            }
        }

        setContent {
            FerjiTheme {
                LoginScreen(
                    onLoginClicked = { rut, nombre, email ->
                        viewModel.onLoginClicked(rut, nombre, email)
                    }
                )
            }
        }
    }
}


@Composable
fun LoginScreen(onLoginClicked: (String, String, String) -> Unit) {
    val viewModel: LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val scope = rememberCoroutineScope()

    var rut by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var rutError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var usuarioExistente by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradiente decorativo superior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Spacing.xxl))

            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo_ferji),
                contentDescription = "Logo Ferji",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            Text(
                "Iniciar Sesión",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Ingresa tus datos para continuar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            FerjiGradientDivider()

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Formulario en card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {

            // RUT
            OutlinedTextField(
                value = rut,
                onValueChange = { newRut ->
                    rut = newRut.filter { it.isDigit() || it.equals('k', true) || it == '-' }
                    rutError = null

                    val rutLimpio = rut.replace("-", "").replace(".", "")
                    if (rutLimpio.length >= 7) {
                        scope.launch {
                            val userFromDb = viewModel.findUserByRut(rut)
                            if (userFromDb != null) {
                                nombre = userFromDb.nombre
                                email = userFromDb.email
                                usuarioExistente = true
                            } else {
                                usuarioExistente = false
                            }
                        }
                    } else {
                        usuarioExistente = false
                    }
                },
                label = { Text("RUT") },
                leadingIcon = {
                    Icon(Icons.Outlined.Badge, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                isError = rutError != null,
                supportingText = {
                    if (rutError != null) {
                        Text(rutError!!)
                    } else if (usuarioExistente) {
                        Text(
                            "✅ Usuario encontrado. Datos cargados automáticamente.",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Nombre
            OutlinedTextField(
                value = nombre,
                onValueChange = { if (!usuarioExistente) nombre = it },
                label = { Text("Nombre Completo") },
                leadingIcon = {
                    Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = usuarioExistente,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { newEmail ->
                    email = newEmail
                    emailError = null
                },
                label = { Text("Correo Electrónico") },
                leadingIcon = {
                    Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = emailError != null,
                supportingText = {
                    if (emailError != null) {
                        Text(emailError!!)
                    } else if (usuarioExistente) {
                        Text(
                            "Puede modificar el correo si lo desea.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Botón Ingresar
            Button(
                onClick = {
                    val isRutValid = rut.validarRutChileno()
                    val isEmailValid = email.esEmailValido()
                    val isNombreValid = nombre.isNotBlank()

                    rutError = if (!isRutValid) "El RUT ingresado no es válido." else null
                    emailError = if (!isEmailValid) "El formato del correo no es válido." else null

                    if (!isNombreValid) {
                        Toast.makeText(context, "El nombre no puede estar vacío.", Toast.LENGTH_SHORT).show()
                    }

                    if (isRutValid && isEmailValid && isNombreValid) {
                        onLoginClicked(rut, nombre, email)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComponentSize.buttonHeight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    "Ingresar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
        }
    }
}
