package com.example.foro2dms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foro2dms.data.auth.GoogleAuthClient
import com.example.foro2dms.data.model.DEFAULT_CATEGORIAS
import com.example.foro2dms.ui.auth.LoginScreen
import com.example.foro2dms.ui.auth.LoginViewModel
import com.example.foro2dms.ui.categorias.CategoriasScreen
import com.example.foro2dms.ui.gastos.AddGastoScreen
import com.example.foro2dms.ui.gastos.GastosListScreen
import com.example.foro2dms.ui.gastos.GastosViewModel
import com.example.foro2dms.ui.theme.Foro2DMSTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private enum class Pantalla { LISTA, AGREGAR, CATEGORIAS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Foro2DMSTheme {
                AuthGate()
            }
        }
    }
}

@Composable
private fun AuthGate() {
    val authViewModel: LoginViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()

    if (authState.isAuthenticated) {
        AuthenticatedFlow(onSignOut = { authViewModel.signOut() })
    } else {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val googleAuthClient = remember { GoogleAuthClient(context) }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            LoginScreen(
                onLoginClick = { email, password -> authViewModel.login(email, password) },
                onRegisterClick = { email, password -> authViewModel.register(email, password) },
                onGoogleClick = {
                    scope.launch {
                        googleAuthClient.getIdToken()
                            .onSuccess { idToken ->
                                authViewModel.loginWithGoogle(idToken)
                            }
                            .onFailure { e ->
                                if (e !is GetCredentialCancellationException) {
                                    authViewModel.setError(
                                        e.localizedMessage ?: "Error al conectar con Google"
                                    )
                                }
                            }
                    }
                },
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun AuthenticatedFlow(onSignOut: () -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val gastosViewModel: GastosViewModel = viewModel(key = uid)
    val state by gastosViewModel.uiState.collectAsState()
    var pantalla by remember(uid) { mutableStateOf(Pantalla.LISTA) }

    when (pantalla) {
        Pantalla.AGREGAR -> AddGastoScreen(
            categorias = state.nombresCategorias,
            onSave = { nombre, monto, categoria, fecha ->
                gastosViewModel.addGasto(nombre, monto, categoria, fecha)
            },
            onClose = { pantalla = Pantalla.LISTA }
        )
        Pantalla.CATEGORIAS -> CategoriasScreen(
            categoriasDefault = DEFAULT_CATEGORIAS,
            categoriasCustom = state.categoriasCustom,
            errorMessage = state.errorMessage,
            onAddCategoria = { nombre -> gastosViewModel.addCategoria(nombre) },
            onDeleteCategoria = { id -> gastosViewModel.deleteCategoria(id) },
            onClose = {
                gastosViewModel.clearError()
                pantalla = Pantalla.LISTA
            }
        )
        Pantalla.LISTA -> GastosListScreen(
            gastos = state.gastosDelMes,
            totalMesSeleccionado = state.totalMesSeleccionado,
            selectedYear = state.selectedYear,
            selectedMonth = state.selectedMonth,
            errorMessage = state.errorMessage,
            onAddClick = { pantalla = Pantalla.AGREGAR },
            onDeleteClick = { id -> gastosViewModel.deleteGasto(id) },
            onPrevMonth = { gastosViewModel.previousMonth() },
            onNextMonth = { gastosViewModel.nextMonth() },
            onManageCategorias = { pantalla = Pantalla.CATEGORIAS },
            onSignOut = onSignOut
        )
    }
}
