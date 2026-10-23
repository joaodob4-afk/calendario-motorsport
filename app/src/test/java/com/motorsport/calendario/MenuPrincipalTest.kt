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

    /** A linha de uma categoria: o nome à esquerda e, ao lado, as três opções. */
    private fun linhaDa(categoria: String): ViewGroup = textView(categoria).parent as ViewGroup

    private fun opcaoDa(categoria: String, rotulo: String): TextView =
        todas(linhaDa(categoria)).filterIsInstance<TextView>().first { it.text.toString() == rotulo }

    private fun textosIguais(texto: String) = textos().count { it == texto }

    private fun textView(texto: String): TextView =
        todas(container).filterIsInstance<TextView>().first { it.text.toString() == texto }

    private fun comDescricao(descricao: String): View =
        todas(container).first { it.contentDescription == descricao }

    /** O cartão que reúne todas as categorias (é nele que o esmaecimento é aplicado). */
    private fun cartaoDaCategoria(categoria: String) = linhaDa(categoria).parent as View

    @Test
    fun asSecoesAparecemNaOrdemPedida() {
        val textos = textos()
        val ordem = listOf(
            R.string.menu_titulo,
            R.string.na_hora_curto,
            R.string.antecedencia_titulo,
            R.string.antecedencia_dica,
            R.string.por_categoria_titulo,
            R.string.botao_teste,
            R.string.alertas_nao_chegam,
            R.string.botao_bateria
        ).map { activity.getString(it) }

        val posicoes = ordem.map { textos.indexOf(it) }

        posicoes.forEachIndexed { i, posicao -> assertTrue("Falta: ${ordem[i]}", posicao >= 0) }
        assertEquals("Fora de ordem: $posicoes", posicoes.sorted(), posicoes)
    }

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
    fun cadaCategoriaAparecePelaCorEPeloNomeUmaVezSo() {
        Categorias.todas.forEach {
            assertEquals("Nome repetido: ${it.nome}", 1, textosIguais(it.nome))
        }
    }

    @Test
    fun oAvisoDaHoraDeComecarVemLigadoNoAndroid12EAnteriores() {
        assertTrue(PreferenciasDeAviso.avisarNaHora(activity))
        assertEquals(emptyList<Int>(), PreferenciasDeAviso.minutosSelecionados(activity))
        assertEquals(1f, cartaoDaCategoria("F1").alpha, 0.001f)
    }

    @Test
    fun ligarEDesligarOAvisoDaHoraDeComecarFuncionaSemNenhumaAntecedencia() {
        comDescricao(activity.getString(R.string.cd_desligar_na_hora)).performClick()
        assertFalse(PreferenciasDeAviso.avisarNaHora(activity))
        // Sem antecedência e sem aviso na hora, nada é avisado: as categorias ficam esmaecidas.
        assertEquals(0.45f, cartaoDaCategoria("F1").alpha, 0.001f)

        comDescricao(activity.getString(R.string.cd_ligar_na_hora)).performClick()
        assertTrue(PreferenciasDeAviso.avisarNaHora(activity))
        assertEquals(1f, cartaoDaCategoria("F1").alpha, 0.001f)
    }

    @Test
    fun podeMarcarMaisDeUmaAntecedenciaEDesmarcarTodasSemMexerNoAvisoNaHora() {
        textView("15 min").performClick()
        assertEquals(listOf(15), PreferenciasDeAviso.minutosSelecionados(activity))

        textView("45 min").performClick()
        assertEquals(listOf(15, 45), PreferenciasDeAviso.minutosSelecionados(activity))

        textView("15 min").performClick()
        assertEquals(listOf(45), PreferenciasDeAviso.minutosSelecionados(activity))

        textView("45 min").performClick()
        assertEquals(emptyList<Int>(), PreferenciasDeAviso.minutosSelecionados(activity))
        assertEquals(listOf(45), PreferenciasDeAviso.ultimasSelecionadas(activity))

        // O aviso da hora de começar não depende das antecedências.
        assertTrue(PreferenciasDeAviso.avisarNaHora(activity))
    }

    @Test
    fun asTresOpcoesFicamSempreAoLadoDoNomeDeCadaCategoria() {
        val rotulos = listOf(R.string.modo_todas, R.string.modo_corridas, R.string.modo_nenhuma)
            .map { activity.getString(it) }

        Categorias.todas.forEach { categoria ->
            val textosDaLinha = todas(linhaDa(categoria.nome))
                .filterIsInstance<TextView>()
                .map { it.text.toString() }

            rotulos.forEach { rotulo ->
                assertTrue("${categoria.nome} sem a opção $rotulo", rotulo in textosDaLinha)
            }
        }
    }

    @Test
    fun escolherUmModoGravaSemAbrirNemFecharNada() {
        val corridas = activity.getString(R.string.modo_corridas)
        val quantasAntes = textosIguais(corridas)

        opcaoDa("F2", corridas).performClick()

        assertEquals(ModoDeAviso.CORRIDAS, PreferenciasDeAviso.modoDaCategoria(activity, "F2"))
        assertEquals(ModoDeAviso.TODAS, PreferenciasDeAviso.modoDaCategoria(activity, "F1"))
        assertEquals(quantasAntes, textosIguais(corridas))

        opcaoDa("F2", activity.getString(R.string.modo_nenhuma)).performClick()
        assertEquals(ModoDeAviso.NENHUMA, PreferenciasDeAviso.modoDaCategoria(activity, "F2"))
    }

    @Test
    fun soOsBotoesERespondemAoToque() {
        val selo = textView("MotoGP")
        val linha = selo.parent as View
        val cartao = linha.parent as View

        assertFalse(selo.isClickable)
        assertFalse(linha.isClickable)
        assertFalse(cartao.isClickable)

        val interruptores = todas(container).filterIsInstance<InterruptorModerno>()
        assertEquals(1, interruptores.size)
        assertTrue(interruptores.all { it.isClickable })

        val titulo = textView(activity.getString(R.string.na_hora_curto))
        assertFalse(titulo.isClickable)
        assertFalse((titulo.parent as View).isClickable)
        assertFalse(((titulo.parent as View).parent as View).isClickable)
    }
}
