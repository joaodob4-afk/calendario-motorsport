package com.motorsport.calendario

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.ZonedDateTime

/** Várias antecedências ao mesmo tempo: cálculo dos avisos (sem Android). */
class AntecedenciaMultiplaTest {

    private val brasilia = PlanoDeAvisos.BRASILIA
    private val agora = ZonedDateTime.of(2026, 10, 16, 9, 0, 0, 0, brasilia).toInstant()
    private val corrida = SessaoParaAviso(
        "F1", "Melbourne", "Corrida",
        ZonedDateTime.of(2026, 10, 18, 10, 0, 0, 0, brasilia)
    )

    @Test
    fun duasAntecedenciasGeramDoisAvisosComCodigosDiferentes() {
        val avisos = PlanoDeAvisos.calcular(listOf(corrida), agora, listOf(15, 45))

        assertEquals(2, avisos.size)
        assertEquals(setOf(15, 45), avisos.map { it.minutosAntes }.toSet())
        assertEquals(2, avisos.map { it.codigoDoAlarme }.toSet().size)
        assertEquals(45, avisos.first().minutosAntes)
        assertTrue(avisos[0].dispararEm.isBefore(avisos[1].dispararEm))
    }

    @Test
    fun comAvisoNaHoraSaemTresAlarmesDiferentes() {
        val avisos = PlanoDeAvisos.calcularTodos(listOf(corrida), agora, listOf(15, 45))

        assertEquals(3, avisos.size)
        assertEquals(1, avisos.count { it.naHoraDeComecar })
        assertEquals(3, avisos.map { it.codigoDoAlarme }.toSet().size)
    }

    @Test
    fun semNenhumaAntecedenciaValidaNaoHaAvisos() {
        assertTrue(PlanoDeAvisos.calcular(listOf(corrida), agora, emptyList<Int>()).isEmpty())
        assertTrue(PlanoDeAvisos.calcular(listOf(corrida), agora, listOf(0, -5)).isEmpty())
        assertTrue(PlanoDeAvisos.calcularTodos(listOf(corrida), agora, emptyList<Int>()).isEmpty())
    }

    @Test
    fun valorRepetidoNaoDuplicaOAviso() {
        assertEquals(1, PlanoDeAvisos.calcular(listOf(corrida), agora, listOf(30, 30)).size)
    }

    @Test
    fun oLimiteFicaComOsAvisosMaisProximos() {
        val avisos = PlanoDeAvisos.calcular(listOf(corrida), agora, listOf(15, 30, 45), limite = 2)

        assertEquals(listOf(45, 30), avisos.map { it.minutosAntes })
    }

    @Test
    fun umaAntecedenciaSoContinuaFuncionandoComoAntes() {
        val avisos = PlanoDeAvisos.calcular(listOf(corrida), agora, 30)

        assertEquals(1, avisos.size)
        assertEquals(30, avisos.single().minutosAntes)
    }
}

/** Várias antecedências ao mesmo tempo: como ficam guardadas no aparelho. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PreferenciasMultiplasTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun limpar() {
        context.getSharedPreferences("preferencias", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun guardaVariasEmOrdemCrescente() {
        PreferenciasDeAviso.definirSelecionados(context, listOf(45, 15))

        assertEquals(listOf(15, 45), PreferenciasDeAviso.minutosSelecionados(context))
        assertEquals(listOf(15, 45), PreferenciasDeAviso.ultimasSelecionadas(context))
    }

    @Test
    fun listaVaziaDesligaMasLembraAUltimaEscolha() {
        PreferenciasDeAviso.definirSelecionados(context, listOf(60, 30))
        PreferenciasDeAviso.definirSelecionados(context, emptyList())

        assertEquals(emptyList<Int>(), PreferenciasDeAviso.minutosSelecionados(context))
        assertEquals(listOf(30, 60), PreferenciasDeAviso.ultimasSelecionadas(context))
    }

    @Test
    fun valoresForaDasOpcoesSaoIgnorados() {
        PreferenciasDeAviso.definirSelecionados(context, listOf(20, 30, 999))

        assertEquals(listOf(30), PreferenciasDeAviso.minutosSelecionados(context))
    }

    @Test
    fun aEscolhaDaVersaoAnteriorContinuaValendo() {
        context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)
            .edit().putInt("aviso_minutos_antes", 45).commit()

        assertEquals(listOf(45), PreferenciasDeAviso.minutosSelecionados(context))
    }

    @Test
    fun aEscolhaNovaSobrepoeAAntigaMesmoQuandoVazia() {
        context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)
            .edit().putInt("aviso_minutos_antes", 45).commit()

        PreferenciasDeAviso.definirSelecionados(context, emptyList())

        assertEquals(emptyList<Int>(), PreferenciasDeAviso.minutosSelecionados(context))
    }
}
