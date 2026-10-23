package com.motorsport.calendario

import androidx.appcompat.app.AppCompatDelegate
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenciasDeTemaTest {

    @Test
    fun cadaChaveVoltaParaOSeuModo() {
        ModoDeTema.values().forEach {
            assertEquals(it, ModoDeTema.deChave(it.chave))
        }
    }

    @Test
    fun semEscolhaOuComValorEstranho_usaOPadraoDoSistema() {
        assertEquals(ModoDeTema.SISTEMA, ModoDeTema.deChave(null))
        assertEquals(ModoDeTema.SISTEMA, ModoDeTema.deChave(""))
        assertEquals(ModoDeTema.SISTEMA, ModoDeTema.deChave("roxo"))
    }

    @Test
    fun cadaModoCorrespondeAoDoAndroid() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ModoDeTema.SISTEMA.modoDoAppCompat)
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ModoDeTema.CLARO.modoDoAppCompat)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ModoDeTema.ESCURO.modoDoAppCompat)
    }

    @Test
    fun asChavesSaoUnicas() {
        assertEquals(ModoDeTema.values().size, ModoDeTema.values().map { it.chave }.toSet().size)
    }
}
