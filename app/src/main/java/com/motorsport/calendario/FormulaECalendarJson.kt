package com.motorsport.calendario

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

data class FormulaESession(
    val nome: String,
    val data: String,
    val horario: String
)

data class FormulaEEvent(
    val etapa: Int,
    val circuito: String,
    val pais: String,
    val inicio: String,
    val fim: String,
    val sessoes: List<FormulaESession>
)

object FormulaECalendarJson {

    private const val URL_JSON =
        "https://raw.githubusercontent.com/joaodob4-afk/" +
        "calendario-motorsport/main/app/src/main/assets/fe_calendar.json"

    private const val ARQUIVO_CACHE = "fe_calendar_cache.json"

    private val regexNumeroRodada = Regex("""(?i)\bRodada\s+(\d+)\b""")

    private fun numeroDaRodada(nome: String): Int? {
        if (!nome.contains("corrida", ignoreCase = true)) return null
        return regexNumeroRodada.find(nome)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private val formatoData: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT)

    private val fonte = FonteDeCalendario(
        nome = "FormulaECalendarJson",
        urlJson = URL_JSON,
        arquivoAsset = "fe_calendar.json",
        arquivoCache = ARQUIVO_CACHE
    ) { interpretar(it) }

    /** Cache baixado ou JSON do APK, o que for mais recente. Lista vazia se nenhum for válido. */
    fun carregar(context: Context): List<FormulaEEvent> =
        fonte.carregar(context).sortedBy { it.etapa }

    /** Atualiza em segundo plano; um JSON inválido nunca substitui o cache atual. */
    fun atualizar(context: Context, aoTerminar: (() -> Unit)? = null) =
        fonte.atualizar(context, aoTerminar)

    /** Valida e grava no cache. Devolve false, sem tocar no cache, se for inválido. */
    internal fun gravarCacheSeValido(context: Context, texto: String): Boolean =
        fonte.gravarCache(context, texto)

    /**
     * Converte os eventos para o modelo usado pela tela inicial.
     * A assinatura e os campos do modelo original são preservados.
     */
    fun comoEtapas(
        eventos: List<FormulaEEvent>
    ): List<MainActivity.Etapa> {
        val lista = mutableListOf<MainActivity.Etapa>()

        for (evento in eventos) {
            try {
                val inicio = LocalDate.parse(evento.inicio, formatoData)
                val fim = LocalDate.parse(evento.fim, formatoData)

                val pais =
                    if (evento.pais == "Emirados Árabes Unidos") {
                        "Abu Dhabi"
                    } else {
                        evento.pais
                    }

                lista.add(
                    MainActivity.Etapa(
                        categoria = "Formula E",
                        pais = pais,
                        circuito = evento.circuito,
                        data = F1CalendarApi.formatarPeriodo(inicio, fim),
                        dataInicio = inicio.toString(),
                        dataFim = fim.toString()
                    )
                )
            } catch (e: Exception) {
                registrarDescarte("FormulaECalendarJson.comoEtapas", e)
                // Ignora somente a etapa que não puder ser convertida.
            }
        }

        return lista.sortedBy { it.dataInicio }
    }
    /**
     * Valida estrutura, categoria, etapas, datas e sessões.
     * Um JSON incompleto ou inconsistente é rejeitado por inteiro.
     */
    private fun interpretar(json: String): List<FormulaEEvent> {
        val raiz = JSONObject(json)

        require(raiz.optString("categoria") == "Formula E") {
            "Categoria inválida"
        }

        val etapas = raiz.getJSONArray("etapas")

        // A fonte oficial define a quantidade de etapas e corridas da temporada.
        // Exigimos ao menos uma etapa válida, mas nunca um total fixo.
        require(etapas.length() > 0) { "Calendário sem etapas" }

        val eventos = mutableListOf<FormulaEEvent>()
        val numerosEtapas = mutableSetOf<Int>()
        val numerosRodadas = mutableSetOf<Int>()

        for (i in 0 until etapas.length()) {
            val etapa = etapas.getJSONObject(i)

            val numero = etapa.getInt("etapa")
            val circuito = etapa.getString("circuito").trim()
            val pais = etapa.getString("pais").trim()
            val inicioTexto = etapa.getString("inicio").trim()
            val fimTexto = etapa.getString("fim").trim()

            require(numero > 0) { "Número de etapa inválido" }
            require(numerosEtapas.add(numero)) {
                "Número de etapa repetido"
            }
            require(circuito.isNotBlank()) { "Circuito vazio" }
            require(pais.isNotBlank()) { "País vazio" }

            val inicio = LocalDate.parse(inicioTexto, formatoData)
            val fim = LocalDate.parse(fimTexto, formatoData)

            require(!fim.isBefore(inicio)) {
                "Data final anterior à data inicial"
            }

            val sessoesJson = etapa.getJSONArray("sessoes")
            require(sessoesJson.length() > 0) {
                "Etapa sem sessões"
            }

            val sessoes = mutableListOf<FormulaESession>()
            var corridasNestaEtapa = 0

            for (j in 0 until sessoesJson.length()) {
                val sessao = sessoesJson.getJSONObject(j)

                val nome = sessao.getString("nome").trim()
                val data = sessao.getString("data").trim()
                val horario = sessao.optString(
                    "horario",
                    "A confirmar"
                ).trim().ifBlank { "A confirmar" }

                require(nome.isNotBlank()) { "Sessão sem nome" }

                // Contabiliza apenas corridas identificadas como "Rodada N".
                val numeroRodada = numeroDaRodada(nome)
                if (numeroRodada != null) {
                    require(numeroRodada > 0) { "Número de rodada inválido" }
                    require(numerosRodadas.add(numeroRodada)) {
                        "Número de rodada repetido"
                    }
                    corridasNestaEtapa++
                }

                // Confirma que a data da sessão existe e está no
                // intervalo da etapa, sem alterar a data original.
                val dataSessao = LocalDate.parse(data, formatoData)

                require(
                    !dataSessao.isBefore(inicio) &&
                    !dataSessao.isAfter(fim)
                ) {
                    "Data da sessão fora do período da etapa"
                }

                sessoes.add(
                    FormulaESession(
                        nome = nome,
                        data = data,
                        horario = horario
                    )
                )
            }

            require(corridasNestaEtapa > 0) {
                "Etapa sem sessão de corrida identificável"
            }

            eventos.add(
                FormulaEEvent(
                    etapa = numero,
                    circuito = circuito,
                    pais = pais,
                    inicio = inicioTexto,
                    fim = fimTexto,
                    sessoes = sessoes
                )
            )
        }

        return eventos
    }
}