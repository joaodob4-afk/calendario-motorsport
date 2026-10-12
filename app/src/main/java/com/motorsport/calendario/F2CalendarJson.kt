package com.motorsport.calendario

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object F2CalendarJson {

    private const val URL_JSON =
        "https://raw.githubusercontent.com/joaodob4-afk/calendario-motorsport/main/app/src/main/assets/f2_calendar.json"

    private const val ARQUIVO_CACHE = "f2_calendar_cache.json"
    private const val MINIMO_ETAPAS_VALIDAS = 10

    private val formatoData = DateTimeFormatter
        .ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT)

    /**
     * Mantém a mesma interface pública utilizada pelas telas existentes.
     * Prioriza o cache válido, depois o JSON incluído no APK e, por fim,
     * combina os dados disponíveis com o calendário de reserva.
     */
    fun carregar(context: Context): List<F2Event> {
        val json = lerCache(context) ?: lerAsset(context)

        val eventosJson = if (json != null) {
            interpretar(json)
        } else {
            emptyList()
        }

        if (eventosJson.size >= MINIMO_ETAPAS_VALIDAS) {
            return eventosJson.sortedBy { it.etapa }
        }

        val combinados = F2Calendar.eventos
            .filterNot { local ->
                eventosJson.any { it.etapa == local.etapa }
            } + eventosJson

        return combinados.sortedBy { it.etapa }
    }

    /**
     * Atualiza em segundo plano.
     * Um JSON inválido ou incompleto nunca substitui o cache válido.
     */
    fun atualizar(
        context: Context,
        aoTerminar: (() -> Unit)? = null
    ) {
        val app = context.applicationContext

        Thread {
            var gravou = false
            var conexao: HttpURLConnection? = null

            try {
                conexao = URL(URL_JSON)
                    .openConnection() as HttpURLConnection

                conexao.connectTimeout = 10000
                conexao.readTimeout = 10000
                conexao.requestMethod = "GET"
                conexao.setRequestProperty(
                    "Cache-Control",
                    "no-cache"
                )
                conexao.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                if (conexao.responseCode == HttpURLConnection.HTTP_OK) {
                    val texto = conexao.inputStream
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }

                    val eventos = interpretar(texto)

                    if (eventos.size >= MINIMO_ETAPAS_VALIDAS) {
                        gravou = gravarCacheSeguro(
                            app,
                            texto
                        )
                    }
                }
            } catch (_: Exception) {
                // Sem internet ou resposta inválida:
                // preserva o cache e os dados de reserva.
            } finally {
                try {
                    conexao?.disconnect()
                } catch (_: Exception) {
                    // A falha ao fechar a conexão não afeta a interface.
                }
            }

            if (gravou && aoTerminar != null) {
                Handler(Looper.getMainLooper()).post {
                    try {
                        aoTerminar()
                    } catch (_: Exception) {
                        // Evita que um callback defeituoso derrube a tarefa.
                    }
                }
            }
        }.start()
    }

    /**
     * Grava usando AtomicFile para preservar o cache anterior
     * caso a escrita ou a finalização falhe.
     */
    private fun gravarCacheSeguro(
        context: Context,
        conteudo: String
    ): Boolean {
        val arquivo = File(context.filesDir, ARQUIVO_CACHE)
        val cacheAtomico = AtomicFile(arquivo)

        var saida: java.io.FileOutputStream? = null

        return try {
            // Validação imediatamente antes de gravar.
            val eventos = interpretar(conteudo)

            if (eventos.size < MINIMO_ETAPAS_VALIDAS) {
                return false
            }

            saida = cacheAtomico.startWrite()

            saida.write(conteudo.toByteArray(Charsets.UTF_8))
            saida.flush()
            saida.fd.sync()

            cacheAtomico.finishWrite(saida)
            saida = null

            true
        } catch (_: Exception) {
            try {
                if (saida != null) {
                    cacheAtomico.failWrite(saida)
                }
            } catch (_: Exception) {
                // AtomicFile tenta recuperar a versão anterior.
            }

            false
        }
    }

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
            } catch (_: Exception) {
                // Ignora somente a etapa cuja data é inválida.
            }
        }

        return lista.sortedBy { it.dataInicio }
    }

    private fun lerCache(context: Context): String? {
        return try {
            val arquivo = File(context.filesDir, ARQUIVO_CACHE)

            if (!arquivo.exists()) return null
                
            val cacheAtomico = AtomicFile(arquivo)

            val texto = cacheAtomico.openRead()
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val eventos = interpretar(texto)

            if (eventos.size >= MINIMO_ETAPAS_VALIDAS) {
                texto
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun lerAsset(context: Context): String? {
        return try {
            context.assets.open("f2_calendar.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
                .takeIf {
                    interpretar(it).size >= MINIMO_ETAPAS_VALIDAS
                }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Rejeita calendários incompletos, etapas duplicadas,
     * datas inválidas, intervalos invertidos e sessões malformadas.
     */
    private fun interpretar(json: String): List<F2Event> {
        return try {
            val raiz = JSONObject(json)
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
        } catch (_: Exception) {
            emptyList()
        }
    }
}