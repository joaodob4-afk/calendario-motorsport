package com.motorsport.calendario

import androidx.appcompat.app.AppCompatActivity

/** Lista de etapas da F3. A tela está em [FormulaJuniorCalendarActivity]. */
class F3CalendarActivity : FormulaJuniorCalendarActivity() {
    override val tituloRes = R.string.formula_3
    override val calendario: CalendarioFormulaJunior = F3CalendarJson
    override val telaDeDetalhes: Class<out AppCompatActivity> = F3Activity::class.java
}
