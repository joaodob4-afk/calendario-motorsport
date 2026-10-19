package com.motorsport.calendario

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/**
 * Carregamento, validação e conversão do calendário de F2 e F3 (mesmo formato de JSON).
 * Cada categoria só informa a origem dos dados: ver F2CalendarJson e F3CalendarJson.
 */
abstract class CalendarioFormulaJunior(
    private val categoria: String,
    urlJson: String,
    arquivoAsset: String,
    arquivoCache: String
) {

    // O calendário pode legitimamente ter menos etapas após uma alteração oficial.
    private val minimoEtapasValidas = 1

    private val formatoData = DateTimeFormatter
        .ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT)

    private val fonte = FonteDeCalendario(
        nome = "${categoria}CalendarJson",
        urlJson = urlJson,
        arquivoAsset = arquivoAsset,
        arquivoCache = arquivoCache
    ) { interpretar(it) }

    /** Cache baixado ou JSON do APK, o que for mais recente. Lista vazia se nenhum for válido. */
    fun carregar(context: Context): List<FormulaJuniorEvent> =
        fonte.carregar(context).map { it.noFusoDoAparelho() }.sortedBy { it.etapa }

    /** Atualiza em segundo plano; um JSON inválido nunca substitui o cache atual. */
    fun atualizar(context: Context, aoTerminar: (() -> Unit)? = null) =
        fonte.atualizar(context, aoTerminar)

    /** Valida e grava no cache. Devolve false, sem tocar no cache, se for inválido. */
    internal fun gravarCacheSeguro(context: Context, conteudo: String): Boolean =
        fonte.gravarCache(context, conteudo)

    fun comoEtapas(eventos: List<FormulaJuniorEvent>): List<MainActivity.Etapa> {
        val lista = mutableListOf<MainActivity.Etapa>()

        for (evento in eventos) {
            try {
                val inicio = LocalDate.parse(evento.inicio, formatoData)
                val fim = LocalDate.parse(evento.fim, formatoData)

                if (fim.isBefore(inicio)) continue

                val pais = if (
                    evento.pais == "Emirados Árabes Unidos"
                ) {
                    "Abu Dhabi"
                } else {
                    evento.pais
                }

                lista.add(
                    MainActivity.Etapa(
                        categoria = categoria,
                        pais = pais,
                        circuito = evento.circuito,
                        data = F1CalendarApi.formatarPeriodo(inicio, fim),
                        dataInicio = inicio.toString(),
                        dataFim = fim.toString()
                    )
                )
            } catch (e: Exception) {
                registrarDescarte("${categoria}CalendarJson.comoEtapas", e)
                // Ignora somente a etapa cuja data é inválida.
            }
        }

        return lista.sortedBy { it.dataInicio }
    }
    /**
     * Rejeita calendários incompletos, etapas duplicadas,
     * datas inválidas, intervalos invertidos e sessões malformadas.
     */
    private fun interpretar(json: String): List<FormulaJuniorEvent> {
        return try {
            val raiz = JSONObject(json)
            if (!raiz.optString("categoria").trim().equals(categoria, ignoreCase = true)) {
                return emptyList()
            }
            val etapas = raiz.getJSONArray("etapas")

            if (etapas.length() < minimoEtapasValidas) {
                return emptyList()
            }

            val eventos = mutableListOf<FormulaJuniorEvent>()
            val numerosEtapas = mutableSetOf<Int>()

            for (i in 0 until etapas.length()) {
                val etapa = etapas.getJSONObject(i)

                val numero = etapa.getInt("etapa")
                val circuito = etapa.getString("circuito").trim()
                val pais = etapa.getString("pais").trim()
                val inicioTexto = etapa.getString("inicio")
                val fimTexto = etapa.getString("fim")

                if (numero <= 0) return emptyList()
                if (!numerosEtapas.add(numero)) return emptyList()
                if (circuito.isBlank() || pais.isBlank()) {
                    return emptyList()
                }

                val inicio = LocalDate.parse(inicioTexto, formatoData)
                val fim = LocalDate.parse(fimTexto, formatoData)

                if (fim.isBefore(inicio)) return emptyList()

                val sessoesJson = etapa.getJSONArray("sessoes")

                if (sessoesJson.length() == 0) {
                    return emptyList()
                }

                val sessoes = mutableListOf<FormulaJuniorSession>()

                for (j in 0 until sessoesJson.length()) {
                    val sessao = sessoesJson.getJSONObject(j)

                    val nome = sessao.getString("nome").trim()
                    val data = sessao.getString("data")
                    val horario = sessao.optString(
                        "horario",
                        "A confirmar"
                    ).ifBlank { "A confirmar" }

                    if (nome.isBlank()) return emptyList()

                    // A data da sessão precisa ser válida.
                    LocalDate.parse(data, formatoData)

                    sessoes.add(
                        FormulaJuniorSession(
                            nome = nome,
                            data = data,
                            horario = horario
                        )
                    )
                }

                eventos.add(
                    FormulaJuniorEvent(
                        etapa = numero,
                        circuito = circuito,
                        pais = pais,
                        inicio = inicioTexto,
                        fim = fimTexto,
                        sessoes = sessoes
                    )
                )
            }

            eventos.sortedBy { it.etapa }
        } catch (e: Exception) {
            registrarDescarte("${categoria}CalendarJson.interpretar", e)
            emptyList()
        }
    }
}