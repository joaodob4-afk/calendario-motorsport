package com.motorsport.calendario

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Detalhes de uma etapa da Fórmula E. A tela é a de [FormulaJuniorActivity]; aqui ficam só as
 * diferenças da categoria: de onde vêm os dados, o nome das sessões (cada corrida da rodada
 * dupla é destacada) e o texto do topo (cidade em vez de autódromo).
 */
class FormulaEActivity : FormulaJuniorActivity() {

    override val categoria = "Formula E"
    override val tituloRes = R.string.formula_e

    private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val horaValida = Regex("^\\d{1,2}:\\d{2}$")

    private fun lerData(texto: String): LocalDate? {
        return try {
            LocalDate.parse(texto, formatoData)
        } catch (e: Exception) {
            registrarDescarte("FormulaEActivity.lerData", e)
            null
        }
    }

    /** Converte o formato da Fórmula E para o da tela; sessões sem data válida ficam de fora. */
    override fun carregarEventos(): List<FormulaJuniorEvent> =
        FormulaECalendarJson.carregar(this).map { evento ->
            FormulaJuniorEvent(
                etapa = evento.etapa,
                circuito = evento.circuito,
                pais = evento.pais,
                inicio = evento.inicio,
                fim = evento.fim,
                sessoes = evento.sessoes.mapNotNull { sessao ->
                    if (lerData(sessao.data) == null) {
                        null
                    } else {
                        FormulaJuniorSession(
                            nome = sessao.nome,
                            data = sessao.data,
                            horario = sessao.horario.trim().takeIf { horaValida.matches(it) }.orEmpty()
                        )
                    }
                }
            )
        }

    override fun atualizarCalendario(aoTerminar: () -> Unit) {
        FormulaECalendarJson.atualizar(this, aoTerminar)
    }

    override fun descreverSessao(sessao: FormulaJuniorSession) = DescricaoDeSessao(
        nome = Idioma.sessao(this, sessao.nome).uppercase(idioma),
        destaque = sessao.nome.contains("corrida", ignoreCase = true)
    )

    override fun nomePadraoDoEvento(evento: FormulaJuniorEvent): String =
        Idioma.evento(this, "${Idioma.lugar(this, evento.circuito)} E-Prix")

    // A Fórmula E corre em ruas de cidades: no topo vai a cidade com o país, sem nome de autódromo.
    override fun circuitoDoHeroi(evento: FormulaJuniorEvent, nomeAutodromo: String): String =
        if (evento.sessoes.size <= 1) getString(R.string.fe_formato_unico) else ""

    override fun localDoHeroi(evento: FormulaJuniorEvent, nomeAutodromo: String): String =
        "${Idioma.lugar(this, evento.circuito)}, ${Idioma.lugar(this, evento.pais)}"

    override fun periodoDoHeroi(evento: FormulaJuniorEvent): String? {
        val inicio = lerData(evento.inicio)
        val fim = lerData(evento.fim)

        if (inicio == null || fim == null) return ""

        if (Idioma.ingles(this)) {
            return Formatacao.periodoEmIngles(inicio, fim).orEmpty().uppercase(idioma)
        }

        val formatoDia = DateTimeFormatter.ofPattern("dd", idioma)
        val formatoMes = DateTimeFormatter.ofPattern("MMM", idioma)
        val formatoMesCompleto = DateTimeFormatter.ofPattern("MMMM", idioma)

        return when {
            inicio == fim ->
                fim.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", idioma))

            inicio.year != fim.year ->
                "${inicio.format(DateTimeFormatter.ofPattern("dd MMM yyyy", idioma))} – " +
                    fim.format(DateTimeFormatter.ofPattern("dd MMM yyyy", idioma))

            inicio.month != fim.month ->
                "${inicio.format(formatoDia)} ${inicio.format(formatoMes)} – " +
                    fim.format(DateTimeFormatter.ofPattern("dd MMM", idioma))

            else ->
                "${inicio.format(formatoDia)} – ${fim.format(formatoDia)} " +
                    fim.format(formatoMesCompleto)
        }.uppercase(idioma)
    }
}
