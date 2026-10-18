package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Protege as traduções: se um texto existir só em um idioma, ou se os marcadores (%d, %s)
 * forem diferentes entre inglês e português, o build falha em vez de o app quebrar.
 */
class TraducoesDoAppTest {

    private fun ler(caminho: String): Map<String, String> {
        val arquivo = File(caminho)
        assertTrue("Arquivo não encontrado: ${arquivo.absolutePath}", arquivo.isFile)

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(arquivo)
        val itens = doc.documentElement.childNodes
        val mapa = mutableMapOf<String, String>()

        for (i in 0 until itens.length) {
            val no = itens.item(i) as? Element ?: continue
            if (no.tagName == "string" || no.tagName == "bool") {
                val nome = no.getAttribute("name")
                assertFalse("Chave repetida: $nome", mapa.containsKey(nome))
                mapa[nome] = no.textContent
            }
        }

        return mapa
    }

    private val ingles get() = ler("src/main/res/values/strings.xml")
    private val portugues get() = ler("src/main/res/values-pt/strings.xml")

    @Test
    fun osDoisIdiomasTemAsMesmasChaves() {
        val en = ingles.keys
        val pt = portugues.keys

        assertEquals("Faltam em português: ${en - pt}", emptySet<String>(), en - pt)
        assertEquals("Faltam em inglês: ${pt - en}", emptySet<String>(), pt - en)
    }

    @Test
    fun nenhumTextoFicaVazio() {
        (ingles + portugues.mapKeys { "pt:" + it.key }).forEach { (chave, texto) ->
            assertTrue("Texto vazio: $chave", texto.isNotBlank())
        }
    }

    @Test
    fun marcadoresDeFormatoSaoOsMesmosNosDoisIdiomas() {
        val marcador = Regex("%(\\d+\\$)?[sd]")
        val en = ingles
        val pt = portugues

        for (chave in en.keys) {
            val a = marcador.findAll(en.getValue(chave)).map { it.value }.sorted().toList()
            val b = marcador.findAll(pt.getValue(chave)).map { it.value }.sorted().toList()

            assertEquals("Marcadores diferentes em $chave", a, b)
        }
    }

    @Test
    fun inglesEPortuguesDeclaramOIdiomaCorreto() {
        assertEquals("true", ingles.getValue("traduzir_sessoes"))
        assertEquals("false", portugues.getValue("traduzir_sessoes"))
    }
}
