package com.motorsport.calendario

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class CategoriasTest {

    @Test
    fun info_ignoraMaiusculasEAcentos() {
        assertEquals("Formula E", Categorias.info("formula e")?.nome)
        assertEquals("Formula E", Categorias.info("FÓRMULA E")?.nome)
        assertEquals("IndyCar", Categorias.info("INDYCAR")?.nome)
    }

    @Test
    fun info_categoriaDesconhecidaRetornaNulo() {
        assertNull(Categorias.info("Stock Car Brasil"))
    }

    @Test
    fun corHex_categoriaConhecida() {
        assertEquals("#FF3B30", Categorias.corHex("F1"))
        assertEquals("#22D3EE", Categorias.corHex("formula e"))
    }

    @Test
    fun corHex_categoriaDesconhecidaUsaVerdePadrao() {
        assertEquals(Categorias.COR_PADRAO, Categorias.corHex("Categoria Nova"))
    }

    @Test
    fun rotuloDoSelo_usaNomeCompletoEmMaiusculasQuandoNaoHaAbreviacao() {
        assertEquals("INDYCAR", Categorias.rotuloDoSelo("IndyCar"))
        assertEquals("STOCK CAR BRASIL", Categorias.rotuloDoSelo("Stock Car Brasil"))
    }

    // ---------- proteções contra erro ao adicionar categorias no futuro ----------

    @Test
    fun todasAsCoresSaoHexValido() {
        val formato = Regex("^#[0-9A-Fa-f]{6}$")
        for (categoria in Categorias.todas) {
            assertTrue("Cor inválida em ${categoria.nome}: ${categoria.cor}", formato.matches(categoria.cor))
        }
        assertTrue(formato.matches(Categorias.COR_PADRAO))
    }

    @Test
    fun nomesSaoUnicosEPreenchidos() {
        val normalizados = Categorias.todas.map { Formatacao.normalizar(it.nome) }
        assertTrue("Há nome vazio em Categorias.todas", normalizados.none { it.isEmpty() })
        assertEquals("Há categorias repetidas em Categorias.todas", normalizados.size, normalizados.toSet().size)
    }

    @Test
    fun abreviacoesNaoSaoVazias() {
        for (categoria in Categorias.todas) {
            categoria.abreviacao?.let {
                assertTrue("Abreviação vazia em ${categoria.nome}", it.isNotBlank())
            }
        }
    }

    @Test
    fun todaCategoriaDosDadosEstaNaLista() {
        val pasta = File("src/main/assets")
        assumeTrue("Pasta de dados não encontrada a partir de ${File(".").absolutePath}", pasta.isDirectory)

        val arquivos = pasta.listFiles { f -> f.name.endsWith(".json") }?.toList().orEmpty()
        assumeTrue("Nenhum JSON encontrado em $pasta", arquivos.isNotEmpty())

        val campo = Regex("\"categoria\"\\s*:\\s*\"([^\"]+)\"")
        for (arquivo in arquivos) {
            val nome = campo.find(arquivo.readText())?.groupValues?.get(1) ?: continue
            assertNotNull(
                "A categoria \"$nome\" de ${arquivo.name} não está em Categorias.todas",
                Categorias.info(nome)
            )
        }
    }
}
