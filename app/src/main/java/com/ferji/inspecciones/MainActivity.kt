package com.ferji.inspecciones

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferji.inspecciones.ui.theme.FerjiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FerjiTheme {
                // Aplicamos edge-to-edge
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PantallaInicial(
                        modifier = Modifier.padding(innerPadding),
                        onIngresarClick = {
                            // Agrega un log para ver si al menos se ejecuta el clic
                            android.util.Log.d("FERJI_APP", "Botón INGRESAR presionado")

                            try {
                                val intent = Intent(this, MenuPrincipalActivity::class.java)
                                startActivity(intent)
                                android.util.Log.d("FERJI_APP", "Intent iniciado correctamente")
                            } catch (e: Exception) {
                                android.util.Log.e("FERJI_APP", "Error al iniciar actividad: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PantallaInicial(
    modifier: Modifier = Modifier,
    onIngresarClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo de Ferji - asegúrate de tener logo_ferji.png en res/drawable
            Image(
                painter = painterResource(id = R.drawable.logo_ferji),
                contentDescription = "Logo de Ferji",
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Título de la aplicación
            Text(
                text = "Inspecciones de Daños por Sismo",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Botón de ingresar
            Button(
                onClick = onIngresarClick,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "INGRESAR",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PantallaInicialPreview() {
    FerjiTheme {
      // Añade Surface aquí para el Preview
            PantallaInicial()
        }

}