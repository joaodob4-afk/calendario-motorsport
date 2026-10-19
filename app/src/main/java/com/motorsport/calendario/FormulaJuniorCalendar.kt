package com.motorsport.calendario

/** Sessão de F2 ou F3 (as duas categorias têm exatamente o mesmo formato de dados). */
data class FormulaJuniorSession(
    val nome: String,
    val data: String,
    val horario: String
)

data class FormulaJuniorEvent(
    val etapa: Int,
    val circuito: String,
    val pais: String,
    val inicio: String,
    val fim: String,
    val sessoes: List<FormulaJuniorSession>
)
