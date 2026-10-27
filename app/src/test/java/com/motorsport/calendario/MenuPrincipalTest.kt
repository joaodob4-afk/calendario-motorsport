package com.motorsport.calendario

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Confere que o painel de alertas monta sem erro, na ordem certa, e que os toques funcionam. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MenuPrincipalTest {

    private lateinit var activity: Activity
    private lateinit var container: LinearLayout

    @Before
    fun preparar() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        activity.getSharedPreferences("preferencias", Context.MODE_PRIVATE).edit().clear().commit()
        MenuPrincipal.reiniciar()
        container = LinearLayout(activity)
        MenuPrincipal.montar(activity, container)
    }

    private fun todas(raiz: View): List<View> =
        listOf(raiz) + if (raiz is ViewGroup) {
            (0 until raiz.childCount).flatMap { todas(raiz.getChildAt(it)) }
        } else {
            emptyList()
        }

    private fun textos(): List<String> =
        todas(container).filterIsInstance<TextView>().map { it.text.toString() }

    private fun textoDe(texto: String): TextView =
        todas(container).filterIsInstance<TextView>().first { it.text.toString() == texto }

    private fun comDescricao(descricao: String): View =
        todas(container).first { it.contentDescription == descricao }

    @Test
    fun asSecoesAparecemNaOrdemPedida() {
        val textos = textos()
        val ordem = listOf(
            R.string.menu_titulo,
            R.string.na_hora_curto,
            R.string.personalizar_alertas,
            R.string.botao_teste,
            R.string.alertas_nao_chegam,
            R.string.botao_bateria
        ).map { activity.getString(it) }

        val posicoes = ordem.map { textos.indexOf(it) }

        posicoes.forEachIndexed { i, posicao -> assertTrue("Falta: ${ordem[i]}", posicao >= 0) }
        assertEquals("Fora de ordem: $posicoes", posicoes.sorted(), posicoes)
    }

    @Test
    fun oPainelNaoTemMaisAntecedenciaNemCategorias() {
        val textos = textos()

        listOf("15 min", "30 min", "45 min", activity.getString(R.string.antecedencia_chip_hora))
            .forEach { assertFalse("Não devia ter $it", it in textos) }

        Categorias.todas.forEach {
            assertFalse("Não devia ter ${it.nome}", it.nome in textos)
        }
        assertFalse(activity.getString(R.string.antecedencia_titulo) in textos)
    }

    @Test
    fun oAvisoDaHoraDeComecarVemLigadoNoAndroid12EAnteriores() {
        assertTrue(PreferenciasDeAviso.avisarNaHora(activity))
        assertEquals(emptyList<Int>(), PreferenciasDeAviso.minutosSelecionados(activity))
    }

    @Test
    fun ligarEDesligarOAvisoDaHoraDeComecar() {
        comDescricao(activity.getString(R.string.cd_desligar_na_hora)).performClick()
        assertFalse(PreferenciasDeAviso.avisarNaHora(activity))

        comDescricao(activity.getString(R.string.cd_ligar_na_hora)).performClick()
        assertTrue(PreferenciasDeAviso.avisarNaHora(activity))
    }

    @Test
    fun oBotaoPersonalizarMostraQuantasAntecedenciasEstaoMarcadas() {
        assertFalse("+2" in textos())

        PreferenciasDeAviso.definirSelecionados(activity, listOf(15, 90))
        MenuPrincipal.montar(activity, container)

        assertTrue("+2" in textos())
    }

    @Test
    fun soOsBotoesEOInterruptorRespondemAoToque() {
        val interruptores = todas(container).filterIsInstance<InterruptorModerno>()
        assertEquals(1, interruptores.size)
        assertTrue(interruptores.all { it.isClickable })

        val titulo = textoDe(activity.getString(R.string.na_hora_curto))
        assertFalse(titulo.isClickable)
        assertFalse((titulo.parent as View).isClickable)
    }
}
