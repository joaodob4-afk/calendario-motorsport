package com.motorsport.calendario

/** Detalhes de uma etapa da F2. A tela está em [FormulaJuniorActivity]. */
class F2Activity : FormulaJuniorActivity() {
    override val categoria = "F2"
    override val tituloRes = R.string.formula_2
    override val calendario: CalendarioFormulaJunior = F2CalendarJson
}
