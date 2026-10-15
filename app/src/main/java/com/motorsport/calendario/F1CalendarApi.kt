
package com.motorsport.calendario

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Monta a lista de etapas da F1 a partir da API Jolpica,
 * guarda em cache com gravação segura e nunca lança exceção
 * para a interface do aplicativo.
 */
object F1CalendarApi {

    private const val URL_API =
        "https://api.jolpi.ca/ergast/f1/current/races/?limit=100"

    private const val ARQUIVO_CACHE = "f1_races_cache.json"
    private const val ARQUIVO_TEMPORARIO = "f1_races_cache.tmp"
    private const val MINIMO_CORRIDAS_VALIDAS = 1

    // Serializa atualizações que podem ser disparadas por mais de uma tela.
    private val cacheLock = Any()

    private val MESES = arrayOf(
        "JAN", "FEV", "MAR", "ABR", "MAI", "JUN",
        "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"
    )

    private val CONHECIDOS = mapOf(
        "albert_park" to Pair("Austrália", "Melbourne"),
        "shanghai" to Pair("China", "Xangai"),
        "suzuka" to Pair("Japão", "Suzuka"),
        "bahrain" to Pair("Bahrein", "Sakhir"),
        "jeddah" to Pair("Arábia Saudita", "Jeddah"),
        "miami" to Pair("Estados Unidos", "Miami"),
        "villeneuve" to Pair("Canadá", "Montreal"),
        "monaco" to Pair("Mônaco", "Monte Carlo"),
        "catalunya" to Pair("Espanha", "Barcelona"),
        "red_bull_ring" to Pair("Áustria", "Spielberg"),
        "silverstone" to Pair("Reino Unido", "Silverstone"),
        "spa" to Pair("Bélgica", "Spa-Francorchamps"),
        "hungaroring" to Pair("Hungria", "Budapeste"),
        "zandvoort" to Pair("Holanda", "Zandvoort"),
        "monza" to Pair("Itália", "Monza"),
        "madring" to Pair("Espanha", "Madrid"),
        "baku" to Pair("Azerbaijão", "Baku"),
        "marina_bay" to Pair("Singapura", "Marina Bay"),
        "americas" to Pair("Estados Unidos", "Austin"),
        "rodriguez" to Pair("México", "Cidade do México"),
        "interlagos" to Pair("Brasil", "Interlagos"),
        "vegas" to Pair("Estados Unidos", "Las Vegas"),
        "losail" to Pair("Catar", "Lusail"),
        "yas_marina" to Pair("Abu Dhabi", "Yas Marina")
    )

    private val PAISES = mapOf(
        "Australia" to "Austrália",
        "China" to "China",
        "Japan" to "Japão",
        "Bahrain" to "Bahrein",
        "Saudi Arabia" to "Arábia Saudita",
        "USA" to "Estados Unidos",
        "United States" to "Estados Unidos",
        "Canada" to "Canadá",
        "Monaco" to "Mônaco",
        "Spain" to "Espanha",
        "Austria" to "Áustria",
        "UK" to "Reino Unido",
        "United Kingdom" to "Reino Unido",
        "Great Britain" to "Reino Unido",
        "Belgium" to "Bélgica",
        "Hungary" to "Hungria",
        "Netherlands" to "Holanda",
        "Italy" to "Itália",
        "Azerbaijan" to "Azerbaijão",
        "Singapore" to "Singapura",
        "Mexico" to "México",
        "Brazil" to "Brasil",
        "Qatar" to "Catar",
        "UAE" to "Abu Dhabi",
        "United Arab Emirates" to "Abu Dhabi"
    )

    private val SESSOES = listOf(
        "FirstPractice", "SecondPractice", "ThirdPractice",
        "SprintQualifying", "SprintShootout", "Sprint", "Qualifying"
    )

    fun carregarCache(context: Context): List<MainActivity.Etapa>? {
        return try {
            val arquivo = File(
                context.applicationContext.filesDir,
                ARQUIVO_CACHE
            )

            if (!arquivo.exists() || !arquivo.isFile) {
                return null
            }

            val lista = interpretar(arquivo.readText(Charsets.UTF_8))

            if (lista.size < MINIMO_CORRIDAS_VALIDAS) {
                null
            } else {
                lista
            }
        } catch (e: Exception) {
            registrarFalha("F1CalendarApi.carregarCache", e)
            null
        }
    }

