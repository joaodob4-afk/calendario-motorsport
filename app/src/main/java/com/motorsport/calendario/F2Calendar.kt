package com.motorsport.calendario

data class F2Event(
    val etapa: Int,
    val circuito: String,
    val pais: String,
    val inicio: String,
    val fim: String
)

object F2Calendar {

    val eventos = listOf(

        F2Event(1, "Melbourne", "Austrália", "06/03/2026", "08/03/2026"),
        F2Event(2, "Miami", "Estados Unidos", "01/05/2026", "03/05/2026"),
        F2Event(3, "Montreal", "Canadá", "22/05/2026", "24/05/2026"),
        F2Event(4, "Monte Carlo", "Mônaco", "04/06/2026", "07/06/2026"),
        F2Event(5, "Barcelona", "Espanha", "12/06/2026", "14/06/2026"),
        F2Event(6, "Spielberg", "Áustria", "26/06/2026", "28/06/2026"),
        F2Event(7, "Silverstone", "Reino Unido", "03/07/2026", "05/07/2026"),
        F2Event(8, "Spa-Francorchamps", "Bélgica", "17/07/2026", "19/07/2026"),
        F2Event(9, "Budapeste", "Hungria", "24/07/2026", "26/07/2026"),
        F2Event(10, "Monza", "Itália", "04/09/2026", "06/09/2026"),
        F2Event(11, "Madrid", "Espanha", "11/09/2026", "13/09/2026"),
        F2Event(12, "Baku", "Azerbaijão", "24/09/2026", "26/09/2026"),
        F2Event(13, "Lusail", "Catar", "27/11/2026", "29/11/2026"),
        F2Event(14, "Yas Marina", "Emirados Árabes Unidos", "04/12/2026", "06/12/2026")
    )
}
