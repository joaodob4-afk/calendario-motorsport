package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class PreparadorDeSessoesTest {
    @Test fun converteCadaDataUmaVezEOrdenaPorDataEHorario() {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.US); var conversoes = 0
        val sessoes = listOf(Sessao("Corrida", "02/03/2026", "14:00"), Sessao("Treino", "01/03/2026", "10:00"), Sessao("Classificação", "01/03/2026", "09:00"))
        val resultado = PreparadorDeSessoes.ordenar(sessoes, { conversoes++; formato.parse(it.data) }, { it.horario })
        assertEquals(3, conversoes)
        assertEquals(listOf("Classificação", "Treino", "Corrida"), resultado.map { it.sessao.nome })
    }
    @Test fun dataInvalidaFicaDepoisDasDatasValidas() {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }
        val sessoes = listOf(Sessao("Sem data", "invalida", "A confirmar"), Sessao("Com data", "01/03/2026", "09:30"))
        val resultado = PreparadorDeSessoes.ordenar(sessoes, { runCatching { formato.parse(it.data) }.getOrNull() }, { it.horario })
        assertEquals(listOf("Com data", "Sem data"), resultado.map { it.sessao.nome })
    }
    private data class Sessao(val nome: String, val data: String, val horario: String)
}