    /**
     * Retorna o JSON de uma corrida específica usando o cache local.
     * Assim a tela de detalhes pode funcionar sem conexão.
     */
    fun corridaJsonDoCache(context: Context, circuitId: String): String? {
        if (circuitId.isBlank()) return null

        return try {
            val arquivo = File(
                context.applicationContext.filesDir,
                ARQUIVO_CACHE
            )
            if (!arquivo.isFile) return null

            val corridas = JSONObject(arquivo.readText(Charsets.UTF_8))
                .getJSONObject("MRData")
                .getJSONObject("RaceTable")
                .getJSONArray("Races")

            for (i in 0 until corridas.length()) {
                val corrida = corridas.getJSONObject(i)
                val id = corrida.optJSONObject("Circuit")
                    ?.optString("circuitId", "") ?: ""
                if (id.equals(circuitId, ignoreCase = true)) {
                    return corrida.toString()
                }
            }
            null
        } catch (e: Exception) {
            registrarFalha("F1CalendarApi.corridaJsonDoCache", e)
            null
        }
    }

    fun atualizar(
        context: Context,
        aoTerminar: (() -> Unit)? = null
    ) {
        val app = context.applicationContext

        Thread {
            var gravou = false
            var conexao: HttpURLConnection? = null

            try {
                // Prioriza o endpoint detalhado, que inclui as sessões e seus horários.
                val ano = java.time.LocalDate.now().year
                val normalizado = try {
                    val detalhado = baixarTexto(
                        "https://api.jolpi.ca/f1/alpha/schedules/$ano/"
                    )
                    normalizarProgramacaoDetalhada(detalhado)
                } catch (e: Exception) {
                    registrarFalha("F1CalendarApi.atualizar", e)
                    null
                }
                if (normalizado != null &&
                    interpretar(normalizado).size >= MINIMO_CORRIDAS_VALIDAS
                ) {
                    gravou = gravarCacheSeguro(app.filesDir, normalizado)
                } else {
                    // Compatibilidade: se o endpoint detalhado falhar, mantém o calendário básico.
                    val basico = baixarTexto(URL_API)
                    if (
                        interpretar(basico).size >= MINIMO_CORRIDAS_VALIDAS &&
                        !cacheDaTemporadaTemSessoes(app, ano)
                    ) {
                        // Não substitui um cache detalhado da mesma temporada por
                        // um calendário básico, que apagaria treinos e classificação offline.
                        gravou = gravarCacheSeguro(app.filesDir, basico)
                    }
                }
            } catch (e: Exception) {
                registrarFalha("F1CalendarApi.atualizar", e)
                // Sem internet ou resposta inválida: preserva o cache anterior.
            }

            if (gravou && aoTerminar != null) {
                Handler(Looper.getMainLooper()).post {
                    try {
                        aoTerminar()
                    } catch (e: Exception) {
                        registrarFalha("F1CalendarApi.atualizar", e)
                        // A atualização do cache já foi concluída.
                    }
                }
            }
        }.start()
    }

    /**
     * Detecta se já existe cache da temporada atual com programação de sessões.
     * O endpoint básico não deve apagar esses horários se o endpoint detalhado falhar.
     */
    private fun cacheDaTemporadaTemSessoes(context: Context, ano: Int): Boolean {
        return try {
            val arquivo = File(context.applicationContext.filesDir, ARQUIVO_CACHE)
            if (!arquivo.isFile) return false

            val tabela = JSONObject(arquivo.readText(Charsets.UTF_8))
                .getJSONObject("MRData")
                .getJSONObject("RaceTable")

            if (tabela.optString("season", "") != ano.toString()) return false

            val corridas = tabela.getJSONArray("Races")
            for (i in 0 until corridas.length()) {
                val corrida = corridas.getJSONObject(i)
                for (sessao in SESSOES) {
                    if (corrida.optJSONObject(sessao) != null) return true
                }
            }
            false
        } catch (e: Exception) {
            registrarFalha("F1CalendarApi.cacheDaTemporadaTemSessoes", e)
            false
        }
    }

    private fun baixarTexto(endereco: String): String {
        var conexao: HttpURLConnection? = null
        try {
            conexao = URL(endereco).openConnection() as HttpURLConnection
            conexao.connectTimeout = 10000
            conexao.readTimeout = 10000
            conexao.requestMethod = "GET"
            conexao.setRequestProperty("User-Agent", "CalendarioMotorsport/1.0")
            conexao.setRequestProperty("Accept", "application/json")
            if (conexao.responseCode != HttpURLConnection.HTTP_OK) {
                throw java.io.IOException("HTTP ${conexao.responseCode}")
            }
            return conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conexao?.disconnect()
        }
    }

