package com.example.foro2dms.ui.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foro2dms.data.model.Gasto
import com.example.foro2dms.data.repository.GastoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class GastosUiState(
    val gastos: List<Gasto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val totalMensual: Double = 0.0
)

class GastosViewModel(
    private val repository: GastoRepository = GastoRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GastosUiState())
    val uiState: StateFlow<GastosUiState> = _uiState.asStateFlow()

    init {
        observarGastos()
    }

    private fun observarGastos() {
        viewModelScope.launch {
            repository.observeGastos()
                .catch { e ->
                    _uiState.update {
                        it.copy(errorMessage = "Error al cargar gastos: ${e.localizedMessage}")
                    }
                }
                .collect { lista ->
                    _uiState.update {
                        it.copy(
                            gastos = lista,
                            totalMensual = calcularTotalMensual(lista),
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun addGasto(nombre: String, monto: Double, categoria: String, fecha: Long) {
        if (nombre.isBlank() || monto <= 0.0 || categoria.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Completa todos los campos con valores válidos")
            }
            return
        }

        val gasto = Gasto(
            nombre = nombre.trim(),
            monto = monto,
            categoria = categoria,
            fecha = fecha
        )

        viewModelScope.launch {
            repository.addGasto(gasto)
                .onFailure { e ->
                    _uiState.update {
                        it.copy(errorMessage = "No se pudo guardar: ${e.localizedMessage}")
                    }
                }
        }
    }

    fun deleteGasto(gastoId: String) {
        viewModelScope.launch {
            repository.deleteGasto(gastoId)
                .onFailure { e ->
                    _uiState.update {
                        it.copy(errorMessage = "No se pudo eliminar: ${e.localizedMessage}")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun calcularTotalMensual(gastos: List<Gasto>): Double {
        val ahora = Calendar.getInstance()
        val mesActual = ahora.get(Calendar.MONTH)
        val anioActual = ahora.get(Calendar.YEAR)

        return gastos
            .filter { gasto ->
                val cal = Calendar.getInstance().apply { timeInMillis = gasto.fecha }
                cal.get(Calendar.MONTH) == mesActual &&
                    cal.get(Calendar.YEAR) == anioActual
            }
            .sumOf { it.monto }
    }
}
