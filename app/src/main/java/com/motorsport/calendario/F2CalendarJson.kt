package com.motorsport.calendario

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object F2CalendarJson {

    private const val URL_JSON =
        "https://raw.githubusercontent.com/joaodob4-afk/calendario-motorsport/main/app/src/main/assets/f2_calendar.json"

    private const val ARQUIVO_CACHE = "f2_calendar_cache.json"
    // O calendário pode legitimamente ter menos etapas após uma alteração oficial.
    private const val MINIMO_ETAPAS_VALIDAS = 1

    private val formatoData = DateTimeFormatter
        .ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT)

    private val fonte = FonteDeCalendario(
        nome = "F2CalendarJson",
        urlJson = URL_JSON,
        arquivoAsset = "f2_calendar.json",
        arquivoCache = ARQUIVO_CACHE
    ) { interpretar(it) }

    /** Cache baixado ou JSON do APK, o que for mais recente. Lista vazia se nenhum for válido. */
    fun carregar(context: Context): List<F2Event> =
        fonte.carregar(context).sortedBy { it.etapa }

    /** Atualiza em segundo plano; um JSON inválido nunca substitui o cache atual. */
    fun atualizar(context: Context, aoTerminar: (() -> Unit)? = null) =
        fonte.atualizar(context, aoTerminar)

    /** Valida e grava no cache. Devolve false, sem tocar no cache, se for inválido. */
    internal fun gravarCacheSeguro(context: Context, conteudo: String): Boolean =
        fonte.gravarCache(context, conteudo)

    fun comoEtapas(eventos: List<F2Event>): List<MainActivity.Etapa> {
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
                        categoria = "F2",
                        pais = pais,
                        circuito = evento.circuito,
                        data = F1CalendarApi.formatarPeriodo(inicio, fim),
                        dataInicio = inicio.toString(),
                        dataFim = fim.toString()
                    )
                )
            } catch (e: Exception) {
                registrarDescarte("F2CalendarJson.comoEtapas", e)
                // Ignora somente a etapa cuja data é inválida.
            }
        }

        return lista.sortedBy { it.dataInicio }
    }
    /**
     * Rejeita calendários incompletos, etapas duplicadas,
     * datas inválidas, intervalos invertidos e sessões malformadas.
     */
    private fun interpretar(json: String): List<F2Event> {
        return try {
            val raiz = JSONObject(json)
            if (!raiz.optString("categoria").trim().equals("F2", ignoreCase = true)) {
                return emptyList()
            }
            val etapas = raiz.getJSONArray("etapas")

            if (etapas.length() < MINIMO_ETAPAS_VALIDAS) {
                return emptyList()
            }

            val eventos = mutableListOf<F2Event>()
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

                val sessoes = mutableListOf<F2Session>()

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
                        F2Session(
                            nome = nome,
                            data = data,
                            horario = horario
                        )
                    )
                }

                eventos.add(
                    F2Event(
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
            registrarDescarte("F2CalendarJson.interpretar", e)
            emptyList()
        }
    }
}