    /**
     * Converte o endpoint alpha (data.events[].schedule[]) para o formato
     * Ergast já consumido pelas telas de calendário e detalhes.
     */
    private fun normalizarProgramacaoDetalhada(texto: String): String? {
        return try {
            val raiz = JSONObject(texto).getJSONObject("data")
            val eventos = raiz.getJSONArray("events")
            val corridas = JSONArray()

            for (i in 0 until eventos.length()) {
                val evento = eventos.getJSONObject(i)
                val round = evento.optJSONObject("round") ?: continue
                val circuito = evento.optJSONObject("circuit") ?: continue
                val agenda = evento.optJSONArray("schedule") ?: JSONArray()
                val sessões = JSONObject()

                var dataCorrida = ""
                var horaCorrida = ""
                for (j in 0 until agenda.length()) {
                    val sessao = agenda.optJSONObject(j) ?: continue
                    val codigo = sessao.optString("code", "").uppercase()
                    val timestamp = sessao.optString("timestamp", "")
                    if (timestamp.isBlank() || timestamp == "null") continue
                    val instante = try {
                        java.time.OffsetDateTime.parse(timestamp)
                            .withOffsetSameInstant(java.time.ZoneOffset.UTC)
                    } catch (e: Exception) {
                        registrarDescarte("F1CalendarApi.normalizarProgramacaoDetalhada", e)
                        try {
                            java.time.Instant.parse(timestamp).atOffset(java.time.ZoneOffset.UTC)
                        } catch (e: Exception) {
                            registrarDescarte("F1CalendarApi.normalizarProgramacaoDetalhada", e)
                            continue
                        }
                    }
                    val data = instante.toLocalDate().toString()
                    val hora = instante.toLocalTime().toString().take(8) + "Z"
                    val campo = when (codigo) {
                        "FP1" -> "FirstPractice"
                        "FP2" -> "SecondPractice"
                        "FP3" -> "ThirdPractice"
                        "Q" -> "Qualifying"
                        "SQ" -> "SprintQualifying"
                        "SR", "S" -> "Sprint"
                        "R" -> {
                            dataCorrida = data
                            horaCorrida = hora
                            null
                        }
                        else -> null
                    }
                    if (campo != null) {
                        sessões.put(campo, JSONObject().put("date", data).put("time", hora))
                    }
                }
                if (dataCorrida.isBlank()) {
                    val primeira = agenda.optJSONObject(0)
                    val timestamp = primeira?.optString("timestamp", "") ?: ""
                    if (timestamp.isNotBlank() && timestamp != "null") {
                        dataCorrida = try {
                            java.time.OffsetDateTime.parse(timestamp)
                                .withOffsetSameInstant(java.time.ZoneOffset.UTC)
                                .toLocalDate().toString()
                        } catch (e: Exception) {
                            registrarDescarte("F1CalendarApi.normalizarProgramacaoDetalhada", e)
                            try { java.time.Instant.parse(timestamp).toString().substring(0, 10) }
                            catch (e: Exception) { registrarDescarte("F1CalendarApi.normalizarProgramacaoDetalhada", e); "" }
                        }
                    }
                }
                if (dataCorrida.isBlank()) continue
                val local = JSONObject()
                    .put("locality", circuito.optString("locality", ""))
                    .put("country", circuito.optString("country", ""))
                val circuitoErgast = JSONObject()
                    .put("circuitId", circuito.optString("id", "round_${i + 1}"))
                    .put("circuitName", circuito.optString("name", "Circuito"))
                    .put("Location", local)
                val corrida = JSONObject()
                    .put("season", raiz.optInt("year", java.time.LocalDate.now().year).toString())
                    .put("round", round.optInt("number", i + 1).toString())
                    .put("raceName", round.optString("name", "Grande Prêmio"))
                    .put("Circuit", circuitoErgast)
                    .put("date", dataCorrida)
                if (horaCorrida.isNotBlank()) corrida.put("time", horaCorrida)
                val nomes = sessões.keys()
                while (nomes.hasNext()) {
                    val nome = nomes.next()
                    corrida.put(nome, sessões.getJSONObject(nome))
                }
                corridas.put(corrida)
            }
            if (corridas.length() < MINIMO_CORRIDAS_VALIDAS) return null
            JSONObject().put("MRData", JSONObject()
                .put("series", "f1")
                .put("RaceTable", JSONObject()
                    .put("season", raiz.optInt("year", java.time.LocalDate.now().year).toString())
                    .put("Races", corridas)))
                .toString()
        } catch (e: Exception) {
            registrarFalha("F1CalendarApi.normalizarProgramacaoDetalhada", e)
            null
        }
    }

