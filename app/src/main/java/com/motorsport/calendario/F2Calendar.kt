
package com.motorsport.calendario

data class F2Session(
    val nome: String,
    val data: String,
    val horario: String
)

data class F2Event(
    val etapa: Int,
    val circuito: String,
    val pais: String,
    val inicio: String,
    val fim: String,
    val sessoes: List<F2Session>
)

object F2Calendar {

    val eventos = listOf(

        F2Event(
            1, "Melbourne", "Austrália",
            "06/03/2026", "08/03/2026",
            listOf(
                F2Session("Treino Livre", "05/03/2026", "20:00"),
                F2Session("Classificação", "06/03/2026", "00:55"),
                F2Session("Corrida Sprint", "07/03/2026", "00:30"),
                F2Session("Corrida Feature", "07/03/2026", "21:25")
            )
        ),

        F2Event(
            2, "Miami", "Estados Unidos",
            "01/05/2026", "03/05/2026",
            listOf(
                F2Session("Treino Livre", "01/05/2026", "10:30"),
                F2Session("Classificação", "01/05/2026", "15:30"),
                F2Session("Corrida Sprint", "02/05/2026", "11:00"),
                F2Session("Corrida Feature", "03/05/2026", "13:30")
            )
        ),

        F2Event(
            3, "Montreal", "Canadá",
            "22/05/2026", "24/05/2026",
            listOf(
                F2Session("Treino Livre", "22/05/2026", "11:05"),
                F2Session("Classificação", "22/05/2026", "15:00"),
                F2Session("Corrida Sprint", "23/05/2026", "15:10"),
                F2Session("Corrida Feature", "24/05/2026", "13:05")
            )
        ),

        F2Event(
            4, "Monte Carlo", "Mônaco",
            "04/06/2026", "07/06/2026",
            listOf(
                F2Session("Treino Livre", "04/06/2026", "10:00"),
                F2Session("Classificação Grupo A", "05/06/2026", "10:10"),
                F2Session("Classificação Grupo B", "05/06/2026", "10:34"),
                F2Session("Corrida Sprint", "06/06/2026", "09:15"),
                F2Session("Corrida Feature", "07/06/2026", "04:25")
            )
        ),

        F2Event(
            5, "Barcelona", "Espanha",
            "12/06/2026", "14/06/2026",
            listOf(
                F2Session("Treino Livre", "12/06/2026", "06:05"),
                F2Session("Classificação", "12/06/2026", "10:55"),
                F2Session("Corrida Sprint", "13/06/2026", "09:15"),
                F2Session("Corrida Feature", "14/06/2026", "06:25")
            )
        ),

        F2Event(
            6, "Spielberg", "Áustria",
            "26/06/2026", "28/06/2026",
            listOf(
                F2Session("Treino Livre", "26/06/2026", "06:05"),
                F2Session("Classificação", "26/06/2026", "10:55"),
                F2Session("Corrida Sprint", "27/06/2026", "09:15"),
                F2Session("Corrida Feature", "28/06/2026", "05:10")
            )
        ),

        F2Event(
            7, "Silverstone", "Reino Unido",
            "03/07/2026", "05/07/2026",
            listOf(
                F2Session("Treino Livre", "03/07/2026", "06:00"),
                F2Session("Classificação", "03/07/2026", "09:55"),
                F2Session("Corrida Sprint", "04/07/2026", "09:45"),
                F2Session("Corrida Feature", "05/07/2026", "07:15")
            )
        ),

        F2Event(
            8, "Spa-Francorchamps", "Bélgica",
            "17/07/2026", "19/07/2026",
            listOf(
                F2Session("Treino Livre", "17/07/2026", "06:05"),
                F2Session("Classificação", "17/07/2026", "10:55"),
                F2Session("Corrida Sprint", "18/07/2026", "09:15"),
                F2Session("Corrida Feature", "19/07/2026", "05:00")
            )
        ),

        F2Event(
            9, "Budapeste", "Hungria",
            "24/07/2026", "26/07/2026",
            listOf(
                F2Session("Treino Livre", "24/07/2026", "06:05"),
                F2Session("Classificação", "24/07/2026", "10:55"),
                F2Session("Corrida Sprint", "25/07/2026", "09:15"),
                F2Session("Corrida Feature", "26/07/2026", "06:25")
            )
        ),

        F2Event(
            10, "Monza", "Itália",
            "04/09/2026", "06/09/2026",
            listOf(
                F2Session("Treino Livre", "04/09/2026", "05:00"),
                F2Session("Classificação", "04/09/2026", "09:55"),
                F2Session("Corrida Sprint", "05/09/2026", "09:15"),
                F2Session("Corrida Feature", "06/09/2026", "04:45")
            )
        ),

        F2Event(
            11, "Madrid", "Espanha",
            "11/09/2026", "13/09/2026",
            listOf(
                F2Session("Treino Livre", "11/09/2026", "06:05"),
                F2Session("Classificação", "11/09/2026", "10:00"),
                F2Session("Corrida Sprint", "12/09/2026", "09:15"),
                F2Session("Corrida Feature", "13/09/2026", "06:25")
            )
        ),

        F2Event(
            12, "Baku City Circuit", "Azerbaijão",
            "24/09/2026", "26/09/2026",
            listOf(
                F2Session("Treino Livre", "24/09/2026", "A confirmar"),
                F2Session("Classificação", "24/09/2026", "A confirmar"),
                F2Session("Corrida Sprint", "25/09/2026", "A confirmar"),
                F2Session("Corrida Feature", "26/09/2026", "A confirmar")
            )
        ),

        F2Event(
            13, "Lusail", "Catar",
            "27/11/2026", "29/11/2026",
            listOf(
                F2Session("Treino Livre", "27/11/2026", "A confirmar"),
                F2Session("Classificação", "27/11/2026", "A confirmar"),
                F2Session("Corrida Sprint", "28/11/2026", "A confirmar"),
                F2Session("Corrida Feature", "29/11/2026", "A confirmar")
            )
        ),

        F2Event(
            14, "Yas Marina", "Emirados Árabes Unidos",
            "04/12/2026", "06/12/2026",
            listOf(
                F2Session("Treino Livre", "04/12/2026", "A confirmar"),
                F2Session("Classificação", "04/12/2026", "A confirmar"),
                F2Session("Corrida Sprint", "05/12/2026", "A confirmar"),
                F2Session("Corrida Feature", "06/12/2026", "A confirmar")
            )
        )
    )
}
