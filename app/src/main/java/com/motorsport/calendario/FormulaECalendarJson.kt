package com.motorsport.calendario

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object FormulaECalendarJson {

    private const val URL_JSON =
        "https://raw.githubusercontent.com/joaodob4-afk/calendario-motorsport/main/app/src/main/assets/fe_calendar.json"

    private const val ARQUIVO_CACHE = "formula_e_calendar_cache.json"

    fun carregar(context: Context): List<FormulaEEvent> {
        val json = lerCache(context) ?: lerAsset(context)

        val eventosJson = try {
            if (json != null) interpretar(json) else emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        if (eventosJson.size >= 10) {
            return eventosJson.sortedBy { it.etapa }
        }

        val combinados = FormulaECalendar.eventos
            .filterNot { local -> eventosJson.any { it.etapa == local.etapa } } +
            eventosJson

        return combinados.sortedBy { it.etapa }
    }

    fun atualizar(context: Context, aoTerminar: (() -> Unit)? = null) {
        val app = context.applicationContext

        Thread {
            var gravou = false

            try {
                val conexao = URL(URL_JSON).openConnection() as HttpURLConnection
                conexao.connectTimeout = 10000
                conexao.readTimeout = 10000
                conexao.setRequestProperty("Cache-Control", "no-cache")

                if (conexao.responseCode == 200) {
                    val texto = conexao.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                    if (interpretar(texto).isNotEmpty()) {
                        val destino = File(app.filesDir, ARQUIVO_CACHE)
                        val temporario = File(app.filesDir, "$ARQUIVO_CACHE.tmp")
                        temporario.writeText(texto)
                        gravou = temporario.renameTo(destino) ||
                            run { destino.writeText(texto); true }
                    }
                }

                conexao.disconnect()
            } catch (_: Exception) {
                // Sem internet ou erro: continua com o que já existe.
            }

            if (gravou && aoTerminar != null) {
                Handler(Looper.getMainLooper()).post { aoTerminar() }
            }
        }.start()
    }

    fun comoEtapas(eventos: List<FormulaEEvent>): List<MainActivity.Etapa> {
        val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val lista = mutableListOf<MainActivity.Etapa>()

        for (evento in eventos) {
            try {
                val inicio = LocalDate.parse(evento.inicio, formato)
                val fim = LocalDate.parse(evento.fim, formato)
                lista.add(
                    MainActivity.Etapa(
                        categoria = "Formula E",
                        pais = evento.pais,
                        circuito = evento.circuito,
                        data = F1CalendarApi.formatarPeriodo(inicio, fim),
                        dataInicio = inicio.toString(),
                        dataFim = fim.toString()
                    )
                )
            } catch (_: Exception) {
                // Ignora etapa com data inválida.
            }
        }

        return lista.sortedBy { it.dataInicio }
    }

    private fun lerCache(context: Context): String? {
        return try {
            val arquivo = File(context.filesDir, ARQUIVO_CACHE)
            if (!arquivo.exists()) return null

            val instalado = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .lastUpdateTime

            if (arquivo.lastModified() < instalado) return null

            val texto = arquivo.readText()
            if (interpretar(texto).isEmpty()) null else texto
        } catch (_: Exception) {
            null
        }
    }

    private fun lerAsset(context: Context): String? {
        return try {
            context.assets.open("fe_calendar.json")
                .bufferedReader()
                .use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }

    private fun interpretar(json: String): List<FormulaEEvent> {
        return try {
            val etapas = JSONObject(json).getJSONArray("etapas")
            val eventos = mutableListOf<FormulaEEvent>()

            for (i in 0 until etapas.length()) {
                val etapa = etapas.getJSONObject(i)
                val sessoesJson = etapa.getJSONArray("sessoes")
                val sessoes = mutableListOf<FormulaESession>()

                for (j in 0 until sessoesJson.length()) {
                    val sessao = sessoesJson.getJSONObject(j)
                    sessoes.add(
                        FormulaESession(
                            nome = sessao.getString("nome"),
                            data = sessao.getString("data"),
                            horario = sessao.optString("horario", "A confirmar")
                        )
                    )
                }

                eventos.add(
                    FormulaEEvent(
                        etapa = etapa.getInt("etapa"),
                        circuito = etapa.getString("circuito"),
                        pais = etapa.getString("pais"),
                        inicio = etapa.getString("inicio"),
                        fim = etapa.getString("fim"),
                        sessoes = sessoes
                    )
                )
            }

            eventos
        } catch (_: Exception) {
            emptyList()
        }
    }
}