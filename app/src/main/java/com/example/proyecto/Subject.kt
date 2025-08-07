package com.example.proyecto

data class Subject(
    val codigo: String,
    val nombre: String,
    val profesor: String,
    val curso: String, // <-- AÑADIDO
    val dia: String,
    val hora_inicio: String,
    val hora_fin: String
)