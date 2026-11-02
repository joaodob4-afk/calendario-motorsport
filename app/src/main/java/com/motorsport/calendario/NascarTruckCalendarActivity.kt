package com.motorsport.calendario

import androidx.appcompat.app.AppCompatActivity

class NascarTruckCalendarActivity : FormulaJuniorCalendarActivity() {
    override val tituloRes = R.string.nascar_truck
    override val calendario: CalendarioFormulaJunior = NascarTruckCalendarJson
    override val telaDeDetalhes: Class<out AppCompatActivity> = NascarTruckActivity::class.java
}
