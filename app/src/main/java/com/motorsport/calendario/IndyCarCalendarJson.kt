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
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

data class IndyCarSession(
    val nome: String,
    val data: String,
    val horario: String
)

data class IndyCarEvent(
    val etapa: Int,
    val nome: String,
    val circuito: String,
    val local: String,
    val pais: String,
    val inicio: String,
    val fim: String,
    val sessoes: List<IndyCarSession>
)

object IndyCarCalendarJson {

    private const val URL_JSON =
        "https://raw.githubusercontent.com/joaodob4-afk/" +
        "calendario-motorsport/main/app/src/main/assets/indycar_calendar.json"
    private const val MINIMO_ETAPAS_VALIDAS = 1
    private const val ARQUIVO_CACHE = "indycar_calendar_cache.json"

    private val atualizacaoEmAndamento = AtomicBoolean(false)

    // Coordena todas as leituras, gravações e recuperações do cache.
    private val cacheLock = Any()

    private val formatoData: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT)

    /**
     * Prioridade: cache válido > JSON embutido no APK.
     * Se nenhum deles for válido, devolve lista vazia.
     */
    fun carregar(context: Context): List<IndyCarEvent> {
        val json = CalendarioJsonTemporada.selecionar(lerCache(context), lerAsset(context))
            ?: return emptyList()

        return try {
            interpretar(json).sortedBy { it.etapa }
        } catch (e: Exception) {
            registrarFalha("IndyCarCalendarJson.carregar", e)
            emptyList()
        }
    }

    /**
     * Baixa o calendário em segundo plano.
     *
     * Valida todos os eventos antes de gravar.
     * Usa AtomicFile e um bloqueio compartilhado para
     * proteger a leitura e a gravação do cache.
     */
    fun atualizar(
        context: Context,
        aoTerminar: (() -> Unit)? = null
    ) {
        val app = context.applicationContext

        if (!atualizacaoEmAndamento.compareAndSet(false, true)) {
            return
        }

        Thread {
            var gravou = false
            var conexao: HttpURLConnection? = null

            try {
                conexao = URL(URL_JSON)
                    .openConnection() as HttpURLConnection

                conexao.connectTimeout = 10000
                conexao.readTimeout = 10000
                conexao.requestMethod = "GET"
                conexao.setRequestProperty("Cache-Control", "no-cache")

                if (conexao.responseCode == HttpURLConnection.HTTP_OK) {
                    val texto = conexao.inputStream
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }

                    // Valida o JSON e só então substitui o cache.
                    gravarCacheSeValido(app, texto)
                    gravou = true
                }
            } catch (e: Exception) {
                registrarFalha("IndyCarCalendarJson.atualizar", e)
                // Falhas de rede, validação ou gravação não derrubam o app.
            } finally {
                try {
                    conexao?.disconnect()
                } catch (e: Exception) {
                    registrarFalha("IndyCarCalendarJson.atualizar", e)
                    // Ignora falhas ao encerrar a conexão.
                }

                atualizacaoEmAndamento.set(false)
            }

            if (gravou && aoTerminar != null) {
                Handler(Looper.getMainLooper()).post {
                    aoTerminar()
                }
            }
        }.start()
    }

    /**
     * Valida o JSON antes de substituir o cache.
     * Se a validação falhar, o arquivo existente não é alterado.
     * A visibilidade internal permite testar esse comportamento.
     */
    
    internal fun gravarCacheSeValido(
    context: Context,
    texto: String
) {
    // Valida o calendário recebido antes de tocar no cache.
    val novosEventos = interpretar(texto)
    val temporadaNova = JSONObject(texto).optInt("temporada", 0)
    var textoFinal = texto

    synchronized(cacheLock) {
        val destino = File(context.filesDir, ARQUIVO_CACHE)
        val atomic = AtomicFile(destino)

        // Remoções oficiais são permitidas. A identidade do circuito é mais estável
        // que o número da etapa, que pode mudar após renumeração.
        val cacheAtual = try {
            atomic.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            registrarFalha("IndyCarCalendarJson.gravarCacheSeValido", e)
            null
        }
        val eventosAtuais = cacheAtual?.let { atual ->
            try { interpretar(atual) } catch (e: Exception) { registrarFalha("IndyCarCalendarJson.gravarCacheSeValido", e); null }
        }

        if (eventosAtuais != null && cacheAtual != null) {
            val temporadaAtual = JSONObject(cacheAtual).optInt("temporada", 0)
            if (temporadaNova == temporadaAtual) {
                val raizNova = JSONObject(textoFinal)
                val etapasJson = raizNova.getJSONArray("etapas")
                // Um circuito pode receber mais de uma corrida na mesma temporada
                // (por exemplo, Milwaukee). O nome do evento completa a identidade
                // para que um evento não herde os horários do outro.
                val novasPorIdentidade = mutableMapOf<String, JSONObject>()
                for (i in 0 until etapasJson.length()) {
                    val etapaJson = etapasJson.getJSONObject(i)
                    val circuito = etapaJson.optString("circuito")
                        .trim().lowercase(Locale.ROOT)
                    val nome = etapaJson.optString("nome")
                        .trim().lowercase(Locale.ROOT)
                    novasPorIdentidade["$circuito|$nome"] = etapaJson
                }

                for (atual in eventosAtuais) {
                    val circuito = atual.circuito.trim().lowercase(Locale.ROOT)
                    val nome = atual.nome.trim().lowercase(Locale.ROOT)
                    val novaJson = novasPorIdentidade["$circuito|$nome"] ?: continue
                    val sessoesJson = novaJson.optJSONArray("sessoes") ?: continue
                    val novasSessoes = mutableMapOf<String, JSONObject>()
                    for (i in 0 until sessoesJson.length()) {
                        val sessaoJson = sessoesJson.getJSONObject(i)
                        novasSessoes[sessaoJson.optString("nome").trim().lowercase(Locale.ROOT)] = sessaoJson
                    }
                    for (sessaoAtual in atual.sessoes) {
                        val sessaoNova = novasSessoes[sessaoAtual.nome.trim().lowercase(Locale.ROOT)] ?: continue
                        val horarioAtual = sessaoAtual.horario.trim()
                        if (horarioAtual.isNotBlank() &&
                            !horarioAtual.equals("A confirmar", ignoreCase = true) &&
                            sessaoNova.optString("horario").trim().equals("A confirmar", ignoreCase = true)
                        ) {
                            sessaoNova.put("horario", horarioAtual)
                        }
                    }
                }
                textoFinal = raizNova.toString(2) + "\n"
            }
        }

        // Só grava depois de todas as validações.
        gravarAtomicamente(atomic, textoFinal)
    }
}


    /**
     * Grava o cache atomicamente.
     * Permite injetar uma falha de escrita nos testes.
     */
    internal fun gravarAtomicamente(
        atomic: AtomicFile,
        texto: String,
        escrever: (FileOutputStream, ByteArray) -> Unit = { saida, bytes ->
            saida.write(bytes)
        }
    ) {
        synchronized(cacheLock) {
            var saida: FileOutputStream? = null

            try {
                val arquivoTemporario = atomic.startWrite()
                saida = arquivoTemporario

                escrever(
                    arquivoTemporario,
                    texto.toByteArray(Charsets.UTF_8)
                )

                arquivoTemporario.flush()
                atomic.finishWrite(arquivoTemporario)
                saida = null
            } catch (e: Exception) {
                val escritaPendente = saida
                saida = null

                if (escritaPendente != null) {
                    try {
                        atomic.failWrite(escritaPendente)
                    } catch (e: Exception) {
                        registrarFalha("IndyCarCalendarJson.gravarAtomicamente", e)
                        // Evita que uma falha de recuperação derrube o app.
                    }
                }

                throw e
            }
        }
    }

    /** Converte os eventos para o formato usado pela tela inicial. */
    fun comoEtapas(
        eventos: List<IndyCarEvent>
    ): List<MainActivity.Etapa> {
        val lista = mutableListOf<MainActivity.Etapa>()

        for (evento in eventos) {
            try {
                val inicio = LocalDate.parse(evento.inicio, formatoData)
                val fim = LocalDate.parse(evento.fim, formatoData)

                if (fim.isBefore(inicio)) continue

                val pais =
                    if (evento.pais == "Emirados Árabes Unidos") {
                        "Abu Dhabi"
                    } else {
                        evento.pais
                    }

                lista.add(
                    MainActivity.Etapa(
                        categoria = "IndyCar",
                        pais = pais,
                        circuito = evento.circuito,
                        data = F1CalendarApi.formatarPeriodo(inicio, fim),
                        dataInicio = inicio.toString(),
                        dataFim = fim.toString()
                    )
                )
            } catch (e: Exception) {
                registrarDescarte("IndyCarCalendarJson.comoEtapas", e)
                // Ignora apenas a etapa que não puder ser convertida.
            }
        }

        return lista.sortedBy { it.dataInicio }
    }

    /**
     * Lê o cache de forma sincronizada.
     * O AtomicFile tenta recuperar o backup antes de entregar o conteúdo.
     */
    private fun lerCache(context: Context): String? {
        return synchronized(cacheLock) {
            try {
                val arquivo = File(context.filesDir, ARQUIVO_CACHE)
                val atomic = AtomicFile(arquivo)

                val texto = atomic.openRead()
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

                // Rejeita cache vazio, incompleto ou inválido.
                interpretar(texto)

                texto
            } catch (e: Exception) {
                registrarFalha("IndyCarCalendarJson.lerCache", e)
                null
            }
        }
    }

    private fun lerAsset(context: Context): String? {
        return try {
            val texto = context.assets
                .open("indycar_calendar.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            interpretar(texto)
            texto
        } catch (e: Exception) {
            registrarFalha("IndyCarCalendarJson.lerAsset", e)
            null
        }
    }

    /**
     * Interpreta e valida o calendário inteiro.
     * Lança uma exceção se encontrar dados inválidos.
     */
    private fun interpretar(json: String): List<IndyCarEvent> {
        val raiz = JSONObject(json)

        require(raiz.optInt("temporada", 0) > 0) {
            "Temporada ausente ou inválida."
        }

        require(
            raiz.optString("categoria")
                .equals("IndyCar", ignoreCase = true)
        ) {
            "Categoria inválida."
        }

        val etapas = raiz.optJSONArray("etapas")
            ?: throw IllegalArgumentException("Lista de etapas ausente.")

        require(etapas.length() >= MINIMO_ETAPAS_VALIDAS) {
    "Calendário incompleto: apenas ${etapas.length()} etapas."
}
        val eventos = mutableListOf<IndyCarEvent>()
        val numerosEtapas = mutableSetOf<Int>()

        for (i in 0 until etapas.length()) {
            val etapaJson = etapas.getJSONObject(i)

            val numero = etapaJson.getInt("etapa")

            require(numero > 0) {
                "Número de etapa inválido."
            }

            require(numerosEtapas.add(numero)) {
                "Número de etapa duplicado: $numero."
            }

            val circuito = textoObrigatorio(etapaJson, "circuito")
            val nome = textoOpcional(etapaJson, "nome")
                .ifBlank { circuito }
            val local = textoOpcional(etapaJson, "local")
            val pais = textoObrigatorio(etapaJson, "pais")
            val inicioTexto = textoObrigatorio(etapaJson, "inicio")
            val fimTexto = textoObrigatorio(etapaJson, "fim")

            val inicio = analisarData(inicioTexto)
            val fim = analisarData(fimTexto)

            require(!fim.isBefore(inicio)) {
                "Data final anterior à inicial na etapa $numero."
            }

            val sessoesJson = etapaJson.optJSONArray("sessoes")
                ?: throw IllegalArgumentException(
                    "Sessões ausentes na etapa $numero."
                )

            require(sessoesJson.length() > 0) {
                "Etapa $numero sem sessões."
            }

            val sessoes = mutableListOf<IndyCarSession>()

            for (j in 0 until sessoesJson.length()) {
                val sessaoJson = sessoesJson.getJSONObject(j)

                val nomeSessao = textoObrigatorio(
                    sessaoJson,
                    "nome"
                )

                val dataSessaoTexto = textoObrigatorio(
                    sessaoJson,
                    "data"
                )

                val dataSessao = analisarData(dataSessaoTexto)

                require(
                    !dataSessao.isBefore(inicio) &&
                        !dataSessao.isAfter(fim)
                ) {
                    "Data de sessão fora do período da etapa $numero."
                }

                val horario = textoOpcional(
                    sessaoJson,
                    "horario"
                ).ifBlank { "A confirmar" }

                require(horarioValido(horario)) {
                    "Horário inválido na etapa $numero: $horario."
                }

                sessoes.add(
                    IndyCarSession(
                        nome = nomeSessao,
                        data = dataSessaoTexto,
                        horario = horario
                    )
                )
            }

            eventos.add(
                IndyCarEvent(
                    etapa = numero,
                    nome = nome,
                    circuito = circuito,
                    local = local,
                    pais = pais,
                    inicio = inicioTexto,
                    fim = fimTexto,
                    sessoes = sessoes
                )
            )
        }

        return eventos
    }

    private fun textoObrigatorio(
        objeto: JSONObject,
        campo: String
    ): String {
        val valor = objeto.optString(campo, "").trim()

        require(valor.isNotEmpty() && valor != "null") {
            "Campo obrigatório ausente ou vazio: $campo."
        }

        return valor
    }

    private fun textoOpcional(
        objeto: JSONObject,
        campo: String
    ): String {
        if (!objeto.has(campo) || objeto.isNull(campo)) {
            return ""
        }

        return objeto.optString(campo, "").trim()
    }

    private fun analisarData(texto: String): LocalDate {
        return LocalDate.parse(texto, formatoData)
    }

    private fun horarioValido(horario: String): Boolean {
        if (
            horario.equals("A confirmar", ignoreCase = true) ||
            horario.equals("TBA", ignoreCase = true)
        ) {
            return true
        }

        return Regex("""^(?:[01]\d|2[0-3]):[0-5]\d$""")
            .matches(horario)
    }
}