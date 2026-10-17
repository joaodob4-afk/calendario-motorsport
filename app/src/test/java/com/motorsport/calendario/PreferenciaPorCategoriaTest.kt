package com.motorsport.calendario

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PreferenciaPorCategoriaTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun limpar() {
        context.getSharedPreferences("preferencias", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun porPadraoCadaCategoriaAvisaDeTodasAsSessoes() {
        Categorias.todas.forEach {
            assertEquals(ModoDeAviso.TODAS, PreferenciasDeAviso.modoDaCategoria(context, it.nome))
        }
    }

    @Test
    fun cadaCategoriaGuardaASuaEscolha() {
        PreferenciasDeAviso.definirModo(context, "F1", ModoDeAviso.TODAS)
        PreferenciasDeAviso.definirModo(context, "F2", ModoDeAviso.CORRIDAS)
        PreferenciasDeAviso.definirModo(context, "Formula E", ModoDeAviso.NENHUMA)

        assertEquals(ModoDeAviso.TODAS, PreferenciasDeAviso.modoDaCategoria(context, "F1"))
        assertEquals(ModoDeAviso.CORRIDAS, PreferenciasDeAviso.modoDaCategoria(context, "F2"))
        assertEquals(ModoDeAviso.NENHUMA, PreferenciasDeAviso.modoDaCategoria(context, "Formula E"))
        assertEquals(ModoDeAviso.TODAS, PreferenciasDeAviso.modoDaCategoria(context, "F3"))
    }

    @Test
    fun nomeDaCategoriaIgnoraMaiusculasEAcentos() {
        PreferenciasDeAviso.definirModo(context, "Fórmula E", ModoDeAviso.CORRIDAS)

        assertEquals(ModoDeAviso.CORRIDAS, PreferenciasDeAviso.modoDaCategoria(context, "formula e"))
    }

    @Test
    fun valorSalvoInvalidoVoltaParaTodas() {
        context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)
            .edit().putString("aviso_modo_f1", "qualquer coisa").commit()

        assertEquals(ModoDeAviso.TODAS, PreferenciasDeAviso.modoDaCategoria(context, "F1"))
    }

    @Test
    fun religarOsAvisosLembraUltimaAntecedencia() {
        assertEquals(30, PreferenciasDeAviso.ultimaAntecedencia(context))

        PreferenciasDeAviso.definir(context, 45)
        PreferenciasDeAviso.definir(context, PreferenciasDeAviso.DESATIVADO)

        assertEquals(PreferenciasDeAviso.DESATIVADO, PreferenciasDeAviso.minutosAntes(context))
        assertEquals(45, PreferenciasDeAviso.ultimaAntecedencia(context))
    }

    @Test
    fun modoNaoMudaAAntecedencia() {
        PreferenciasDeAviso.definir(context, 15)
        PreferenciasDeAviso.definirModo(context, "F1", ModoDeAviso.CORRIDAS)

        assertEquals(15, PreferenciasDeAviso.minutosAntes(context))
    }
}
