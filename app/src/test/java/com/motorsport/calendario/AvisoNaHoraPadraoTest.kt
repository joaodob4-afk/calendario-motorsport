package com.motorsport.calendario

import android.content.Context
import android.os.Build
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * "Alertar na hora de começar" vem ligado na primeira abertura do app no Android 12 e
 * anterior (onde não se pede permissão), sem atrapalhar quem já tinha uma escolha.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AvisoNaHoraPadraoTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val salvos get() = context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)

    @Before
    fun limpar() {
        salvos.edit().clear().commit()
    }

    @After
    fun restaurarVersao() {
        versaoDoAndroid(28)
    }

    private fun versaoDoAndroid(sdk: Int) {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
    }

    @Test
    fun primeiraAberturaNoAndroid12EAnterioresVemLigado() {
        listOf(28, 31, 32).forEach {
            versaoDoAndroid(it)
            assertTrue("Android $it", PreferenciasDeAviso.avisarNaHora(context))
        }
    }

    @Test
    fun primeiraAberturaNoAndroid13EPosterioresVemDesligado() {
        listOf(33, 34).forEach {
            versaoDoAndroid(it)
            assertFalse("Android $it", PreferenciasDeAviso.avisarNaHora(context))
        }
    }

    @Test
    fun aEscolhaSalvaSempreVenceOPadrao() {
        versaoDoAndroid(33)
        PreferenciasDeAviso.definirAvisarNaHora(context, true)
        assertTrue(PreferenciasDeAviso.avisarNaHora(context))

        versaoDoAndroid(28)
        PreferenciasDeAviso.definirAvisarNaHora(context, false)
        assertFalse(PreferenciasDeAviso.avisarNaHora(context))
    }

    @Test
    fun quemJaTinhaAvisosLigadosNaVersaoAnteriorContinuaRecebendo() {
        versaoDoAndroid(33)
        salvos.edit().putInt("aviso_minutos_antes", 30).commit()

        assertTrue(PreferenciasDeAviso.avisarNaHora(context))
    }

    @Test
    fun quemTinhaDesligadoTudoNaVersaoAnteriorContinuaDesligado() {
        versaoDoAndroid(28)
        salvos.edit().putInt("aviso_minutos_antes", 0).commit()

        assertFalse(PreferenciasDeAviso.avisarNaHora(context))
    }

    @Test
    fun mudarAAntecedenciaFixaOQueValiaParaOAvisoNaHora() {
        versaoDoAndroid(33)
        salvos.edit().putInt("aviso_minutos_antes", 30).commit()

        PreferenciasDeAviso.definirSelecionados(context, listOf(15))

        assertEquals(listOf(15), PreferenciasDeAviso.minutosSelecionados(context))
        assertTrue(PreferenciasDeAviso.avisarNaHora(context))
    }
}
