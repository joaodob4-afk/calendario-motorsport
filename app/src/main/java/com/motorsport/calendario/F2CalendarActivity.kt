package com.motorsport.calendario

import androidx.appcompat.app.AppCompatActivity

/** Lista de etapas da F2. A tela está em [FormulaJuniorCalendarActivity]. */
class F2CalendarActivity : FormulaJuniorCalendarActivity() {
    override val tituloRes = R.string.formula_2
    override val calendario: CalendarioFormulaJunior = F2CalendarJson
    override val telaDeDetalhes: Class<out AppCompatActivity> = F2Activity::class.java
}
