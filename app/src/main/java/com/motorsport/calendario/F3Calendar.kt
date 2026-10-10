package com.motorsport.calendario

data class F3Session(
    val nome: String,
    val data: String,
    val horario: String
)

data class F3Event(
    val etapa: Int,
    val circuito: String,
    val pais: String,
    val inicio: String,
    val fim: String,
    val sessoes: List<F3Session>
)

object F3Calendar {

    val eventos = listOf(

        F3Event(
            1,
            "Melbourne",
            "Austrália",
            "06/03/2026",
            "08/03/2026",
            listOf(
                F3Session("Treino Livre", "06/03/2026", "A confirmar"),
                F3Session("Classificação", "06/03/2026", "A confirmar"),
                F3Session("Corrida Sprint", "07/03/2026", "A confirmar"),
                F3Session("Corrida Feature", "08/03/2026", "A confirmar")
            )
        ),

        F3Event(
            2,
            "Monte Carlo",
            "Mônaco",
            "04/06/2026",
            "07/06/2026",
            listOf(
                F3Session("Treino Livre", "04/06/2026", "A confirmar"),
                F3Session("Classificação", "05/06/2026", "A confirmar"),
                F3Session("Corrida Sprint", "06/06/2026", "A confirmar"),
                F3Session("Corrida Feature", "07/06/2026", "A confirmar")
            )
        ),

        F3Event(
            3,
            "Barcelona",
            "Espanha",
            "12/06/2026",
            "14/06/2026",
            listOf(
                F3Session("Treino Livre", "12/06/2026", "A confirmar"),
                F3Session("Classificação", "12/06/2026", "A confirmar"),
                F3Session("Corrida Sprint", "13/06/2026", "A confirmar"),
                F3Session("Corrida Feature", "14/06/2026", "A confirmar")
            )
        ),

        F3Event(
            4,
            "Spielberg",
            "Áustria",
            "26/06/2026",
            "28/06/2026",
            listOf(
                F3Session("Treino Livre", "26/06/2026", "A confirmar"),
                F3Session("Classificação", "26/06/2026", "A confirmar"),
                F3Session("Corrida Sprint", "27/06/2026", "A confirmar"),
                F3Session("Corrida Feature", "28/06/2026", "A confirmar")
            )
        ),

        F3Event(
            5,
            "Silverstone",
            "Reino Unido",
            "03/07/2026",
            "05/07/2026",
            listOf(
                F3Session("Treino Livre", "03/07/2026", "A confirmar"),
                F3Session("Classificação", "03/07/2026", "A confirmar"),
                F3Session("Corrida Sprint", "04/07/2026", "A confirmar"),
                F3Session("Corrida Feature", "05/07/2026", "A confirmar")
            )
        ),

        F3Event(
            6,
            "Spa-Francorchamps",
            "Bélgica",
            "17/07/2026",
            "19/07/2026",
            listOf(
                F3Session("Treino Livre", "17/07/2026", "A confirmar"),
                F3Session("Classificação", "17/07/2026", "A confirmar"),
                F3Session("Corrida Sprint", "18/07/2026", "A confirmar"),
                F3Session("Corrida Feature", "19/07/2026", "A confirmar")
            )
        ),

        F3Event(
            7,
            "Budapeste",
            "Hungria",
            "24/07/2026",
            "26/07/2026",
            listOf(
                F3Session("Treino Livre", "24/07/2026", "A confirmar"),
                F3Session("Classificação", "24/07/2026", "A confirmar"),
                F3Session("Corrida Sprint", "25/07/2026", "A confirmar"),
                F3Session("Corrida Feature", "26/07/2026", "A confirmar")
            )
        ),

        F3Event(
            8,
            "Monza",
            "Itália",
            "04/09/2026",
            "06/09/2026",
            listOf(
                F3Session("Treino Livre", "04/09/2026", "A confirmar"),
                F3Session("Classificação", "04/09/2026", "A confirmar"),
                F3Session("Corrida Sprint", "05/09/2026", "A confirmar"),
                F3Session("Corrida Feature", "06/09/2026", "A confirmar")
            )
        ),

        F3Event(
            9,
            "Madrid",
            "Espanha",
            "11/09/2026",
            "13/09/2026",
            listOf(
                F3Session("Treino Livre", "11/09/2026", "A confirmar"),
                F3Session("Classificação", "11/09/2026", "A confirmar"),
                F3Session("Corrida Sprint", "12/09/2026", "A confirmar"),
                F3Session("Corrida Feature", "13/09/2026", "A confirmar")
            )
        )
    )
}