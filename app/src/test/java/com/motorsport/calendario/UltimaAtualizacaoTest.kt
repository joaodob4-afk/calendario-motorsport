package com.motorsport.calendario

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** A data da última atualização dos calendários, mostrada nas configurações. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UltimaAtualizacaoTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun limpar() {
        context.getSharedPreferences("preferencias", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun semAtualizacaoNaoHaTexto() {
        assertNull(UltimaAtualizacao.ler(context))
        assertNull(UltimaAtualizacao.texto(context))
    }

    @Test
    fun registrarGravaOInstanteEMostraDataEHora() {
        UltimaAtualizacao.registrar(context, 1_700_000_000_000L)

        assertEquals(1_700_000_000_000L, UltimaAtualizacao.ler(context))
        val texto = UltimaAtualizacao.texto(context)
        assertNotNull(texto)
        assertTrue(texto!!.contains("·"))
    }
}
