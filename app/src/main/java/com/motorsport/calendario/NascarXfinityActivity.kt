package com.motorsport.calendario

class NascarXfinityActivity : FormulaJuniorActivity() {
    override val categoria = "NASCAR Xfinity Series"
    override val tituloRes = R.string.nascar_xfinity
    override val calendario: CalendarioFormulaJunior = NascarXfinityCalendarJson
}