    /**
     * Salva o JSON completo do calendário recebido pela tela, preservando
     * os campos de todas as sessões para consulta offline.
     */
    fun salvarCalendarioCompleto(context: Context, json: String): Boolean {
        return try {
            val corridas = JSONObject(json)
                .getJSONObject("MRData")
                .getJSONObject("RaceTable")
                .getJSONArray("Races")
            if (corridas.length() < MINIMO_CORRIDAS_VALIDAS) {
                return false
            }
            gravarCacheSeguro(context.applicationContext.filesDir, json)
        } catch (e: Exception) {
            registrarFalha("F1CalendarApi.salvarCalendarioCompleto", e)
            false
        }
    }

    /**
     * Escreve primeiro em um arquivo temporário no mesmo diretório.
     * Só substitui o cache quando a gravação temporária está completa.
     * Se a substituição falhar, preserva o cache anterior.
     */
    /**
     * Grava o cache com proteção contra respostas parciais da mesma temporada.
     * Também evita que o calendário básico apague sessões detalhadas já salvas.
     */
    internal fun gravarCacheSeguro(
        diretorio: File,
        conteudo: String
    ): Boolean = synchronized(cacheLock) {
        val destino = File(diretorio, ARQUIVO_CACHE)
        val temporario = File(diretorio, ARQUIVO_TEMPORARIO)

        try {
            if (!diretorio.exists() && !diretorio.mkdirs()) {
                return@synchronized false
            }
            if (!diretorio.isDirectory) {
                return@synchronized false
            }

            // Valida antes de criar ou substituir qualquer arquivo.
            if (interpretar(conteudo).size < MINIMO_CORRIDAS_VALIDAS) {
                return@synchronized false
            }
            val conteudoFinal = mesclarCamposConhecidos(destino, conteudo)
            if (interpretar(conteudoFinal).isEmpty()) {
                temporario.delete()
                return@synchronized false
            }

            FileOutputStream(temporario, false).use { saida ->
                saida.write(conteudoFinal.toByteArray(Charsets.UTF_8))
                saida.flush()
                saida.fd.sync()
            }

            // Confere novamente o arquivo escrito antes da troca.
            val textoGravado = temporario.readText(Charsets.UTF_8)
            if (interpretar(textoGravado).size < MINIMO_CORRIDAS_VALIDAS) {
                temporario.delete()
                return@synchronized false
            }

            // O arquivo temporário fica no mesmo diretório; se a troca falhar,
            // o cache antigo permanece disponível.
            val substituiu = temporario.renameTo(destino)
            if (!substituiu) temporario.delete()
            substituiu
        } catch (e: Exception) {
            registrarFalha("F1CalendarApi.gravarCacheSeguro", e)
            try {
                temporario.delete()
            } catch (e: Exception) {
                registrarFalha("F1CalendarApi.gravarCacheSeguro", e)
                // A limpeza não deve afetar o aplicativo.
            }
            false
        }
    }

