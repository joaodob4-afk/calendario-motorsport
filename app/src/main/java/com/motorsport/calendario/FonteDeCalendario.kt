package com.motorsport.calendario

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Regra única de carregamento e atualização do calendário de uma categoria:
 *
 *  1. o JSON embutido no APK sempre existe e funciona offline;
 *  2. o cache baixado vale se for da mesma temporada do APK ou de uma mais nova;
 *  3. com internet, o JSON do GitHub é baixado e só vai para o cache se for válido.
 *
 * Um JSON é válido quando [interpretar] devolve pelo menos uma etapa, sem lançar exceção.
 * Cada categoria só informa de onde vêm os dados e como interpretá-los.
 */
internal class FonteDeCalendario<T>(
    private val nome: String,
    private val urlJson: String,
    private val arquivoAsset: String,
    private val arquivoCache: String,
    private val validarIndiceConfirmado: (String) -> Boolean = { true },
    private val interpretar: (String) -> List<T>
) {
    private val trava = Any()

    // O mesmo texto é interpretado várias vezes (cache, asset e o escolhido, a cada tela que
    // abre). Guardamos o resultado dos últimos textos para não repetir o trabalho; um texto
    // diferente sempre é interpretado de novo.
    private val memoria = object : LinkedHashMap<String, List<T>?>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<T>?>?): Boolean =
            size > 6
    }

    /** Eventos do texto, ou null se o texto for inválido ou não tiver etapas. */
    private fun eventosValidos(texto: String): List<T>? {
        synchronized(memoria) {
            if (memoria.containsKey(texto)) return memoria[texto]
        }

        val resultado = try {
            interpretar(texto).takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            registrarDescarte("$nome.validar", e)
            null
        }

        synchronized(memoria) { memoria[texto] = resultado }
        return resultado
    }

    /** Cache baixado ou JSON do APK, o que for mais recente. Lista vazia se nenhum for válido. */
    fun carregar(context: Context): List<T> {
        val json = CalendarioJsonTemporada.selecionar(
            lerCache(context),
            lerAsset(context),
            cacheAnteriorAoApk(context)
        ) ?: return emptyList()
        return eventosValidos(json).orEmpty()
    }

    /**
     * Baixa o calendário em segundo plano. Resposta inválida, sem internet ou erro de
     * gravação nunca alteram o cache anterior. [aoTerminar] roda na tela só se gravou.
     */
    fun atualizar(context: Context, aoTerminar: (() -> Unit)? = null) {
        val app = context.applicationContext

        Thread {
            var gravou = false
            var conexao: HttpURLConnection? = null

            try {
                conexao = URL(urlJson).openConnection() as HttpURLConnection
                conexao.connectTimeout = 10000
                conexao.readTimeout = 10000
                conexao.requestMethod = "GET"
                conexao.setRequestProperty("Cache-Control", "no-cache")
                conexao.setRequestProperty("Accept", "application/json")

                if (conexao.responseCode == HttpURLConnection.HTTP_OK) {
                    val texto = conexao.inputStream
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }

                    gravou = gravarCacheBaixado(app, texto)
                }
            } catch (e: Exception) {
                registrarFalha("$nome.atualizar", e)
                // Sem internet ou resposta inválida: o cache anterior continua valendo.
            } finally {
                try {
                    conexao?.disconnect()
                } catch (e: Exception) {
                    registrarFalha("$nome.atualizar", e)
                }
            }

            if (gravou && aoTerminar != null) {
                Handler(Looper.getMainLooper()).post {
                    try {
                        aoTerminar()
                    } catch (e: Exception) {
                        registrarFalha("$nome.atualizar", e)
                    }
                }
            }
        }.start()
    }

    /**
     * Valida e grava no cache. Devolve false, sem tocar no cache, se o texto for
     * inválido ou se a gravação falhar.
     */
    fun gravarCache(context: Context, texto: String): Boolean =
        gravarCacheInterno(context, texto, exigirIndiceConfirmado = false)

    /** Só aceita conteúdo remoto produzido após a confirmação do índice oficial. */
    internal fun gravarCacheBaixado(context: Context, texto: String): Boolean =
        gravarCacheInterno(context, texto, exigirIndiceConfirmado = true)

    private fun gravarCacheInterno(
        context: Context,
        texto: String,
        exigirIndiceConfirmado: Boolean
    ): Boolean {
        if (eventosValidos(texto) == null) return false

        val indiceConfirmado = !exigirIndiceConfirmado || try {
            validarIndiceConfirmado(texto)
        } catch (e: Exception) {
            registrarDescarte("$nome.indiceNaoConfirmado", e)
            false
        }
        if (!indiceConfirmado) return false

        return try {
            gravarAtomicamente(AtomicFile(File(context.filesDir, arquivoCache)), texto)
            true
        } catch (e: Exception) {
            registrarFalha("$nome.gravarCache", e)
            false
        }
    }

    /**
     * Grava de forma atômica: se algo falhar no meio, o AtomicFile preserva a versão
     * anterior. Relança a exceção. O parâmetro [escrever] existe para os testes
     * simularem uma falha de escrita.
     */
    internal fun gravarAtomicamente(
        atomic: AtomicFile,
        texto: String,
        escrever: (FileOutputStream, ByteArray) -> Unit = { saida, bytes -> saida.write(bytes) }
    ) {
        synchronized(trava) {
            var saida: FileOutputStream? = null

            try {
                val temporario = atomic.startWrite()
                saida = temporario

                escrever(temporario, texto.toByteArray(Charsets.UTF_8))
                temporario.flush()
                temporario.fd.sync()

                atomic.finishWrite(temporario)
                saida = null
            } catch (e: Exception) {
                val pendente = saida
                if (pendente != null) {
                    try {
                        atomic.failWrite(pendente)
                    } catch (falha: Exception) {
                        registrarFalha("$nome.gravarAtomicamente", falha)
                    }
                }
                throw e
            }
        }
    }

    /**
     * O cache foi gravado antes da última instalação/atualização do APK? Então os dados embutidos
     * no APK novo são mais recentes e valem até o próximo download. Na dúvida, mantém o cache.
     */
    private fun cacheAnteriorAoApk(context: Context): Boolean =
        try {
            val arquivo = File(context.filesDir, arquivoCache)
            arquivo.isFile && arquivo.lastModified() < InstalacaoDoApp.ultimaAtualizacao(context)
        } catch (e: Exception) {
            registrarDescarte("$nome.cacheAnteriorAoApk", e)
            false
        }

    private fun lerCache(context: Context): String? {
        val arquivo = File(context.filesDir, arquivoCache)
        if (!arquivo.exists()) return null

        return synchronized(trava) {
            try {
                // O AtomicFile recupera o backup se a última gravação foi interrompida.
                val texto = AtomicFile(arquivo).openRead()
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

                texto.takeIf { eventosValidos(it) != null }
            } catch (e: Exception) {
                registrarFalha("$nome.lerCache", e)
                null
            }
        }
    }

    private fun lerAsset(context: Context): String? =
        try {
            context.assets.open(arquivoAsset)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
                .takeIf { eventosValidos(it) != null }
        } catch (e: Exception) {
            registrarFalha("$nome.lerAsset", e)
            null
        }
}

/** Momento da última instalação/atualização do APK (não muda enquanto o processo vive). */
private object InstalacaoDoApp {
    @Volatile
    private var momento: Long? = null

    fun ultimaAtualizacao(context: Context): Long =
        momento ?: context.packageManager
            .getPackageInfo(context.packageName, 0)
            .lastUpdateTime
            .also { momento = it }
}
