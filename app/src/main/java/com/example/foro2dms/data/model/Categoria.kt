package com.example.foro2dms.data.model

data class Categoria(
    val id: String = "",
    val nombre: String = ""
)

val DEFAULT_CATEGORIAS = listOf(
    "Comida",
    "Transporte",
    "Servicios",
    "Entretenimiento",
    "Salud",
    "Educación",
    "Otros"
)
