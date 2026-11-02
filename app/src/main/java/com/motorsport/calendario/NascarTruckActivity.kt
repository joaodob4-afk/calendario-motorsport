package com.motorsport.calendario

class NascarTruckActivity : FormulaJuniorActivity() {
    override val categoria = "NASCAR Craftsman Truck Series"
    override val tituloRes = R.string.nascar_truck
    override val calendario: CalendarioFormulaJunior = NascarTruckCalendarJson
}
