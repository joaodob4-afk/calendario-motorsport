package com.motorsport.calendario

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PreferenciasDeAvisoTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun limpar() {
        context.getSharedPreferences("preferencias", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun opcoesVaoDeUmaHoraAteQuinzeMinutos() {
        assertEquals(listOf(60, 45, 30, 15), PreferenciasDeAviso.opcoes)
    }

    @Test
    fun soFavoritasDesligadoNaoMudaOModoDeNinguem() {
        PreferenciasDeFavoritas.definirCategorias(context, setOf("f1"))
        PreferenciasDeAviso.definirModo(context, "F2", ModoDeAviso.CORRIDAS)

        assertEquals(ModoDeAviso.CORRIDAS, PreferenciasDeAviso.modoDaCategoria(context, "F2"))
        assertFalse(PreferenciasDeAviso.silenciadaPorFavoritas(context, "F2"))
    }

    @Test
    fun soFavoritasLigadoCalaAsOutrasEPreservaOModoSalvo() {
        PreferenciasDeFavoritas.definirCategorias(context, setOf("f1"))
        PreferenciasDeAviso.definirModo(context, "F2", ModoDeAviso.CORRIDAS)
        PreferenciasDeAviso.definirAvisarSoFavoritas(context, true)

        assertEquals(ModoDeAviso.NENHUMA, PreferenciasDeAviso.modoDaCategoria(context, "F2"))
        assertEquals(ModoDeAviso.CORRIDAS, PreferenciasDeAviso.modoSalvoDaCategoria(context, "F2"))
        assertEquals(ModoDeAviso.TODAS, PreferenciasDeAviso.modoDaCategoria(context, "F1"))

        PreferenciasDeAviso.definirAvisarSoFavoritas(context, false)
        assertEquals(ModoDeAviso.CORRIDAS, PreferenciasDeAviso.modoDaCategoria(context, "F2"))
    }

    @Test
    fun modoFoiEscolhidoSoDepoisDeDefinir() {
        assertFalse(PreferenciasDeAviso.modoFoiEscolhido(context, "F3"))

        PreferenciasDeAviso.definirModo(context, "F3", ModoDeAviso.NENHUMA)

        assertTrue(PreferenciasDeAviso.modoFoiEscolhido(context, "F3"))
    }

    @Test
    fun antecedenciasExtrasDaPaginaPersonalizarSaoValidas() {
        PreferenciasDeAviso.opcoesExtras.forEach { minutos ->
            PreferenciasDeAviso.definir(context, minutos)
            assertEquals(minutos, PreferenciasDeAviso.minutosAntes(context))
        }

        PreferenciasDeAviso.definirSelecionados(context, listOf(1440, 5, 90, 15))
        assertEquals(listOf(5, 15, 90, 1440), PreferenciasDeAviso.minutosSelecionados(context))
    }

    @Test
    fun osBotoesRapidosContinuamSendoOsQuatroDeSempre() {
        assertTrue(PreferenciasDeAviso.opcoes.all { it in PreferenciasDeAviso.todasAsOpcoes })
        assertTrue(PreferenciasDeAviso.opcoesExtras.none { it in PreferenciasDeAviso.opcoes })
    }

    @Test
    fun descricoesDasAntecedenciasExtras() {
        assertEquals("90 minutos antes", PreferenciasDeAviso.descricao(90))
        assertEquals("2 horas antes", PreferenciasDeAviso.descricao(120))
        assertEquals("1 dia antes", PreferenciasDeAviso.descricao(1440))
        assertEquals("5 minutos antes", PreferenciasDeAviso.descricao(5))
    }

    @Test
    fun semEscolhaOAvisoFicaDesativado() {
        assertEquals(PreferenciasDeAviso.DESATIVADO, PreferenciasDeAviso.minutosAntes(context))
    }

    @Test
    fun escolhaValidaFicaSalva() {
        PreferenciasDeAviso.opcoes.forEach { minutos ->
            PreferenciasDeAviso.definir(context, minutos)
            assertEquals(minutos, PreferenciasDeAviso.minutosAntes(context))
        }
    }

    @Test
    fun valorForaDasOpcoesViraDesativado() {
        PreferenciasDeAviso.definir(context, 30)
        PreferenciasDeAviso.definir(context, 20)
        assertEquals(PreferenciasDeAviso.DESATIVADO, PreferenciasDeAviso.minutosAntes(context))
    }

    @Test
    fun valorInvalidoJaSalvoNoAparelhoEIgnorado() {
        context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)
            .edit().putInt("aviso_minutos_antes", 999).commit()

        assertEquals(PreferenciasDeAviso.DESATIVADO, PreferenciasDeAviso.minutosAntes(context))
    }

    @Test
    fun descricoesEstaoEmPortugues() {
        assertEquals("1 hora antes", PreferenciasDeAviso.descricao(60))
        assertEquals("45 minutos antes", PreferenciasDeAviso.descricao(45))
        assertEquals("30 minutos antes", PreferenciasDeAviso.descricao(30))
        assertEquals("15 minutos antes", PreferenciasDeAviso.descricao(15))
        assertEquals("Desativado", PreferenciasDeAviso.descricao(PreferenciasDeAviso.DESATIVADO))
    }

    @Test
    fun barraInferiorTemEntreTresECincoItensComRotulosUnicos() {
        val rotulos = BarraInferior.itens.map { it.rotulo }

        assertTrue("a barra deve ter de 3 a 5 itens", rotulos.size in 3..5)
        assertEquals(rotulos.size, rotulos.toSet().size)
    }
}
