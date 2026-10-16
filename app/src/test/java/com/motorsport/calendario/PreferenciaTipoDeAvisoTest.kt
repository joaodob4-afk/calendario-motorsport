package com.motorsport.calendario

import android.content.Context
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
class PreferenciaTipoDeAvisoTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun limpar() {
        context.getSharedPreferences("preferencias", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun porPadraoAvisaDeTodasAsSessoes() {
        assertFalse(PreferenciasDeAviso.apenasCorridas(context))
    }

    @Test
    fun escolhaDeApenasCorridasFicaSalva() {
        PreferenciasDeAviso.definirApenasCorridas(context, true)
        assertTrue(PreferenciasDeAviso.apenasCorridas(context))

        PreferenciasDeAviso.definirApenasCorridas(context, false)
        assertFalse(PreferenciasDeAviso.apenasCorridas(context))
    }

    @Test
    fun tipoDeSessaoNaoMudaOTempoDeAntecedencia() {
        PreferenciasDeAviso.definir(context, 30)
        PreferenciasDeAviso.definirApenasCorridas(context, true)

        assertTrue(PreferenciasDeAviso.minutosAntes(context) == 30)
    }
}
