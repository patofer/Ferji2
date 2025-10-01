package com.ferji.inspecciones

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ferji.inspecciones.ui.theme.FerjiTheme
import com.ferji.inspecciones.utils.esEmailValido
import com.ferji.inspecciones.utils.validarRutChileno
import com.ferji.inspecciones.viewmodels.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.ferji.inspecciones.utils.esEmailValido


@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    // El ViewModel se obtiene a través de Hilt
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Observa el evento de login exitoso para cerrar la actividad
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
                        // Llama a la función del ViewModel cuando se hace clic en el botón
                        viewModel.onLoginClicked(rut, nombre, email)
                    }
                )
            }
        }
    }
}


/**
 * Composable que define la pantalla de Login con todas las validaciones y autocompletado.
 */
@Composable
fun LoginScreen(onLoginClicked: (String, String, String) -> Unit) {
    // Obtenemos una instancia del ViewModel para acceder a sus funciones
    val viewModel: LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    // Creamos un CoroutineScope para llamar funciones 'suspend' (como la búsqueda en BD)
    val scope = rememberCoroutineScope()

    // Estados para almacenar los valores de los campos de texto
    var rut by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    // Estados para almacenar los mensajes de error de validación
    var rutError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Iniciar Sesión", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            // --- CAMPO DE TEXTO PARA EL RUT ---
            OutlinedTextField(
                value = rut,
                onValueChange = { newRut ->
                    rut = newRut.filter { it.isDigit() || it.equals('k', true) || it == '-' }
                    rutError = null // Limpia el error al empezar a escribir

                    // Lógica de Autocompletado: Si el RUT es potencialmente válido, busca en la BD
                    if (rut.length >= 8) {
                        scope.launch {
                            val userFromDb = viewModel.findUserByRut(rut)
                            if (userFromDb != null) {
                                nombre = userFromDb.nombre
                                email = userFromDb.email
                            }
                        }
                    }
                },
                label = { Text("RUT") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                isError = rutError != null, // El campo se pone rojo si hay error
                supportingText = { if (rutError != null) Text(rutError!!) } // Muestra el mensaje de error
            )
            Spacer(modifier = Modifier.height(16.dp))

            // --- CAMPO DE TEXTO PARA EL NOMBRE ---
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // --- CAMPO DE TEXTO PARA EL EMAIL ---
            OutlinedTextField(
                value = email,
                onValueChange = { newEmail ->
                    email = newEmail
                    emailError = null // Limpia el error al escribir
                },
                label = { Text("Correo Electrónico") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = emailError != null,
                supportingText = { if (emailError != null) Text(emailError!!) }
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTÓN DE INGRESAR ---
            Button(
                onClick = {
                    // Lógica de Validación al hacer clic
                    val isRutValid = rut.validarRutChileno()
                    val isEmailValid = email.esEmailValido()
                    val isNombreValid = nombre.isNotBlank()

                    // Asigna los mensajes de error si las validaciones fallan
                    rutError = if (!isRutValid) "El RUT ingresado no es válido." else null
                    emailError = if (!isEmailValid) "El formato del correo no es válido." else null

                    if (!isNombreValid) {
                        Toast.makeText(context, "El nombre no puede estar vacío.", Toast.LENGTH_SHORT).show()
                    }

                    // Si todas las validaciones son correctas, se ejecuta el login
                    if (isRutValid && isEmailValid && isNombreValid) {
                        onLoginClicked(rut, nombre, email)
                    }

               3 },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ingresar")
            }
        }
    }
}
