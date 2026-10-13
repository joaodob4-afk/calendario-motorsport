package com.motorsport.calendario

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object F3CalendarJson {

    private const val URL_JSON =
        "https://raw.githubusercontent.com/joaodob4-afk/calendario-motorsport/main/app/src/main/assets/f3_calendar.json"

    private const val ARQUIVO_CACHE = "f3_calendar_cache.json"

    // A validação estrutural de cada etapa protege contra JSON incompleto;
    // não impomos uma quantidade fixa de etapas, que pode mudar oficialmente.
    private const val MINIMO_ETAPAS_VALIDAS = 1

    private val formatoData = DateTimeFormatter
        .ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT)

    fun carregar(context: Context): List<F3Event> {
        val json = lerCache(context) ?: lerAsset(context)
        val eventosJson = json?.let { interpretar(it) }.orEmpty()
        val temporadaJson = json?.let {
            try {
                JSONObject(it).optInt("temporada", 0)
            } catch (_: Exception) {
                0
            }
        } ?: 0
        val temporadaFallback = F3Calendar.eventos.firstOrNull()
            ?.fim?.substringAfterLast('/')?.toIntOrNull() ?: 0

        // Um calendário remoto estruturalmente válido é a fonte oficial desta temporada.
        // Não mescla o fallback por número de etapa: isso poderia ressuscitar etapas
        // removidas ou associar o circuito errado depois de uma renumeração.
        if (eventosJson.isNotEmpty()) {
            return eventosJson.sortedBy { it.etapa }
        }

        return F3Calendar.eventos.sortedBy { it.etapa }
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
                        gravou = gravarCacheSeguro(app, texto)
                    }
                }
            } catch (_: Exception) {
                // Falha de rede ou dados inválidos:
                // mantém o cache anterior e o calendário local.
            } finally {
                try {
                    conexao?.disconnect()
                } catch (_: Exception) {
                    // Não deixa uma falha ao fechar afetar o aplicativo.
                }
            }

            if (gravou && aoTerminar != null) {
                Handler(Looper.getMainLooper()).post {
                    try {
                        aoTerminar()
                    } catch (_: Exception) {
                        // A gravação do cache já foi concluída.
                    }
                }
            }
        }.start()
    }

    /**
     * AtomicFile protege o cache anterior caso a gravação falhe.
     * O conteúdo é validado antes e depois da escrita.
     */
    internal fun gravarCacheSeguro(
        context: Context,
        conteudo: String
    ): Boolean {
        val eventosNovos = interpretar(conteudo)

        if (eventosNovos.size < MINIMO_ETAPAS_VALIDAS) {
            return false
        }

        val arquivo = File(context.filesDir, ARQUIVO_CACHE)
        val cacheAtomico = AtomicFile(arquivo)

        var saida: FileOutputStream? = null

        return try {
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

            // Confere o arquivo definitivo depois da troca.
            val verificado = cacheAtomico.openRead()
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            interpretar(verificado).size >= MINIMO_ETAPAS_VALIDAS
        } catch (_: Exception) {
            try {
                if (saida != null) {
                    cacheAtomico.failWrite(saida)
                }
            } catch (_: Exception) {
                // A falha na recuperação não deve fechar o aplicativo.
            }

            false
        }
    }
    fun comoEtapas(
        eventos: List<F3Event>
    ): List<MainActivity.Etapa> {
        val lista = mutableListOf<MainActivity.Etapa>()

        for (evento in eventos) {
            try {
                val inicio = LocalDate.parse(evento.inicio, formatoData)
                val fim = LocalDate.parse(evento.fim, formatoData)

                if (fim.isBefore(inicio)) continue

                lista.add(
                    MainActivity.Etapa(
                        categoria = "F3",
                        pais = evento.pais,
                        circuito = evento.circuito,
                        data = F1CalendarApi.formatarPeriodo(inicio, fim),
                        dataInicio = inicio.toString(),
                        dataFim = fim.toString()
                    )
                )
            } catch (_: Exception) {
                // Ignora somente etapas com datas inválidas.
            }
        }

        return lista.sortedBy { it.dataInicio }
    }

    private fun lerCache(context: Context): String? {
        return try {
            val arquivo = File(context.filesDir, ARQUIVO_CACHE)

            if (!arquivo.exists()) return null
                
            val texto = AtomicFile(arquivo)
                .openRead()
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
            val texto = context.assets
                .open("f3_calendar.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            if (interpretar(texto).isNotEmpty()) texto else null
        } catch (_: Exception) {
            null
        }
    }

    private fun interpretar(json: String): List<F3Event> {
        return try {
            val raiz = JSONObject(json)

            // Evita aceitar acidentalmente um JSON de outra categoria.
            if (raiz.optString("categoria") != "F3") {
                return emptyList()
            }

            val etapas = raiz.getJSONArray("etapas")

            if (etapas.length() < MINIMO_ETAPAS_VALIDAS) {
                return emptyList()
            }

            val eventos = mutableListOf<F3Event>()
            val numerosEtapas = mutableSetOf<Int>()

            for (i in 0 until etapas.length()) {
                val etapa = etapas.getJSONObject(i)

                val numero = etapa.getInt("etapa")
                val circuito = etapa.getString("circuito").trim()
                val pais = etapa.getString("pais").trim()
                val inicioTexto = etapa.getString("inicio")
                val fimTexto = etapa.getString("fim")

                if (numero <= 0 || !numerosEtapas.add(numero)) {
                    return emptyList()
                }

                if (circuito.isBlank() || pais.isBlank()) {
                    return emptyList()
                }

                val inicio = LocalDate.parse(inicioTexto, formatoData)
                val fim = LocalDate.parse(fimTexto, formatoData)

                if (fim.isBefore(inicio)) {
                    return emptyList()
                }

                val sessoesJson = etapa.getJSONArray("sessoes")

                if (sessoesJson.length() == 0) {
                    return emptyList()
                }

                val sessoes = mutableListOf<F3Session>()

                for (j in 0 until sessoesJson.length()) {
                    val sessao = sessoesJson.getJSONObject(j)

                    val nome = sessao.getString("nome").trim()
                    val data = sessao.getString("data")
                    val horario = sessao.optString(
                        "horario",
                        "A confirmar"
                    ).ifBlank { "A confirmar" }

                    if (nome.isBlank()) {
                        return emptyList()
                    }

                    // Confere o formato da data sem alterar o valor original.
                    LocalDate.parse(data, formatoData)

                    sessoes.add(
                        F3Session(
                            nome = nome,
                            data = data,
                            horario = horario
                        )
                    )
                }

                eventos.add(
                    F3Event(
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