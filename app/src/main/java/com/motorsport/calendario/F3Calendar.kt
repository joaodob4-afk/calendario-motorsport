package com.motorsport.calendario

data class F3Session(
    val nome: String,
    val data: String,
    val horario: String
)

data class F3Event(
    val etapa: Int,
    val circuito: String,
    val pais: String,
    val inicio: String,
    val fim: String,
    val sessoes: List<F3Session>
)
