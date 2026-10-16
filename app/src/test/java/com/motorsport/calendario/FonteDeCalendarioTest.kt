package com.motorsport.calendario

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

/**
 * Testa a regra comum de carregamento e cache usada por F2, F3, Fórmula E e IndyCar,
 * com um formato de dados mínimo (só uma lista "etapas").
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FonteDeCalendarioTest {

    private val arquivoCache = "teste_fonte_cache.json"
    private val context: Context get() = RuntimeEnvironment.getApplication()

    /** Válido se tiver a lista "etapas" com pelo menos um item. */
    private fun interpretar(texto: String): List<String> {
        val etapas = JSONObject(texto).getJSONArray("etapas")
        require(etapas.length() > 0) { "calendário sem etapas" }
        return (0 until etapas.length()).map { etapas.get(it).toString() }
    }

    private fun json(temporada: Int, etapas: Int): String =
        """{"temporada": $temporada, "etapas": [${(1..etapas).joinToString(",")}]}"""

    /** [asset] "f2_calendar.json" existe no APK; o padrão não existe (simula APK sem dados). */
    private fun fonte(asset: String = "nao_existe.json") =
        FonteDeCalendario("Teste", "http://localhost/nao-usado", asset, arquivoCache) {
            interpretar(it)
        }

    @Before
    fun limparCache() {
        File(context.filesDir, arquivoCache).delete()
        File(context.filesDir, "$arquivoCache.bak").delete()
    }

    @Test
    fun semCacheEsemAssetDevolveListaVazia() {
        assertTrue(fonte().carregar(context).isEmpty())
    }

    @Test
    fun semCacheUsaOCalendarioEmbutidoNoApk() {
        // f2_calendar.json é o JSON real do APK, que não tem o formato do teste;
        // por isso usamos o interpretador real do F2 para provar que o asset é lido.
        val eventos = F2CalendarJson.carregar(context)
        assertTrue(eventos.isNotEmpty())
    }

    @Test
    fun cacheValidoEGravadoELido() {
        val fonte = fonte()

        assertTrue(fonte.gravarCache(context, json(2026, 3)))
        assertEquals(3, fonte.carregar(context).size)
    }

    @Test
    fun conteudoInvalidoNaoGravaENaoAlteraOCacheAnterior() {
        val fonte = fonte()
        assertTrue(fonte.gravarCache(context, json(2026, 5)))

        assertFalse(fonte.gravarCache(context, "isto não é um json"))
        assertFalse(fonte.gravarCache(context, """{"temporada": 2026, "etapas": []}"""))
        assertFalse(fonte.gravarCache(context, """{"temporada": 2026}"""))

        assertEquals(5, fonte.carregar(context).size)
    }

    @Test
    fun conteudoInvalidoNaoCriaArquivoDeCache() {
        assertFalse(fonte().gravarCache(context, "lixo"))
        assertFalse(File(context.filesDir, arquivoCache).exists())
    }

    @Test
    fun falhaDeEscritaRelancaAExcecaoEPreservaOCacheAnterior() {
        val fonte = fonte()
        val destino = File(context.filesDir, arquivoCache)
        assertTrue(fonte.gravarCache(context, json(2026, 4)))

        assertThrows(IOException::class.java) {
            fonte.gravarAtomicamente(AtomicFile(destino), json(2026, 9)) { saida, bytes ->
                saida.write(bytes, 0, 5)
                throw IOException("falha simulada")
            }
        }

        assertEquals(4, fonte.carregar(context).size)
    }
}
