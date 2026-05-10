package com.example.foro2dms.ui.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foro2dms.data.model.Categoria
import com.example.foro2dms.data.model.DEFAULT_CATEGORIAS
import com.example.foro2dms.data.model.Gasto
import com.example.foro2dms.data.repository.CategoriaRepository
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
    val categoriasCustom: List<Categoria> = emptyList(),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val errorMessage: String? = null
) {
    val gastosDelMes: List<Gasto>
        get() = gastos.filter { gasto ->
            val cal = Calendar.getInstance().apply { timeInMillis = gasto.fecha }
            cal.get(Calendar.MONTH) == selectedMonth &&
                cal.get(Calendar.YEAR) == selectedYear
        }

    val totalMesSeleccionado: Double
        get() = gastosDelMes.sumOf { it.monto }

    val nombresCategorias: List<String>
        get() = DEFAULT_CATEGORIAS + categoriasCustom.map { it.nombre }
}

class GastosViewModel(
    private val gastoRepository: GastoRepository = GastoRepository(),
    private val categoriaRepository: CategoriaRepository = CategoriaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GastosUiState())
    val uiState: StateFlow<GastosUiState> = _uiState.asStateFlow()

    init {
        observarGastos()
        observarCategorias()
    }

    private fun observarGastos() {
        viewModelScope.launch {
            gastoRepository.observeGastos()
                .catch { e ->
                    _uiState.update {
                        it.copy(errorMessage = "Error al cargar gastos: ${e.localizedMessage}")
                    }
                }
                .collect { lista ->
                    _uiState.update { it.copy(gastos = lista, errorMessage = null) }
                }
        }
    }

    private fun observarCategorias() {
        viewModelScope.launch {
            categoriaRepository.observeCategorias()
                .catch { }
                .collect { lista ->
                    _uiState.update { it.copy(categoriasCustom = lista) }
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
            gastoRepository.addGasto(gasto)
                .onFailure { e ->
                    _uiState.update {
                        it.copy(errorMessage = "No se pudo guardar: ${e.localizedMessage}")
                    }
                }
        }
    }

    fun deleteGasto(gastoId: String) {
        viewModelScope.launch {
            gastoRepository.deleteGasto(gastoId)
                .onFailure { e ->
                    _uiState.update {
                        it.copy(errorMessage = "No se pudo eliminar: ${e.localizedMessage}")
                    }
                }
        }
    }

    fun addCategoria(nombre: String) {
        val limpio = nombre.trim()
        if (limpio.isBlank()) return

        val yaExiste = _uiState.value.nombresCategorias.any {
            it.equals(limpio, ignoreCase = true)
        }
        if (yaExiste) {
            _uiState.update { it.copy(errorMessage = "Esa categoría ya existe") }
            return
        }

        viewModelScope.launch {
            categoriaRepository.addCategoria(limpio)
                .onFailure { e ->
                    _uiState.update {
                        it.copy(errorMessage = "No se pudo crear: ${e.localizedMessage}")
                    }
                }
        }
    }

    fun deleteCategoria(categoriaId: String) {
        viewModelScope.launch {
            categoriaRepository.deleteCategoria(categoriaId)
                .onFailure { e ->
                    _uiState.update {
                        it.copy(errorMessage = "No se pudo eliminar: ${e.localizedMessage}")
                    }
                }
        }
    }

    fun previousMonth() {
        _uiState.update {
            val cal = Calendar.getInstance().apply {
                set(it.selectedYear, it.selectedMonth, 1)
                add(Calendar.MONTH, -1)
            }
            it.copy(
                selectedYear = cal.get(Calendar.YEAR),
                selectedMonth = cal.get(Calendar.MONTH)
            )
        }
    }

    fun nextMonth() {
        _uiState.update {
            val cal = Calendar.getInstance().apply {
                set(it.selectedYear, it.selectedMonth, 1)
                add(Calendar.MONTH, 1)
            }
            it.copy(
                selectedYear = cal.get(Calendar.YEAR),
                selectedMonth = cal.get(Calendar.MONTH)
            )
        }
    }

    fun goToCurrentMonth() {
        val cal = Calendar.getInstance()
        _uiState.update {
            it.copy(
                selectedYear = cal.get(Calendar.YEAR),
                selectedMonth = cal.get(Calendar.MONTH)
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
