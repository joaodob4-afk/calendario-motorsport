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

    private const val ARQUIVO_CACHE = "indycar_calendar_cache.json"

    private val atualizacaoEmAndamento = AtomicBoolean(false)

    private val formatoData: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT)

    /**
     * Prioridade: cache válido > JSON embutido no APK.
     * Se nenhum deles for válido, devolve lista vazia.
     */
    fun carregar(context: Context): List<IndyCarEvent> {
        val json = lerCache(context) ?: lerAsset(context)
            ?: return emptyList()

        return try {
            interpretar(json).sortedBy { it.etapa }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Baixa o calendário em segundo plano.
     *
     * Só grava o JSON depois de validar todos os eventos.
     * Usa AtomicFile para proteger o cache anterior.
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
            var arquivoAtomic: AtomicFile? = null
            var saida: FileOutputStream? = null

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

                    // Se qualquer dado obrigatório for inválido,
                    // interpretar lança uma exceção e o cache não muda.
                    interpretar(texto)

                    val destino = File(app.filesDir, ARQUIVO_CACHE)
                    arquivoAtomic = AtomicFile(destino)

                    saida = arquivoAtomic.startWrite()
                    saida.write(texto.toByteArray(Charsets.UTF_8))
                    saida.flush()
                    arquivoAtomic.finishWrite(saida)
                    saida = null
                    gravou = true
                }
            } catch (_: Exception) {
                // Tenta restaurar o arquivo anterior se a gravação falhar.
                try {
                    if (saida != null) {
                        arquivoAtomic?.failWrite(saida)
                        saida = null
                    }
                } catch (_: Exception) {
                    // Mantém a falha isolada para não encerrar o aplicativo.
                }
            } finally {
                try {
                    conexao?.disconnect()
                } catch (_: Exception) {
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
            } catch (_: Exception) {
                // Ignora apenas a etapa que não puder ser convertida.
            }
        }

        return lista.sortedBy { it.dataInicio }
    }

    private fun lerCache(context: Context): String? {
        return try {
            val arquivo = File(context.filesDir, ARQUIVO_CACHE)

            if (!arquivo.exists()) return null

            val texto = AtomicFile(arquivo).openRead()
                .bufferedReader(Charsets.UTF_8)
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
                .open("indycar_calendar.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            interpretar(texto)
            texto
        } catch (_: Exception) {
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

        require(etapas.length() > 0) {
            "O calendário não contém etapas."
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