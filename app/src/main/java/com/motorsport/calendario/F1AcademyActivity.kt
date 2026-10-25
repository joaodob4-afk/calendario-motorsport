package com.motorsport.calendario

/** Detalhes de uma etapa da F1 Academy. A tela está em [FormulaJuniorActivity]. */
class F1AcademyActivity : FormulaJuniorActivity() {
    override val categoria = "F1 Academy"
    override val tituloRes = R.string.f1_academy
    override val calendario: CalendarioFormulaJunior = F1AcademyCalendarJson
}
