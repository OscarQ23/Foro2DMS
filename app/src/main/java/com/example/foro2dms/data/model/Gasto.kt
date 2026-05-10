package com.example.foro2dms.data.model

data class Gasto(
    val id: String = "",
    val nombre: String = "",
    val monto: Double = 0.0,
    val categoria: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val userId: String = ""
)
