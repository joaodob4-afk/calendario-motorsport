package com.motorsport.calendario

class NascarXfinityActivity : FormulaJuniorActivity() {
    override val categoria = "NASCAR O'Reilly Auto Parts Series"
    override val tituloRes = R.string.nascar_xfinity
    override val calendario: CalendarioFormulaJunior = NascarXfinityCalendarJson
}
