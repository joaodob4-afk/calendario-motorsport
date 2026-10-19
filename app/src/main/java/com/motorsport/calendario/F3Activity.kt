package com.motorsport.calendario

/** Detalhes de uma etapa da F3. A tela está em [FormulaJuniorActivity]. */
class F3Activity : FormulaJuniorActivity() {
    override val categoria = "F3"
    override val tituloRes = R.string.formula_3
    override val calendario: CalendarioFormulaJunior = F3CalendarJson
}
