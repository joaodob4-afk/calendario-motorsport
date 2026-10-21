package com.motorsport.calendario

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FiltrosDeEtapasTest {

    private val hoje = LocalDate.of(2026, 10, 21)

    private fun etapa(
        categoria: String,
        pais: String,
        circuito: String,
        inicio: String,
        fim: String = ""
    ) = MainActivity.Etapa(
        categoria = categoria,
        pais = pais,
        circuito = circuito,
        data = "",
        dataInicio = inicio,
        dataFim = fim
    )

    private val passada = etapa("F1", "Brasil", "passada", "2026-03-06", "2026-03-08")
    private val monaco = etapa("F2", "Mônaco", "monaco", "2026-05-24", "2026-05-26")
    private val andando = etapa("F2", "Itália", "andando", "2026-10-20", "2026-10-22")
    private val novembro = etapa("F1", "Brasil", "novembro", "2026-10-30", "2026-11-01")
    private val dezembro = etapa("Moto2", "Qatar", "dezembro", "2026-12-04", "2026-12-06")
    private val semData = etapa("WEC", "Japão", "semdata", "")

    private val todas = listOf(passada, monaco, andando, novembro, dezembro, semData)

    private fun circuitos(lista: List<MainActivity.Etapa>) = lista.map { it.circuito }

    @Test
    fun semFiltros_escondeFinalizadasEOrdenaPorData() {
        val lista = RegrasDeFiltro.aplicar(todas, FiltrosDeEtapas(), hoje)

        assertEquals(
            listOf("andando", "novembro", "dezembro", "semdata"),
            circuitos(lista)
        )
    }

    @Test
    fun mostrarFinalizadas_incluiAsQueJaTerminaram() {
        val lista = RegrasDeFiltro.aplicar(todas, FiltrosDeEtapas(mostrarFinalizadas = true), hoje)

        assertEquals(
            listOf("passada", "monaco", "andando", "novembro", "dezembro", "semdata"),
            circuitos(lista)
        )
    }

    @Test
    fun etapaQueTerminaHojeAindaNaoEstaFinalizada() {
        val terminaHoje = etapa("F1", "Brasil", "hoje", "2026-10-19", "2026-10-21")
        val terminouOntem = etapa("F1", "Brasil", "ontem", "2026-10-18", "2026-10-20")

        assertFalse(RegrasDeFiltro.finalizada(terminaHoje, hoje))
        assertTrue(RegrasDeFiltro.finalizada(terminouOntem, hoje))
    }

    @Test
    fun semDataDeFim_contaTresDiasDeEvento() {
        val semFim = etapa("F1", "Brasil", "semfim", "2026-10-19")

        assertEquals(LocalDate.of(2026, 10, 21), RegrasDeFiltro.fim(semFim, hoje))
        assertFalse(RegrasDeFiltro.finalizada(semFim, hoje))
    }

    @Test
    fun categoria_ignoraMaiusculasEAcentos() {
        val lista = RegrasDeFiltro.aplicar(
            todas,
            FiltrosDeEtapas(categoria = "f1", mostrarFinalizadas = true),
            hoje
        )

        assertEquals(listOf("passada", "novembro"), circuitos(lista))
    }

    @Test
    fun categoria_semFinalizadasMostraSoAsProximas() {
        val lista = RegrasDeFiltro.aplicar(todas, FiltrosDeEtapas(categoria = "F1"), hoje)

        assertEquals(listOf("novembro"), circuitos(lista))
    }

    @Test
    fun pais_mostraSoEtapasDoPais() {
        val lista = RegrasDeFiltro.aplicar(
            todas,
            FiltrosDeEtapas(paises = setOf("Brasil", "Qatar")),
            hoje
        )

        assertEquals(listOf("novembro", "dezembro"), circuitos(lista))
    }

    @Test
    fun mes_etapaQueViraOMesApareceNosDois() {
        val outubro = RegrasDeFiltro.aplicar(todas, FiltrosDeEtapas(meses = setOf("2026-10")), hoje)
        val novembroLista = RegrasDeFiltro.aplicar(todas, FiltrosDeEtapas(meses = setOf("2026-11")), hoje)

        assertEquals(listOf("andando", "novembro"), circuitos(outubro))
        assertEquals(listOf("novembro"), circuitos(novembroLista))
    }

    @Test
    fun mesDaEtapa_usaInicioEFim() {
        assertEquals(setOf("2026-10", "2026-11"), RegrasDeFiltro.mesesDaEtapa(novembro))
        assertEquals(setOf("2026-12"), RegrasDeFiltro.mesesDaEtapa(dezembro))
        assertEquals(emptySet<String>(), RegrasDeFiltro.mesesDaEtapa(semData))
    }

    @Test
    fun paisEMes_somamOsFiltros() {
        val lista = RegrasDeFiltro.aplicar(
            todas,
            FiltrosDeEtapas(paises = setOf("Brasil"), meses = setOf("2026-12")),
            hoje
        )

        assertTrue(lista.isEmpty())
    }

    @Test
    fun opcoes_vemSoDasEtapasQueAparecem() {
        val base = RegrasDeFiltro.base(todas, FiltrosDeEtapas(), hoje)

        assertEquals(listOf("Itália", "Brasil", "Qatar", "Japão"), RegrasDeFiltro.paisesDisponiveis(base))
        assertEquals(listOf("2026-10", "2026-11", "2026-12"), RegrasDeFiltro.mesesDisponiveis(base))
    }

    @Test
    fun opcoes_comFinalizadasIncluemOPassado() {
        val base = RegrasDeFiltro.base(todas, FiltrosDeEtapas(mostrarFinalizadas = true), hoje)

        assertTrue("Mônaco" in RegrasDeFiltro.paisesDisponiveis(base))
        assertEquals("2026-03", RegrasDeFiltro.mesesDisponiveis(base).first())
    }

    @Test
    fun podar_tiraFiltrosQueDeixaramDeExistir() {
        val filtros = FiltrosDeEtapas(
            mostrarFinalizadas = false,
            paises = setOf("Mônaco", "Brasil"),
            meses = setOf("2026-05", "2026-11")
        )

        val podado = RegrasDeFiltro.podar(todas, filtros, hoje)

        assertEquals(setOf("Brasil"), podado.paises)
        assertEquals(setOf("2026-11"), podado.meses)
    }

    @Test
    fun podar_mantemTudoQuandoAsFinalizadasEstaoVisiveis() {
        val filtros = FiltrosDeEtapas(
            mostrarFinalizadas = true,
            paises = setOf("Mônaco"),
            meses = setOf("2026-05")
        )

        assertEquals(filtros, RegrasDeFiltro.podar(todas, filtros, hoje))
    }

    @Test
    fun quantosAtivos_naoContaACategoria() {
        assertEquals(0, FiltrosDeEtapas(categoria = "F1").quantosAtivos)
        assertEquals(
            4,
            FiltrosDeEtapas(
                mostrarFinalizadas = true,
                paises = setOf("A", "B"),
                meses = setOf("2026-01")
            ).quantosAtivos
        )
    }
}
