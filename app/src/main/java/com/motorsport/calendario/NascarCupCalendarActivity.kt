package com.motorsport.calendario

import androidx.appcompat.app.AppCompatActivity

class NascarCupCalendarActivity : FormulaJuniorCalendarActivity() {
    override val tituloRes = R.string.nascar_cup
    override val calendario: CalendarioFormulaJunior = NascarCupCalendarJson
    override val telaDeDetalhes: Class<out AppCompatActivity> = NascarCupActivity::class.java
}
