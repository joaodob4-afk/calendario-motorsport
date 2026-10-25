package com.motorsport.calendario

import androidx.appcompat.app.AppCompatActivity

/** Lista de etapas da F1 Academy. A tela está em [FormulaJuniorCalendarActivity]. */
class F1AcademyCalendarActivity : FormulaJuniorCalendarActivity() {
    override val tituloRes = R.string.f1_academy
    override val calendario: CalendarioFormulaJunior = F1AcademyCalendarJson
    override val telaDeDetalhes: Class<out AppCompatActivity> = F1AcademyActivity::class.java
}