    /**
     * Não reduz o calendário nem remove sessões já presentes quando a temporada
     * é a mesma. Temporadas diferentes podem ter legitimamente outra quantidade.
     */
    /**
     * A fonte oficial controla a lista de corridas e sessões. Mesclamos somente
     * campos vazios de uma corrida/sessão que continua existindo, para não perder
     * um horário conhecido quando a fonte ainda não o publicou. Corridas e sessões
     * removidas não são recriadas; valores oficiais não vazios sempre prevalecem.
     */
    private fun mesclarCamposConhecidos(destino: File, novoTexto: String): String {
        if (!destino.isFile) return novoTexto

        return try {
            val raizNova = JSONObject(novoTexto)
            val tabelaNova = raizNova.getJSONObject("MRData").getJSONObject("RaceTable")
            val tabelaAntiga = JSONObject(destino.readText(Charsets.UTF_8))
                .getJSONObject("MRData").getJSONObject("RaceTable")
            val corridasNovas = tabelaNova.getJSONArray("Races")
            val corridasAntigas = tabelaAntiga.getJSONArray("Races")

            if (tabelaNova.optString("season", "") != tabelaAntiga.optString("season", "")) {
                return novoTexto
            }

            fun conhecido(valor: String): Boolean {
                val v = valor.trim()
                return v.isNotEmpty() &&
                    !v.equals("null", ignoreCase = true) &&
                    !v.equals("A confirmar", ignoreCase = true) &&
                    !v.equals("TBD", ignoreCase = true) &&
                    !v.equals("TBA", ignoreCase = true)
            }

            val antigasPorCircuito = mutableMapOf<String, JSONObject>()
            for (i in 0 until corridasAntigas.length()) {
                val corrida = corridasAntigas.optJSONObject(i) ?: continue
                val id = corrida.optJSONObject("Circuit")
                    ?.optString("circuitId", "")?.trim().orEmpty()
                if (id.isNotBlank()) antigasPorCircuito[id] = corrida
            }

            for (i in 0 until corridasNovas.length()) {
                val nova = corridasNovas.optJSONObject(i) ?: continue
                val id = nova.optJSONObject("Circuit")
                    ?.optString("circuitId", "")?.trim().orEmpty()
                val antiga = antigasPorCircuito[id] ?: continue

                for (campo in listOf("date", "time")) {
                    if (!conhecido(nova.optString(campo, "")) &&
                        conhecido(antiga.optString(campo, ""))) {
                        nova.put(campo, antiga.optString(campo))
                    }
                }

                for (nomeSessao in SESSOES) {
                    val sessaoNova = nova.optJSONObject(nomeSessao) ?: continue
                    val sessaoAntiga = antiga.optJSONObject(nomeSessao) ?: continue
                    for (campo in listOf("date", "time")) {
                        if (!conhecido(sessaoNova.optString(campo, "")) &&
                            conhecido(sessaoAntiga.optString(campo, ""))) {
                            sessaoNova.put(campo, sessaoAntiga.optString(campo))
                        }
                    }
                }
            }
            raizNova.toString()
        } catch (e: Exception) {
            registrarFalha("F1CalendarApi.conhecido", e)
            // Se não for possível ler o cache antigo, não bloqueia a fonte nova.
            novoTexto
        }
    }

    private fun interpretar(json: String): List<MainActivity.Etapa> {
        return try {
            val corridas = JSONObject(json)
                .getJSONObject("MRData")
                .getJSONObject("RaceTable")
                .getJSONArray("Races")

            val lista = mutableListOf<MainActivity.Etapa>()

            for (i in 0 until corridas.length()) {
                val corrida = corridas.getJSONObject(i)
                val circuito = corrida.getJSONObject("Circuit")
                val circuitId = circuito.getString("circuitId")
                val local = circuito.optJSONObject("Location")
                val localidade = local?.optString("locality", "") ?: ""
                val paisIngles = local?.optString("country", "") ?: ""

                val fim = try {
                    LocalDate.parse(corrida.getString("date"))
                } catch (e: Exception) {
                    registrarDescarte("F1CalendarApi.interpretar", e)
                    continue
                }

                var inicio = fim.minusDays(2)
                var menor: LocalDate? = null

                for (chave in SESSOES) {
                    val texto = corrida.optJSONObject(chave)
                        ?.optString("date", "") ?: ""

                    if (texto.isBlank()) continue

                    val data = try {
                        LocalDate.parse(texto)
                    } catch (e: Exception) {
                        registrarDescarte("F1CalendarApi.interpretar", e)
                        continue
                    }

                    if (menor == null || data.isBefore(menor)) {
                        menor = data
                    }
                }

                if (
                    menor != null &&
                    !menor.isAfter(fim) &&
                    !menor.isBefore(fim.minusDays(4))
                ) {
                    inicio = menor
                }

                val conhecido = CONHECIDOS[circuitId]
                val pais = conhecido?.first
                    ?: PAISES[paisIngles]
                    ?: paisIngles
                val cidade = conhecido?.second ?: localidade

                lista.add(
                    MainActivity.Etapa(
                        categoria = "F1",
                        pais = pais,
                        circuito = cidade,
                        data = formatarPeriodo(inicio, fim),
                        dataInicio = inicio.toString(),
                        circuitId = circuitId,
                        dataFim = fim.toString()
                    )
                )
            }

            lista.sortedBy { it.dataInicio }
        } catch (e: Exception) {
            registrarDescarte("F1CalendarApi.interpretar", e)
            emptyList()
        }
    }

    fun formatarPeriodo(
        inicio: LocalDate,
        fim: LocalDate
    ): String {
        fun dia(d: LocalDate) =
            d.dayOfMonth.toString().padStart(2, '0')

        fun mes(d: LocalDate) =
            MESES[d.monthValue - 1]

        return when {
            inicio == fim ->
                "${dia(fim)} ${mes(fim)}"

            inicio.monthValue == fim.monthValue ->
                "${dia(inicio)}–${dia(fim)} ${mes(fim)}"

            else ->
                "${dia(inicio)} ${mes(inicio)}–${dia(fim)} ${mes(fim)}"
        }
    }
}