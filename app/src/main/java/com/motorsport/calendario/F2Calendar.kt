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
            1,
            "Melbourne",
            "Austrália",
            "06/03/2026",
            "08/03/2026",
            listOf(
                F2Session("Treino Livre", "06/03/2026", "00:00"),
                F2Session("Classificação", "06/03/2026", "00:00"),
                F2Session("Corrida 1", "07/03/2026", "00:00"),
                F2Session("Corrida 2", "08/03/2026", "00:00")
            )
        ),

        F2Event(
            2,
            "Miami",
            "Estados Unidos",
            "01/05/2026",
            "03/05/2026",
            listOf(
                F2Session("Treino Livre", "01/05/2026", "00:00"),
                F2Session("Classificação", "01/05/2026", "00:00"),
                F2Session("Corrida 1", "02/05/2026", "00:00"),
                F2Session("Corrida 2", "03/05/2026", "00:00")
            )
        ),

        F2Event(
            3,
            "Montreal",
            "Canadá",
            "22/05/2026",
            "24/05/2026",
            listOf(
                F2Session("Treino Livre", "22/05/2026", "00:00"),
                F2Session("Classificação", "22/05/2026", "00:00"),
                F2Session("Corrida 1", "23/05/2026", "00:00"),
                F2Session("Corrida 2", "24/05/2026", "00:00")
            )
        ),

        F2Event(
            4,
            "Monte Carlo",
            "Mônaco",
            "04/06/2026",
            "07/06/2026",
            listOf(
                F2Session("Treino Livre", "04/06/2026", "00:00"),
                F2Session("Classificação", "05/06/2026", "00:00"),
                F2Session("Corrida 1", "06/06/2026", "00:00"),
                F2Session("Corrida 2", "07/06/2026", "00:00")
            )
        ),

        F2Event(
            5,
            "Barcelona",
            "Espanha",
            "12/06/2026",
            "14/06/2026",
            listOf(
                F2Session("Treino Livre", "12/06/2026", "00:00"),
                F2Session("Classificação", "12/06/2026", "00:00"),
                F2Session("Corrida 1", "13/06/2026", "00:00"),
                F2Session("Corrida 2", "14/06/2026", "00:00")
            )
        ),

        F2Event(
            6,
            "Spielberg",
            "Áustria",
            "26/06/2026",
            "28/06/2026",
            listOf(
                F2Session("Treino Livre", "26/06/2026", "00:00"),
                F2Session("Classificação", "26/06/2026", "00:00"),
                F2Session("Corrida 1", "27/06/2026", "00:00"),
                F2Session("Corrida 2", "28/06/2026", "00:00")
            )
        ),

        F2Event(
            7,
            "Silverstone",
            "Reino Unido",
            "03/07/2026",
            "05/07/2026",
            listOf(
                F2Session("Treino Livre", "03/07/2026", "00:00"),
                F2Session("Classificação", "03/07/2026", "00:00"),
                F2Session("Corrida 1", "04/07/2026", "00:00"),
                F2Session("Corrida 2", "05/07/2026", "00:00")
            )
        ),

        F2Event(
            8,
            "Spa-Francorchamps",
            "Bélgica",
            "17/07/2026",
            "19/07/2026",
            listOf(
                F2Session("Treino Livre", "17/07/2026", "00:00"),
                F2Session("Classificação", "17/07/2026", "00:00"),
                F2Session("Corrida 1", "18/07/2026", "00:00"),
                F2Session("Corrida 2", "19/07/2026", "00:00")
            )
        ),

        F2Event(
            9,
            "Budapeste",
            "Hungria",
            "24/07/2026",
            "26/07/2026",
            listOf(
                F2Session("Treino Livre", "24/07/2026", "00:00"),
                F2Session("Classificação", "24/07/2026", "00:00"),
                F2Session("Corrida 1", "25/07/2026", "00:00"),
                F2Session("Corrida 2", "26/07/2026", "00:00")
            )
        ),

        F2Event(
            10,
            "Monza",
            "Itália",
            "04/09/2026",
            "06/09/2026",
            listOf(
                F2Session("Treino Livre", "04/09/2026", "00:00"),
                F2Session("Classificação", "04/09/2026", "00:00"),
                F2Session("Corrida 1", "05/09/2026", "00:00"),
                F2Session("Corrida 2", "06/09/2026", "00:00")
            )
        ),

        F2Event(
            11,
            "Madrid",
            "Espanha",
            "11/09/2026",
            "13/09/2026",
            listOf(
                F2Session("Treino Livre", "11/09/2026", "00:00"),
                F2Session("Classificação", "11/09/2026", "00:00"),
                F2Session("Corrida 1", "12/09/2026", "00:00"),
                F2Session("Corrida 2", "13/09/2026", "00:00")
            )
        ),

        F2Event(
            12,
            "Baku",
            "Azerbaijão",
            "24/09/2026",
            "26/09/2026",
            listOf(
                F2Session("Treino Livre", "24/09/2026", "00:00"),
                F2Session("Classificação", "24/09/2026", "00:00"),
                F2Session("Corrida 1", "25/09/2026", "00:00"),
                F2Session("Corrida 2", "26/09/2026", "00:00")
            )
        ),

        F2Event(
            13,
            "Lusail",
            "Catar",
            "27/11/2026",
            "29/11/2026",
            listOf(
                F2Session("Treino Livre", "27/11/2026", "00:00"),
                F2Session("Classificação", "27/11/2026", "00:00"),
                F2Session("Corrida 1", "28/11/2026", "00:00"),
                F2Session("Corrida 2", "29/11/2026", "00:00")
            )
        ),

        F2Event(
            14,
            "Yas Marina",
            "Emirados Árabes Unidos",
            "04/12/2026",
            "06/12/2026",
            listOf(
                F2Session("Treino Livre", "04/12/2026", "00:00"),
                F2Session("Classificação", "04/12/2026", "00:00"),
                F2Session("Corrida 1", "05/12/2026", "00:00"),
                F2Session("Corrida 2", "06/12/2026", "00:00")
            )
        )
    )
}
