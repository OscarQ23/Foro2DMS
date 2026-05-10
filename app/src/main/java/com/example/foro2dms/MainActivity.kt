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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foro2dms.ui.auth.LoginScreen
import com.example.foro2dms.ui.auth.LoginViewModel
import com.example.foro2dms.ui.gastos.AddGastoScreen
import com.example.foro2dms.ui.gastos.GastosListScreen
import com.example.foro2dms.ui.gastos.GastosViewModel
import com.example.foro2dms.ui.theme.Foro2DMSTheme

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
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            LoginScreen(
                onLoginClick = { email, password -> authViewModel.login(email, password) },
                onRegisterClick = { email, password -> authViewModel.register(email, password) },
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun AuthenticatedFlow(onSignOut: () -> Unit) {
    val gastosViewModel: GastosViewModel = viewModel()
    val state by gastosViewModel.uiState.collectAsState()
    var showAddScreen by remember { mutableStateOf(false) }

    if (showAddScreen) {
        AddGastoScreen(
            onSave = { nombre, monto, categoria, fecha ->
                gastosViewModel.addGasto(nombre, monto, categoria, fecha)
            },
            onClose = { showAddScreen = false }
        )
    } else {
        GastosListScreen(
            gastos = state.gastos,
            totalMensual = state.totalMensual,
            errorMessage = state.errorMessage,
            onAddClick = { showAddScreen = true },
            onDeleteClick = { id -> gastosViewModel.deleteGasto(id) },
            onSignOut = onSignOut
        )
    }
}
