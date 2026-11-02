package com.motorsport.calendario

import androidx.appcompat.app.AppCompatActivity

class NascarXfinityCalendarActivity : FormulaJuniorCalendarActivity() {
    override val tituloRes = R.string.nascar_xfinity
    override val calendario: CalendarioFormulaJunior = NascarXfinityCalendarJson
    override val telaDeDetalhes: Class<out AppCompatActivity> = NascarXfinityActivity::class.java
}
