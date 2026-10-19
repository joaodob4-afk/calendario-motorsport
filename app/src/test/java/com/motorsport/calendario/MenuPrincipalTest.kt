package com.motorsport.calendario

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Confere que o painel de avisos monta sem erro e que os toques principais funcionam. */
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

    private fun linhaDa(categoria: String): View =
        todas(container).first { it.contentDescription?.startsWith("$categoria,") == true }

    private fun textosIguais(texto: String) = textos().count { it == texto }

    @Test
    fun mostraOsQuatroBotoesDeAntecedenciaEUmaLinhaPorCategoria() {
        val textos = textos()

        listOf("15 min", "30 min", "45 min", activity.getString(R.string.antecedencia_chip_hora))
            .forEach { assertTrue("Falta o botão $it", it in textos) }

        Categorias.todas.forEach {
            assertTrue("Falta a categoria ${it.nome}", it.nome in textos)
        }
    }

    @Test
    fun semEscolhaOsAvisosAparecemDesligados() {
        assertTrue(activity.getString(R.string.avisos_desligados) in textos())
    }

    @Test
    fun tocarNoCartaoGeralLigaOsAvisosComAUltimaAntecedencia() {
        todas(container)
            .first { it.contentDescription == activity.getString(R.string.cd_ligar_avisos) }
            .performClick()

        assertEquals(PreferenciasDeAviso.PADRAO_MINUTOS, PreferenciasDeAviso.minutosAntes(activity))
        assertTrue(activity.getString(R.string.avisos_ligados) in textos())
    }

    @Test
    fun tocarNaLinhaAbreAsOpcoesESoUmaCategoriaFicaAbertaPorVez() {
        val corridas = activity.getString(R.string.modo_corridas)
        assertEquals(0, textosIguais(corridas))

        linhaDa("F1").performClick()
        assertEquals(1, textosIguais(corridas))

        linhaDa("F2").performClick()
        assertEquals(1, textosIguais(corridas))

        linhaDa("F2").performClick()
        assertEquals(0, textosIguais(corridas))
    }

    @Test
    fun escolherUmModoGravaEFechaALinha() {
        val corridas = activity.getString(R.string.modo_corridas)

        linhaDa("F2").performClick()
        todas(container)
            .filterIsInstance<TextView>()
            .first { it.text.toString() == corridas }
            .performClick()

        assertEquals(ModoDeAviso.CORRIDAS, PreferenciasDeAviso.modoDaCategoria(activity, "F2"))
        assertEquals(0, textosIguais(corridas))
        assertTrue(activity.getString(R.string.modo_so_corridas) in textos())
    }
}
