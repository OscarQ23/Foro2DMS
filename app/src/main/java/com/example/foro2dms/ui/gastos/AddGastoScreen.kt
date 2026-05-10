package com.example.foro2dms.ui.gastos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.foro2dms.data.model.Gasto
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGastoScreen(
    categorias: List<String>,
    gastoExistente: Gasto? = null,
    onSave: (nombre: String, monto: Double, categoria: String, fecha: Long) -> Unit,
    onUpdate: (id: String, nombre: String, monto: Double, categoria: String, fecha: Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val esEdicion = gastoExistente != null

    var nombre by remember { mutableStateOf(gastoExistente?.nombre ?: "") }
    var montoTexto by remember {
        mutableStateOf(gastoExistente?.monto?.takeIf { it > 0 }?.toString() ?: "")
    }
    var categoria by remember(categorias, gastoExistente) {
        mutableStateOf(
            gastoExistente?.categoria?.takeIf { it.isNotBlank() }
                ?: categorias.firstOrNull()
                ?: ""
        )
    }
    var fecha by remember {
        mutableStateOf(gastoExistente?.fecha ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    BackHandler { onClose() }

    val montoDouble = montoTexto.toDoubleOrNull()
    val formularioValido = nombre.isNotBlank() &&
        montoDouble != null &&
        montoDouble > 0.0 &&
        categoria.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esEdicion) "Editar gasto" else "Nuevo gasto") },
                navigationIcon = {
                    TextButton(onClick = onClose) { Text("Cancelar") }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del gasto") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = montoTexto,
                onValueChange = { nuevo ->
                    if (nuevo.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        montoTexto = nuevo
                    }
                },
                label = { Text("Monto ($)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                OutlinedTextField(
                    value = categoria,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    categorias.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                categoria = cat
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Fecha", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                val formato = SimpleDateFormat("dd MMM yyyy", Locale("es"))
                Text(formato.format(fecha))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        val monto = montoDouble ?: 0.0
                        if (esEdicion) {
                            onUpdate(gastoExistente!!.id, nombre.trim(), monto, categoria, fecha)
                        } else {
                            onSave(nombre.trim(), monto, categoria, fecha)
                        }
                        onClose()
                    },
                    enabled = formularioValido,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Text(if (esEdicion) "Actualizar" else "Guardar")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = fecha)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { fecha = it }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
