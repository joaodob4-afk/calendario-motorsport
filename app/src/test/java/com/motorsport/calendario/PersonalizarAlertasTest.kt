package com.motorsport.calendario

import android.content.Context
import android.view.View
import android.view.ViewGroup
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

/** A página "Personalizar alertas": antecedências e opções por categoria. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PersonalizarAlertasTest {

    private lateinit var activity: PersonalizarAlertasActivity

    @Before
    fun preparar() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        app.getSharedPreferences("preferencias", Context.MODE_PRIVATE).edit().clear().commit()
        activity = Robolectric.buildActivity(PersonalizarAlertasActivity::class.java).create().get()
    }

    private fun todas(raiz: View): List<View> =
        listOf(raiz) + if (raiz is ViewGroup) {
            (0 until raiz.childCount).flatMap { todas(raiz.getChildAt(it)) }
        } else {
            emptyList()
        }

    private fun raiz(): View = activity.findViewById<ViewGroup>(android.R.id.content)

    private fun textos(): List<String> =
        todas(raiz()).filterIsInstance<TextView>().map { it.text.toString() }

    private fun textView(texto: String): TextView =
        todas(raiz()).filterIsInstance<TextView>().first { it.text.toString() == texto }

    @Test
    fun mostraTodasAsAntecedenciasEOInterruptorDasFavoritas() {
        val textos = textos()

        PreferenciasDeAviso.todasAsOpcoes.forEach {
            val rotulo = rotuloCurtoDaAntecedencia(activity, it)
            assertTrue("Falta $rotulo", rotulo in textos)
        }
        assertTrue(activity.getString(R.string.avisar_so_favoritas) in textos)
    }

    @Test
    fun aPaginaNaoTemMaisAListaDeCategorias() {
        val textos = textos()

        Categorias.todas.forEach { assertFalse("Não devia ter ${it.nome}", it.nome in textos) }
    }

    @Test
    fun naoTemALegendaDeMaisDeUm() {
        assertFalse(textos().any { it.contains("mais de um", true) || it.contains("more than one", true) })
    }

    @Test
    fun podeMarcarVariasAntecedenciasEDesmarcar() {
        textView("15 min").performClick()
        textView("5 min").performClick()
        assertEquals(listOf(5, 15), PreferenciasDeAviso.minutosSelecionados(activity))

        textView("15 min").performClick()
        assertEquals(listOf(5), PreferenciasDeAviso.minutosSelecionados(activity))
    }

    @Test
    fun ligarSoFavoritasSilenciaQuemNaoEFavoritaSemPerderAEscolha() {
        PreferenciasDeAviso.definirModo(activity, "F2", ModoDeAviso.TODAS)
        PreferenciasDeFavoritas.definirCategorias(activity, setOf("f1"))

        todas(raiz()).filterIsInstance<InterruptorModerno>().first().performClick()

        assertTrue(PreferenciasDeAviso.avisarSoFavoritas(activity))
        assertEquals(ModoDeAviso.NENHUMA, PreferenciasDeAviso.modoDaCategoria(activity, "F2"))
        assertEquals(ModoDeAviso.TODAS, PreferenciasDeAviso.modoDaCategoria(activity, "F1"))

        // A escolha de F2 continua guardada: ao desligar, ela volta.
        todas(raiz()).filterIsInstance<InterruptorModerno>().first().performClick()
        assertEquals(ModoDeAviso.TODAS, PreferenciasDeAviso.modoDaCategoria(activity, "F2"))
    }
}
