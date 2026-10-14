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
    // O calendário pode legitimamente ter menos etapas após uma alteração oficial.
    private const val MINIMO_ETAPAS_VALIDAS = 1

    private val formatoData = DateTimeFormatter
        .ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT)

    /**
     * Mantém a mesma interface pública utilizada pelas telas existentes.
     * Prioriza o cache válido e depois o JSON incluído no APK.
     * Usa o calendário de reserva somente quando os JSONs não são válidos.
     */
    fun carregar(context: Context): List<F2Event> {
        val json = lerCache(context) ?: lerAsset(context)
        val eventosJson = json?.let { interpretar(it) }.orEmpty()
        // Um calendário remoto estruturalmente válido é a fonte oficial desta temporada.
        // Não mescla o fallback por número de etapa: isso poderia ressuscitar etapas
        // removidas ou associar o circuito errado depois de uma renumeração.
        if (eventosJson.isNotEmpty()) {
            return eventosJson.sortedBy { it.etapa }
        }

        return F2Calendar.eventos.sortedBy { it.etapa }
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
    internal fun gravarCacheSeguro(
        context: Context,
        conteudo: String
    ): Boolean {
        val arquivo = File(context.filesDir, ARQUIVO_CACHE)
        val cacheAtomico = AtomicFile(arquivo)

        var saida: java.io.FileOutputStream? = null

        return try {
            val eventosNovos = interpretar(conteudo)

            if (eventosNovos.size < MINIMO_ETAPAS_VALIDAS) {
                return false
            }

            // Só compara calendários da mesma temporada: a quantidade de etapas
            // pode mudar legitimamente quando uma nova temporada é publicada.
            val temporadaNova = JSONObject(conteudo).optInt("temporada", 0)
            if (temporadaNova > 0) {
                val eventosAtuais = try {
                    val textoAtual = cacheAtomico.openRead()
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    val temporadaAtual = JSONObject(textoAtual)
                        .optInt("temporada", 0)
                    val atuais = interpretar(textoAtual)

                    if (
                        temporadaAtual == temporadaNova &&
                        atuais.size >= MINIMO_ETAPAS_VALIDAS
                    ) {
                        atuais
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }

                // Na mesma temporada, compara por circuito (não pelo número da etapa).
                // Etapas/sessões ausentes podem ter sido removidas ou renomeadas oficialmente;
                // não as ressuscita. Só bloqueia a perda de um horário ainda representado
                // pela mesma sessão, pois isso degradaria os detalhes offline.
                if (eventosAtuais != null) {
                    val novosPorCircuito = eventosNovos.associateBy {
                        it.circuito.trim().lowercase()
                    }
                    for (atual in eventosAtuais) {
                        val novo = novosPorCircuito[atual.circuito.trim().lowercase()]
                            ?: continue
                        val novasSessoes = novo.sessoes.associateBy {
                            it.nome.trim().lowercase()
                        }
                        for (sessaoAtual in atual.sessoes) {
                            val sessaoNova = novasSessoes[sessaoAtual.nome.trim().lowercase()]
                                ?: continue
                            val horarioAtual = sessaoAtual.horario.trim()
                            val horarioNovo = sessaoNova.horario.trim()
                            if (
                                horarioAtual.isNotBlank() &&
                                !horarioAtual.equals("A confirmar", ignoreCase = true) &&
                                horarioNovo.equals("A confirmar", ignoreCase = true)
                            ) {
                                return false
                            }
                        }
                    }
                }
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
        } catch (_: Exception) {
            emptyList()
        }
    }
}