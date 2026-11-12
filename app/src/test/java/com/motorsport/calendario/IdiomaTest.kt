package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdiomaTest {

    @Test
    fun mapeiaCategoriasParaRecursosLocalizados() {
        assertEquals(R.string.formula_1, Idioma.recursoCategoria("F1"))
        assertEquals(R.string.formula_2, Idioma.recursoCategoria("F2"))
        assertEquals(R.string.formula_3, Idioma.recursoCategoria("F3"))
        assertEquals(R.string.f1_academy, Idioma.recursoCategoria("F1 Academy"))
        assertEquals(R.string.formula_e, Idioma.recursoCategoria("Formula E"))
        assertEquals(R.string.nome_indycar, Idioma.recursoCategoria("IndyCar"))
        assertEquals(R.string.nascar_cup, Idioma.recursoCategoria("NASCAR Cup Series"))
        assertEquals(R.string.nascar_xfinity, Idioma.recursoCategoria("NASCAR Xfinity Series"))
        assertEquals(R.string.nascar_truck, Idioma.recursoCategoria("NASCAR Craftsman Truck Series"))
    }

    @Test
    fun reconheceVariacoesDeEspacosEMaiusculas() {
        assertEquals(R.string.formula_e, Idioma.recursoCategoria("  FORMULA E "))
    }

    @Test
    fun preservaCategoriasDesconhecidas() {
        assertNull(Idioma.recursoCategoria("Nova categoria"))
    }
}
