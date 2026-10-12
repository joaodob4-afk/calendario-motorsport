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
import java.nio.charset.StandardCharsets
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

    private val cacheLock = Any()

    // Os mínimos fixos se aplicam apenas à temporada 2026/27.
    private data class LimitesTemporada(val etapas: Int, val rodadas: Int)

    private fun limitesDaTemporada(temporada: String): LimitesTemporada? =
        when (temporada.trim()) {
            "2026/27" -> LimitesTemporada(etapas = 13, rodadas = 21)
            else -> null
        }

    private val regexNumeroRodada = Regex("""(?i)\bRodada\s+(\d+)\b""")

    private fun numeroDaRodada(nome: String): Int? {
        if (!nome.contains("corrida", ignoreCase = true)) return null
        return regexNumeroRodada.find(nome)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private val formatoData: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT)

    /**
     * Mantém a prioridade original:
     * cache válido > JSON embutido no APK > lista vazia.
     */
    fun carregar(context: Context): List<FormulaEEvent> {
        val json = lerCache(context) ?: lerAsset(context)
            ?: return emptyList()

        return try {
            interpretar(json).sortedBy { it.etapa }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Baixa e valida o calendário em segundo plano.
     * O cache anterior só é substituído depois de validar o novo JSON
     * e concluir uma gravação segura.
     */
    fun atualizar(context: Context, aoTerminar: (() -> Unit)? = null) {
        val app = context.applicationContext

        Thread {
            var gravou = false
            var conexao: HttpURLConnection? = null

            try {
                conexao = URL(URL_JSON).openConnection() as HttpURLConnection
                conexao.connectTimeout = 10000
                conexao.readTimeout = 10000
                conexao.requestMethod = "GET"
                conexao.setRequestProperty("Cache-Control", "no-cache")
                conexao.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                if (conexao.responseCode == HttpURLConnection.HTTP_OK) {
                    val texto = conexao.inputStream
                        .bufferedReader(StandardCharsets.UTF_8)
                        .use { it.readText() }

                    // Valida o calendário e preserva cache mais completo da mesma temporada.
                    gravou = gravarCacheSeValido(app, texto)
                }
            } catch (_: Exception) {
                // Sem internet, JSON inválido ou falha de gravação:
                // mantém o cache anterior e o JSON do APK.
            } finally {
                conexao?.disconnect()
            }

            if (gravou && aoTerminar != null) {
                Handler(Looper.getMainLooper()).post {
                    aoTerminar()
                }
            }
        }.start()
    }

    /**
     * Valida o calendário antes de gravar e impede que uma resposta parcial
     * da mesma temporada substitua um cache com mais etapas.
     * Uma temporada diferente pode ter legitimamente outra quantidade de etapas.
     */
    internal fun gravarCacheSeValido(
        context: Context,
        texto: String
    ): Boolean {
        return try {
            interpretar(texto)
            val novaRaiz = JSONObject(texto)
            val novaTemporada = novaRaiz.optString("temporada").trim()
            val novasEtapas = novaRaiz.getJSONArray("etapas").length()

            synchronized(cacheLock) {
                val arquivo = File(context.filesDir, ARQUIVO_CACHE)
                val atomico = AtomicFile(arquivo)
                val cacheAtual = try {
                    atomico.openRead().bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                } catch (_: Exception) {
                    null
                }

                if (cacheAtual != null) {
                    val eventosAtuais = try {
                        interpretar(cacheAtual)
                    } catch (_: Exception) {
                        null
                    }
                    if (eventosAtuais != null) {
                        val raizAtual = JSONObject(cacheAtual)
                        val temporadaAtual = raizAtual.optString("temporada").trim()
                        val etapasAtuais = raizAtual.getJSONArray("etapas").length()
                        if (novaTemporada == temporadaAtual && novasEtapas < etapasAtuais) {
                            return false
                        }
                    }
                }

                gravarCacheSeguro(context, texto)
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Grava usando AtomicFile para evitar deixar o cache definitivo
     * parcialmente escrito se ocorrer uma falha durante a operação.
     */
    private fun gravarCacheSeguro(
        context: Context,
        texto: String
    ): Boolean {
        val arquivo = File(context.filesDir, ARQUIVO_CACHE)
        val atomico = AtomicFile(arquivo)
        var saida: FileOutputStream? = null

        return try {
            saida = atomico.startWrite()

            saida.write(texto.toByteArray(StandardCharsets.UTF_8))
            saida.flush()
            saida.fd.sync()

            atomico.finishWrite(saida)
            saida = null

            true
        } catch (_: Exception) {
            if (saida != null) {
                try {
                    atomico.failWrite(saida)
                } catch (_: Exception) {
                    // O AtomicFile tenta recuperar a versão anterior.
                }
            }
            false
        }
    }

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
            } catch (_: Exception) {
                // Ignora somente a etapa que não puder ser convertida.
            }
        }

        return lista.sortedBy { it.dataInicio }
    }

    private fun lerCache(context: Context): String? {
        return try {
            val arquivo = File(context.filesDir, ARQUIVO_CACHE)
            if (!arquivo.exists()) return null
            
            val atomico = AtomicFile(arquivo)
            val texto = atomico.openRead()
                .bufferedReader(StandardCharsets.UTF_8)
                .use { it.readText() }

            interpretar(texto)
            texto
        } catch (_: Exception) {
            null
        }
    }

    private fun lerAsset(context: Context): String? {
        return try {
            val texto = context.assets
                .open("fe_calendar.json")
                .bufferedReader(StandardCharsets.UTF_8)
                .use { it.readText() }

            interpretar(texto)
            texto
        } catch (_: Exception) {
            null
        }
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

        val temporada = raiz.optString("temporada").trim()
        val limites = limitesDaTemporada(temporada)
        val etapas = raiz.getJSONArray("etapas")

        require(etapas.length() > 0) { "Calendário sem etapas" }
        if (limites != null) {
            require(etapas.length() >= limites.etapas) {
                "Calendário incompleto para a temporada $temporada"
            }
        }

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

        if (limites != null) {
            require(numerosRodadas.size >= limites.rodadas) {
                "Calendário incompleto para a temporada $temporada: " +
                    "menos de ${limites.rodadas} rodadas identificadas"
            }
        }

        return eventos
    }
}