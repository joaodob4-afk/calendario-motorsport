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

    /**
     * Prioridade: cache válido > JSON embutido no APK.
     * Sem nenhum dos dois, devolve lista vazia.
     */
    fun carregar(context: Context): List<IndyCarEvent> {
        val json = lerCache(context) ?: lerAsset(context) ?: return emptyList()

        return try {
            interpretar(json).sortedBy { it.etapa }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Baixa o calendário em segundo plano.
     *
     * Proteções:
     * - Impede atualizações simultâneas.
     * - Valida o JSON antes de gravar.
     * - Usa AtomicFile para preservar o cache anterior
     *   se a gravação for interrompida.
     * - Mantém o cache existente em caso de falha.
     */
    fun atualizar(context: Context, aoTerminar: (() -> Unit)? = null) {
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
                conexao = URL(URL_JSON).openConnection() as HttpURLConnection
                conexao.connectTimeout = 10000
                conexao.readTimeout = 10000
                conexao.setRequestProperty("Cache-Control", "no-cache")

                if (conexao.responseCode == HttpURLConnection.HTTP_OK) {
                    val texto = conexao.inputStream
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }

                    val eventos = interpretar(texto)

                    if (eventos.isNotEmpty()) {
                        val destino = File(app.filesDir, ARQUIVO_CACHE)
                        arquivoAtomic = AtomicFile(destino)

                        saida = arquivoAtomic.startWrite()

                        saida.write(texto.toByteArray(Charsets.UTF_8))
                        saida.flush()

                        arquivoAtomic.finishWrite(saida)
                        saida = null

                        gravou = true
                    }
                }
            } catch (_: Exception) {
                // Se a gravação começou, tenta preservar o cache anterior.
                try {
                    if (saida != null) {
                        arquivoAtomic?.failWrite(saida)
                        saida = null
                    }
                } catch (_: Exception) {
                    // Não substitui o erro original nem encerra o app.
                }
            } finally {
                try {
                    conexao?.disconnect()
                } catch (_: Exception) {
                    // A falha ao encerrar a conexão não deve encerrar o app.
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

    /** Converte os eventos para o formato da lista da tela inicial. */
    fun comoEtapas(eventos: List<IndyCarEvent>): List<MainActivity.Etapa> {
        val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val lista = mutableListOf<MainActivity.Etapa>()

        for (evento in eventos) {
            try {
                val inicio = LocalDate.parse(evento.inicio, formato)
                val fim = LocalDate.parse(evento.fim, formato)

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
                // Ignora etapas com datas inválidas.
            }
        }

        return lista.sortedBy { it.dataInicio }
    }

    private fun lerCache(context: Context): String? {
        return try {
            val arquivo = File(context.filesDir, ARQUIVO_CACHE)

            if (!arquivo.exists()) return null

            // Ignora cache anterior à instalação/atualização do aplicativo.
            val instalado = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .lastUpdateTime

            if (arquivo.lastModified() < instalado) return null

            val texto = arquivo.readText(Charsets.UTF_8)

            if (interpretar(texto).isEmpty()) null else texto
        } catch (_: Exception) {
            null
        }
    }

    private fun lerAsset(context: Context): String? {
        return try {
            context.assets
                .open("indycar_calendar.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
                .also { interpretar(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun interpretar(json: String): List<IndyCarEvent> {
        val etapas = JSONObject(json).getJSONArray("etapas")
        val eventos = mutableListOf<IndyCarEvent>()

        for (i in 0 until etapas.length()) {
            val etapa = etapas.getJSONObject(i)
            val sessoesJson = etapa.getJSONArray("sessoes")
            val sessoes = mutableListOf<IndyCarSession>()

            for (j in 0 until sessoesJson.length()) {
                val sessao = sessoesJson.getJSONObject(j)

                sessoes.add(
                    IndyCarSession(
                        nome = sessao.getString("nome"),
                        data = sessao.getString("data"),
                        horario = sessao.optString("horario", "A confirmar")
                    )
                )
            }

            eventos.add(
                IndyCarEvent(
                    etapa = etapa.getInt("etapa"),
                    nome = etapa.optString("nome", etapa.getString("circuito")),
                    circuito = etapa.getString("circuito"),
                    local = etapa.optString("local", ""),
                    pais = etapa.getString("pais"),
                    inicio = etapa.getString("inicio"),
                    fim = etapa.getString("fim"),
                    sessoes = sessoes
                )
            )
        }

        return eventos
    }
}