package com.motorsport.calendario

class NascarCupActivity : FormulaJuniorActivity() {
    override val categoria = "NASCAR Cup Series"
    override val tituloRes = R.string.nascar_cup
    override val calendario: CalendarioFormulaJunior = NascarCupCalendarJson
}
