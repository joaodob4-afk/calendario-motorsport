package com.motorsport.calendario

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Este arquivo existia numa versão anterior, quando a escolha era única ("todas" ou
 * "apenas corridas"). Hoje a escolha é por categoria (veja PreferenciaPorCategoriaTest).
 * Foi mantido, atualizado, para não deixar um teste antigo quebrando a compilação.
 * Pode ser apagado do repositório quando quiser.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PreferenciaTipoDeAvisoTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun limpar() {
        context.getSharedPreferences("preferencias", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun porPadraoAvisaDeTodasAsSessoes() {
        assertEquals(ModoDeAviso.TODAS, PreferenciasDeAviso.modoDaCategoria(context, "F1"))
    }

    @Test
    fun tipoDeSessaoNaoMudaOTempoDeAntecedencia() {
        PreferenciasDeAviso.definir(context, 30)
        PreferenciasDeAviso.definirModo(context, "F2", ModoDeAviso.CORRIDAS)

        assertEquals(30, PreferenciasDeAviso.minutosAntes(context))
    }
}